/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringPolicyUtils {

    static @NotNull Set<Character> stringAsCharacters(@Nullable String value) {
        if (value == null) {
            return Collections.emptySet();
        } else {
            Set<Character> characters = new HashSet<>();
            for (char c : value.toCharArray()) {
                characters.add(c);
            }
            return characters;
        }
    }

    static @NotNull String charactersAsString(@NotNull Collection<Character> characters) {
        StringBuilder sb = new StringBuilder();
        for (Character c : characters) {
            sb.append(c);
        }
        return sb.toString();
    }
}
