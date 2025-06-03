/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.security.SecureRandom;
import java.util.*;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Chars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Set of characters to be used when generating or checking a string value.
 *
 * May be derived from the configuration or created artificially.
 *
 * Supports basic operations such as random character selection and counting occurrences of characters in a string.
 * More complex algorithms are implemented in e.g. {@link ValueGenerator}.
 */
@VisibleForTesting
public class CharacterClass {

    private static final Random RANDOM = new SecureRandom();

    private static final CharacterClass DEFAULT = unnamed(ascii7());

    private static Set<Character> ascii7() {
        Set<Character> chars = new TreeSet<>();
        for (char c = 0x20; c <= 0x7e; c++) {
            chars.add(c);
        }
        return chars;
    }

    static CharacterClass getDefault() {
        return DEFAULT;
    }

    @Nullable private final QName name;

    /** The set of characters in this class. Never empty. */
    @NotNull final Set<Character> characters;

    /** The same content as {@link #characters}, but in an array. */
    private final char[] charactersArray;

    private CharacterClass(@Nullable QName name, @NotNull Set<Character> characters) {
        this.name = name;
        this.characters = Collections.unmodifiableSet(characters);
        Preconditions.checkArgument(!characters.isEmpty(), "Empty class: %s", name);
        this.charactersArray = Chars.toArray(characters);
    }

    public static CharacterClass unnamed(Set<Character> characters) {
        return new CharacterClass(null, characters);
    }

    /** Converts the bean into parsed form, taking into account also embedded classes. NOT dealing with references! */
    public static CharacterClass parse(@NotNull CharacterClassType bean) {
        Set<Character> characters = new TreeSet<>(StringPolicyUtils.stringAsCharacters(bean.getValue()));
        for (CharacterClassType embeddedClass : bean.getCharacterClass()) {
            characters.addAll(parse(embeddedClass).characters);
        }
        return new CharacterClass(
                bean.getName(),
                !characters.isEmpty() ? characters : DEFAULT.characters);
    }

    char random() {
        return charactersArray[RANDOM.nextInt(characters.size())];
    }

    /** How many characters of the string match this class. */
    int countOccurrences(@NotNull List<Character> string) {
        return (int) string.stream()
                .filter(characters::contains)
                .count();
    }

    /** How many characters of the string match this class. */
    int countOccurrences(@NotNull String string) {
        return (int) string.chars()
                .filter(c -> characters.contains((char) c))
                .count();
    }

    public @NotNull Set<Character> getCharacters() {
        return characters;
    }

    public @NotNull String getCharactersAsString() {
        return StringPolicyUtils.charactersAsString(characters);
    }

    @Override
    public String toString() {
        return "CharacterClass{" +
                "name=" + name +
                ", characters=" + characters +
                '}';
    }
}
