/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyDefinitionAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan semancik
 */
public class ResourceAttributeDefinitionAsserter<T,RA> extends PrismPropertyDefinitionAsserter<T,RA> {

    private ResourceObjectDefinition objectDefinition;

    public ResourceAttributeDefinitionAsserter(ShadowSimpleAttributeDefinition<T> attrDefinition) {
        super(attrDefinition);
    }

    public ResourceAttributeDefinitionAsserter(ShadowSimpleAttributeDefinition<T> attrDefinition, String detail) {
        super(attrDefinition, detail);
    }

    public ResourceAttributeDefinitionAsserter(ShadowSimpleAttributeDefinition<T> attrDefinition, RA returnAsserter, String detail) {
        super(attrDefinition, returnAsserter, detail);
    }

    public static <T> ResourceAttributeDefinitionAsserter<T,Void> forAttributeDefinition(ShadowSimpleAttributeDefinition<T> attrDefinition) {
        return new ResourceAttributeDefinitionAsserter<>(attrDefinition);
    }

    public static <T,RA> ResourceAttributeDefinitionAsserter<T,RA> forAttribute(
            ResourceObjectDefinition objectDefinition, QName attrName, RA returnAsserter, String desc) {
        //noinspection unchecked
        ShadowSimpleAttributeDefinition<T> attrDefinition =
                (ShadowSimpleAttributeDefinition<T>) objectDefinition.findSimpleAttributeDefinition(attrName);
        assertNotNull("No definition for attribute "+attrName+" in " + desc, attrDefinition);
        ResourceAttributeDefinitionAsserter<T, RA> asserter = new ResourceAttributeDefinitionAsserter<>(attrDefinition, returnAsserter, desc);
        asserter.objectDefinition = objectDefinition;
        return asserter;
    }

    public static <T,RA> ResourceAttributeDefinitionAsserter<T,RA> forAttribute(ResourceObjectDefinition objectClassDefinition, String attrName, RA returnAsserter, String desc) {
        //noinspection unchecked
        ShadowSimpleAttributeDefinition<T> attrDefinition =
                (ShadowSimpleAttributeDefinition<T>) objectClassDefinition.findSimpleAttributeDefinition(attrName);
        assertNotNull("No definition for attribute "+attrName+" in " + desc, attrDefinition);
        ResourceAttributeDefinitionAsserter<T, RA> asserter = new ResourceAttributeDefinitionAsserter<>(attrDefinition, returnAsserter, desc);
        asserter.objectDefinition = objectClassDefinition;
        return asserter;
    }

    public ShadowSimpleAttributeDefinition<T> getDefinition() {
        return (ShadowSimpleAttributeDefinition<T>) super.getDefinition();
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertIsPrimaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectDefinition);
        assertTrue("Not a primary identifier:" + desc(),
                objectDefinition.isPrimaryIdentifier(
                        getDefinition().getItemName()));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertNotPrimaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectDefinition);
        assertFalse("Primary identifier but should not be:" + desc(),
                objectDefinition.isPrimaryIdentifier(
                        getDefinition().getItemName()));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertIsSecondaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectDefinition);
        assertTrue("Not a secondary identifier:" + desc(),
                objectDefinition.isSecondaryIdentifier(
                        getDefinition().getItemName()));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertNotSecondaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectDefinition);
        assertFalse("Secondary identifier but should not be:" + desc(),
                objectDefinition.isSecondaryIdentifier(
                        getDefinition().getItemName()));
        return this;
    }

    protected String desc() {
        return descWithDetails("resource attribute definition " + PrettyPrinter.prettyPrint(getDefinition().getItemName()));
    }

    public ResourceAttributeDefinitionAsserter<T,RA> display() {
        display(desc());
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> display(String message) {
        PrismTestUtil.display(message, getDefinition());
        return this;
    }
}
