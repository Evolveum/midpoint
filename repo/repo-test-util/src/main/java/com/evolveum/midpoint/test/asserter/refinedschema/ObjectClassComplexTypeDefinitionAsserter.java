/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.ComplexTypeDefinitionAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismSchemaAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
