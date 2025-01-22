/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.*;

import com.google.common.base.Joiner;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicy.CharacterClassLimitation;

/**
 * Generates string values according to the cardinality requirements of the {@link #stringPolicy}
 * with the size of (at least) {@link #plannedLength}.
 *
 * It has no mutable internal state (e.g., regarding a specific generation attempt).
 */
class ValueGenerator {

    @NotNull private final StringPolicy stringPolicy;

    /**
     * Minimum number of characters that we plan to generate.
     * Derived from min/minUnique (from string policy), or it's defaultLength, if the policy does not specify min length.
     *
     * In reality, we may generate longer string if needed to satisfy "min" requirements of individual classes.
     */
    private final int plannedLength;

    /**
     * Minimum number of unique characters that we plan to generate.
     * Normally derived from the policy, but a default is used here if the policy specify no limitations.
     */
    private final int plannedUniqueCharacters;

    /**
     * Instantiates the generator with the given policy and the default length
     * (which is used when no min length nor min unique chars are specified).
     *
     * TODO should we take the maxLength into account when having no minLength/minUniqueChars?
     */
    ValueGenerator(@NotNull StringPolicy stringPolicy, int defaultLength) {
        this.stringPolicy = stringPolicy;

        if (stringPolicy.getMinLength() == 0 && stringPolicy.getMinUniqueChars() == 0) {
            this.plannedLength = defaultLength;
            this.plannedUniqueCharacters = defaultLength / 2;
        } else {
            this.plannedLength = Math.max(stringPolicy.getMinLength(), stringPolicy.getMinUniqueChars());
            this.plannedUniqueCharacters = stringPolicy.getMinUniqueChars();
        }
    }

    /** Generates a string value according to the policy and the requested length. */
    @NotNull String generate() throws GenerationException {

        List<Character> generatedSequence = new ArrayList<>();

        while (!isGeneratedSequenceSatisfying(generatedSequence)) {
            CharacterClass eligibleCharacters = getEligibleCharacters(generatedSequence);
            generatedSequence.add(
                    eligibleCharacters.random());
        }

        if (generatedSequence.size() > 2) {
            Collections.shuffle(
                    generatedSequence.subList(1, generatedSequence.size()));
        }

        return Joiner.on("").join(generatedSequence);
    }

    /**
     * Given `alreadyGenerated` characters, this method determines the set of characters that can be used at the next position.
     */
    private CharacterClass getEligibleCharacters(List<Character> alreadyGenerated) throws GenerationException {

        boolean selectingFirstCharacter = alreadyGenerated.isEmpty();
        boolean hasFirstClassesSpecified = stringPolicy.hasFirstClassesSpecified();

        Set<Character> disallowedCharacters = new HashSet<>(); // we cannot use these in any case
        if (!isUniquenessReached(alreadyGenerated)) {
            disallowedCharacters.addAll(alreadyGenerated);
        }

        // Applicable for selection of the first character; intersection of these sets is used
        List<CharacterClass> eligibleFirstClasses = new ArrayList<>();

        // Applicable for selection of the next character (or the first if no mustBeFirst is specified);
        // union of these sets is used.
        List<CharacterClass> eligibleClasses = new ArrayList<>();
        List<CharacterClass> preferredClasses = new ArrayList<>(); // these are not yet used enough

        var allLimitations = new ArrayList<>(stringPolicy.getCharacterClassLimitations());
        if (allLimitations.isEmpty()) {
            // We need something to base our character generation on.
            allLimitations.add(CharacterClassLimitation.defaultForGeneration());
        }

        boolean allClassesIgnored = true;

        for (CharacterClassLimitation characterClassLimitation : allLimitations) {

            if (characterClassLimitation.ignoreWhenGenerating()) {
                if (characterClassLimitation.minOccurrences() > 0) {
                    throw new GenerationException(
                            "Character class is marked as ignored for generation, but has non-zero min occurrences: "
                                    + characterClassLimitation.getHumanReadableName());
                }
                continue;
            }

            allClassesIgnored = false;

            CharacterClass characterClass = characterClassLimitation.characterClass();

            int currentOccurrences = characterClass.countOccurrences(alreadyGenerated);
            int minOccurrences = characterClassLimitation.minOccurrences();
            int maxOccurrences = characterClassLimitation.effectiveMaxOccurrences();

            if (currentOccurrences > maxOccurrences) {
                // This should not happen, because the offending character should never be offered.
                throw new AssertionError("Exceeded occurrences for " + characterClass);
            } else if (currentOccurrences == maxOccurrences) {
                // We reached the upper limit. We cannot use characters from this class any more.
                disallowedCharacters.addAll(characterClass.characters);
            } else {
                // We are not forbidden to use characters this class by the upper limit,
                // put the class into the set of eligible classes.

                if (characterClassLimitation.mustBeFirst()) {
                    eligibleFirstClasses.add(characterClass);
                }

                eligibleClasses.add(characterClass);
                if (currentOccurrences < minOccurrences) {
                    // We did not reach the lower limit yet. We should prefer characters from this class.
                    preferredClasses.add(characterClass);
                }
            }
        }

        if (allClassesIgnored) {
            throw new GenerationException(
                    "Couldn't generate the value, all character classes are marked as ignored for generation");
        }

        Set<Character> resultSet;

        if (selectingFirstCharacter && hasFirstClassesSpecified) {
            // We have some restrictions on the first character!
            resultSet = intersection(eligibleFirstClasses);
            resultSet.removeAll(disallowedCharacters);
        } else {
            // We select the next characters, or there are no restrictions on the first one.
            Set<Character> eligibleCharacters = union(eligibleClasses);
            Set<Character> preferredCharacters = union(preferredClasses);
            eligibleCharacters.removeAll(disallowedCharacters);
            preferredCharacters.removeAll(disallowedCharacters);
            resultSet = !preferredCharacters.isEmpty() ? preferredCharacters : eligibleCharacters;
        }

        if (resultSet.isEmpty()) {
            throw new GenerationException(
                    "Couldn't generate any more characters, check the string policy. Already generated: " + alreadyGenerated);
        }
        return CharacterClass.unnamed(resultSet);
    }

    private boolean isGeneratedSequenceSatisfying(List<Character> generatedSequence) throws GenerationException {

        // First, global constraints.

        if (generatedSequence.size() > stringPolicy.getEffectiveMaxLength()) {
            throw new GenerationException(
                    "Couldn't cover minimal requirements given the maximum size limitation (%s characters)".formatted(
                            stringPolicy.getEffectiveMaxLength()));
        }

        if (generatedSequence.size() < plannedLength) {
            // This covers minLength and minUniqueChars. The latter is covered because the first "minUniqueCharacters" are
            // really unique, see getEligibleCharacters algorithm.
            return false; // continue generating
        }

        // Then, class-level constraints
        for (CharacterClassLimitation classLimitation : stringPolicy.getCharacterClassLimitations()) {

            CharacterClass characterClass = classLimitation.characterClass();

            int currentOccurrences = characterClass.countOccurrences(generatedSequence);

            if (currentOccurrences > classLimitation.effectiveMaxOccurrences()) {
                // This should not happen, see similar assertion in getEligibleCharacters
                throw new AssertionError("Exceeded occurrences for " + characterClass);
            }
            if (currentOccurrences < classLimitation.minOccurrences()) {
                return false; // continue generating
            }
        }

        return true;
    }

    private Set<Character> intersection(List<CharacterClass> classes) {
        Set<Character> rv = null;
        for (CharacterClass aClass : classes) {
            if (rv == null) {
                rv = new HashSet<>(aClass.characters);
            } else {
                rv.removeIf(c -> !aClass.characters.contains(c));
            }
        }
        return rv != null ? rv : Set.of();
    }

    private Set<Character> union(List<CharacterClass> classes) {
        Set<Character> rv = new HashSet<>();
        for (CharacterClass aClass : classes) {
            rv.addAll(aClass.characters);
        }
        return rv;
    }

    private boolean isUniquenessReached(List<Character> alreadyGenerated) {
        if (plannedUniqueCharacters <= 0) {
            return true;
        } else {
            var uniqueCharacters = new HashSet<>(alreadyGenerated);
            return uniqueCharacters.size() >= plannedUniqueCharacters;
        }
    }

    /** Internal exception to be thrown if the requested string cannot be generated. */
    static class GenerationException extends Exception {
        GenerationException(String message) {
            super(message);
        }
    }
}
