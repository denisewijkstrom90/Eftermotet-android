package se.denise.eftermotet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class MainActivity extends Activity {
    private static final int FILE_REQUEST = 101;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private Uri selectedImageUri;
    private String backupToSave;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(true);
        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assets.shouldInterceptRequest(request.getUrl());
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
            @JavascriptInterface public void recognizeDocument(String croppedImage) {
                Uri uri = selectedImageUri;
                if (uri == null && (croppedImage == null || croppedImage.isEmpty())) { sendRecognition("", "Välj en bild och försök igen."); return; }
                runOnUiThread(() -> {
                    try {
                        InputImage image;
                        if (croppedImage != null && croppedImage.startsWith("data:image/jpeg;base64,")) {
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
        }, "AndroidApp");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 102) {
            if (result == RESULT_OK && data != null && data.getData() != null && backupToSave != null) {
                try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
                    if (stream != null) stream.write(backupToSave.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) { }
            }
            backupToSave = null; return;
        }
        if (request != FILE_REQUEST || fileCallback == null) return;
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
        int reliableWords = 0;
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String cleaned = line.getText().replaceAll("[^\\p{L}\\p{N}\\s.,!?()]", " ")
                    .replaceAll("[ \\t]+", " ").trim();
                android.graphics.Rect box = line.getBoundingBox();
                int letters = cleaned.replaceAll("[^\\p{L}]", "").length();
                int words = cleaned.isEmpty() ? 0 : cleaned.split("\\s+").length;
                // Short headings and individual values are valid document text.
                // Reject only obvious fragments, unreadably small glyphs and low-confidence lines.
                if (box == null || box.height() < 10 || letters < 3 ||
                    line.getConfidence() < 0.40f ||
                    (words == 1 && letters < 5 && !cleaned.matches(".*\\d{2,}.*"))) continue;
                if (output.length() > 0) output.append('\n');
                output.append(cleaned);
                reliableWords += words;
            }
        }
        return reliableWords >= 3 ? output.toString() : "";
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
