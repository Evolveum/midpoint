/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Related to PrismContainerDefinitionAsserter
 *
 * @author semancik
 */
public class ComplexTypeDefinitionAsserter<RA> extends AbstractAsserter<RA> {

    private ComplexTypeDefinition complexTypeDefinition;

    public ComplexTypeDefinitionAsserter(ComplexTypeDefinition complexTypeDefinition) {
        super();
        this.complexTypeDefinition = complexTypeDefinition;
    }

    public ComplexTypeDefinitionAsserter(ComplexTypeDefinition complexTypeDefinition, String detail) {
        super(detail);
        this.complexTypeDefinition = complexTypeDefinition;
    }

    public ComplexTypeDefinitionAsserter(ComplexTypeDefinition complexTypeDefinition, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.complexTypeDefinition = complexTypeDefinition;
    }

    public static <O extends ObjectType> ComplexTypeDefinitionAsserter<Void> forComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
        return new ComplexTypeDefinitionAsserter<>(complexTypeDefinition);
    }

    public ComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    public ComplexTypeDefinitionAsserter<RA> assertTypeQName(QName expected) {
        assertEquals("Wrong type QName in "+desc(), expected, complexTypeDefinition.getTypeName());
        return this;
    }

    protected String desc() {
        return descWithDetails("complex type definition " + PrettyPrinter.prettyPrint(complexTypeDefinition.getTypeName()));
    }

    public ComplexTypeDefinitionAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ComplexTypeDefinitionAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, complexTypeDefinition);
        return this;
    }
}
