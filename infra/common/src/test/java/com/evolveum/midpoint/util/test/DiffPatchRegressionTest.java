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

package com.evolveum.midpoint.util.test;

import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import static junit.framework.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Igor Farinic
 */
public class DiffPatchRegressionTest {

    public DiffPatchRegressionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static Document parseXmlToDocument(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        return loader.parse(new StringBufferInputStream(xml));
    }

    private Document parseXmlToDocument(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        return loader.parse(file);
    }

    @Test
    public void testPatchRealEsbUserAccountRef() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/user-real-esb-account-ref-old.xml"), new File("src/test/resources/user-real-esb-account-ref-new.xml"));
        assertNotNull(changes);
        assertEquals("383af517-2b29-4ab3-9bf1-07e60075e7ec", changes.getOid());
        assertEquals(1, changes.getPropertyModification().size());
        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-real-esb-account-ref-old.xml"));

        DiffPatchTest.assertPatch(new File("src/test/resources/user-real-esb-account-ref-new.xml"), patchedXml);
    }

    @Test
    public void testPatchRealEsbUser() throws Exception {
        ObjectModificationType changes = new ObjectModificationType();
        changes.setOid("383af517-2b29-4ab3-9bf1-07e60075e7ec");

        PropertyModificationType change = new PropertyModificationType();
        change.setModificationType(PropertyModificationTypeType.replace);

        Document userDoc = parseXmlToDocument(new File("src/test/resources/user-real-esb.xml"));
        XPathType xphatType = new XPathType(".", userDoc.getFirstChild());
        change.setPath(xphatType.toElement(SchemaConstants.NS_C, "xpath"));

        change.setValue(new Value());
        Document doc = parseXmlToDocument("<ns2:givenName xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" xmlns:ns2=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\">hbujghgl</ns2:givenName>");
        change.getValue().getAny().add((Element) doc.getFirstChild());

        changes.getPropertyModification().add(change);

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-real-esb.xml"));

        DiffPatchTest.assertPatch(new File("src/test/resources/user-real-esb-patched.xml"), patchedXml);
    }

    @Test
    public void testPatchRealEsbUserWherePathIsNullInPropertyModification() throws Exception {
        ObjectModificationType changes = new ObjectModificationType();
        changes.setOid("383af517-2b29-4ab3-9bf1-07e60075e7ec");

        PropertyModificationType change = new PropertyModificationType();
        change.setModificationType(PropertyModificationTypeType.replace);
        change.setPath(null);
        change.setValue(new Value());
        Document doc = parseXmlToDocument("<ns2:givenName xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" xmlns:ns2=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\">hbujghgl</ns2:givenName>");
        change.getValue().getAny().add((Element) doc.getFirstChild());

        changes.getPropertyModification().add(change);

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-real-esb.xml"));

        DiffPatchTest.assertPatch(new File("src/test/resources/user-real-esb-patched.xml"), patchedXml);
    }

    private PropertyModificationType createPasswordModification(String newPassword) {
        if (null == newPassword) {
            return null;
        }
        PropertyModificationType modification = null;
        Document doc = DOMUtil.getDocument();
        modification = new PropertyModificationType();
        modification.setModificationType(PropertyModificationTypeType.replace);
        List<XPathSegment> segments = new ArrayList<XPathSegment>();
        segments.add(new XPathSegment(SchemaConstants.I_CREDENTIALS));
        XPathType xpath = new XPathType(segments);
        modification.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));
        Element e = doc.createElementNS(SchemaConstants.NS_C, "password");
        e.setTextContent(newPassword);

        modification.setValue(new Value());
        modification.getValue().getAny().add(e);
        return modification;

    }

    @Test
    public void testPatchAccountForPasswordModificationServiceNoCredentials() throws Exception {
        ObjectModificationType mod = new ObjectModificationType();
        mod.setOid("c2daebc0-3ca9-401c-81cd-52a1975b0dc6");
        PropertyModificationType passwordChange = createPasswordModification("b");
        mod.getPropertyModification().add(passwordChange);

        String patchedXml = (new PatchXml()).applyDifferences(mod, new File("src/test/resources/account-password-modification-no-credentials-tag.xml"));
        DiffPatchTest.assertPatch(new File("src/test/resources/account-password-modification-no-credentials-tag-patched.xml"), patchedXml);
    }

    @Test
    public void testPatchAccountForPasswordModificationServiceWithCredentials() throws Exception {
        ObjectModificationType mod = new ObjectModificationType();
        mod.setOid("c2daebc0-3ca9-401c-81cd-52a1975b0dc6");
        PropertyModificationType passwordChange = createPasswordModification("newPassword");
        mod.getPropertyModification().add(passwordChange);

        String patchedXml = (new PatchXml()).applyDifferences(mod, new File("src/test/resources/account-password-modification-with-credentials-tag.xml"));
        DiffPatchTest.assertPatch(new File("src/test/resources/account-password-modification-with-credentials-tag-patched.xml"), patchedXml);
    }
}
