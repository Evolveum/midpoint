/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/**
 * Default dictionary of common passwords, loaded from the bundled resource.
 * It is not used if the policy configures external dictionary files.
 */
@Component
public class BundledPasswordDictionary implements PasswordDictionary {

    private static final String RESOURCE = "/dictionaries/common-passwords.txt";

    private final Set<String> words = load();

    @Override
    public boolean contains(@NotNull String word, @NotNull LimitationsType limitations) {
        return applies(limitations) && words.contains(word);
    }

    @Override
    public @NotNull Stream<String> words(@NotNull LimitationsType limitations) {
        return applies(limitations) ? words.stream() : Stream.empty();
    }

    private static boolean applies(LimitationsType limitations) {
        return limitations.getDictionaryFile().isEmpty();
    }

    private static Set<String> load() {
        try (InputStream stream = BundledPasswordDictionary.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new SystemException("Common password dictionary " + RESOURCE + " not found");
            }
            return PasswordDictionaryFormat.read(stream);
        } catch (IOException e) {
            throw new SystemException("Cannot read common password dictionary " + RESOURCE + ": " + e.getMessage(), e);
        }
    }
}
