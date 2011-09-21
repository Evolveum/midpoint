/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType.Password;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * @author Radovan Semancik
 *
 */
public class ObjectTypeUtilTest {

	@Test
	public void testGetPropertyNewValue() throws JAXBException {
		// GIVEN
		
		ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
				.unmarshal(new File("src/test/resources/util/object-change-modify-password.xml")))
				.getValue();
		
		// WHEN
		
		ObjectModificationType objectModification = objectChange.getObjectModification();
		Password newPasswordStructure = ObjectTypeUtil.getPropertyNewValue(objectModification,"credentials","password",Password.class);
		
		// THEN
		
		assertNotNull("No Password structure",newPasswordStructure);
		assertNotNull("No protectedString in Password structure",newPasswordStructure.getProtectedString());
		assertNotNull("No value in protectedString in Password structure",newPasswordStructure.getProtectedString().getClearValue());
	}

	/**
	 * Test for a wrong way how to represent property changes.
	 *  
	 * THIS IS ALL WRONG! This is a wrong way how to change password. the <clearValue> is not a property
     * and therefore it cannot be changed by itself. But current problems with the diff algorithm cause
     * that modifications like this appear in the system.
	 */
	@Test
	public void testGetPropertyNewValueHack() throws JAXBException {
		// GIVEN
		
		ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
				.unmarshal(new File("src/test/resources/util/object-change-modify-password-hack.xml")))
				.getValue();
		
		// WHEN
		
		ObjectModificationType objectModification = objectChange.getObjectModification();
		Password newPasswordStructure = ObjectTypeUtil.getPropertyNewValue(objectModification,"credentials","password",Password.class);
		
		// THEN
		
		assertNotNull("No Password structure",newPasswordStructure);
		assertNotNull("No protectedString in Password structure",newPasswordStructure.getProtectedString());
		assertNotNull("No value in protectedString in Password structure",newPasswordStructure.getProtectedString().getClearValue());		
	}
	
	@Test
	public void testGetPropertyNewValue2() throws JAXBException {
		// GIVEN
		
		ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
				.unmarshal(new File("src/test/resources/util/object-change-modify-activation.xml")))
				.getValue();
		
		// WHEN
		
		ObjectModificationType objectModification = objectChange.getObjectModification();
		Boolean enabled = ObjectTypeUtil.getPropertyNewValue(objectModification,"activation","enabled",Boolean.class);
		
		// THEN
		
		assertNotNull("returned null", enabled);
		assertTrue("Expected true for enabled",enabled);
	}

}
