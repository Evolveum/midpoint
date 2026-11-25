/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DictionaryCheckType;

/**
 * Forms of a value that are compared with the words of password dictionaries.
 * The value itself is always among them. The options of the dictionary check add more.
 */
final class PasswordVariants {

    /** Substitutions reversed by the leetspeak option. The digit 1 is handled separately, it can be i or l. */
    private static final Map<Integer, Integer> LEETSPEAK = Map.of(
            (int) '0', (int) 'o',
            (int) '3', (int) 'e',
            (int) '4', (int) 'a',
            (int) '5', (int) 's',
            (int) '7', (int) 't',
            (int) '@', (int) 'a',
            (int) '$', (int) 's',
            (int) '+', (int) 't');

    private PasswordVariants() {
    }

    /**
     * Returns the normalized value and its variants. If eachWord is on, every word of the value
     * and the variants of each word are included too. An empty string is never returned.
     */
    static @NotNull Set<String> candidates(@NotNull String value, @Nullable DictionaryCheckType options) {
        String normalized = PasswordDictionaryFormat.normalize(value);
        Set<String> parts = new LinkedHashSet<>();
        parts.add(normalized);
        if (options != null && Boolean.TRUE.equals(options.isEachWord())) {
            for (String word : normalized.split(" ")) {
                parts.add(word);
            }
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (String part : parts) {
            candidates.addAll(variants(part, options));
        }
        candidates.remove("");
        return candidates;
    }

    private static Set<String> variants(String part, DictionaryCheckType options) {
        Set<String> variants = new LinkedHashSet<>();
        variants.add(part);
        if (options == null) {
            return variants;
        }
        if (Boolean.TRUE.equals(options.isStripNonAlpha())) {
            variants.add(trimNonAlpha(part));
        }
        if (Boolean.TRUE.equals(options.isLeetspeak())) {
            for (String variant : List.copyOf(variants)) {
                variants.add(undoLeetspeak(variant, 'i'));
                variants.add(undoLeetspeak(variant, 'l'));
            }
        }
        if (Boolean.TRUE.equals(options.isStripNonAlpha())) {
            for (String variant : List.copyOf(variants)) {
                variants.add(stripNonAlpha(variant));
            }
        }
        if (Boolean.TRUE.equals(options.isReversed())) {
            for (String variant : List.copyOf(variants)) {
                variants.add(new StringBuilder(variant).reverse().toString());
            }
        }
        return variants;
    }

    private static String undoLeetspeak(String value, char replacementOfOne) {
        StringBuilder result = new StringBuilder();
        value.codePoints().forEach(codePoint -> {
            if (codePoint == '1') {
                result.append(replacementOfOne);
            } else {
                result.appendCodePoint(LEETSPEAK.getOrDefault(codePoint, codePoint));
            }
        });
        return result.toString();
    }

    /** Removes the characters that are not letters from both ends, so that e.g. 42p4ssw0rd! is left as p4ssw0rd. */
    private static String trimNonAlpha(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && !Character.isLetter(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && !Character.isLetter(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    private static String stripNonAlpha(String value) {
        StringBuilder result = new StringBuilder();
        value.codePoints().filter(Character::isLetter).forEach(result::appendCodePoint);
        return result.toString();
    }
}
