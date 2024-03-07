/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.ParticipantIndex.FIRST;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.ParticipantIndex.SECOND;

/**
 * Defines a class (type) of links between two participating sets of objects (also called "link class participants").
 *
 * Some examples:
 *
 * - Simple "account-group membership" link between accounts and groups.
 * - Generalized "object-group membership" link between any object classes (accounts, groups, orgs, ...) and groups.
 * That is, a group can contain objects of many classes.
 * - "Manager-subordinate" link between accounts.
 *
 * Hence, the "set" in the definition above consists of one or more object classes. Smaller sets, covering e.g. only a port
 * of an object class, are not supported.
 *
 * Individual participants are equal. We call them simply "first" and "second".
 *
 * This is to model associations in midPoint that appear in 4.9.
 *
 * TODO Decide on the name of this class. Currently, it is intentionally not called "association". Maybe it will get that name
 *  in the future.
 */
public class LinkClassDefinition {

    /** Name of this link class. Must be resource-wide unique. */
    @NotNull private final String name;

    /** Definition of the first participant of this link. */
    @NotNull private final Participant firstParticipant;

    /** Definition of the first participant of this link. */
    @NotNull private final Participant secondParticipant;

    private LinkClassDefinition(
            @NotNull String name, @NotNull Participant firstParticipant, @NotNull Participant secondParticipant) {
        this.name = name;
        this.firstParticipant = firstParticipant;
        this.secondParticipant = secondParticipant;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Participant getFirstParticipant() {
        return firstParticipant;
    }

    public @NotNull Participant getSecondParticipant() {
        return secondParticipant;
    }

    public List<LinkDefinition> getLinkDefinitions(@NotNull String objectClassName, @NotNull String linkName) {
        var first = firstParticipant.getLinkDefinition(objectClassName, linkName, this, FIRST);
        var second = secondParticipant.getLinkDefinition(objectClassName, linkName, this, SECOND);
        if (first != null) {
            if (second != null) {
                return List.of(first, second);
            } else {
                return List.of(first);
            }
        } else if (second != null) {
            return List.of(second);
        } else {
            return List.of();
        }
    }

    public static class Participant {

        /** Names of object classes comprising this participant. Never empty. */
        @NotNull private final Set<String> objectClassNames;

        /**
         * Name of the link (~ attribute) under which this link is presented in the participating objects.
         * Null if the link class is not visible in this participant.
         */
        @Nullable private final String linkName;

        /** Is the link updatable from this side? Requires {@link #linkName} to be non-null. */
        private final boolean updatable;

        /** Is the link returned by default at this side? Requires {@link #linkName} to be non-null. */
        private final boolean returnedByDefault;

        /** Are the references for this link expanded by default? Requires {@link #linkName} to be non-null. */
        private final boolean expandedByDefault;

        /**
         * Minimal cardinality of the link from this side. It can be set regardless of whether {@link #linkName} is present.
         * It is interpreted of the required number of values of that attribute, should it be visible on the participant.
         */
        private final int minOccurs;

        /** Maximal cardinality, see {@link #minOccurs}. */
        private final int maxOccurs;

        Participant(
                @NotNull Set<String> objectClassNames,
                @Nullable String linkName,
                boolean updatable,
                boolean returnedByDefault,
                boolean expandedByDefault,
                int minOccurs,
                int maxOccurs) {
            this.objectClassNames = objectClassNames;
            this.linkName = linkName;
            this.updatable = updatable;
            this.returnedByDefault = returnedByDefault;
            this.expandedByDefault = expandedByDefault;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        public @NotNull Set<String> getObjectClassNames() {
            return objectClassNames;
        }

        public @Nullable String getLinkName() {
            return linkName;
        }

        public boolean isUpdatable() {
            return updatable;
        }

        public boolean isReturnedByDefault() {
            return returnedByDefault;
        }

        public boolean isExpandedByDefault() {
            return expandedByDefault;
        }

        public int getMinOccurs() {
            return minOccurs;
        }

        public int getMaxOccurs() {
            return maxOccurs;
        }

        @Nullable LinkDefinition getLinkDefinition(
                String objectClassName, String linkName,
                LinkClassDefinition classDefinition, ParticipantIndex participantIndex) {
            if (objectClassNames.contains(objectClassName)
                    && this.linkName != null
                    && this.linkName.equals(linkName)) {
                return new LinkDefinition(classDefinition, participantIndex);
            } else {
                return null;
            }
        }

        /** Currently we cascade the delete only if the cardinality is min=1, max=1 (so we don't need to count remaining links. */
        boolean isCascadeDelete() {
            return minOccurs == 1 && maxOccurs == 1;
        }

        public static final class ParticipantBuilder {
            private Set<String> objectClassNames;
            private String attributeName;
            private boolean updatable;
            private boolean returnedByDefault;
            private boolean expandedByDefault;
            private int minOccurs;
            private int maxOccurs;

            private ParticipantBuilder() {
            }

            public static ParticipantBuilder aParticipant() {
                return new ParticipantBuilder();
            }

            public ParticipantBuilder withObjectClassNames(Set<String> objectClassNames) {
                this.objectClassNames = objectClassNames;
                return this;
            }

            public ParticipantBuilder withAttributeName(String attributeName) {
                this.attributeName = attributeName;
                return this;
            }

            public ParticipantBuilder withUpdatable(boolean updatable) {
                this.updatable = updatable;
                return this;
            }

            public ParticipantBuilder withReturnedByDefault(boolean returnedByDefault) {
                this.returnedByDefault = returnedByDefault;
                return this;
            }

            public ParticipantBuilder withExpandedByDefault(boolean expandedByDefault) {
                this.expandedByDefault = expandedByDefault;
                return this;
            }

            public ParticipantBuilder withMinOccurs(int minOccurs) {
                this.minOccurs = minOccurs;
                return this;
            }

            public ParticipantBuilder withMaxOccurs(int maxOccurs) {
                this.maxOccurs = maxOccurs;
                return this;
            }

            public Participant build() {
                return new Participant(
                        objectClassNames, attributeName, updatable, returnedByDefault, expandedByDefault, minOccurs, maxOccurs);
            }
        }
    }

    public static final class LinkClassDefinitionBuilder {
        private String name;
        private Participant firstParticipant;
        private Participant secondParticipant;

        private LinkClassDefinitionBuilder() {
        }

        public static LinkClassDefinitionBuilder aLinkClassDefinition() {
            return new LinkClassDefinitionBuilder();
        }

        public LinkClassDefinitionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public LinkClassDefinitionBuilder withFirstParticipant(Participant firstParticipant) {
            this.firstParticipant = firstParticipant;
            return this;
        }

        public LinkClassDefinitionBuilder withSecondParticipant(Participant secondParticipant) {
            this.secondParticipant = secondParticipant;
            return this;
        }

        public LinkClassDefinition build() {
            return new LinkClassDefinition(name, firstParticipant, secondParticipant);
        }
    }

    public enum ParticipantIndex {
        FIRST, SECOND
    }
}
