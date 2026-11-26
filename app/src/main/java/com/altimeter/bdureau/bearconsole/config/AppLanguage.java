package com.altimeter.bdureau.bearconsole.config;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AppLanguage
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Single definition of the languages of the application and of the language really used.
 *
 *  The "AppLanguage" setting (preferences "BearConsoleCfg") is an index in LANGUAGES:
 *   - 0 = language of the phone; if the application is not translated in that language,
 *         English is used (strings, help pages and spoken telemetry);
 *   - 1 = English (same value as the former "force English" choice, so saved settings keep
 *         their meaning), 2.. = the other translated languages, forced whatever the phone.
 *
 *  resolveLocale() gives the locale applied to every activity (BaseActivity) and to the
 *  application context (ConsoleApplication); it is also set as Locale.getDefault(), which is
 *  what the help pages and the text-to-speech use. To add a language: add its values-xx
 *  folder and one line in LANGUAGES / NATIVE_NAMES.
 * ============================================================================
 */

import android.content.Context;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;

import com.altimeter.bdureau.bearconsole.R;

import java.util.Locale;

public final class AppLanguage {

    /** Setting value: follow the phone (English if the phone language is not translated). */
    public static final int PHONE = 0;
    /** Setting value of English (also the former "force English" value). */
    public static final int ENGLISH = 1;

    /**
     * Language codes, in the order of the setting values (index 0 = phone language).
     * Each code has a res/values-xx folder, except English which is res/values.
     */
    private static final String[] LANGUAGES = {
            null, "en", "fr", "de", "es", "it", "nl", "pl", "pt", "ru", "tr", "hu"
    };

    /** Name of each language written in that language (not translated, as on Android). */
    private static final String[] NATIVE_NAMES = {
            null, "English", "Français", "Deutsch", "Español", "Italiano", "Nederlands",
            "Polski", "Português", "Русский", "Türkçe", "Magyar"
    };

    private AppLanguage() {
    }

    /** Items of the language list of the settings: phone language, then the languages. */
    public static String[] getSettingItems(Context context) {
        String[] items = new String[LANGUAGES.length];
        items[0] = context.getString(R.string.phone_language);
        System.arraycopy(NATIVE_NAMES, 1, items, 1, NATIVE_NAMES.length - 1);
        return items;
    }

    /** Number of possible setting values. */
    public static int getSettingCount() {
        return LANGUAGES.length;
    }

    /** True if the application is translated in this language (ISO 639 code). */
    public static boolean isSupported(String language) {
        if (language == null) return false;
        for (int i = 1; i < LANGUAGES.length; i++) {
            if (LANGUAGES[i].equals(language)) return true;
        }
        return false;
    }

    /**
     * Locale to apply for a setting value.
     * Phone language: the first language of the phone (read from the system configuration,
     * Locale.getDefault() may already hold a forced language), kept with its region for the
     * number and date formats; English when the application has no translation for it.
     */
    public static Locale resolveLocale(int setting) {
        if (setting > PHONE && setting < LANGUAGES.length) {
            return new Locale(LANGUAGES[setting]);
        }
        Locale phone = Resources.getSystem().getConfiguration().getLocales().get(0);
        if (phone != null && isSupported(phone.getLanguage())) {
            return phone;
        }
        return Locale.ENGLISH;
    }

    /**
     * Sets the language of a text-to-speech engine to the language of the application
     * (Locale.getDefault(), set by resolveLocale()), or English when the engine has no voice
     * for it.
     *
     * @return the result of TextToSpeech.setLanguage()
     */
    public static int applyToTextToSpeech(TextToSpeech tts) {
        int result = tts.setLanguage(Locale.getDefault());
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = tts.setLanguage(Locale.ENGLISH);
        }
        return result;
    }
}
