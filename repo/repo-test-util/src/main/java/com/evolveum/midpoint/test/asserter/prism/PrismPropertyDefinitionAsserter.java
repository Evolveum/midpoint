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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
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
public class PrismPropertyDefinitionAsserter<T,RA> extends PrismDefinitionAsserter<RA> {
	
	public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition) {
		super(definition);
	}
	
	public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition, String detail) {
		super(definition, detail);
	}
	
	public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition, RA returnAsserter, String detail) {
		super(definition, returnAsserter, detail);
	}
	
	public static <T> PrismPropertyDefinitionAsserter<T,Void> forPropertyDefinition(PrismPropertyDefinition<T> attrDefinition) {
		return new PrismPropertyDefinitionAsserter<>(attrDefinition);
	}
		
	public PrismPropertyDefinition<T> getDefinition() {
		return (PrismPropertyDefinition<T>) super.getDefinition();
	}
		
	protected String desc() {
		return descWithDetails("property definition " + PrettyPrinter.prettyPrint(getDefinition().getItemName()));
	}

	public PrismPropertyDefinitionAsserter<T,RA> display() {
		display(desc());
		return this;
	}
	
	public PrismPropertyDefinitionAsserter<T,RA> display(String message) {
		IntegrationTestTools.display(message, getDefinition());
		return this;
	}
}
