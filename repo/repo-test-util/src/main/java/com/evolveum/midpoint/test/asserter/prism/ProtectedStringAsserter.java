/**
 * Copyright (c) 2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class ProtectedStringAsserter<RA> extends AbstractAsserter<RA> {
	
	private ProtectedStringType protectedString;

	public ProtectedStringAsserter(ProtectedStringType protectedString) {
		super();
		this.protectedString = protectedString;
	}
	
	public ProtectedStringAsserter(ProtectedStringType protectedString, String detail) {
		super(detail);
		this.protectedString = protectedString;
	}
	
	public ProtectedStringAsserter(ProtectedStringType protectedString, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.protectedString = protectedString;
	}
	
	public ProtectedStringType getProtectedString() {
		return protectedString;
	}

	public ProtectedStringAsserter<RA> assertIsEncrypted() {
		assertTrue("Non-encrypted procted string in "+desc(), protectedString.isEncrypted());
		return this;
	}

	public ProtectedStringAsserter<RA> assertIsHashed() {
		assertTrue("Non-encrypted procted string in "+desc(), protectedString.isHashed());
		return this;
	}
	
	public ProtectedStringAsserter<RA> assertHasClearValue() {
		assertNotNull("No clear value procted string in "+desc(), protectedString.getClearValue());
		return this;
	}
	
	public ProtectedStringAsserter<RA> assertNoClearValue() {
		assertNull("Unexpected clear value procted string in "+desc(), protectedString.getClearValue());
		return this;
	}

	public ProtectedStringAsserter<RA> assertClearValue(String expected) {
		AssertJUnit.assertEquals("Wrong clear value in "+desc(), expected, protectedString.getClearValue());
		return this;
	}
	
	/**
	 * Asserts that protected string is completely equal, including all the randomized things (salt, IV).
	 */
	public ProtectedStringAsserter<RA> assertEquals(ProtectedStringType expected) {
		AssertJUnit.assertEquals("Non-equal protected string in "+desc(), expected, protectedString);
		return this;
	}
	
	/**
	 * Checks whether the value inside protected string matches with specified value.
	 * Works for all types: encrypted, hashed, clear.
	 */
	public ProtectedStringAsserter<RA> assertCompareCleartext(String expected) throws SchemaException, EncryptionException {
		ProtectedStringType expectedPs = new ProtectedStringType();
		expectedPs.setClearValue(expected);
		assertTrue("Wrong value, cannot match in "+desc(), getProtector().compareCleartext(expectedPs, protectedString));
		return this;
	}
	
	protected String desc() {
		return descWithDetails(protectedString);
	}

	public ProtectedStringAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public ProtectedStringAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, protectedString);
		return this;
	}
}
