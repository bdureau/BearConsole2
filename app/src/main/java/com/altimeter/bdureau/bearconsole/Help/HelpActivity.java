package com.altimeter.bdureau.bearconsole.Help;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: HelpActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Displays the HTML help page of the calling screen in a WebView. Each screen passes the
 *  base name of its page in the "help_file" extra (e.g. "help_flight"). The pages are stored
 *  in assets/help as "<name>.html" (English) and "<name>_<language>.html" (translations).
 *  The page in the phone language is used whenever it exists, otherwise the English page;
 *  when the application is forced to English the English page is always used.
 * ============================================================================
 */

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class HelpActivity extends BaseActivity {
    private static final String TAG = "HelpActivity";
    private static final String HELP_ASSET_DIR = "help/";

    private Button btnDismiss;
    private WebView webView;
    private ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_help);
        webView = findViewById(R.id.webView);

        WebSettings webSetting = webView.getSettings();
        webSetting.setBuiltInZoomControls(true);
        webSetting.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setAllowFileAccess(true);

        Intent newint = getIntent();
        String fileName = newint.getStringExtra("help_file");
        if (fileName == null) fileName = "help";

        webView.loadUrl("file:///android_asset/" + selectHelpPage(fileName));

        btnDismiss = findViewById(R.id.butClose);
        btnDismiss.setOnClickListener(v -> finish());
    }

    /**
     * Chooses the help page to display: the translation in the language of the application
     * if it exists, otherwise the English page.
     *
     * @param baseName name of the page without language suffix and extension
     * @return asset path of the page
     */
    private String selectHelpPage(String baseName) {
        String english = HELP_ASSET_DIR + baseName + ".html";
        // Language of the application (phone language with English fallback, or the language
        // forced in the settings): set as default locale by AppLanguage / BaseActivity
        String language = Locale.getDefault().getLanguage();
        String localized = HELP_ASSET_DIR + baseName + "_" + language + ".html";
        return (!"en".equals(language) && assetExists(localized)) ? localized : english;
    }

    /** True if the asset can be opened. */
    private boolean assetExists(String path) {
        try (InputStream in = getAssets().open(path)) {
            return true;
        } catch (IOException e) {
            Log.d(TAG, "No translated help page: " + path);
            return false;
        }
    }

    /** Keeps the navigation between help pages inside the WebView. */
    private static class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
}
