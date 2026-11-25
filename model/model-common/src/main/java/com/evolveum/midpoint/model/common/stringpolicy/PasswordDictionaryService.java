/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/**
 * Checks values against all known password dictionaries.
 * Every {@link PasswordDictionary} component is picked up automatically.
 */
@Service
public class PasswordDictionaryService {

    private final List<PasswordDictionary> dictionaries;

    public PasswordDictionaryService(List<PasswordDictionary> dictionaries) {
        this.dictionaries = List.copyOf(dictionaries);
    }

    /**
     * Returns false if the value is found in a dictionary that applies for the limitations.
     * Whether the check is enabled is decided by the caller.
     */
    public boolean isValid(@NotNull String value, @NotNull LimitationsType limitations) {
        String normalized = PasswordDictionaryFormat.normalize(value);
        return dictionaries.stream().noneMatch(dictionary -> dictionary.contains(normalized, limitations));
    }
}
