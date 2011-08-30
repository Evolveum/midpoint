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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = "classpath:application-context-configuration-test.xml")
public class ProtectorTest extends AbstractTestNGSpringContextTests {

	@Autowired(required = true)
	private Protector protector;

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

	@Test
	public void encryptDecryptString() throws EncryptionException {
		final String plain = "welcome to protector";
		String decrypted = protector.decryptString(protector.encryptString(plain));

		assertEquals(decrypted, plain);
	}

	@Test
	public void encryptDecryptElement() throws EncryptionException {
		final String plain = "welcome to protector";
		Document document = DOMUtil.getDocument();
		Element element = document.createElement("element");
		element.setTextContent(plain);
		document.appendChild(element);

		Element decrypted = protector.decrypt(protector.encrypt(element));
		assertNotNull(decrypted);
		assertEquals(decrypted.getTextContent(), plain);
		assertEquals(element.getTagName(), "element");
	}
}
