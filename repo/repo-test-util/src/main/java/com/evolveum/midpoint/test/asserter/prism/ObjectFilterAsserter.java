/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ContainerDeltaAsserter;
import com.evolveum.midpoint.test.asserter.PropertyDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.testng.AssertJUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class ObjectFilterAsserter<RA> extends AbstractAsserter<RA> {

	private ObjectFilter filter;

	public ObjectFilterAsserter(ObjectFilter filter) {
		super();
		this.filter = filter;
	}

	public ObjectFilterAsserter(ObjectFilter filter, String detail) {
		super(detail);
		this.filter = filter;
	}

	public ObjectFilterAsserter(ObjectFilter filter, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.filter = filter;
	}
	
	public static ObjectFilterAsserter<Void> forDelta(ObjectFilter filter) {
		return new ObjectFilterAsserter<>(filter);
	}

	public ObjectFilter getFilter() {
		return filter;
	}

	public ObjectFilterAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), filter);
		return this;
	}

	public ObjectFilterAsserter<RA> assertNotNull() {
		AssertJUnit.assertNotNull("Null " + desc(), filter);
		return this;
	}

	public ObjectFilterAsserter<RA> assertClass(Class<? extends ObjectFilter> expectedClass) {
		assertNotNull();
		if (!expectedClass.isAssignableFrom(filter.getClass())) {
			fail("Expected that filter will be of class "+expectedClass.getSimpleName()+", but was "+filter.getClass().getSimpleName()+", in "+desc());
		}
		return this;
	}

	public ObjectFilterAsserter<RA> assertNone() {
		if (!ObjectQueryUtil.isNone(filter)) {
			fail("Expected that filter will be NONE, but was "+filter+", in "+desc());
		}
		return this;
	}

	public ObjectFilterAsserter<RA> assertAll() {
		if (!ObjectQueryUtil.isAll(filter)) {
			fail("Expected that filter will be NONE, but was "+filter+", in "+desc());
		}
		return this;
	}
	
	// TODO
	
	protected String desc() {
		return descWithDetails(filter);
	}

	public ObjectFilterAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public ObjectFilterAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, filter);
		return this;
	}
}
