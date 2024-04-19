/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.AbstractTypeDefinition;
import com.evolveum.midpoint.prism.impl.schema.SchemaDomSerializer;
import com.evolveum.midpoint.prism.schema.SerializableDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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

    /** Definitions of native object classes and associations. */
    @NotNull private final Map<String, NativeComplexTypeDefinitionImpl> nativeComplexTypeDefinitionMap =
            new ConcurrentHashMap<>();

    @Override
    public NativeObjectClassDefinition findObjectClassDefinition(@NotNull QName objectClassName) {
        return checkType(findInMap(objectClassName), false);
    }

    @Override
    public NativeAssociationClassDefinition findAssociationClassDefinition(@NotNull QName name) {
        return checkType(findInMap(name), true);
    }

    private NativeComplexTypeDefinitionImpl checkType(NativeComplexTypeDefinitionImpl definition, boolean association) {
        if (definition == null || definition.isAssociation() == association) {
            return definition;
        } else {
            throw new IllegalStateException(
                    "Expected " + (association ? "association" : "object") + " class definition, but got " + definition);
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
                .filter(def -> !def.isAssociation())
                .toList();
    }

    @Override
    public @NotNull Collection<? extends NativeAssociationClassDefinition> getAssociationClassDefinitions() {
        return nativeComplexTypeDefinitionMap.values().stream()
                .filter(def -> def.isAssociation())
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

    /** Transforms references between object classes into a collection of association classes. */
    @Override
    public void computeAssociationClasses() throws SchemaException {
        new AssociationClassesComputer().compute();
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

    private class AssociationClassesComputer {
        void compute() throws SchemaException {

            // Creating the association class definitions
            nativeComplexTypeDefinitionMap.entrySet().removeIf(entry -> entry.getValue().isAssociation());
            for (var objectClassDefinition : getObjectClassDefinitions()) {
                for (var associationDefinition : objectClassDefinition.getAssociationDefinitions()) {
                    var associationClassName = associationDefinition.getTypeName();
                    stateCheck(NS_RI.equals(associationClassName.getNamespaceURI()),
                            "Association class name must be in the RI namespace: %s", associationClassName);
                    findOrCreateAssociationClassDefinition(associationClassName)
                            .addParticipant(
                                    objectClassDefinition.getName(),
                                    associationDefinition.getItemName(),
                                    associationDefinition.getAssociationParticipantRole());
                }
            }

            // Checking the consistency of the association class definitions
            for (var associationClassDefinition : getAssociationClassDefinitions()) {
                MiscUtil.schemaCheck(!associationClassDefinition.getSubjects().isEmpty(),
                        "Association class %s has no subject classes", associationClassDefinition);
                MiscUtil.schemaCheck(!associationClassDefinition.getObjects().isEmpty(),
                        "Association class %s has no object classes", associationClassDefinition);
            }
        }

        private NativeAssociationClassDefinition findOrCreateAssociationClassDefinition(QName typeName) {
            var existingClass = findAssociationClassDefinition(typeName);
            if (existingClass != null) {
                return existingClass;
            }
            String localTypeName = typeName.getLocalPart();
            var newClass = new NativeComplexTypeDefinitionImpl(localTypeName);
            newClass.setAssociation();
            nativeComplexTypeDefinitionMap.put(typeName.getLocalPart(), newClass);
            return newClass;
        }
    }
}
