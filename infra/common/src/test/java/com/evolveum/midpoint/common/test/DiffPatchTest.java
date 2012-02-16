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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common.test;

import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

import java.io.File;

import com.evolveum.midpoint.test.diff.CalculateXmlDiff;
import com.evolveum.midpoint.test.patch.PatchXml;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * 
 * @author Igor Farinic
 */
public class DiffPatchTest {

	@Test
	public void testDiffPatchAccount() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/account-old.xml"), new File("src/test/resources/patch/account-new.xml"));
		assertNotNull(changes);
		assertEquals(5, changes.getPropertyModification().size());
		assertEquals("12345", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/account-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/account-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchAccountWithResourceSchemaHandlingConfiguration() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/account-full-old.xml"), new File(
				"src/test/resources/patch/account-full-new.xml"));
		assertNotNull(changes);
		assertEquals(5, changes.getPropertyModification().size());
		assertEquals("12345", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/account-full-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/account-full-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchUser() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/user-old.xml"), new File("src/test/resources/patch/user-new.xml"));
		assertNotNull(changes);
		assertEquals(7, changes.getPropertyModification().size());
		assertEquals("007", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/user-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/user-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchUserExtension() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/user-extension-old.xml"), new File(
				"src/test/resources/patch/user-extension-new.xml"));
		assertNotNull(changes);
		assertEquals(1, changes.getPropertyModification().size());
		assertEquals("007", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/user-extension-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/user-extension-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchResource() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/resource-old.xml"), new File("src/test/resources/patch/resource-new.xml"));
		assertNotNull(changes);
		assertEquals(1, changes.getPropertyModification().size());
		assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/resource-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/resource-new.xml"), patchedXml);
	}

	// Disabled Oct 26 2011 due to schema change in accountConstruction
	@Test(enabled=false)
	public void testDiffPatchAdvancedResource() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/resource-advanced-old.xml"), new File(
				"src/test/resources/patch/resource-advanced-new.xml"));
		assertNotNull(changes);
		assertEquals(1, changes.getPropertyModification().size());
		assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/resource-advanced-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/resource-advanced-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchResourceSchemaHandling() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/resource-schemahandling-old.xml"), new File(
				"src/test/resources/patch/resource-schemahandling-new.xml"));
		assertNotNull(changes);
		assertEquals(1, changes.getPropertyModification().size());
		assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/resource-schemahandling-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/resource-schemahandling-new.xml"), patchedXml);
	}

	@Test
	public void testDiffPatchUserCredentials() throws Exception {
		ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File(
				"src/test/resources/patch/user-credentials-old.xml"), new File(
				"src/test/resources/patch/user-credentials-new.xml"));
		assertNotNull(changes);
		assertEquals(2, changes.getPropertyModification().size());
		assertEquals("d7f1f990-b1fc-4001-9003-2106bd289c5b", changes.getOid());

		String patchedXml = (new PatchXml()).applyDifferences(changes, new File(
				"src/test/resources/patch/user-credentials-old.xml"));
		XmlAsserts.assertPatch(new File("src/test/resources/patch/user-credentials-new.xml"), patchedXml);
	}
}
