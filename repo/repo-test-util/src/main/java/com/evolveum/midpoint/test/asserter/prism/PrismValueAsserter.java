/**
 * Copyright (c) 2018 Evolveum and contributors
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

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;


/**
 * @author semancik
 *
 */
public abstract class PrismValueAsserter<V extends PrismValue, RA> extends AbstractAsserter<RA> {
	
	private V prismValue;

	public PrismValueAsserter(V prismValue) {
		super();
		this.prismValue = prismValue;
	}
	
	public PrismValueAsserter(V prismValue, String detail) {
		super(detail);
		this.prismValue = prismValue;
	}
	
	public PrismValueAsserter(V prismValue, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.prismValue = prismValue;
	}
	
	public V getPrismValue() {
		return prismValue;
	}
	
	// TODO

	protected String desc() {
		return getDetails();
	}

}
