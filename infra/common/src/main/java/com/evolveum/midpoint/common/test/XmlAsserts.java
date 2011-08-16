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


import org.testng.Assert;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringBufferInputStream;
import java.util.List;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.InputSource;

import com.evolveum.midpoint.common.diff.MidPointDifferenceListener;
import com.evolveum.midpoint.common.diff.OidQualifier;

public class XmlAsserts {

	private static void setupXmlUnitForTest() {
		// XmlUnit setup
		// Note: compareUnmatched has to be set to false to calculate diff
		// properly, to avoid matching of nodes that are not comparable
		XMLUnit.setCompareUnmatched(false);
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setNormalize(true);
		XMLUnit.setNormalizeWhitespace(true);
	}

	private static String readFileAsString(File fileNewXml) throws java.io.IOException {
		byte[] buffer = new byte[(int) fileNewXml.length()];
		FileInputStream f = new FileInputStream(fileNewXml);
		f.read(buffer);
		return new String(buffer);
	}

	public static void assertPatch(File fileNewXml, String patchedXml) throws Exception {
		assertPatch(readFileAsString(fileNewXml), patchedXml);

	}

	public static void assertPatch(String origXml, String patchedXml) throws Exception {
		setupXmlUnitForTest();
		Diff d = new Diff(new InputSource(new StringBufferInputStream(origXml)), new InputSource(
				new StringBufferInputStream(patchedXml)));
		DetailedDiff dd = new DetailedDiff(d);
		dd.overrideElementQualifier(new OidQualifier());
		dd.overrideDifferenceListener(new MidPointDifferenceListener());
		List<Difference> differences = dd.getAllDifferences();

		for (Difference diff : differences) {
			switch (diff.getId()) {
				// case DifferenceConstants.NAMESPACE_PREFIX_ID:
				// //ignore namespaces
				// //FIXME: ^^^
				// break;
				case DifferenceConstants.ATTR_VALUE_ID:
					if (diff.getControlNodeDetail().getNode().getNodeName().contains("type")) {
						// ignore attribute values for xsi type, because of
						// namespaces
						// FIXME: ^^^
						break;
					}
				case DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID:
				case DifferenceConstants.SCHEMA_LOCATION_ID:
					break;
				default:
					throw new AssertionError(diff.toString());
			}
		}

	}

}
