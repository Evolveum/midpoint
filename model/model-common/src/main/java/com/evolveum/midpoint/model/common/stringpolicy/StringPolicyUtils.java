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

    //useful for clarification of the special symbol, maps the special symbol and localization key with its name
    private static final Map<Character, String> SPECIAL_SYMBOLS_MAP = new HashMap<>();

    static {
        SPECIAL_SYMBOLS_MAP.put('!', "ValuePolicy.character.exclamationMark");
        SPECIAL_SYMBOLS_MAP.put('$', "ValuePolicy.character.dollarSign");
        SPECIAL_SYMBOLS_MAP.put('%', "ValuePolicy.character.percent");
        SPECIAL_SYMBOLS_MAP.put('(', "ValuePolicy.character.leftParenthesis");
        SPECIAL_SYMBOLS_MAP.put(')', "ValuePolicy.character.rightParenthesis");
        SPECIAL_SYMBOLS_MAP.put('+', "ValuePolicy.character.plusSign");
        SPECIAL_SYMBOLS_MAP.put(',', "ValuePolicy.character.comma");
        SPECIAL_SYMBOLS_MAP.put('-', "ValuePolicy.character.minusSign");
        SPECIAL_SYMBOLS_MAP.put('.', "ValuePolicy.character.period");
        SPECIAL_SYMBOLS_MAP.put(':', "ValuePolicy.character.colon");
        SPECIAL_SYMBOLS_MAP.put(';', "ValuePolicy.character.semicolon");
        SPECIAL_SYMBOLS_MAP.put('<', "ValuePolicy.character.lessThanSign");
        SPECIAL_SYMBOLS_MAP.put('>', "ValuePolicy.character.greaterThanSign");
        SPECIAL_SYMBOLS_MAP.put('?', "ValuePolicy.character.questionMark");
        SPECIAL_SYMBOLS_MAP.put('@', "ValuePolicy.character.atSymbol");
        SPECIAL_SYMBOLS_MAP.put('[', "ValuePolicy.character.leftSquareBracket");
        SPECIAL_SYMBOLS_MAP.put(']', "ValuePolicy.character.rightSquareBracket");
        SPECIAL_SYMBOLS_MAP.put('^', "ValuePolicy.character.caret");
        SPECIAL_SYMBOLS_MAP.put('_', "ValuePolicy.character.underscore");
        SPECIAL_SYMBOLS_MAP.put('`', "ValuePolicy.character.backtick");
        SPECIAL_SYMBOLS_MAP.put('{', "ValuePolicy.character.leftCurlyBrace");
        SPECIAL_SYMBOLS_MAP.put('|', "ValuePolicy.character.verticalBar");
        SPECIAL_SYMBOLS_MAP.put('}', "ValuePolicy.character.rightCurlyBrace");
        SPECIAL_SYMBOLS_MAP.put('~', "ValuePolicy.character.tilde");
        SPECIAL_SYMBOLS_MAP.put('#', "ValuePolicy.character.hash");
        SPECIAL_SYMBOLS_MAP.put('&', "ValuePolicy.character.ampersand");
        SPECIAL_SYMBOLS_MAP.put('"', "ValuePolicy.character.doubleQuote");
        SPECIAL_SYMBOLS_MAP.put('*', "ValuePolicy.character.asterisk");
        SPECIAL_SYMBOLS_MAP.put('\'', "ValuePolicy.character.apostrophe");
    }

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

    public static @Nullable String getTranslationKeyForCharacter(Character ch) {
        return SPECIAL_SYMBOLS_MAP.get(ch);
    }
}
