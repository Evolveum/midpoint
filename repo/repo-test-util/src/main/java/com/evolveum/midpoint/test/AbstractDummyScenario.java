/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Defines a structure of comprehensive dummy-resource-based scenario:
 * Set of object classes, link classes, item names, etc.
 *
 * Self-initializable. Should replace methods like {@link DummyResourceContoller#extendSchemaAd()} etc.
 */
public class AbstractDummyScenario {

    /** The controller for the dummy resource on which this scenario runs. */
    @NotNull protected final DummyResourceContoller controller;

    /** Resource schema - it may or may not be present. See {@link #attachResourceSchema(CompleteResourceSchema)}. */
    protected CompleteResourceSchema resourceSchema;

    protected AbstractDummyScenario(@NotNull DummyResourceContoller controller) {
        this.controller = controller;
    }

    public @NotNull DummyResource getDummyResource() {
        return controller.getDummyResource();
    }

    /** Attaches the resource schema to this scenario, allowing the retrieval of attribute/association definitions. */
    public void attachResourceSchema(CompleteResourceSchema schema) {
        this.resourceSchema = schema;
    }

    public boolean isSchemaAttached() {
        return resourceSchema != null;
    }

    public @NotNull CompleteResourceSchema getResourceSchemaRequired() {
        return stateNonNull(resourceSchema, "Resource schema is not attached to %s", this);
    }

    public @NotNull ResourceType getResourceBean() {
        return stateNonNull(controller.getResourceType(),
                "Resource definition object (ResourceType) is not present in %s", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "instance: " + getDummyResource().getInstanceName()
                + "schema present: " + isSchemaAttached()
                + "}";
    }

    protected abstract class ScenarioObjectClass {

        /** Creates a new object. Does *not* add it to the resource. */
        public DummyObject create(String name) {
            var object = controller.getDummyResource().newObject(getNativeObjectClassName());
            object.setName(name);
            return object;
        }

        /** Creates and adds a new object to the resource. Does not expect any exceptions (break mode should not be set here). */
        public DummyObject add(String name) {
            var object = create(name);
            try {
                controller.getDummyResource().addObject(object);
            } catch (Exception e) {
                throw SystemException.unexpected(e, "while adding dummy object - break mode should not be set here!");
            }
            return object;
        }

        public abstract @NotNull String getNativeObjectClassName();

        // temporary implementation
        public @NotNull QName getMidPointObjectClassName() {
            var nativeName = getNativeObjectClassName();
            if (nativeName.equals("account")) {
                return RI_ACCOUNT_OBJECT_CLASS;
            } else if (nativeName.equals("group")) {
                return RI_GROUP_OBJECT_CLASS;
            } else {
                return new QName(NS_RI, "Custom" + nativeName + "ObjectClass");
            }
        }

        /** Requires the schema be attached first; see {@link #attachResourceSchema(CompleteResourceSchema)}. */
        public @NotNull ResourceObjectDefinition getObjectClassDefinition() {
            try {
                return getResourceSchemaRequired()
                        .findDefinitionForObjectClassRequired(getMidPointObjectClassName());
            } catch (SchemaException e) {
                throw SystemException.unexpected(e, "something is seriously broken, no definition for class");
            }
        }
    }

    protected abstract class ScenarioLinkClass {

        /** Creates the respective link on the resource. */
        public void add(DummyObject first, DummyObject second) {
            controller.getDummyResource().addLink(getLinkClassName().local(), first, second);
        }

        public abstract @NotNull ObjectClassName getLinkClassName();

    }
}
