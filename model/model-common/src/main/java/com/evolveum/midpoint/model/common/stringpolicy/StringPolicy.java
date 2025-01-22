/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Compiled string policy.
 *
 * Represents a {@link StringPolicyType} transformed into structures that can efficiently execute value generation and checking.
 */
public class StringPolicy {

    private static final int UNLIMITED = -1;

    private static final int DEFAULT_MAX_GENERATION_ATTEMPTS = 10;

    /** Origin of the string policy - needed for error reporting and expression evaluation. */
    @NotNull private final ConfigurationItemOrigin origin;

    /** (Resolved) minimal length of the string value. Should never be negative. */
    private final int minLength;

    /** (Resolved) maximal length of the string value; negative (e.g. {@link #UNLIMITED}) means unlimited. */
    private final int maxLength;

    /** Minimum number of unique characters in the string value. Should never be negative. */
    private final int minUniqueChars;

    /** How many times we try to generate a value that satisfies the policy. */
    private final int maxGenerationAttempts;

    /** All known limitations on character classes. */
    @NotNull private final Collection<CharacterClassLimitation> characterClassLimitations;

    /** All declared check expressions. */
    @NotNull private final List<CheckExpressionType> checkExpressions;

    private StringPolicy(
            @NotNull ConfigurationItemOrigin origin,
            int minLength, int maxLength, int minUniqueChars,
            int maxGenerationAttempts,
            @NotNull Collection<CharacterClassLimitation> characterClassLimitations,
            @NotNull List<CheckExpressionType> checkExpressions) {
        this.origin = origin;
        assert minLength >= 0;
        this.minLength = minLength;
        this.maxLength = maxLength;
        assert minUniqueChars >= 0;
        this.minUniqueChars = minUniqueChars;
        this.maxGenerationAttempts = maxGenerationAttempts;
        this.characterClassLimitations = characterClassLimitations;
        this.checkExpressions = checkExpressions;
    }

    public static @NotNull StringPolicy compile(@Nullable ConfigurationItem<StringPolicyType> policyConfigItem)
            throws ConfigurationException {

        if (policyConfigItem == null) {
            return noLimitations(
                    ConfigurationItemOrigin.generated(), null);
        }

        StringPolicyType policyBean = policyConfigItem.value();
        LimitationsType limitationsBean = policyBean.getLimitations();
        if (limitationsBean == null) {
            return noLimitations(
                    policyConfigItem.origin(), policyBean);
        }

        int minLength = defaultIfNull(limitationsBean.getMinLength(), 0);
        int maxLength = defaultIfNull(limitationsBean.getMaxLength(), UNLIMITED);
        int minUniqueChars = defaultIfNull(limitationsBean.getMinUniqueChars(), minLength);
        int maxAttempts = defaultIfNull(limitationsBean.getMaxAttempts(), DEFAULT_MAX_GENERATION_ATTEMPTS);

        return new StringPolicy(
                policyConfigItem.origin(),
                minLength, maxLength, minUniqueChars,
                maxAttempts,
                CharacterClassLimitation.compile(policyConfigItem),
                limitationsBean.getCheckExpression());
    }

    private static @NotNull StringPolicy noLimitations(
            @NotNull ConfigurationItemOrigin origin, @Nullable StringPolicyType bean) {
        assert bean == null || bean.getLimitations() == null;
        return new StringPolicy(
                origin, 0, UNLIMITED, 0,
                DEFAULT_MAX_GENERATION_ATTEMPTS,
                List.of(),
                List.of());
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /** Returns a value we can safely use for comparisons. */
    int getEffectiveMaxLength() {
        return maxLength >= 0 ? maxLength : Integer.MAX_VALUE;
    }

    public int getMinUniqueChars() {
        return minUniqueChars;
    }

    public @NotNull Collection<CharacterClassLimitation> getCharacterClassLimitations() {
        return characterClassLimitations;
    }

    int getMaxGenerationAttempts() {
        return maxGenerationAttempts;
    }

    @NotNull List<CheckExpressionType> getCheckExpressions() {
        return checkExpressions;
    }

    boolean hasFirstClassesSpecified() {
        return characterClassLimitations.stream()
                .anyMatch(limitation -> limitation.mustBeFirst);
    }

    @Override
    public String toString() {
        return "StringPolicy{" +
                "origin=" + origin +
                ", minLength=" + minLength +
                ", maxLength=" + maxLength +
                ", minUniqueChars=" + minUniqueChars +
                ", characterClassLimitations: " + characterClassLimitations.size() +
                ", checkExpressions: " + checkExpressions.size() +
                '}';
    }

    /**
     * "Declared" means that either `null` (no limit) or a non-negative value is returned.
     * This is a more modern style than using negative numbers to denote "no limit", as is in the schema.
     */
    Integer getDeclaredMaxLength() {
        return maxLength >= 0 ? maxLength : null;
    }

    /**
     * Limitation regarding a single character class: required minimum and allowed maximum occurrences
     * of characters from within this class in the generated or checked string value.
     */
    public record CharacterClassLimitation(
            @NotNull CharacterClass characterClass,
            @Nullable StringLimitType bean,
            boolean mustBeFirst,
            boolean ignoreWhenGenerating,
            int minOccurrences,
            Integer declaredMaxOccurrences,
            int effectiveMaxOccurrences) {

        private static final CharacterClassLimitation DEFAULT_FOR_GENERATION =
                new CharacterClassLimitation(
                        CharacterClass.unnamed(
                                StringPolicyUtils.stringAsCharacters(
                                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")),
                        null,
                        false,
                        false,
                        0,
                        null,
                        Integer.MAX_VALUE);

        static Collection<CharacterClassLimitation> compile(ConfigurationItem<StringPolicyType> policyConfigItem)
                throws ConfigurationException {

            List<CharacterClassLimitation> limitations = new ArrayList<>();

            StringPolicyType policyBean = policyConfigItem.value();
            LimitationsType limitationsBean = policyBean.getLimitations();
            CharacterClassType globalClassDefinition = policyBean.getCharacterClass();

            List<StringLimitType> limitBeans = limitationsBean.getLimit();
            for (StringLimitType limitBean : limitBeans) {
                int minOccurrences = defaultIfNull(limitBean.getMinOccurs(), 0);
                int maxOccurrences = defaultIfNull(limitBean.getMaxOccurs(), UNLIMITED);
                limitations.add(new CharacterClassLimitation(
                        resolveCharacterClass(
                                limitBean.getCharacterClass(), globalClassDefinition, policyConfigItem.origin()),
                        limitBean,
                        Boolean.TRUE.equals(limitBean.isMustBeFirst()),
                        Boolean.TRUE.equals(limitBean.isIgnoreWhenGenerating()),
                        minOccurrences,
                        maxOccurrences >= 0 ? maxOccurrences : null,
                        maxOccurrences >= 0 ? maxOccurrences : Integer.MAX_VALUE));
            }

            return limitations;
        }

        private static CharacterClass resolveCharacterClass(
                @Nullable CharacterClassOrRefType classBean,
                @Nullable CharacterClassType globalClassDefinitionBean,
                @NotNull ConfigurationItemOrigin policyOrigin) throws ConfigurationException {

            if (classBean == null) {
                return CharacterClass.getDefault();
            }

            QName ref = classBean.getRef();
            if (ref != null) {
                var matching = findMatchingClasses(globalClassDefinitionBean, ref);
                if (matching.isEmpty()) {
                    throw new ConfigurationException("No character class named '%s' found in %s".formatted(ref, policyOrigin));
                } else if (matching.size() > 1) {
                    throw new ConfigurationException("Multiple character classes (%s) named '%s' found in %s".formatted(
                            ref, matching.size(), policyOrigin));
                } else {
                    return matching.get(0);
                }
            } else {
                return CharacterClass.parse(classBean);
            }
        }

        private static List<CharacterClass> findMatchingClasses(CharacterClassType classDefinitionBean, @NotNull QName ref) {
            if (classDefinitionBean == null) {
                return List.of();
            } else if (ref.equals(classDefinitionBean.getName())) {
                return List.of(CharacterClass.parse(classDefinitionBean));
            } else {
                List<CharacterClass> matching = new ArrayList<>();
                for (CharacterClassType childBean : classDefinitionBean.getCharacterClass()) {
                    matching.addAll(findMatchingClasses(childBean, ref));
                }
                return matching;
            }
        }

        static @NotNull CharacterClassLimitation defaultForGeneration() {
            return DEFAULT_FOR_GENERATION;
        }

        public @Nullable PolyStringType getName() {
            return bean != null ? bean.getName() : null;
        }

        public @Nullable String getDescription() {
            return bean != null ? bean.getDescription() : null;
        }

        public String getHumanReadableName() {
            if (bean != null) {
                var name = bean.getName();
                if (name != null) {
                    return name.getOrig();
                }
                var description = bean.getDescription();
                if (description != null) {
                    return description;
                }
            }
            return characterClass.getCharactersAsString();
        }
    }
}
