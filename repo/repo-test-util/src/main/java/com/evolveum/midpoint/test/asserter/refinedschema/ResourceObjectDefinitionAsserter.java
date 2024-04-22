/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author Radovan semancik
 */
public class ResourceObjectDefinitionAsserter<RA> extends AbstractAsserter<RA> {

    private ResourceObjectDefinition resourceObjectDefinition;

    public ResourceObjectDefinitionAsserter(ResourceObjectDefinition objectDefinition) {
        this.resourceObjectDefinition = objectDefinition;
    }

    public ResourceObjectDefinitionAsserter(ResourceObjectDefinition objectDefinition, String detail) {
        super(detail);
        this.resourceObjectDefinition = objectDefinition;
    }

    public ResourceObjectDefinitionAsserter(ResourceObjectDefinition objectDefinition, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.resourceObjectDefinition = objectDefinition;
    }

    public static ResourceObjectDefinitionAsserter<Void> forObjectClassDefinition(ResourceObjectDefinition objectDefinition) {
        return new ResourceObjectDefinitionAsserter<>(objectDefinition);
    }

    public ResourceObjectDefinition getComplexTypeDefinition() {
        return resourceObjectDefinition;
    }

    public <T> ResourceAttributeDefinitionAsserter<T, ResourceObjectDefinitionAsserter<RA>> attribute(QName attrName) {
        ResourceAttributeDefinitionAsserter<T, ResourceObjectDefinitionAsserter<RA>> asserter =
                ResourceAttributeDefinitionAsserter.forAttribute(getComplexTypeDefinition(), attrName, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    public <T> ResourceAttributeDefinitionAsserter<T, ResourceObjectDefinitionAsserter<RA>> attribute(String attrName) {
        ResourceAttributeDefinitionAsserter<T, ResourceObjectDefinitionAsserter<RA>> asserter =
                ResourceAttributeDefinitionAsserter.forAttribute(getComplexTypeDefinition(), attrName, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return descWithDetails("object class definition " + PrettyPrinter.prettyPrint(getComplexTypeDefinition().getTypeName()));
    }

    public ResourceObjectDefinitionAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ResourceObjectDefinitionAsserter<RA> display(String message) {
        PrismTestUtil.display(message, getComplexTypeDefinition());
        return this;
    }
}
