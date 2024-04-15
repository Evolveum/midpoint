/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Definition.DefinitionBuilder;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;
import com.evolveum.midpoint.schema.processor.ResourceSchema.ResourceSchemaMutator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * Direct implementation of {@link ResourceSchema} interface.
 *
 * It contains only the "prismified" (refined) object classes and types definitions.
 * Native definitions were moved out to {@link NativeResourceSchema} objects.
 *
 * @author semancik
 */
public class ResourceSchemaImpl
        extends PrismSchemaImpl
        implements ResourceSchema, ResourceSchemaMutator {

    /** Contains native (non-refined) object class and their items' definitions. Immutable. */
    @NotNull final NativeResourceSchema nativeSchema;

    /**
     * All known association classes, native or simulated. Indexed by local name.
     * Note that these are not among the definitions, because they are not "prismified".
     *
     * The values should be immutable.
     *
     * TODO make the collection freezable
     */
    @Experimental
    @NotNull private final Map<String, ShadowAssociationClassImplementation> associationClassImplementationsMap = new HashMap<>();

    /**
     * All known association types, based on native or simulated association classes. Indexed by the local name.
     * Note that these are not among the definitions, because they are not "prismified".
     *
     * The values should be immutable.
     *
     * TODO make the collection freezable
     */
    @Experimental
    @NotNull private final Map<String, ShadowAssociationClassDefinition> associationTypeDefinitionsMap = new HashMap<>();

    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    @NotNull private final LayerType currentLayer;

    ResourceSchemaImpl(@NotNull NativeResourceSchema nativeSchema) {
        this(nativeSchema, DEFAULT_LAYER);
    }

    ResourceSchemaImpl(@NotNull NativeResourceSchema nativeSchema, @NotNull LayerType currentLayer) {
        super(MidPointConstants.NS_RI);
        this.nativeSchema = nativeSchema;
        this.currentLayer = currentLayer;
    }

    @Override
    public @NotNull NativeObjectClassDefinitionBuilder newComplexTypeDefinitionLikeBuilder(String localTypeName) {
        throw new UnsupportedOperationException("This object cannot be created by parsing XSD or similar means");
    }

    @Override
    public void add(@NotNull DefinitionBuilder builder) {
        throw new UnsupportedOperationException("This object cannot be created by parsing XSD or similar means");
    }

    @Override
    public void add(@NotNull Definition def) {
        argCheck(def instanceof ResourceObjectDefinition,
                "Only resource object definitions can be added to a resource schema: %s", def);
        super.add(def);
    }

    @Override
    public ResourceSchemaMutator mutator() {
        return this;
    }

    /** We do not want to serialize this schema to XSD. */
    @Override
    public @NotNull Document serializeToXsd() throws SchemaException {
        throw new UnsupportedOperationException("Resource schema cannot be serialized to XSD. Only native schema can.");
    }

    @Override
    public @NotNull Document serializeNativeToXsd() throws SchemaException {
        return nativeSchema.serializeToXsd();
    }

    void addAssociationClassImplementation(@NotNull ShadowAssociationClassImplementation associationTypeImplementation)
            throws ConfigurationException {
        var existing = associationClassImplementationsMap.put(
                associationTypeImplementation.getName(), associationTypeImplementation);
        configCheck(existing == null,
                "Duplicate definition of association class %s in %s",
                associationTypeImplementation.getName(), this);
    }

    @Nullable ShadowAssociationClassImplementation getAssociationClassImplementation(@NotNull String name) {
        return associationClassImplementationsMap.get(name);
    }

    @NotNull Collection<ShadowAssociationClassImplementation> getAssociationClassImplementations() {
        return associationClassImplementationsMap.values();
    }

    void addAssociationTypeDefinition(@NotNull ShadowAssociationClassDefinition associationTypeDefinition) {
        associationTypeDefinitionsMap.put(associationTypeDefinition.getName(), associationTypeDefinition);
    }

    @Nullable ShadowAssociationClassDefinition getAssociationTypeDefinitionOld(@NotNull String name) {
        return associationTypeDefinitionsMap.get(name);
    }

    @Nullable ShadowAssociationClassDefinition getAssociationTypeDefinitionOld(@NotNull QName name) {
        return getAssociationTypeDefinitionOld(QNameUtil.getLocalPartCheckingNamespace(name, NS_RI));
    }

    @NotNull Collection<ShadowAssociationClassDefinition> getAssociationTypes() {
        return associationTypeDefinitionsMap.values();
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        super.extendDebugDump(sb, indent);
        if (!associationTypeDefinitionsMap.isEmpty()) {
            sb.append("\n");
            sb.append(DebugUtil.debugDump(associationTypeDefinitionsMap.values(), indent, false));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " (" + getObjectClassDefinitions().size() + " classes, "
                + getObjectTypeDefinitions().size() + " types)";
    }

    @Override
    public void validate() throws SchemaException {
        Set<QName> classes = new HashSet<>();
        for (ResourceObjectClassDefinition ocDefinition : getObjectClassDefinitions()) {
            QName ocName = ocDefinition.getTypeName();
            schemaCheck(
                    classes.add(ocName),
                    "Duplicate definition of object class %s in %s", ocName, this);
            ocDefinition.validate();
        }

        Set<RefinedObjectClassDefinitionKey> types = new HashSet<>();
        for (ResourceObjectTypeDefinition typeDefinition: getObjectTypeDefinitions()) {
            RefinedObjectClassDefinitionKey key = new RefinedObjectClassDefinitionKey(typeDefinition);
            schemaCheck(
                    types.add(key),
                    "Duplicate definition of object type %s in %s", key, this);
            typeDefinition.validate();
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ResourceSchemaImpl clone() {
        ResourceSchemaImpl clone = createEmptyClone(currentLayer);
        copyContent(clone);
        return clone;
    }

    private void copyContent(ResourceSchemaImpl target) {
        super.copyContent(target);
        target.associationClassImplementationsMap.putAll(associationClassImplementationsMap);
        target.associationTypeDefinitionsMap.putAll(associationTypeDefinitionsMap);
    }

    /**
     * We re-add layer-modified immutable versions of all our definitions.
     *
     * We have to use this approach because otherwise the internal lookup structures would be hard to fill in correctly.
     */
    private void copyAllDefinitionsImmutable(LayerType layer, ResourceSchemaMutator target) {
        for (Definition definition : definitions) {
            target.add(((ResourceObjectDefinition) definition).forLayerImmutable(layer));
        }
    }

    @Override
    public ResourceSchema forLayerImmutable(@NotNull LayerType layer) {
        if (isImmutable() && layer == currentLayer) {
            return this;
        }

        assertNoDelayedDefinitionsOnClone();

        ResourceSchemaImpl clone = createEmptyClone(layer);
        copyAllDefinitionsImmutable(layer, clone);
        // TODO what about substitutions?

        clone.freeze();
        return clone;
    }

    @NotNull ResourceSchemaImpl createEmptyClone(@NotNull LayerType layer) {
        return new ResourceSchemaImpl(nativeSchema, layer);
    }

    /** This is just a reminder - here we should put any freezing calls to own properties, should there be any. */
    @Override
    public void performFreeze() {
        super.performFreeze();
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    @Override
    public @NotNull NativeResourceSchema getNativeSchema() {
        return nativeSchema;
    }

    @Override
    public ComplexTypeDefinition findComplexTypeDefinitionByType(@NotNull QName typeName) {
        // FIXME remove eventually
        throw new UnsupportedOperationException("Object definitions are no longer CTDs please don't ask for them in this way");
    }
}
