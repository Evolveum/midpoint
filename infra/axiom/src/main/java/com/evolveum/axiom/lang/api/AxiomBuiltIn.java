/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

public class AxiomBuiltIn {

    public static final Lazy<Map<AxiomIdentifier, AxiomItemDefinition>> EMPTY = Lazy.instant(ImmutableMap.of());
    public static final Lazy<AxiomItemDefinition> NO_ARGUMENT = Lazy.nullValue();


    private AxiomBuiltIn() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class Item implements AxiomItemDefinition {
        public static final Item NAME = new Item("name", Type.IDENTIFIER, true);
        public static final Item ARGUMENT = new Item("argument", Type.IDENTIFIER, false);
        public static final AxiomItemDefinition DOCUMENTATION = new Item("documentation", Type.STRING, true);
        public static final AxiomItemDefinition NAMESPACE = new Item("namespace", Type.STRING, true);
        public static final AxiomItemDefinition VERSION = new Item("version", Type.STRING, true);
        public static final AxiomItemDefinition TYPE_REFERENCE = new Item("type", Type.TYPE_REFERENCE, true);
        public static final AxiomItemDefinition TYPE_DEFINITION = new Item("type", Type.TYPE_DEFINITION, false);
        public static final AxiomItemDefinition SUPERTYPE_REFERENCE = new Item("extends", Type.TYPE_REFERENCE, false);
        public static final Item ROOT_DEFINITION = new Item("root", Type.ROOT_DEFINITION, false);
        public static final AxiomItemDefinition ITEM_DEFINITION = new Item("item", Type.ITEM_DEFINITION, false);
        public static final Item MODEL_DEFINITION = new Item("model", Type.MODEL, false);
        public static final AxiomItemDefinition MIN_OCCURS = new Item("minOccurs", Type.STRING, false);
        public static final AxiomItemDefinition MAX_OCCURS = new Item("maxOccurs", Type.STRING, false);
        public static final AxiomItemDefinition TARGET_TYPE = new Item("targetType", Type.IDENTIFIER, true);

        public static final AxiomItemDefinition IDENTIFIER_DEFINITION = new Item("identifier", Type.IDENTIFIER_DEFINITION, true);

        public static final AxiomItemDefinition ID_MEMBER = new Item("key", Type.STRING, false);
        public static final AxiomItemDefinition ID_SCOPE = new Item("scope", Type.STRING, false);
        public static final AxiomItemDefinition ID_SPACE = new Item("space", Type.IDENTIFIER, false);

        public static final AxiomItemDefinition TARGET = new Item("target", Type.TYPE_REFERENCE, true);

        private final AxiomIdentifier identifier;
        private final AxiomTypeDefinition type;
        private boolean required;


        private Item(String identifier, AxiomTypeDefinition type, boolean required) {
            this.identifier = AxiomIdentifier.axiom(identifier);
            this.type = type;
            this.required = required;
        }

        @Override
        public Optional<AxiomTypeDefinition> type() {
            return Optional.of(type);
        }

        @Override
        public AxiomIdentifier name() {
            return identifier;
        }

        @Override
        public String documentation() {
            return "";
        }


        @Override
        public AxiomTypeDefinition typeDefinition() {
            return type;
        }

        @Override
        public boolean required() {
            return required;
        }

        @Override
        public int minOccurs() {
            return 0;
        }

        @Override
        public int maxOccurs() {
            return Integer.MAX_VALUE;
        }

        @Override
        public String toString() {
            return AxiomItemDefinition.toString(this);
        }

        @Override
        public AxiomItemDefinition get() {
            return this;
        }
    }

    public static class Type implements AxiomTypeDefinition {
        public static final Type UUID = new Type("uuid");
        public static final Type STRING = new Type("string");
        public static final Type IDENTIFIER = new Type("AxiomIdentifier");
        public static final Type TYPE_REFERENCE = new Type("AxiomTypeReference");
        public static final Type BASE_DEFINITION =
                new Type("AxiomBaseDefinition", null, () -> Item.NAME, () -> itemDefs(
                        Item.NAME,
                        Item.DOCUMENTATION
                ));

        public static final Type MODEL =
                new Type("AxiomModel", BASE_DEFINITION,  () -> itemDefs(
                    Item.NAMESPACE,
                    Item.VERSION,
                    Item.TYPE_DEFINITION,
                    Item.ROOT_DEFINITION
                ));


        public static final Type TYPE_DEFINITION =
                new Type("AxiomTypeDefinition", BASE_DEFINITION, () -> itemDefs(
                    Item.ARGUMENT,
                    Item.IDENTIFIER_DEFINITION,
                    Item.SUPERTYPE_REFERENCE,
                    Item.ITEM_DEFINITION
                )) {
            @Override
            public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
                return TYPE_IDENTIFIER_DEFINITION.get();
            }
        };

        protected static final Lazy<Collection<AxiomIdentifierDefinition>> TYPE_IDENTIFIER_DEFINITION = Lazy.from(()->
            ImmutableSet.of(AxiomIdentifierDefinition.global(TYPE_DEFINITION.name(), Item.NAME)));


        public static final Type ITEM_DEFINITION =
                new Type("AxiomItemDefinition", BASE_DEFINITION, () -> itemDefs(
                    Item.TYPE_REFERENCE,
                    Item.MIN_OCCURS,
                    Item.MAX_OCCURS
                )) {

            @Override
            public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
                return ITEM_IDENTIFIER_DEFINITION.get();
            }
        };

        public static final Type ROOT_DEFINITION =
                new Type("AxiomRootDefinition", ITEM_DEFINITION, () -> itemDefs()) {

            @Override
            public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
                return ROOT_IDENTIFIER_DEFINITION.get();
            }
        };

        protected static final Lazy<Collection<AxiomIdentifierDefinition>> ITEM_IDENTIFIER_DEFINITION = Lazy.from(()->
        ImmutableSet.of(AxiomIdentifierDefinition.parent(ITEM_DEFINITION.name(), Item.NAME)));

        protected static final Lazy<Collection<AxiomIdentifierDefinition>> ROOT_IDENTIFIER_DEFINITION = Lazy.from(()->
        ImmutableSet.of(AxiomIdentifierDefinition.global(AxiomItemDefinition.ROOT_SPACE, Item.NAME)));


        public static final Type IDENTIFIER_DEFINITION =
                new Type("AxiomIdentifierDefinition", BASE_DEFINITION, () -> Item.ID_MEMBER, () -> itemDefs(
                    Item.ID_MEMBER,
                    Item.ID_SCOPE,
                    Item.ID_SPACE
                ));
        public static final Type IMPORT_DEFINITION = new Type("AxiomImportDeclaration");
        public static final Type EXTENSION_DEFINITION = new Type("AxiomExtensionDefinition");

        private final AxiomIdentifier identifier;
        private final AxiomTypeDefinition superType;
        private final Lazy<AxiomItemDefinition> argument;
        private final Lazy<Map<AxiomIdentifier, AxiomItemDefinition>> items;


        private Type(String identifier) {
            this(identifier, null, Lazy.nullValue(), EMPTY);
        }

        private Type(String identifier, Lazy.Supplier<Map<AxiomIdentifier, AxiomItemDefinition>> items) {
            this(identifier, null, Lazy.nullValue(), Lazy.from(items));
        }

        private Type(String identifier, AxiomTypeDefinition superType, Lazy.Supplier<Map<AxiomIdentifier, AxiomItemDefinition>> items) {
            this(identifier, superType, NO_ARGUMENT, Lazy.from(items));
        }


        private Type(String identifier, AxiomTypeDefinition superType, Lazy.Supplier<AxiomItemDefinition> argument,
                Lazy.Supplier<Map<AxiomIdentifier, AxiomItemDefinition>> items) {
            this(identifier, superType, Lazy.from(argument), Lazy.from(items));
        }

        private Type(String identifier, AxiomTypeDefinition superType, Lazy<AxiomItemDefinition> argument,
                Lazy<Map<AxiomIdentifier, AxiomItemDefinition>> items) {
            this.identifier = AxiomIdentifier.axiom(identifier);
            this.argument = argument;
            this.superType = superType;
            this.items = items;
        }

        @Override
        public AxiomIdentifier name() {
            return identifier;
        }

        @Override
        public String documentation() {
            return "";
        }

        @Override
        public Optional<AxiomTypeDefinition> superType() {
            return Optional.ofNullable(superType);
        }

        @Override
        public Map<AxiomIdentifier, AxiomItemDefinition> itemDefinitions() {
            return items.get();
        }

        private static Map<AxiomIdentifier, AxiomItemDefinition> itemDefs(AxiomItemDefinition... items) {
            Builder<AxiomIdentifier, AxiomItemDefinition> builder = ImmutableMap.builder();

            for (AxiomItemDefinition item : items) {
                builder.put(item.name(), item);
            }
            return builder.build();
        }

        @Override
        public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
            return Collections.emptyList();
        }

        @Override
        public Optional<AxiomItemDefinition> argument() {
            if(argument.get() != null) {
                return Optional.of(argument.get());
            }
            if(superType != null) {
                return superType.argument();
            }
            return Optional.empty();
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return "typedef " + name();
        }

    }


}
