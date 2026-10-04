package se.denise.eftermotet;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import com.android.billingclient.api.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/** Queries Play on foreground entry. No timer or editable local flag grants access. */
final class SubscriptionManager {
    static final String PRODUCT_ID = "eftermotet_manad";
    static final String BASE_PLAN_ID = "manad";
    static final String TRIAL_ID = "gratis-14-dagar";
    interface Listener {
        void onState(boolean active, boolean available, boolean trial, String price, String message);
    }
    private final Activity activity;
    private final Listener listener;
    private final BillingClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean connecting, closed, active, trial;
    private String price = "";
    private ProductDetails product;
    private ProductDetails.SubscriptionOfferDetails offer;
    private int generation;
    private final Runnable retry = this::refresh;

    SubscriptionManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        client = BillingClient.newBuilder(activity)
            .setListener((result, purchases) -> handler.post(() -> {
                if (closed) return;
                // Query the full inventory; a purchase-update list may be partial.
                if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    emit("Köpet avbröts. Ingen ny prenumeration startades.");
                } else refresh();
            }))
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection().build();
    }

    private void emit(String message) {
        if (!closed) listener.onState(active, offer != null && !active && !BuildConfig.PLAY_PUBLIC_KEY.isEmpty(), trial, price, message);
    }

    void start() { refresh(); }

    void refresh() {
        if (closed) return;
        handler.removeCallbacks(retry);
        if (!client.isReady()) {
            if (connecting) return;
            connecting = true;
            emit("Ansluter till Google Play…");
            client.startConnection(new BillingClientStateListener() {
                @Override public void onBillingSetupFinished(BillingResult result) {
                    handler.post(() -> {
                        connecting = false;
                        if (closed) return;
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) refresh();
                        else unavailable("Google Play kunde inte ansluta. Dina uppgifter kan läsas och exporteras. Försök igen.");
                    });
                }
                @Override public void onBillingServiceDisconnected() {
                    handler.post(() -> {
                        connecting = false;
                        if (!closed) unavailable("Anslutningen till Google Play bröts. Kontrollera igen.");
                    });
                }
            });
            return;
        }
        final int request = ++generation;
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build(), (result, purchases) ->
                handler.post(() -> {
                    if (closed || request != generation) return;
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        unavailable("Prenumerationen kunde inte kontrolleras. Kontrollera anslutningen och försök igen.");
                        return;
                    }
                    verifyPurchases(purchases, request);
                }));
        queryOffer(request, false);
        // Revalidate during a long foreground session, including expiry/refunds.
        handler.postDelayed(retry, 60_000);
    }

    private void unavailable(String message) {
        active = false;
        offer = null;
        product = null;
        ++generation;
        emit(message);
    }

    private void queryOffer(int request, boolean buy) {
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
            .setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()))
            .build(), (result, products) -> handler.post(() -> {
                if (closed || request != generation) return;
                product = null;
                offer = null;
                price = "";
                trial = false;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails details : products.getProductDetailsList()) {
                        if (!PRODUCT_ID.equals(details.getProductId())) continue;
                        List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
                        if (offers == null) continue;
                        for (ProductDetails.SubscriptionOfferDetails candidate : offers) {
                            if (!BASE_PLAN_ID.equals(candidate.getBasePlanId())) continue;
                            List<ProductDetails.PricingPhase> phases = candidate.getPricingPhases().getPricingPhaseList();
                            boolean freeTrial = TRIAL_ID.equals(candidate.getOfferId()) && phases.size() == 2
                                && phases.get(0).getPriceAmountMicros() == 0
                                && phases.get(0).getBillingCycleCount() == 1
                                && ("P14D".equals(phases.get(0).getBillingPeriod()) || "P2W".equals(phases.get(0).getBillingPeriod()));
                            boolean base = candidate.getOfferId() == null && phases.size() == 1;
                            if (!(base || freeTrial)) continue;
                            ProductDetails.PricingPhase recurring = phases.get(phases.size() - 1);
                            if (!"P1M".equals(recurring.getBillingPeriod()) || recurring.getPriceAmountMicros() <= 0
                                || recurring.getRecurrenceMode() != ProductDetails.RecurrenceMode.INFINITE_RECURRING) continue;
                            // Sweden must match the agreed 29 kr/month. Other regions use Play's localized price.
                            if ("SEK".equals(recurring.getPriceCurrencyCode()) && recurring.getPriceAmountMicros() != 29_000_000L) continue;
                            if (offer == null || freeTrial && !trial) {
                                product = details;
                                offer = candidate;
                                trial = freeTrial;
                                price = recurring.getFormattedPrice();
                            }
                        }
                    }
                }
                emit(offer == null && !active ? "Prenumerationen är inte tillgänglig. Försök igen senare." : "");
                if (buy && offer != null && !active) launch();
            }));
    }

    private void verifyPurchases(List<Purchase> purchases, int request) {
        active = false;
        boolean pending = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(PRODUCT_ID) || purchase.isSuspended()) continue;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) { pending = true; continue; }
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED || !validSignature(purchase)) continue;
            if (purchase.isAcknowledged()) { active = true; break; }
            // Grant only after acknowledgement succeeds; failure is visible and retried.
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken()).build(), result -> handler.post(() -> {
                    if (closed || request != generation) return;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        active = true;
                        emit("");
                    } else {
                        emit("Köpet kunde inte bekräftas. Kontrollera igen innan du försöker köpa på nytt.");
                        handler.removeCallbacks(retry);
                        handler.postDelayed(retry, 5_000);
                    }
                }));
            emit("Bekräftar köpet med Google Play…");
            return;
        }
        emit(active ? "" : pending ? "Betalningen väntar på godkännande. Åtkomsten öppnas när Google Play har bekräftat köpet."
            : "Ingen aktiv prenumeration hittades. Dina sparade uppgifter kan läsas och exporteras.");
    }

    private boolean validSignature(Purchase purchase) {
        try {
            if (!activity.getPackageName().equals(new JSONObject(purchase.getOriginalJson()).optString("packageName"))) return false;
            if (BuildConfig.PLAY_PUBLIC_KEY.isEmpty()) return false;
            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                Base64.decode(BuildConfig.PLAY_PUBLIC_KEY, Base64.DEFAULT))));
            verifier.update(purchase.getOriginalJson().getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.decode(purchase.getSignature(), Base64.DEFAULT));
        } catch (Exception error) { return false; }
    }

    void subscribe() {
        if (closed || active) return;
        if (BuildConfig.PLAY_PUBLIC_KEY.isEmpty()) {
            emit("Betalning är inte konfigurerad i denna testversion.");
            return;
        }
        if (!client.isReady()) { refresh(); return; }
        // Fetch fresh ProductDetails instead of using an indefinitely cached offer token.
        queryOffer(generation, true);
    }

    private void launch() {
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(Collections.singletonList(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product).setOfferToken(offer.getOfferToken()).build())).build());
        if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh();
        else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) emit("Betalrutan kunde inte öppnas. Kontrollera igen.");
    }

    void manage() {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku=" + PRODUCT_ID + "&package=" + activity.getPackageName())));
        } catch (ActivityNotFoundException error) { emit("Öppna Google Play och välj Betalningar och prenumerationer."); }
    }

    void pause() { handler.removeCallbacks(retry); }
    void close() { closed = true; ++generation; handler.removeCallbacksAndMessages(null); client.endConnection(); }
}
