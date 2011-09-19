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
		
		assertNotNull(newPasswordStructure);
		
	}
	
}
