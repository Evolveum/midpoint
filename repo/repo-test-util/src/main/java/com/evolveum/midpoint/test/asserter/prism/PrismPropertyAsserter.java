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

import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class PrismPropertyAsserter<T, RA> extends PrismItemAsserter<PrismProperty<T>, RA> {
	
	public PrismPropertyAsserter(PrismProperty<T> property) {
		super(property);
	}
	
	public PrismPropertyAsserter(PrismProperty<T> property, String detail) {
		super(property, detail);
	}
	
	public PrismPropertyAsserter(PrismProperty<T> property, RA returnAsserter, String detail) {
		super(property, returnAsserter, detail);
	}
	
	@Override
	public PrismPropertyAsserter<T,RA> assertSize(int expected) {
		super.assertSize(expected);
		return this;
	}
	
	@Override
	public PrismPropertyAsserter<T,RA> assertComplete() {
		super.assertComplete();
		return this;
	}
	
	@Override
	public PrismPropertyAsserter<T,RA> assertIncomplete() {
		super.assertIncomplete();
		return this;
	}
	
	public PrismPropertyValueAsserter<T,PrismPropertyAsserter<T,RA>> singleValue() {
		assertSize(1);
		PrismPropertyValue<T> pval = getItem().getValue();
		PrismPropertyValueAsserter<T,PrismPropertyAsserter<T,RA>> asserter = new PrismPropertyValueAsserter<>(pval, this, "single value in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	// TODO

	protected String desc() {
		return getDetails();
	}

}
