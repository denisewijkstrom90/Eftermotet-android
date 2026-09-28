package se.denise.eftermotet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.Purchase;
import java.util.Collections;
import java.util.List;

/** Client-side Play Billing integration. Verify purchase tokens server-side before a production release. */
final class SubscriptionManager {
    static final String PRODUCT_ID = "eftermotet_manad";
    interface Listener {
        void onState(boolean active, boolean available, boolean trial, String price, String message);
    }
    private final Activity activity;
    private final Listener listener;
    private final BillingClient client;
    private ProductDetails product;
    private ProductDetails.SubscriptionOfferDetails selectedOffer;
    private String price = "";
    private boolean trial;
    private boolean connecting;

    SubscriptionManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        client = BillingClient.newBuilder(activity)
            .setListener((result, purchases) -> {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
                    handlePurchases(purchases);
                else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
                    refresh();
            })
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build();
    }

    void start() {
        if (client.isReady()) { refresh(); return; }
        if (connecting) return;
        connecting = true;
        listener.onState(false, false, false, "", "Ansluter till Google Play…");
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                connecting = false;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) refresh();
                else listener.onState(false, false, false, "", "Google Play Billing kunde inte ansluta. Försök igen.");
            }
            @Override public void onBillingServiceDisconnected() {
                connecting = false;
                listener.onState(false, false, false, "", "Anslutningen till Google Play bröts. Försök igen.");
            }
        });
    }

    void refresh() {
        if (!client.isReady()) { start(); return; }
        QueryPurchasesParams query = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build();
        client.queryPurchasesAsync(query, (result, purchases) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onState(false, false, false, "", "Prenumerationen kunde inte kontrolleras. Försök igen.");
                return;
            }
            handlePurchases(purchases);
        });
        QueryProductDetailsParams details = QueryProductDetailsParams.newBuilder()
            .setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build())).build();
        client.queryProductDetailsAsync(details, (result, products) -> {
            product = null;
            selectedOffer = null;
            trial = false;
            price = "";
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK &&
                    !products.getProductDetailsList().isEmpty()) {
                product = products.getProductDetailsList().get(0);
                List<ProductDetails.SubscriptionOfferDetails> offers = product.getSubscriptionOfferDetails();
                if (offers != null) {
                    for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                        boolean freeTrial = false;
                        String recurringPrice = "";
                        for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                            if (phase.getPriceAmountMicros() == 0 &&
                                    ("P2W".equals(phase.getBillingPeriod()) ||
                                     "P14D".equals(phase.getBillingPeriod()))) freeTrial = true;
                            if (phase.getPriceAmountMicros() > 0 &&
                                    "P1M".equals(phase.getBillingPeriod())) recurringPrice = phase.getFormattedPrice();
                        }
                        if (recurringPrice.isEmpty()) continue;
                        if (selectedOffer == null || freeTrial && !trial) {
                            selectedOffer = offer;
                            trial = freeTrial;
                            price = recurringPrice;
                        }
                    }
                }
            }
            listener.onState(false, selectedOffer != null, trial, price,
                selectedOffer == null ? "Prenumerationen är inte tillgänglig i Google Play ännu." : "");
            // Recheck entitlement after fetching offers so an active purchase is never masked.
            checkEntitlement();
        });
    }

    private void checkEntitlement() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build(),
            (result, purchases) -> {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) handlePurchases(purchases);
            });
    }

    private void handlePurchases(List<Purchase> purchases) {
        boolean active = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(PRODUCT_ID) ||
                purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            active = true;
            if (!purchase.isAcknowledged()) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken()).build(), result -> { });
            }
        }
        listener.onState(active, selectedOffer != null, trial, price,
            active ? "" : "Du behöver godkänna prenumerationen i Google Play för att börja provperioden.");
    }

    void subscribe() {
        if (product == null || selectedOffer == null || !client.isReady()) {
            refresh();
            return;
        }
        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product).setOfferToken(selectedOffer.getOfferToken()).build();
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(Collections.singletonList(params)).build());
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) refresh();
    }

    void manage() {
        activity.startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account/subscriptions")));
    }

    void close() { client.endConnection(); }
}
