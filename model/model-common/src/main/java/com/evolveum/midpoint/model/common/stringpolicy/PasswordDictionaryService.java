/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DictionaryCheckType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/**
 * Checks values against all known password dictionaries.
 * Every {@link PasswordDictionary} component is picked up automatically.
 */
@Service
public class PasswordDictionaryService {

    /** Words shorter than this are ignored by the substring check, unless minWordLength says otherwise. */
    private static final int DEFAULT_SUBSTRING_MIN_WORD_LENGTH = 4;

    private final List<PasswordDictionary> dictionaries;

    public PasswordDictionaryService(List<PasswordDictionary> dictionaries) {
        this.dictionaries = List.copyOf(dictionaries);
    }

    /**
     * Returns false if the value, or one of its forms selected by the options of the check, is found in a dictionary
     * that applies for the limitations. Whether the check is enabled is decided by the caller.
     */
    public boolean isValid(@NotNull String value, @NotNull LimitationsType limitations) {
        DictionaryCheckType options = limitations.getDictionaryCheck();
        Set<String> candidates = PasswordVariants.candidates(value, options);
        Integer configuredMinWordLength = options != null ? options.getMinWordLength() : null;
        int exactMinWordLength = configuredMinWordLength != null ? configuredMinWordLength : 0;
        int substringMinWordLength =
                configuredMinWordLength != null ? configuredMinWordLength : DEFAULT_SUBSTRING_MIN_WORD_LENGTH;
        boolean substring = options != null && Boolean.TRUE.equals(options.isSubstring());

        for (String candidate : candidates) {
            if (candidate.length() >= exactMinWordLength && isInDictionary(candidate, limitations)) {
                return false;
            }
        }
        return !substring || !containsDictionaryWord(candidates, limitations, substringMinWordLength);
    }

    private boolean isInDictionary(String word, LimitationsType limitations) {
        return dictionaries.stream().anyMatch(dictionary -> dictionary.contains(word, limitations));
    }

    private boolean containsDictionaryWord(Set<String> candidates, LimitationsType limitations, int minWordLength) {
        return dictionaries.stream()
                .flatMap(dictionary -> dictionary.words(limitations))
                .filter(word -> word.length() >= minWordLength)
                .anyMatch(word -> candidates.stream().anyMatch(candidate -> candidate.contains(word)));
    }
}
