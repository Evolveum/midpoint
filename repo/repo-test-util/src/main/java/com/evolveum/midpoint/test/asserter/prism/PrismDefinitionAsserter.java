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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public abstract class PrismDefinitionAsserter<RA> extends AbstractAsserter<RA> {
	
	private Definition definition;

	public PrismDefinitionAsserter(Definition definition) {
		super();
		this.definition = definition;
	}
	
	public PrismDefinitionAsserter(Definition definition, String detail) {
		super(detail);
		this.definition = definition;
	}
	
	public PrismDefinitionAsserter(Definition definition, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.definition = definition;
	}
		
	public Definition getDefinition() {
		return definition;
	}
		
	protected String desc() {
		return descWithDetails(definition);
	}

	public PrismDefinitionAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public PrismDefinitionAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, definition);
		return this;
	}
}
