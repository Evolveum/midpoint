/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.AbstractTypeDefinition;
import com.evolveum.midpoint.prism.impl.schema.SchemaDomSerializer;
import com.evolveum.midpoint.prism.schema.SerializableDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.google.common.collect.ArrayListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.Definition.DefinitionBuilder;
import com.evolveum.midpoint.prism.schema.ItemDefinitionSupplier;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema.NativeResourceSchemaBuilder;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

public class NativeResourceSchemaImpl
        extends AbstractFreezable
        implements NativeResourceSchema, NativeResourceSchemaBuilder {

    /** Definitions of native object classes and reference types. */
    @NotNull private final Map<String, NativeComplexTypeDefinitionImpl> nativeComplexTypeDefinitionMap =
            new ConcurrentHashMap<>();

    @Override
    public NativeObjectClassDefinition findObjectClassDefinition(@NotNull QName objectClassName) {
        return checkType(findInMap(objectClassName), false);
    }

    @Override
    public NativeReferenceTypeDefinition findReferenceTypeDefinition(@NotNull QName name) {
        return checkType(findInMap(name), true);
    }

    private NativeComplexTypeDefinitionImpl checkType(NativeComplexTypeDefinitionImpl definition, boolean referenceType) {
        if (definition == null || definition.isReferenceType() == referenceType) {
            return definition;
        } else {
            throw new IllegalStateException(
                    "Expected " + (referenceType ? "reference type" : "object class") + " definition, but got " + definition);
        }
    }

    private NativeComplexTypeDefinitionImpl findInMap(@NotNull QName name) {
        var ns = name.getNamespaceURI();
        if (StringUtils.hasLength(ns) && !NS_RI.equals(ns)) {
            return null;
        } else {
            return nativeComplexTypeDefinitionMap.get(name.getLocalPart());
        }
    }

    @Override
    public @NotNull Collection<? extends NativeObjectClassDefinition> getObjectClassDefinitions() {
        return nativeComplexTypeDefinitionMap.values().stream()
                .filter(def -> !def.isReferenceType())
                .toList();
    }

    @Override
    public @NotNull Collection<? extends NativeReferenceTypeDefinition> getReferenceTypeDefinitions() {
        return nativeComplexTypeDefinitionMap.values().stream()
                .filter(def -> def.isReferenceType())
                .toList();
    }

    @Override
    public @NotNull Collection<? extends SerializableDefinition> getDefinitionsToSerialize() {
        return Collections.unmodifiableCollection(nativeComplexTypeDefinitionMap.values());
    }

    @Override
    public int size() {
        return nativeComplexTypeDefinitionMap.size();
    }

    @Override
    public @NotNull NativeObjectClassDefinitionBuilder newComplexTypeDefinitionLikeBuilder(String localTypeName) {
        return new NativeComplexTypeDefinitionImpl(localTypeName);
    }

    @Override
    public @NotNull NativeResourceSchema getObjectBuilt() {
        return this;
    }

    /** Transforms references between object classes into a collection of reference types. */
    @Override
    public void computeReferenceTypes() throws SchemaException {
        new ReferenceTypesComputer().compute();
    }

    @Override
    public void add(@NotNull DefinitionBuilder builder) {
        if (builder.getObjectBuilt() instanceof NativeComplexTypeDefinitionImpl nativeComplexTypeDefinition) {
            addComplexTypeDefinition(nativeComplexTypeDefinition);
        } else {
            throw new IllegalStateException(
                    "Only native object class definitions can be added to a native resource schema; got " + builder);
        }
    }

    private void addComplexTypeDefinition(NativeComplexTypeDefinitionImpl def) {
        String name = def.getName();
        if (nativeComplexTypeDefinitionMap.put(name, def) != null) {
            throw new IllegalStateException("Native object class definition with name " + name + " already exists in " + this);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public @NotNull NativeResourceSchemaImpl clone() {
        var clone = new NativeResourceSchemaImpl();
        nativeComplexTypeDefinitionMap.forEach(
                (key, value) -> clone.nativeComplexTypeDefinitionMap.put(key, value.clone()));
        return clone;
    }

    @Override
    public @NotNull Document serializeToXsd() throws SchemaException {
        return new SchemaDomSerializer(this).serializeSchema();
    }

    @Override
    public @NotNull String getNamespace() {
        return NS_RI;
    }

    @Override
    public AbstractTypeDefinition findTypeDefinitionByType(@NotNull QName typeName) {
        return nativeComplexTypeDefinitionMap.get(
                QNameUtil.getLocalPartCheckingNamespace(typeName, NS_RI));
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public void setRuntime(boolean value) {
        // TEMPORARY --- IGNORE FOR NOW
    }

    @Override
    public void addDelayedItemDefinition(ItemDefinitionSupplier supplier) {
        throw new UnsupportedOperationException("Delayed item definitions are not supported in native resource schema");
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(this + "\n", indent);
        DebugUtil.debugDumpMapMultiLine(sb, nativeComplexTypeDefinitionMap, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Native resource schema: %d complex types%s".formatted(
                nativeComplexTypeDefinitionMap.size(), isMutable() ? " (mutable)" : "");
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        nativeComplexTypeDefinitionMap.values().forEach(NativeComplexTypeDefinitionImpl::freeze);
    }

    private class ReferenceTypesComputer {

        void compute() throws SchemaException {

            // Creating the reference type definitions
            nativeComplexTypeDefinitionMap.entrySet().removeIf(entry -> entry.getValue().isReferenceType());

            // This is what we learn from "referencedObjectClassName" information
            // Keyed by the participant, values are (local) names of object classes
            var knownParticipatingObjectClasses = ArrayListMultimap.<ReferenceTypeParticipant, String>create();

            for (var objectClassDefinition : getObjectClassDefinitions()) {
                for (var referenceAttributeDefinition : objectClassDefinition.getReferenceAttributeDefinitions()) {
                    var referenceTypeName = referenceAttributeDefinition.getTypeName();
                    stateCheck(NS_RI.equals(referenceTypeName.getNamespaceURI()),
                            "Reference type name must be in the RI namespace: %s", referenceTypeName);
                    var role = referenceAttributeDefinition.getReferenceParticipantRole();
                    findOrCreateReferenceTypeDefinition(referenceTypeName)
                            .addParticipant(
                                    objectClassDefinition.getName(),
                                    referenceAttributeDefinition.getItemName(),
                                    role);
                    var referencedObjectClassName = referenceAttributeDefinition.getReferencedObjectClassName();
                    if (referencedObjectClassName != null) {
                        stateCheck(
                                NS_RI.equals(referencedObjectClassName.getNamespaceURI()),
                                "Referenced object class name must be in the RI namespace: %s",
                                referencedObjectClassName);
                        knownParticipatingObjectClasses.put(
                                new ReferenceTypeParticipant(referenceTypeName, role.other()),
                                referencedObjectClassName.getLocalPart());
                    }
                }
            }

            // Adding object classes learned from "referencedObjectClassName"
            for (var entry : knownParticipatingObjectClasses.entries()) {
                var existingRefType = Objects.requireNonNull(findReferenceTypeDefinition(entry.getKey().referenceTypeName));
                existingRefType.addParticipantIfNotThere(entry.getValue(), entry.getKey().role);
            }

            // Checking the consistency of the reference type definitions
            for (var referenceTypeDefinition : getReferenceTypeDefinitions()) {
                MiscUtil.schemaCheck(!referenceTypeDefinition.getSubjects().isEmpty(),
                        "Reference type %s has no subject classes", referenceTypeDefinition);
                MiscUtil.schemaCheck(!referenceTypeDefinition.getObjects().isEmpty(),
                        "Reference type %s has no object classes", referenceTypeDefinition);
            }
        }

        private NativeReferenceTypeDefinition findOrCreateReferenceTypeDefinition(QName typeName) {
            var existingRefType = findReferenceTypeDefinition(typeName);
            if (existingRefType != null) {
                return existingRefType;
            }
            String localTypeName = typeName.getLocalPart();
            var newClass = new NativeComplexTypeDefinitionImpl(localTypeName);
            newClass.setReferenceType();
            nativeComplexTypeDefinitionMap.put(typeName.getLocalPart(), newClass);
            return newClass;
        }

        // referenceTypeName is fully qualified
        record ReferenceTypeParticipant(@NotNull QName referenceTypeName, @NotNull ShadowReferenceParticipantRole role) {
        }
    }
}
