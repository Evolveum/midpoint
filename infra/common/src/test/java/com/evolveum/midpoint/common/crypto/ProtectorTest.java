/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.crypto;

import static org.testng.Assert.assertNull;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author lazyman
 * 
 */
public class ProtectorTest {

	private Protector protector;

	@BeforeTest
	public void before() {
		protector = new Protector();
	}

	@Test
	public void encryptNullString() throws EncryptionException {
		assertNull(protector.encryptString(null));
	}

	@Test
	public void encryptEmptyString() throws EncryptionException {
		assertNull(protector.encryptString(""));
	}

	@Test
	public void encryptNullElement() throws EncryptionException {
		assertNull(protector.encrypt(null));
	}
}
