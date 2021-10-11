/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyDefinitionAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan semancik
 */
public class ResourceAttributeDefinitionAsserter<T,RA> extends PrismPropertyDefinitionAsserter<T,RA> {

    private ObjectClassComplexTypeDefinition objectClassDefinition;

    public ResourceAttributeDefinitionAsserter(ResourceAttributeDefinition<T> attrDefinition) {
        super(attrDefinition);
    }

    public ResourceAttributeDefinitionAsserter(ResourceAttributeDefinition<T> attrDefinition, String detail) {
        super(attrDefinition, detail);
    }

    public ResourceAttributeDefinitionAsserter(ResourceAttributeDefinition<T> attrDefinition, RA returnAsserter, String detail) {
        super(attrDefinition, returnAsserter, detail);
    }

    public static <T> ResourceAttributeDefinitionAsserter<T,Void> forAttributeDefinition(ResourceAttributeDefinition<T> attrDefinition) {
        return new ResourceAttributeDefinitionAsserter<>(attrDefinition);
    }

    public static <T,RA> ResourceAttributeDefinitionAsserter<T,RA> forAttribute(ObjectClassComplexTypeDefinition objectClassDefinition, QName attrName, RA returnAsserter, String desc) {
        ResourceAttributeDefinition<T> attrDefinition = objectClassDefinition.findAttributeDefinition(attrName);
        assertNotNull("No definition for attribute "+attrName+" in " + desc, attrDefinition);
        ResourceAttributeDefinitionAsserter<T, RA> asserter = new ResourceAttributeDefinitionAsserter<>(attrDefinition, returnAsserter, desc);
        asserter.objectClassDefinition = objectClassDefinition;
        return asserter;
    }

    public static <T,RA> ResourceAttributeDefinitionAsserter<T,RA> forAttribute(ObjectClassComplexTypeDefinition objectClassDefinition, String attrName, RA returnAsserter, String desc) {
        ResourceAttributeDefinition<T> attrDefinition = objectClassDefinition.findAttributeDefinition(attrName);
        assertNotNull("No definition for attribute "+attrName+" in " + desc, attrDefinition);
        ResourceAttributeDefinitionAsserter<T, RA> asserter = new ResourceAttributeDefinitionAsserter<>(attrDefinition, returnAsserter, desc);
        asserter.objectClassDefinition = objectClassDefinition;
        return asserter;
    }

    public ResourceAttributeDefinition<T> getDefinition() {
        return (ResourceAttributeDefinition<T>) super.getDefinition();
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertIsPrimaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectClassDefinition);
        assertTrue("Not a primary identifier:" + desc(), getDefinition().isPrimaryIdentifier(objectClassDefinition));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertNotPrimaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectClassDefinition);
        assertFalse("Primary identifier but should not be:" + desc(), getDefinition().isPrimaryIdentifier(objectClassDefinition));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertIsSecondaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectClassDefinition);
        assertTrue("Not a secondary identifier:" + desc(), getDefinition().isSecondaryIdentifier(objectClassDefinition));
        return this;
    }

    public ResourceAttributeDefinitionAsserter<T,RA> assertNotSecondaryIdentifier() {
        assertNotNull("Cannot evaluate whether attribute is identifier because there is no objectClassDefinition set up in asserter", objectClassDefinition);
        assertFalse("Secondary identifier but should not be:" + desc(), getDefinition().isSecondaryIdentifier(objectClassDefinition));
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
