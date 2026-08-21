/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

/**
 * Verifies that {@link AvailableLocale.LocaleDescriptor} sorts by display name (the value
 * configured as {@code <locale>.name} in {@code locale.properties}) rather than by locale
 * identifier. The old sort key was country -> language -> variant, which pushed every
 * country-coded locale ({@code en_US}, {@code pt_BR}, {@code zh_CN}) below every
 * language-only locale ({@code de}, {@code fr}) regardless of how the entries read in the
 * GUI language selector.
 */
public class AvailableLocaleTest {

    private static AvailableLocale.LocaleDescriptor desc(String name, String flag, Locale locale) {
        return new AvailableLocale.LocaleDescriptor(name, flag, null, locale);
    }

    /**
     * After sorting, the display names must be in non-decreasing collation order per the JVM
     * default {@link Collator}. The exact order may vary slightly by JVM locale (accent
     * handling etc.), but the list must remain monotonically sorted.
     */
    @Test
    public void sortedListIsMonotonicByDisplayName() {
        List<AvailableLocale.LocaleDescriptor> list = new ArrayList<>(List.of(
                desc("English (US)", "us", new Locale("en", "US")),
                desc("English (UK)", "gb", new Locale("en", "GB")),
                desc("Français", "fr", new Locale("fr")),
                desc("Deutsch", "de", new Locale("de")),
                desc("Português (Brasil)", "br", new Locale("pt", "BR"))));
        Collections.sort(list);

        Collator collator = Collator.getInstance();
        for (int i = 1; i < list.size(); i++) {
            assertThat(collator.compare(list.get(i - 1).getName(), list.get(i).getName()))
                    .as("element %d (%s) must collate <= element %d (%s)",
                            i - 1, list.get(i - 1).getName(), i, list.get(i).getName())
                    .isLessThanOrEqualTo(0);
        }
    }

    /**
     * A country-coded locale ({@code zh_CN}, display name "Chinese") must NOT be pushed below
     * a language-only locale ({@code de}, display name "Deutsch") just because it has a
     * country code. "C" collates before "D" in every Locale's collation, so this assertion is
     * robust across test environments.
     */
    @Test
    public void countryCodedLocaleIsNotPushedBelowLanguageOnlyByCountryCode() {
        AvailableLocale.LocaleDescriptor zhCn = desc("Chinese", "cn", new Locale("zh", "CN"));
        AvailableLocale.LocaleDescriptor de = desc("Deutsch", "de", new Locale("de"));

        List<AvailableLocale.LocaleDescriptor> list = new ArrayList<>(List.of(zhCn, de));
        Collections.sort(list);

        assertThat(list.get(0).getName()).isEqualTo("Chinese");
        assertThat(list.get(1).getName()).isEqualTo("Deutsch");
    }

    /**
     * Two locales with identical display names compare as equal regardless of their locale
     * identifier. Edge case to guard against the previous implementation's fallback to
     * country/language comparison sneaking back in.
     */
    @Test
    public void identicalDisplayNamesCompareEqual() {
        AvailableLocale.LocaleDescriptor a = desc("Same", "us", new Locale("en", "US"));
        AvailableLocale.LocaleDescriptor b = desc("Same", "gb", new Locale("en", "GB"));
        assertThat(a.compareTo(b)).isZero();
        assertThat(b.compareTo(a)).isZero();
    }
}
