/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.concepts.Lazy.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomBuiltIn {

    public static final Lazy<Map<AxiomIdentifier, AxiomItemDefinition>> EMPTY = Lazy.instant(ImmutableMap.of());
    public static final Lazy<AxiomItemDefinition> NO_ARGUMENT = Lazy.nullValue();

    private AxiomBuiltIn() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class Item implements AxiomItemDefinition {
        public static final AxiomItemDefinition NAME = new Item("name", Type.IDENTIFIER, true);
        public static final AxiomItemDefinition IDENTIFIER = new Item("identifier", Type.IDENTIFIER, true);
        public static final AxiomItemDefinition ARGUMENT = new Item("argument", Type.IDENTIFIER, false);
        public static final AxiomItemDefinition DOCUMENTATION = new Item("documentation", Type.STRING, true);
        public static final AxiomItemDefinition NAMESPACE = new Item("namespace", Type.STRING, true);
        public static final AxiomItemDefinition VERSION = new Item("version", Type.STRING, true);
        public static final AxiomItemDefinition TYPE_REFERENCE = new Item("type", Type.TYPE_REFERENCE, true);
        public static final AxiomItemDefinition TYPE_DEFINITION = new Item("type", Type.TYPE_DEFINITION, false);
        public static final AxiomItemDefinition SUPERTYPE_REFERENCE = new Item("extends", Type.TYPE_REFERENCE, false);
        public static final AxiomItemDefinition ROOT_DEFINITION = new Item("root", Type.ITEM_DEFINITION, false);
        public static final AxiomItemDefinition OBJECT_DEFINITION = new Item("object", Type.OBJECT_DEFINITION, false);
        public static final AxiomItemDefinition REFERENCE_DEFINITION = new Item("reference", Type.ITEM_DEFINITION, false);
        public static final AxiomItemDefinition ITEM_DEFINITION = new Item("item", Type.ITEM_DEFINITION, false);
        public static final AxiomItemDefinition OBJECT_REFERENCE_DEFINITION = new Item("objectReference", Type.OBJECT_REFERENCE_DEFINITION, false);
        public static final AxiomItemDefinition MODEL_DEFINITION = new Item("model", Type.MODEL, false);
        public static final AxiomItemDefinition ITEM_NAME = new Item("itemName", Type.IDENTIFIER, false);
        public static final AxiomItemDefinition MIN_OCCURS = new Item("minOccurs", Type.STRING, false);
        public static final AxiomItemDefinition MAX_OCCURS = new Item("maxOccurs", Type.STRING, false);
        public static final AxiomItemDefinition TARGET_TYPE = new Item("targetType", Type.IDENTIFIER, true);

        private final AxiomIdentifier identifier;
        private final AxiomTypeDefinition type;
        private boolean required;


        private Item(String identifier, AxiomTypeDefinition type, boolean required) {
            this.identifier = AxiomIdentifier.axiom(identifier);
            this.type = type;
            this.required = required;
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
        public AxiomTypeDefinition type() {
            return type;
        }

        @Override
        public boolean required() {
            return required;
        }

        @Override
        public String toString() {
            return AxiomItemDefinition.toString(this);
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
                    Item.OBJECT_DEFINITION,
                    Item.ROOT_DEFINITION
                ));
        public static final Type TYPE_DEFINITION =
                new Type("AxiomTypeDefinition", BASE_DEFINITION, () -> itemDefs(
                    Item.ARGUMENT,
                    Item.SUPERTYPE_REFERENCE,
                    Item.ITEM_DEFINITION,
                    Item.OBJECT_REFERENCE_DEFINITION
                ));
        public static final Type ITEM_DEFINITION =
                new Type("AxiomItemDefinition", BASE_DEFINITION, () -> itemDefs(
                    Item.TYPE_REFERENCE,
                    Item.MIN_OCCURS,
                    Item.MAX_OCCURS
                ));
        public static final Type REFERENCE_DEFINITION =
                new Type("AxiomReferenceDefinition", ITEM_DEFINITION, () -> itemDefs(

                ));
        public static final Type OBJECT_DEFINITION =
                new Type("AxiomObjectDefinition", TYPE_DEFINITION, () -> itemDefs(
                        Item.ITEM_NAME
                ));
        public static final Type OBJECT_REFERENCE_DEFINITION =
                new Type("AxiomObjectReferenceDefinition", ITEM_DEFINITION, () -> itemDefs(
                        Item.TARGET_TYPE
                ));

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
        public Map<AxiomIdentifier, AxiomItemDefinition> items() {
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
        public Optional<AxiomItemDefinition> argument() {
            if(argument.get() != null) {
                return Optional.of(argument.get());
            }
            if(superType != null) {
                return superType.argument();
            }
            return Optional.empty();
        }

    }


}
