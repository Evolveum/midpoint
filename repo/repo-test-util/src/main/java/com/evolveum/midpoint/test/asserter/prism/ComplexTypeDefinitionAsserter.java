/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.refinedschema.ObjectClassComplexTypeDefinitionAsserter;
import com.evolveum.midpoint.test.asserter.refinedschema.RefinedResourceSchemaAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
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
