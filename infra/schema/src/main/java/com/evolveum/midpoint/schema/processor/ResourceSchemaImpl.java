/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Direct implementation of {@link ResourceSchema} interface.
 *
 * Definitions are stored in {@link PrismSchemaImpl#definitions}.
 * Besides that, it has no own state.
 *
 * @author semancik
 */
public class ResourceSchemaImpl extends PrismSchemaImpl implements MutableResourceSchema {

    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    @NotNull private final LayerType currentLayer;

    ResourceSchemaImpl() {
        this(DEFAULT_LAYER);
    }

    private ResourceSchemaImpl(@NotNull LayerType currentLayer) {
        super(MidPointConstants.NS_RI);
        this.currentLayer = currentLayer;
    }

    @VisibleForTesting
    @Override
    public MutableResourceObjectClassDefinition createObjectClassDefinition(QName typeName) {
        ResourceObjectClassDefinitionImpl objectClassDef = ResourceObjectClassDefinitionImpl.raw(typeName);
        add(objectClassDef);
        return objectClassDef;
    }

    @Override
    public MutableResourceSchema toMutable() {
        return this;
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
        ResourceSchemaImpl clone = new ResourceSchemaImpl(currentLayer);
        super.copyContent(clone);
        return clone;
    }

    /**
     * We re-add layer-modified immutable versions of all our definitions.
     *
     * We have to use this approach because otherwise the internal lookup structures would be hard to fill in correctly.
     */
    private void copyAllDefinitionsImmutable(LayerType layer, MutableResourceSchema target) {
        for (Definition definition : definitions) {
            stateCheck(definition instanceof ResourceObjectDefinition,
                    "Non-ResourceObjectDefinition in %s: %s (%s)", this, definition, definition.getClass());
            target.add(((ResourceObjectDefinition) definition).forLayerImmutable(layer));
        }
    }

    @Override
    public ResourceSchema forLayerImmutable(@NotNull LayerType layer) {
        if (isImmutable() && layer == currentLayer) {
            return this;
        }

        assertNoDelayedDefinitionsOnClone();

        ResourceSchemaImpl clone = new ResourceSchemaImpl(layer);
        copyAllDefinitionsImmutable(layer, clone);
        // TODO what about substitutions?

        clone.freeze();
        return clone;
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
}
