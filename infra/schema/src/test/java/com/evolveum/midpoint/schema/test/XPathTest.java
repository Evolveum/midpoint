/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.schema.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.parser.TrivialXPathParser;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.parser.XPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author semancik
 */
public class XPathTest {

    private static final String FILENAME_CHANGETYPE = "src/test/resources/xpath/changetype-1.xml";
    private static final String FILENAME_STRANGECHARS = "src/test/resources/xpath/strange.txt";
	private static final String NS_FOO = "http://foo.com/";
	private static final String NS_BAR = "http://bar.com/";;

    public XPathTest() {
    }
    
    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    /**
     * This is now a proper test yet.
     * It does some operations with XPath. If it does not die, then the
     * code some somehow consistent.
     *
     * It should be improved later.
     */
    @Test
    public void xpathTest() throws JAXBException, FileNotFoundException, IOException, ParserConfigurationException, SchemaException {

    	ObjectModificationType objectModification = PrismTestUtil.parseAtomicValue(new File(FILENAME_CHANGETYPE),
                ObjectModificationType.COMPLEX_TYPE);

        for (ItemDeltaType change : objectModification.getItemDelta()) {
            ItemPathType pathType = change.getPath();
            System.out.println("  path=" + pathType + " (" + pathType.getClass().getName() + ") " + pathType.toString());
//            NamedNodeMap attributes = path.getAttributes();
//            for (int i = 0; i < attributes.getLength(); i++) {
//                Node n = attributes.item(i);
//                System.out.println("   A: " + n.getClass().getName() + " " + n.getNodeName() + "(" + n.getPrefix() + " : " + n.getLocalName() + ") = " + n.getNodeValue());
//            }
//            List<Object> any = change.getValue().getAny();
//            for (Object e : any) {
//                if (e instanceof Element) {
//                    System.out.println("  E: " + ((Element) e).getLocalName());
//                }
//            }
            ItemPath path = pathType.getItemPath();
            XPathHolder xpath = new XPathHolder(path);

            AssertJUnit.assertEquals("c:extension/piracy:ship[2]/c:name", xpath.getXPathWithoutDeclarations());

            System.out.println("XPATH: " + xpath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder loader = factory.newDocumentBuilder();
            Document doc = loader.newDocument();

            Element xpathElement = xpath.toElement("http://elelel/", "path", doc);

            Attr nsC = xpathElement.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "c");
            Attr nsPiracy = xpathElement.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "piracy");

            System.out.println("c: "+nsC);
            System.out.println("piracy: "+nsPiracy);

//            AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/public/common/common-3", nsC.getValue());
//            AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/samples/piracy", nsPiracy.getValue());

            System.out.println("XPATH Element: " + xpathElement);

            XPathHolder xpathFromElement = new XPathHolder(xpathElement);
            AssertJUnit.assertEquals(xpath, xpathFromElement);

//            attributes = xpathElement.getAttributes();
//            for (int i = 0; i < attributes.getLength(); i++) {
//                Node n = attributes.item(i);
//                System.out.println(" A: " + n.getNodeName() + "(" + n.getPrefix() + " : " + n.getLocalName() + ") = " + n.getNodeValue());
//            }

            List<XPathSegment> segments = xpath.toSegments();

            System.out.println("XPATH segments: " + segments);

            XPathHolder xpathFromSegments = new XPathHolder(segments);

            System.out.println("XPath from segments: " + xpathFromSegments);

            AssertJUnit.assertEquals("c:extension/piracy:ship[2]/c:name", xpathFromSegments.getXPathWithoutDeclarations());

        }

    }

    @Test
    public void xPathFromDomNode1() throws ParserConfigurationException, SAXException, IOException {

        // Given
        Element el1 = parseDataGetEl1();   
        String xpathString = "/root/x:el1[100]";
        el1.setTextContent(xpathString);

        // When

        XPathHolder xpath = new XPathHolder(el1);

        // Then

        Map<String, String> namespaceMap = xpath.getNamespaceMap();

        //AssertJUnit.assertEquals("http://default.com/", namespaceMap.get("c"));

        List<XPathSegment> segments = xpath.toSegments();

        AssertJUnit.assertNotNull(segments);
        AssertJUnit.assertEquals(3, segments.size());
        AssertJUnit.assertEquals(new QName("", "root"), segments.get(0).getQName());
        AssertJUnit.assertFalse(segments.get(0).isVariable());
        AssertJUnit.assertFalse(segments.get(0).isIdValueFilter());
        AssertJUnit.assertEquals(new QName("http://xx.com/", "el1"), segments.get(1).getQName());
        AssertJUnit.assertFalse(segments.get(1).isVariable());
        AssertJUnit.assertFalse(segments.get(1).isIdValueFilter());
        AssertJUnit.assertNull(segments.get(2).getQName());
        AssertJUnit.assertFalse(segments.get(2).isVariable());
        AssertJUnit.assertTrue(segments.get(2).isIdValueFilter());
        AssertJUnit.assertEquals("100", segments.get(2).getValue());
    }

    private Element parseDataGetEl1() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        File file = new File("src/test/resources/xpath/data.xml");
        Document doc = builder.parse(file);

        //NodeList childNodes = doc.getChildNodes();

        NodeList rootNodes = doc.getElementsByTagName("root");
        Node rootNode = rootNodes.item(0);

        NodeList nodes = ((Element) rootNode).getElementsByTagNameNS("http://xx.com/", "el1");

        Node el1 = nodes.item(0);
        
        return (Element)el1;
	}

	@Test
    public void xPathFromDomNode2() throws ParserConfigurationException, SAXException, IOException {

        // Given
        Element el1 = parseDataGetEl1();

        String xpathString = "/:root/x:el1";

        // When

        XPathHolder xpath = new XPathHolder(xpathString, el1);

        // Then

        Map<String, String> namespaceMap = xpath.getNamespaceMap();

        //AssertJUnit.assertEquals("http://default.com/", namespaceMap.get(XPathHolder.DEFAULT_PREFIX));
    }

    @Test
    public void variableTest() {

        String xpathStr =
                "declare namespace v='http://vvv.com';" +
                "declare namespace x='http://www.xxx.com';" +
                "$v:var/x:xyz[10]";

        XPathHolder xpath = new XPathHolder(xpathStr);

        AssertJUnit.assertEquals("$v:var/x:xyz[10]", xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("http://vvv.com", xpath.getNamespaceMap().get("v"));
        AssertJUnit.assertEquals("http://www.xxx.com", xpath.getNamespaceMap().get("x"));
    }

    @Test
    public void dotTest() {

        XPathHolder dotPath = new XPathHolder(".");

        AssertJUnit.assertTrue(dotPath.toSegments().isEmpty());
        AssertJUnit.assertEquals(".", dotPath.getXPathWithoutDeclarations());
    }

    @Test
    public void explicitNsParseTest() {

        String xpathStr =
                "declare namespace foo='http://ff.com/';\ndeclare default namespace 'http://default.com/';\n declare  namespace bar = 'http://www.b.com' ;declare namespace x= \"http://xxx.com/\";\nfoo:foofoo[1]/x:bar";

        TrivialXPathParser parser = TrivialXPathParser.parse(xpathStr);

        AssertJUnit.assertEquals("http://ff.com/", parser.getNamespaceMap().get("foo"));
        AssertJUnit.assertEquals("http://www.b.com", parser.getNamespaceMap().get("bar"));
        AssertJUnit.assertEquals("http://xxx.com/", parser.getNamespaceMap().get("x"));
        AssertJUnit.assertEquals("http://default.com/", parser.getNamespaceMap().get(""));

        AssertJUnit.assertEquals("foo:foofoo[1]/x:bar", parser.getPureXPathString());
    }

    @Test
    public void simpleXPathParseTest() {
        String xpathStr =
                "foo/bar";

        TrivialXPathParser parser = TrivialXPathParser.parse(xpathStr);
        AssertJUnit.assertEquals("foo/bar", parser.getPureXPathString());
    }

    @Test
    public void explicitNsRoundTripTest() {

        String xpathStr =
                "declare namespace foo='http://ff.com/';\ndeclare default namespace 'http://default.com/';\n declare  namespace bar = 'http://www.b.com' ;declare namespace x= \"http://xxx.com/\";\nfoo:foofoo/x:bar";

        XPathHolder xpath = new XPathHolder(xpathStr);

        System.out.println("Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("foo:foofoo/x:bar", xpath.getXPathWithoutDeclarations());

        System.out.println("ROUND TRIP: "+xpath.getXPathWithDeclarations());
        AssertJUnit.assertEquals("declare default namespace 'http://default.com/'; declare namespace foo='http://ff.com/'; declare namespace bar='http://www.b.com'; declare namespace x='http://xxx.com/'; foo:foofoo/x:bar",
                xpath.getXPathWithDeclarations());
        
    }

    @Test
    public void pureXPathRoundTripTest() {

    	Map<String, String> namespaceMap = new HashMap<String, String>();
    	namespaceMap.put("foo", "http://foo");
    	namespaceMap.put("bar", "http://bar");
    	
        String xpathStr = "foo:foo/bar:bar";

        XPathHolder xpath = new XPathHolder(xpathStr, namespaceMap);

        System.out.println("Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("foo:foo/bar:bar", xpath.getXPathWithoutDeclarations());

        System.out.println("ROUND TRIP: "+xpath.getXPathWithDeclarations());
        AssertJUnit.assertEquals("foo:foo/bar:bar", xpath.getXPathWithDeclarations());

    }


    @Test
    public void strangeCharsTest() throws FileNotFoundException, UnsupportedEncodingException, IOException {

        String xpathStr;

        // The file contains strange chanrs (no-break spaces), so we need to pull
        // it in exactly as it is.
        File file = new File(FILENAME_STRANGECHARS);
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            xpathStr = Charset.forName("UTF-8").decode(bb).toString();
        }
        finally {
            stream.close();
        }

        XPathHolder xpath = new XPathHolder(xpathStr);

        System.out.println("Stragechars Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("$i:user/i:extension/ri:foobar", xpath.getXPathWithoutDeclarations());

        System.out.println("Stragechars ROUND TRIP: "+xpath.getXPathWithDeclarations());

    }
    
    @Test
    public void xpathFromQNameTest() {
    	// GIVEN
    	QName qname = new QName(NS_FOO, "foo");
    	XPathHolder xpath = new XPathHolder(qname);
    	QName elementQName = new QName(NS_BAR, "bar");
    	
    	// WHEN
    	Element element = xpath.toElement(elementQName, DOMUtil.getDocument());
    	
    	// THEN
    	System.out.println("XPath from Qname:");
    	System.out.println(DOMUtil.serializeDOMToString(element));
    	
    	assertEquals("Wrong element name", "bar", element.getLocalName());
    	assertEquals("Wrong element namespace", NS_BAR, element.getNamespaceURI());
    	Map<String, String> nsdecls = DOMUtil.getNamespaceDeclarations(element);
//    	assertEquals("Wrong declaration for prefix "+XPathHolder.DEFAULT_PREFIX, NS_FOO, nsdecls.get(XPathHolder.DEFAULT_PREFIX));
        String prefix = nsdecls.keySet().iterator().next();
    	assertEquals("Wrong element content", prefix+":foo", element.getTextContent());
    }

    @Test
    public void testXPathSerializationToDom() {
        // GIVEN
        QName qname1 = new QName(SchemaConstants.NS_C, "extension");
        QName qname2 = new QName(NS_FOO, "foo");
        XPathHolder xPathHolder1 = new XPathHolder(qname1, qname2);
        QName elementQName = new QName(NS_BAR, "bar");

        // WHEN
        Element element = xPathHolder1.toElement(elementQName, DOMUtil.getDocument());
        XPathHolder xPathHolder2 = new XPathHolder(element);

        // THEN
        System.out.println("XPath from QNames:");
        System.out.println(DOMUtil.serializeDOMToString(element));

        ItemPath xpath1 = xPathHolder1.toItemPath();
        ItemPath xpath2 = xPathHolder2.toItemPath();
        assertTrue("Paths are not equivalent", xpath1.equivalent(xpath2));
    }

    //not actual anymore..we have something like "wildcard" in xpath..there don't need to be prefix specified.we will try to match the local names
    @Test(enabled=false)
    public void testUndefinedPrefix() throws ParserConfigurationException, SAXException, IOException {

        // GIVEN
        Element el1 = parseDataGetEl1();
        String xpathString = "/root/undef:el1";
        el1.setTextContent(xpathString);

        try {
	        // WHEN
	        XPathHolder xpath = new XPathHolder(el1);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (IllegalArgumentException e) {
        	// This is expected
        }

    }

}
