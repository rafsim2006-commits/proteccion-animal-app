package com.guarenas.proteccionanimal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

/**
 * App de Proteccion Animal - Alcaldia de Guarenas.
 *
 * Resuelve los tres problemas tipicos de envolver una web PHP en una WebView:
 *   1. Seleccion de archivos (camara y galeria) -> onShowFileChooser
 *   2. Permisos en tiempo de ejecucion          -> CAMERA / imagenes
 *   3. Sesion PHP por cookies                   -> CookieManager + third-party
 */
public class MainActivity extends BridgeActivity {

    private static final int PETICION_ARCHIVO   = 1001;
    private static final int PETICION_PERMISOS  = 1002;

    private ValueCallback<Uri[]> callbackArchivos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();
        WebSettings ajustes = webView.getSettings();

        // --- JavaScript y almacenamiento local ---
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setDatabaseEnabled(true);

        // --- Cookies: imprescindible para mantener la sesion PHP ---
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // --- Ajuste de contenido ---
        ajustes.setLoadWithOverviewMode(true);
        ajustes.setUseWideViewPort(true);
        ajustes.setSupportZoom(false);
        ajustes.setBuiltInZoomControls(false);

        // --- Ventanas ---
        ajustes.setSupportMultipleWindows(false);
        ajustes.setJavaScriptCanOpenWindowsAutomatically(true);

        // --- Contenido mixto ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ajustes.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // --- Enlaces externos se abren en el navegador del telefono ---
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

        // --- Camara y galeria ---
        webView.setWebChromeClient(new WebChromeClient() {

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
                // Cancelar una seleccion previa pendiente para evitar bloqueos
                if (callbackArchivos != null) {
                    callbackArchivos.onReceiveValue(null);
                }
                callbackArchivos = filePathCallback;

                try {
                    Intent intent = fileChooserParams.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, PETICION_ARCHIVO);
                    return true;
                } catch (Exception e) {
                    callbackArchivos = null;
                    return false;
                }
            }
        });

        // Pedir permisos al iniciar
        solicitarPermisos();
    }

    /** Abre una URL en el navegador externo del telefono. */
    private void abrirFuera(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) { }
    }

    /** Los enlaces que salen del sitio institucional se abren fuera de la app. */
    private boolean esEnlaceExterno(String url) {
        if (url == null) return false;
        if (url.startsWith("mailto:") || url.startsWith("tel:")
                || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
            return true;
        }
        return url.startsWith("http") && !url.contains("alcaldiadeplaza.com");
    }

    /** Pide los permisos de camara y almacenamiento si aun no estan concedidos. */
    private void solicitarPermisos() {
        java.util.List<String> faltantes = new java.util.ArrayList<String>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            faltantes.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES")
                    != PackageManager.PERMISSION_GRANTED) {
                faltantes.add("android.permission.READ_MEDIA_IMAGES");
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
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    // Varias imagenes seleccionadas
                    int cantidad = data.getClipData().getItemCount();
                    resultado = new Uri[cantidad];
                    for (int i = 0; i < cantidad; i++) {
                        resultado[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    // Una sola imagen
                    resultado = new Uri[]{ data.getData() };
                }
            }

            callbackArchivos.onReceiveValue(resultado);
            callbackArchivos = null;
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
