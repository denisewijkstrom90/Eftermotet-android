package se.denise.eftermotet;

import android.Manifest;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebStorage;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;
import java.io.IOException;
import org.json.JSONObject;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.Text;

public class MainActivity extends ComponentActivity {
    private static final int FILE_REQUEST = 101;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private Uri selectedImageUri;
    private String backupToSave;
    private SubscriptionManager subscription;
    private String subscriptionState;
    private boolean pageReady;

    private boolean reviewerActive() {
        return BuildConfig.REVIEW_ACCESS_SHA256.equals(getPreferences(MODE_PRIVATE).getString("reviewAccess", ""));
    }

    private void publishAccess(boolean active, boolean available, boolean trial, String price, String message) {
        boolean review = reviewerActive();
        subscriptionState = "window.updateSubscription(" + (active || review) + "," + (available && !review) + "," + trial + "," + JSONObject.quote(price) + "," + JSONObject.quote(message) + ");window.updateReviewerAccess(" + review + ")";
        if (pageReady) web.evaluateJavascript(subscriptionState, null);
    }

    private void showReminderSettings() {
        if (Build.VERSION.SDK_INT < 31 || ReminderScheduler.preciseAllowed(this)) {
            android.widget.Toast.makeText(this, "Exakta påminnelser är tillåtna. Kontrollera också att aviseringar är på.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Påminnelser vid vald tid")
            .setMessage("Tillåt EfterMötet under Alarm och påminnelser för att få mötespåminnelser vid vald tid. Utan tillstånd kan Android fördröja dem upp till en timme, ibland längre i batterisparläge.")
            .setNegativeButton("Inte nu", null)
            .setPositiveButton("Öppna inställningar", (dialog, which) -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
                } catch (android.content.ActivityNotFoundException error) {
                    android.widget.Toast.makeText(this, "Öppna Alarm och påminnelser i mobilens appinställningar.", android.widget.Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    private void showReviewerAccess() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Reviewer access / Granskaråtkomst")
            .setMessage("Enter the review code supplied in Play Console. No purchase is required.")
            .setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton("Unlock", (dialog, which) -> {
                if (ReviewAccess.accepts(input.getText().toString(), BuildConfig.REVIEW_ACCESS_SHA256)) {
                    getPreferences(MODE_PRIVATE).edit().putString("reviewAccess", BuildConfig.REVIEW_ACCESS_SHA256).apply();
                    publishAccess(false, false, false, "", "Reviewer access enabled. No subscription or payment has been started.");
                } else android.widget.Toast.makeText(this, "Invalid review code", android.widget.Toast.LENGTH_LONG).show();
            }).show();
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        android.app.NotificationManager notifications = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new android.app.NotificationChannel(ReminderReceiver.CHANNEL, "Mötespåminnelser", android.app.NotificationManager.IMPORTANCE_DEFAULT));
        web = new WebView(this);
        setContentView(web);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (web.canGoBack()) web.goBack();
                else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        web.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.getSettings().setAllowContentAccess(true);
        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = url.equals("https://appassets.androidplatform.net/assets/index.html");
                if (pageReady && subscriptionState != null) web.evaluateJavascript(subscriptionState, null);
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if ("appassets.androidplatform.net".equals(request.getUrl().getHost()))
                    return assets.shouldInterceptRequest(request.getUrl());
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                String[] accepts = params.getAcceptTypes();
                boolean image = accepts.length == 0 || accepts[0] == null || !accepts[0].contains("json");
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType(image ? "image/*" : "application/json").addCategory(Intent.CATEGORY_OPENABLE);
                if (!image) { startActivityForResult(pick, FILE_REQUEST); return true; }
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File photo = File.createTempFile("eftermotet-", ".jpg", getCacheDir());
                    cameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photo);
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                    camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Intent chooser = Intent.createChooser(pick, "Välj dokumentbild");
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
                    startActivityForResult(chooser, FILE_REQUEST);
                } catch (IOException e) {
                    startActivityForResult(pick, FILE_REQUEST);
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public boolean isDemoBuild() {
                return BuildConfig.DEMO;
            }
            @JavascriptInterface public void recognizeDocument(String croppedImage) {
                Uri uri = selectedImageUri;
                if (uri == null && (croppedImage == null || croppedImage.isEmpty())) { sendRecognition("", "Välj en bild och försök igen."); return; }
                runOnUiThread(() -> {
                    try {
                        InputImage image;
                        if (croppedImage != null && croppedImage.startsWith("data:image/jpeg;base64,")) {
                            if (croppedImage.length() > 16_000_000) throw new IOException("Bilden är för stor");
                            byte[] bytes = Base64.decode(croppedImage.substring(croppedImage.indexOf(',') + 1), Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (bitmap == null) throw new IOException("Ogiltig bild");
                            image = InputImage.fromBitmap(bitmap, 0);
                        } else image = InputImage.fromFilePath(MainActivity.this, uri);
                        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                        recognizer.process(image)
                            .addOnSuccessListener(result -> {
                                sendRecognition(cleanRecognizedText(result), "");
                                recognizer.close();
                            })
                            .addOnFailureListener(error -> {
                                sendRecognition("", "Texten kunde inte läsas. Ta om bilden i bra ljus, nära pappret.");
                                recognizer.close();
                            });
                    } catch (Exception error) {
                        sendRecognition("", "Bilden kunde inte öppnas. Välj en annan bild.");
                    }
                });
            }
            @JavascriptInterface public void openReminderSettings() { runOnUiThread(() -> showReminderSettings()); }
            @JavascriptInterface public void syncReminders(String json) {
                ReminderScheduler.sync(getApplicationContext(), json);
                if (Build.VERSION.SDK_INT >= 33 && !"[]".equals(json) && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 103));
            }
            @JavascriptInterface public void saveBackup(String json) {
                if (json == null || json.length() > 50_000_000) return;
                backupToSave = json;
                runOnUiThread(() -> { Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    save.addCategory(Intent.CATEGORY_OPENABLE); save.setType("application/json");
                    save.putExtra(Intent.EXTRA_TITLE, "EfterMotet-sakerhetskopia.json");
                    startActivityForResult(save, 102); });
            }
            @JavascriptInterface public void openReviewerAccess() { runOnUiThread(() -> showReviewerAccess()); }
            @JavascriptInterface public void endReviewerAccess() {
                runOnUiThread(() -> {
                    getPreferences(MODE_PRIVATE).edit().remove("reviewAccess").apply();
                    publishAccess(false, false, false, "", "Kontrollerar prenumerationen…");
                    if (subscription != null) subscription.refresh();
                });
            }
            @JavascriptInterface public void startSubscription() {
                runOnUiThread(() -> { if (subscription != null && !reviewerActive()) subscription.subscribe(); });
            }
            @JavascriptInterface public void refreshSubscription() {
                runOnUiThread(() -> { if (subscription != null) subscription.refresh(); });
            }
            @JavascriptInterface public void manageSubscription() {
                runOnUiThread(() -> { if (subscription != null) subscription.manage(); });
            }
        }, "AndroidApp");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        if (!BuildConfig.DEMO) {
            subscription = new SubscriptionManager(this, (active, available, trial, price, message) ->
                runOnUiThread(() -> {
                    publishAccess(active, available, trial, price, message);
                }));
            subscription.start();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        ReminderScheduler.restore(getApplicationContext());
        if (subscription != null) subscription.refresh();
    }

    @Override protected void onPause() {
        if (subscription != null) subscription.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (subscription != null) subscription.close();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 102) {
            if (result == RESULT_OK && data != null && data.getData() != null && backupToSave != null) {
                try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
                    if (stream == null) throw new IOException("Ingen fil öppnades");
                    stream.write(backupToSave.getBytes(StandardCharsets.UTF_8));
                    android.widget.Toast.makeText(this, "Säkerhetskopian sparades", android.widget.Toast.LENGTH_LONG).show();
                } catch (IOException error) { android.widget.Toast.makeText(this, "Kopian kunde inte sparas. Försök igen.", android.widget.Toast.LENGTH_LONG).show(); }
            }
            backupToSave = null; return;
        }
        if (request != FILE_REQUEST || fileCallback == null) return;
        selectedImageUri = null;
        Uri[] selected = null;
        if (result == RESULT_OK) {
            Uri uri = data == null ? cameraUri : data.getData();
            if (uri != null) { selected = new Uri[]{uri}; selectedImageUri = uri; }
        }
        fileCallback.onReceiveValue(selected);
        fileCallback = null;
        cameraUri = null;
    }

    private void sendRecognition(String recognized, String error) {
        runOnUiThread(() -> web.evaluateJavascript("window.receiveAndroidOcr(" +
            JSONObject.quote(recognized) + "," + JSONObject.quote(error) + ")", null));
    }

    private static String cleanRecognizedText(Text result) {
        StringBuilder output = new StringBuilder();
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String cleaned = line.getText().replaceAll("[^\\p{L}\\p{N}\\s.,!?()]", " ")
                    .replaceAll("[ \\t]+", " ").trim();
                // OCR confidence and glyph size are unreliable for screenshots of zoomed documents.
                // Keep readable candidates and let the person correct them against the image.
                if (!cleaned.matches(".*[\\p{L}\\p{N}].*")) continue;
                if (output.length() > 0) output.append('\n');
                output.append(cleaned);
            }
        }
        return output.toString();
    }

}
