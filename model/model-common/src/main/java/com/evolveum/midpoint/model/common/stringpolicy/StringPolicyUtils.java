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
    private static final Map<Character, String> specialSymbolsMap = new HashMap<>();

    static {
        specialSymbolsMap.put('!', "ValuePolicy.character.exclamationMark");
        specialSymbolsMap.put('$', "ValuePolicy.character.dollarSign");
        specialSymbolsMap.put('%', "ValuePolicy.character.percent");
        specialSymbolsMap.put('(', "ValuePolicy.character.leftParenthesis");
        specialSymbolsMap.put(')', "ValuePolicy.character.rightParenthesis");
        specialSymbolsMap.put('+', "ValuePolicy.character.plusSign");
        specialSymbolsMap.put(',', "ValuePolicy.character.comma");
        specialSymbolsMap.put('-', "ValuePolicy.character.minusSign");
        specialSymbolsMap.put('.', "ValuePolicy.character.period");
        specialSymbolsMap.put(':', "ValuePolicy.character.colon");
        specialSymbolsMap.put(';', "ValuePolicy.character.semicolon");
        specialSymbolsMap.put('<', "ValuePolicy.character.lessThanSign");
        specialSymbolsMap.put('>', "ValuePolicy.character.greaterThanSign");
        specialSymbolsMap.put('?', "ValuePolicy.character.questionMark");
        specialSymbolsMap.put('@', "ValuePolicy.character.atSymbol");
        specialSymbolsMap.put('[', "ValuePolicy.character.leftSquareBracket");
        specialSymbolsMap.put(']', "ValuePolicy.character.rightSquareBracket");
        specialSymbolsMap.put('^', "ValuePolicy.character.caret");
        specialSymbolsMap.put('_', "ValuePolicy.character.underscore");
        specialSymbolsMap.put('`', "ValuePolicy.character.backtick");
        specialSymbolsMap.put('{', "ValuePolicy.character.leftCurlyBrace");
        specialSymbolsMap.put('|', "ValuePolicy.character.verticalBar");
        specialSymbolsMap.put('}', "ValuePolicy.character.rightCurlyBrace");
        specialSymbolsMap.put('~', "ValuePolicy.character.tilde");
        specialSymbolsMap.put('#', "ValuePolicy.character.hash");
        specialSymbolsMap.put('&', "ValuePolicy.character.ampersand");
        specialSymbolsMap.put('"', "ValuePolicy.character.doubleQuote");
        specialSymbolsMap.put('*', "ValuePolicy.character.asterisk");
        specialSymbolsMap.put('\'', "ValuePolicy.character.apostrophe");
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
        return specialSymbolsMap.get(ch);
    }
}
