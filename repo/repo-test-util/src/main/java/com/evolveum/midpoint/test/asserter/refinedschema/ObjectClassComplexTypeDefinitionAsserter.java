/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.ComplexTypeDefinitionAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author Radovan semancik
 *
 */
public class ObjectClassComplexTypeDefinitionAsserter<RA> extends ComplexTypeDefinitionAsserter<RA> {

    public ObjectClassComplexTypeDefinitionAsserter(ObjectClassComplexTypeDefinition objectClassDefinition) {
        super(objectClassDefinition);
    }

    public ObjectClassComplexTypeDefinitionAsserter(ObjectClassComplexTypeDefinition objectClassDefinition, String detail) {
        super(objectClassDefinition, detail);
    }

    public ObjectClassComplexTypeDefinitionAsserter(ObjectClassComplexTypeDefinition objectClassDefinition, RA returnAsserter, String detail) {
        super(objectClassDefinition, returnAsserter, detail);
    }

    public static ObjectClassComplexTypeDefinitionAsserter<Void> forObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
        return new ObjectClassComplexTypeDefinitionAsserter<>(objectClassDefinition);
    }

    public ObjectClassComplexTypeDefinition getComplexTypeDefinition() {
        return (ObjectClassComplexTypeDefinition) super.getComplexTypeDefinition();
    }

    public <T> ResourceAttributeDefinitionAsserter<T,ObjectClassComplexTypeDefinitionAsserter<RA>> attribute(QName attrName) {
        ResourceAttributeDefinitionAsserter<T,ObjectClassComplexTypeDefinitionAsserter<RA>> asserter = ResourceAttributeDefinitionAsserter.forAttribute(getComplexTypeDefinition(), attrName, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    public <T> ResourceAttributeDefinitionAsserter<T,ObjectClassComplexTypeDefinitionAsserter<RA>> attribute(String attrName) {
        ResourceAttributeDefinitionAsserter<T,ObjectClassComplexTypeDefinitionAsserter<RA>> asserter = ResourceAttributeDefinitionAsserter.forAttribute(getComplexTypeDefinition(), attrName, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return descWithDetails("object class definition " + PrettyPrinter.prettyPrint(getComplexTypeDefinition().getTypeName()));
    }

    public ObjectClassComplexTypeDefinitionAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ObjectClassComplexTypeDefinitionAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, getComplexTypeDefinition());
        return this;
    }
}
