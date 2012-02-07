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
package com.evolveum.midpoint.common.patch;

import org.testng.annotations.Test;
import java.io.File;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * 
 * @author lazyman
 * 
 */
public class PatchXmlTest {

	@Test
	@SuppressWarnings("unchecked")
	public void patchXmlTest() throws Exception {
		ObjectModificationType changes = ((JAXBElement<ObjectModificationType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/patch/change.xml"))).getValue();

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/account-shadow.xml"));
		System.out.println("Patched XML: ");
		System.out.println(patchedXml);
		XmlAsserts.assertPatch(new File("src/test/resources/patch/result.xml"), patchedXml);
	}
}
