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
import java.util.Objects;
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
 * Hence, the "set" in the definition above consists of one or more object classes. Smaller sets, covering e.g. only a part
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

    @NotNull List<LinkDefinition> getLinkDefinitions() {
        return List.of(
                new LinkDefinition(this, FIRST),
                new LinkDefinition(this, SECOND));
    }

    @NotNull Participant getFirstParticipant() {
        return firstParticipant;
    }

    @NotNull Participant getSecondParticipant() {
        return secondParticipant;
    }

    public static class Participant {

        /** Names of object classes comprising this participant. Never empty. */
        @NotNull private final Set<String> objectClassNames;

        /**
         * Name of the link (~ attribute) under which this link is presented in the participating objects.
         * Null if the link is not visible on this participant.
         */
        @Nullable private final String linkName;

        /** Is the link updatable from this side? Requires {@link #linkName} to be non-null. */
        private final boolean updatable;

        /** Is the link returned by default at this side? Requires {@link #linkName} to be non-null. */
        private final boolean returnedByDefault;

        /** Are the references for this link expanded by default? Requires {@link #linkName} to be non-null. */
        private final boolean expandedByDefault;

        /**
         * If `true`, the references provided by the connector through this link have no object class information.
         * This is to test this situation e.g. in LDAP groups.
         */
        private final boolean providingUnclassifiedReferences;

        /**
         * Minimal cardinality of the link from this side. It can be set regardless of whether {@link #linkName} is present.
         * It is interpreted of the required number of values of that attribute, should it be visible on the participant.
         */
        private final int minOccurs;

        /** Maximal cardinality, see {@link #minOccurs}. */
        private final int maxOccurs;

        /**
         * - If `true`, the link is using `EmbeddedObject` instances when accessed via ConnId.
         * - If `false`, the link is using `ConnectorObjectReference` instead.
         *
         * The difference is that the latter require name and UID attributes.
         * The former style is newer on (ConnId 1.7+) and is generally preferred.
         */
        private final boolean usingEmbeddedObjects;

        Participant(
                @NotNull Set<String> objectClassNames,
                @Nullable String linkName,
                boolean updatable,
                boolean returnedByDefault,
                boolean expandedByDefault,
                boolean providingUnclassifiedReferences,
                int minOccurs,
                int maxOccurs,
                boolean usingEmbeddedObjects) {
            this.objectClassNames = Objects.requireNonNull(objectClassNames, "object class name");
            this.linkName = linkName;
            this.updatable = updatable;
            this.returnedByDefault = returnedByDefault;
            this.expandedByDefault = expandedByDefault;
            this.providingUnclassifiedReferences = providingUnclassifiedReferences;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
            this.usingEmbeddedObjects = usingEmbeddedObjects;
        }

        @NotNull Set<String> getObjectClassNames() {
            return objectClassNames;
        }

        public @NotNull String getLinkNameRequired() {
            return Objects.requireNonNull(linkName);
        }

        @SuppressWarnings("unused")
        public boolean isUpdatable() {
            return updatable;
        }

        public boolean isReturnedByDefault() {
            return returnedByDefault;
        }

        public boolean isExpandedByDefault() {
            return expandedByDefault;
        }

        public boolean isProvidingUnclassifiedReferences() {
            return providingUnclassifiedReferences;
        }

        public int getMinOccurs() {
            return minOccurs;
        }

        public int getMaxOccurs() {
            return maxOccurs;
        }

        public boolean isUsingEmbeddedObjects() {
            return usingEmbeddedObjects;
        }

        public String getSingleObjectClassNameIfApplicable() {
            if (objectClassNames.size() == 1) {
                return objectClassNames.iterator().next();
            } else {
                return null;
            }
        }

        /** Currently we cascade the delete only if the cardinality is min=1, max=1 (so we don't need to count remaining links. */
        boolean isCascadeDelete() {
            return minOccurs == 1 && maxOccurs == 1;
        }

        public boolean isVisible() {
            return linkName != null;
        }

        public static final class ParticipantBuilder {
            private Set<String> objectClassNames;
            private String attributeName;
            private boolean updatable;
            private boolean returnedByDefault;
            private boolean expandedByDefault;
            private boolean providingUnclassifiedReferences;
            private int minOccurs;
            private int maxOccurs;
            private boolean usingEmbeddedObjects;

            private ParticipantBuilder() {
            }

            public static ParticipantBuilder aParticipant() {
                return new ParticipantBuilder();
            }

            @SuppressWarnings("unused")
            public ParticipantBuilder withObjectClassNames(Set<String> objectClassNames) {
                this.objectClassNames = objectClassNames;
                return this;
            }

            public ParticipantBuilder withObjectClassNames(String... objectClassNames) {
                this.objectClassNames = Set.of(objectClassNames);
                return this;
            }

            public ParticipantBuilder withLinkAttributeName(String name) {
                this.attributeName = name;
                return this;
            }

            @SuppressWarnings("unused")
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

            public ParticipantBuilder withProvidingUnclassifiedReferences(boolean providingUnclassifiedReferences) {
                this.providingUnclassifiedReferences = providingUnclassifiedReferences;
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

            public ParticipantBuilder withUsingEmbeddedObjects(boolean usingEmbeddedObjects) {
                this.usingEmbeddedObjects = usingEmbeddedObjects;
                return this;
            }

            public Participant build() {
                return new Participant(
                        objectClassNames, attributeName, updatable,
                        returnedByDefault, expandedByDefault, providingUnclassifiedReferences,
                        minOccurs, maxOccurs, usingEmbeddedObjects);
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
        FIRST(1), SECOND(2);

        private final int order;

        ParticipantIndex(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }
}
