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

package com.evolveum.midpoint.provisioning.schema.util;

import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.PrefixResolverDefault;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;




import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *
 * @author elek
 */
public class ObjectValueWriterTest {


   

    @Test
    public void buildResourceObject() throws JAXBException {
        //given
        ObjectValueWriter ovw = new ObjectValueWriter();
        ResourceSchema schema = Utils.createSampleSchema();
        ResourceObjectShadowType st = load(new File("src/test/resources/resourceshadow-example.xml"));

        //when
        ResourceObject ro = ovw.buildResourceObject(st, schema);

        //then
        ResourceAttribute value = ro.getValue(new QName(Utils.ICC, "__UID__"));
        assertNotNull(value);
        String uid = value.getSingleJavaValue(String.class);
        assertEquals("oidoidoid-heyheyhey", uid);     
    }

    @Test
    public void testWrite() throws Exception {
        //given
        Document doc = ShadowUtil.getXmlDocument();

        ResourceSchema schema = Utils.createSampleSchema();
        ResourceObjectDefinition def = schema.getObjectClassesCopy().get(0);
        ResourceObject o = new ResourceObject(def);

        assertNotNull(def.getAttributeDefinition(new QName(Utils.ICC, "__UID__", "icc")));
        ResourceAttribute attr1 = new ResourceAttribute(def.getAttributeDefinition(new QName(Utils.ICC, "__UID__", "icc")));

        Element e = doc.createElementNS(Utils.ICC, "__UID__");
        e.appendChild(doc.createTextNode("TEST"));
        attr1.addValue(e);
        o.addValue(attr1);

        Element rootElement = (Element) doc.appendChild(doc.createElement("root"));

        //when
        new ObjectValueWriter().write(o, rootElement);

        //then
        File of = new File("target/test.xml");
        FileWriter fw = new FileWriter(of);
        Utils.writeXml(doc.getDocumentElement(), fw);
        //FIXME implement real assertions
        System.out.println("see " + of.getAbsolutePath());

        fw.close();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix.equals("icc")) {
                    return Utils.ICC;
                }
                return null;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        });
        //PrefixResolver resolver = new PrefixResolverDefault(doc);


        XPathExpression expr = xpath.compile("//root/icc:__UID__/text()");
        Text result = (Text) expr.evaluate(doc, XPathConstants.NODE);
        assertEquals("TEST", result.getNodeValue());




    }

    private ResourceObjectShadowType load(File file) throws JAXBException {
        JAXBContext c = JAXBContext.newInstance(ResourceObjectShadowType.class);
        javax.xml.bind.Unmarshaller um = c.createUnmarshaller();
        return ((JAXBElement<ResourceObjectShadowType>) um.unmarshal(file)).getValue();
    }
}
