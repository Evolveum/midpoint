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
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.prism.ComplexTypeDefinitionAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyDefinitionAsserter;
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
		IntegrationTestTools.display(message, getDefinition());
		return this;
	}
}
