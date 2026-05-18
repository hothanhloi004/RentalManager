package com.example.rentalmanager.util;

import android.annotation.SuppressLint;
import android.util.LruCache;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class TextUtil {
    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final LruCache<String, String> NORMALIZED_CACHE = new LruCache<>(256);

    @SuppressLint("DefaultLocale")
    public static String removeAccents(String s) {
        if (s == null) return "";

        synchronized (NORMALIZED_CACHE) {
            String cached = NORMALIZED_CACHE.get(s);
            if (cached != null) {
                return cached;
            }
        }

        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        String result = DIACRITICS_PATTERN.matcher(temp).replaceAll("")
                .replace("\u0110", "D")
                .replace("\u0111", "d")
                .toLowerCase();

        synchronized (NORMALIZED_CACHE) {
            NORMALIZED_CACHE.put(s, result);
        }
        return result;
    }
}
