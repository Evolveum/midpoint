/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.*;

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

    /**
     * Covers the need to have separated characters for easier (screen-reader) reading
     * @param characters
     * @return
     */
    static @NotNull String dividedCharactersAsString(@NotNull Collection<Character> characters, String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<Character> it = characters.iterator();
        while (it.hasNext()) {
            Character character = it.next();
            sb.append(character);
            if (it.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
}
