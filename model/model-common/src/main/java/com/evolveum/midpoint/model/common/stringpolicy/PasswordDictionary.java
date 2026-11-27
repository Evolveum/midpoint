/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/**
 * Source of words that must not be used as passwords. Each implementation is a Spring component
 * and decides by the policy limitations which of its words apply. The words are already normalized,
 * and so are the words passed to {@link #contains(String, LimitationsType)}.
 * A new kind of dictionary is just another component. The comparison is done by
 * {@link PasswordDictionaryService}.
 *
 * The limitations are the part of the policy the dictionary is bound to. Currently, the dictionaries read
 * only {@link LimitationsType#getDictionaryFile()}.
 */
public interface PasswordDictionary {

    /** Returns true if the (normalized) word is in this dictionary and the dictionary applies for the limitations. */
    boolean contains(@NotNull String word, @NotNull LimitationsType limitations);

    /**
     * Returns all words of this dictionary that apply for the limitations, or an empty stream if none do.
     * Used only where the words must be enumerated, i.e. for the check of words contained in the value.
     */
    @NotNull Stream<String> words(@NotNull LimitationsType limitations);
}
