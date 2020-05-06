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


    public enum Item implements AxiomItemDefinition {
        IDENTIFIER("identifier", Type.IDENTIFIER, true),
        ARGUMENT("argument", Type.IDENTIFIER, false),
        DOCUMENTATION("documentation", Type.STRING, true),
        NAMESPACE("namespace", Type.STRING, true),
        VERSION("version", Type.STRING, true),
        TYPE_REFERENCE("type", Type.IDENTIFIER, true),
        TYPE_DEFINITION("type", Type.TYPE_DEFINITION, false),
        OBJECT_DEFINITION("object", Type.OBJECT_DEFINITION, false),
        PROPERTY_DEFINITION("property", Type.ITEM_DEFINITION, false),
        CONTAINER_DEFINITION("container", Type.ITEM_DEFINITION, false),
        OBJECT_REFERENCE_DEFINITION("objectReference", Type.OBJECT_REFERENCE_DEFINITION, false),
        MODEL_DEFINITION("model", Type.MODEL, false),
        ITEM_NAME("itemName", Type.IDENTIFIER, false),
        MIN_OCCURS("minOccurs", Type.STRING, false),
        MAX_OCCURS("maxOccurs", Type.STRING, false),
        TARGET_TYPE("targetType", Type.IDENTIFIER, true)
        ;
        private AxiomIdentifier identifier;
        private AxiomTypeDefinition type;
        private boolean required;


        private Item(String identifier, AxiomTypeDefinition type, boolean required) {
            this.identifier = AxiomIdentifier.axiom(identifier);
            this.type = type;
            this.required = required;
        }

        @Override
        public AxiomIdentifier identifier() {
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
    }

    public enum Type implements AxiomTypeDefinition {
        UUID("uuid"),
        STRING("string"),
        IDENTIFIER("AxiomIdentifier"),

        BASE_DEFINITION("AxiomBaseDefinition", null, () -> Item.IDENTIFIER, () -> itemDefs(
                Item.IDENTIFIER,
                Item.DOCUMENTATION
                )),

        MODEL("AxiomModel", BASE_DEFINITION,  () -> itemDefs(
                Item.NAMESPACE,
                Item.VERSION,
                Item.TYPE_DEFINITION,
                Item.OBJECT_DEFINITION
                )),
        TYPE_DEFINITION("AxiomTypeDefinition", BASE_DEFINITION, () -> itemDefs(
                Item.PROPERTY_DEFINITION,
                Item.CONTAINER_DEFINITION,
                Item.OBJECT_REFERENCE_DEFINITION
                )),
        ITEM_DEFINITION("AxiomItemDefinition", BASE_DEFINITION, () -> itemDefs(
                Item.TYPE_REFERENCE,
                Item.MIN_OCCURS,
                Item.MAX_OCCURS
                )),
        OBJECT_DEFINITION("AxiomObjectDefinition", TYPE_DEFINITION, () -> itemDefs(
                Item.ITEM_NAME
                )),
        OBJECT_REFERENCE_DEFINITION("AxiomObjectReferenceDefinition", ITEM_DEFINITION, () -> itemDefs(
                Item.TARGET_TYPE
                ))
        ;

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
        public AxiomIdentifier identifier() {
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
                builder.put(item.identifier(), item);
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
