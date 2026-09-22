package com.guarenas.proteccionanimal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * App de Proteccion Animal - Alcaldia de Guarenas.
 *
 * - Camara nativa + galeria (FileProvider)
 * - GPS / geolocalizacion del WebView
 * - Sesion PHP por cookies
 */
public class MainActivity extends BridgeActivity {

    private static final int PETICION_ARCHIVO  = 1001;
    private static final int PETICION_PERMISOS = 1002;

    private ValueCallback<Uri[]> callbackArchivos;
    private Uri uriFotoCamara;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();
        WebSettings ajustes = webView.getSettings();

        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setDatabaseEnabled(true);
        ajustes.setGeolocationEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ajustes.setGeolocationDatabasePath(getFilesDir().getPath());
        }
        ajustes.setAllowFileAccess(true);
        ajustes.setAllowContentAccess(true);
        ajustes.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        ajustes.setLoadWithOverviewMode(true);
        ajustes.setUseWideViewPort(true);
        ajustes.setSupportZoom(false);
        ajustes.setBuiltInZoomControls(false);
        ajustes.setSupportMultipleWindows(false);
        ajustes.setJavaScriptCanOpenWindowsAutomatically(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ajustes.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (esEnlaceExterno(url)) {
                    abrirFuera(url);
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (esEnlaceExterno(url)) {
                    abrirFuera(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (callbackArchivos != null) {
                    callbackArchivos.onReceiveValue(null);
                }
                callbackArchivos = filePathCallback;

                boolean soloCamara = fileChooserParams != null
                        && fileChooserParams.isCaptureEnabled();

                try {
                    if (soloCamara) {
                        startActivityForResult(crearIntentCamara(), PETICION_ARCHIVO);
                    } else {
                        startActivityForResult(crearChooser(fileChooserParams), PETICION_ARCHIVO);
                    }
                    return true;
                } catch (Exception e) {
                    callbackArchivos = null;
                    uriFotoCamara = null;
                    return false;
                }
            }
        });

        solicitarPermisos();
    }

    private Intent crearIntentCamara() {
        Intent camara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File foto = new File(getCacheDir(), "captura_" + System.currentTimeMillis() + ".jpg");
        uriFotoCamara = FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", foto);
        camara.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoCamara);
        camara.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        List<ResolveInfo> apps = getPackageManager()
                .queryIntentActivities(camara, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : apps) {
            grantUriPermission(
                    info.activityInfo.packageName,
                    uriFotoCamara,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return camara;
    }

    private Intent crearChooser(WebChromeClient.FileChooserParams params) {
        Intent galeria = new Intent(Intent.ACTION_GET_CONTENT);
        galeria.addCategory(Intent.CATEGORY_OPENABLE);
        galeria.setType("image/*");
        if (params != null && params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            galeria.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        Intent chooser = Intent.createChooser(galeria, "Seleccionar foto");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{ crearIntentCamara() });
        return chooser;
    }

    private void abrirFuera(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) { }
    }

    private boolean esEnlaceExterno(String url) {
        if (url == null) return false;
        if (url.startsWith("mailto:") || url.startsWith("tel:")
                || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
            return true;
        }
        return url.startsWith("http") && !url.contains("alcaldiadeplaza.com");
    }

    private void solicitarPermisos() {
        List<String> faltantes = new ArrayList<String>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            faltantes.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            faltantes.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            faltantes.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                faltantes.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                faltantes.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!faltantes.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    faltantes.toArray(new String[faltantes.size()]), PETICION_PERMISOS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PETICION_ARCHIVO) {
            if (callbackArchivos == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] resultado = null;
            if (resultCode == RESULT_OK) {
                if (data != null && data.getClipData() != null) {
                    int cantidad = data.getClipData().getItemCount();
                    resultado = new Uri[cantidad];
                    for (int i = 0; i < cantidad; i++) {
                        resultado[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data != null && data.getData() != null) {
                    resultado = new Uri[]{ data.getData() };
                } else if (uriFotoCamara != null) {
                    resultado = new Uri[]{ uriFotoCamara };
                }
            }

            callbackArchivos.onReceiveValue(resultado);
            callbackArchivos = null;
            uriFotoCamara = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        WebView webView = getBridge().getWebView();
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
