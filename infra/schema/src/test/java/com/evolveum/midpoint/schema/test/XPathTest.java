/*
 * Copyright (c) 2010-2015 Evolveum
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

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.marshaller.TrivialItemPathParser;
import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.marshaller.PathHolderSegment;
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
import java.util.Arrays;
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
            ItemPathHolder xpath = new ItemPathHolder(path);

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

            ItemPathHolder xpathFromElement = new ItemPathHolder(xpathElement);
            AssertJUnit.assertEquals(xpath, xpathFromElement);

//            attributes = xpathElement.getAttributes();
//            for (int i = 0; i < attributes.getLength(); i++) {
//                Node n = attributes.item(i);
//                System.out.println(" A: " + n.getNodeName() + "(" + n.getPrefix() + " : " + n.getLocalName() + ") = " + n.getNodeValue());
//            }

            List<PathHolderSegment> segments = xpath.toSegments();

            System.out.println("XPATH segments: " + segments);

            ItemPathHolder xpathFromSegments = new ItemPathHolder(segments);

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

        ItemPathHolder xpath = new ItemPathHolder(el1);

        // Then

        Map<String, String> namespaceMap = xpath.getNamespaceMap();

        //AssertJUnit.assertEquals("http://default.com/", namespaceMap.get("c"));

        List<PathHolderSegment> segments = xpath.toSegments();

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

        String xpathString = "declare namespace x='http://xx.com/'; /root/x:el1";

        // When

        ItemPathHolder xpath = new ItemPathHolder(xpathString, el1);

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

        ItemPathHolder xpath = new ItemPathHolder(xpathStr);

        AssertJUnit.assertEquals("$v:var/x:xyz[10]", xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("http://vvv.com", xpath.getNamespaceMap().get("v"));
        AssertJUnit.assertEquals("http://www.xxx.com", xpath.getNamespaceMap().get("x"));
    }

    @Test
    public void dotTest() {

        ItemPathHolder dotPath = new ItemPathHolder(".");

        AssertJUnit.assertTrue(dotPath.toSegments().isEmpty());
        AssertJUnit.assertEquals(".", dotPath.getXPathWithoutDeclarations());
    }

    @Test
    public void explicitNsParseTest() {

        String xpathStr =
                "declare namespace foo='http://ff.com/';\ndeclare default namespace 'http://default.com/';\n declare  namespace bar = 'http://www.b.com' ;declare namespace x= \"http://xxx.com/\";\nfoo:foofoo[1]/x:bar";

        TrivialItemPathParser parser = TrivialItemPathParser.parse(xpathStr);

        AssertJUnit.assertEquals("http://ff.com/", parser.getNamespaceMap().get("foo"));
        AssertJUnit.assertEquals("http://www.b.com", parser.getNamespaceMap().get("bar"));
        AssertJUnit.assertEquals("http://xxx.com/", parser.getNamespaceMap().get("x"));
        AssertJUnit.assertEquals("http://default.com/", parser.getNamespaceMap().get(""));

        AssertJUnit.assertEquals("foo:foofoo[1]/x:bar", parser.getPureItemPathString());
    }

    @Test
    public void simpleXPathParseTest() {
        String xpathStr =
                "foo/bar";

        TrivialItemPathParser parser = TrivialItemPathParser.parse(xpathStr);
        AssertJUnit.assertEquals("foo/bar", parser.getPureItemPathString());
    }

    @Test
    public void explicitNsRoundTripTest() {

        String xpathStr =
                "declare namespace foo='http://ff.com/';\ndeclare default namespace 'http://default.com/';\n declare  namespace bar = 'http://www.b.com' ;declare namespace x= \"http://xxx.com/\";\nfoo:foofoo/x:bar";

        ItemPathHolder xpath = new ItemPathHolder(xpathStr);

        System.out.println("Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("foo:foofoo/x:bar", xpath.getXPathWithoutDeclarations());

        System.out.println("ROUND TRIP: "+xpath.getXPathWithDeclarations());

        List<String> expected = Arrays.asList(
		        "declare default namespace 'http://default.com/'; declare namespace foo='http://ff.com/'; declare namespace bar='http://www.b.com'; declare namespace x='http://xxx.com/'; foo:foofoo/x:bar",   // java7
		        "declare default namespace 'http://default.com/'; declare namespace bar='http://www.b.com'; declare namespace foo='http://ff.com/'; declare namespace x='http://xxx.com/'; foo:foofoo/x:bar",   // java8
		        "declare default namespace 'http://default.com/'; declare namespace x='http://xxx.com/'; declare namespace bar='http://www.b.com'; declare namespace foo='http://ff.com/'; foo:foofoo/x:bar" // after JSON/YAML serialization fix (java8)
        );
        AssertJUnit.assertTrue("Unexpected path with declarations: "+xpath.getXPathWithDeclarations(), expected.contains(xpath.getXPathWithDeclarations()));
    }

    @Test
    public void pureXPathRoundTripTest() {

    	Map<String, String> namespaceMap = new HashMap<String, String>();
    	namespaceMap.put("foo", "http://foo");
    	namespaceMap.put("bar", "http://bar");

        String xpathStr = "foo:foo/bar:bar";

        ItemPathHolder xpath = new ItemPathHolder(xpathStr, namespaceMap);

        System.out.println("Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("foo:foo/bar:bar", xpath.getXPathWithoutDeclarations());

        System.out.println("ROUND TRIP: "+xpath.getXPathWithDeclarations());
        AssertJUnit.assertEquals("foo:foo/bar:bar", xpath.getXPathWithDeclarations());

    }


    @Test
    public void strangeCharsTest() throws IOException {

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

        ItemPathHolder xpath = new ItemPathHolder(xpathStr);

        System.out.println("Stragechars Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("$i:user/i:extension/ri:foobar", xpath.getXPathWithoutDeclarations());

        System.out.println("Stragechars ROUND TRIP: "+xpath.getXPathWithDeclarations());

    }

    @Test
    public void xpathFromQNameTest() {
    	// GIVEN
    	QName qname = new QName(NS_FOO, "foo");
    	ItemPathHolder xpath = new ItemPathHolder(qname);
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
        QName qname1 = new QName(NS_C, "extension");
        QName qname2 = new QName(NS_FOO, "foo");
        ItemPathHolder itemPathHolder1 = new ItemPathHolder(qname1, qname2);
        QName elementQName = new QName(NS_BAR, "bar");

        // WHEN
        Element element = itemPathHolder1.toElement(elementQName, DOMUtil.getDocument());
        ItemPathHolder itemPathHolder2 = new ItemPathHolder(element);

        // THEN
        System.out.println("XPath from QNames:");
        System.out.println(DOMUtil.serializeDOMToString(element));

        ItemPath xpath1 = itemPathHolder1.toItemPath();
        ItemPath xpath2 = itemPathHolder2.toItemPath();
        assertTrue("Paths are not equivalent", xpath1.equivalent(xpath2));
    }

	@Test
	public void parseSpecial() {
		final String D = "declare namespace x='http://xyz.com/'; ";
		AssertJUnit.assertEquals("..", TrivialItemPathParser.parse("..").getPureItemPathString());
		AssertJUnit.assertEquals("..", TrivialItemPathParser.parse(D+"..").getPureItemPathString());
		AssertJUnit.assertEquals("a/../b", TrivialItemPathParser.parse("a/../b").getPureItemPathString());
		AssertJUnit.assertEquals("a/../b", TrivialItemPathParser.parse(D+"a/../b").getPureItemPathString());
		AssertJUnit.assertEquals("@", TrivialItemPathParser.parse("@").getPureItemPathString());
		AssertJUnit.assertEquals("@", TrivialItemPathParser.parse(D+"@").getPureItemPathString());
		AssertJUnit.assertEquals("a/@/b", TrivialItemPathParser.parse("a/@/b").getPureItemPathString());
		AssertJUnit.assertEquals("a/@/b", TrivialItemPathParser.parse(D+"a/@/b").getPureItemPathString());
		AssertJUnit.assertEquals("#", TrivialItemPathParser.parse("#").getPureItemPathString());
		AssertJUnit.assertEquals("#", TrivialItemPathParser.parse(D+"#").getPureItemPathString());
		AssertJUnit.assertEquals("a/#/b", TrivialItemPathParser.parse("a/#/b").getPureItemPathString());
		AssertJUnit.assertEquals("a/#/b", TrivialItemPathParser.parse(D+"a/#/b").getPureItemPathString());
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
	        ItemPathHolder xpath = new ItemPathHolder(el1);

	        AssertJUnit.fail("Unexpected success");
        } catch (IllegalArgumentException e) {
        	// This is expected
        }

    }

	@Test
	public void testCanonicalizationEmpty() throws Exception {
		assertCanonical(null, null, "");
		assertCanonical(ItemPath.EMPTY_PATH, null, "");
	}

	private static final String COMMON = "${common}3";
	private static final String ICFS = "${icf}1/connector-schema-3";
	private static final String ICF = "${icf}1";
	private static final String ZERO = "${0}";
	private static final String ONE = "${1}";

	@Test
	public void testCanonicalizationSimple() throws Exception {
		ItemPath path = new ItemPath(UserType.F_NAME);
		assertCanonical(path, null, "\\" + COMMON + "#name");
	}

	@Test
	public void testCanonicalizationSimpleNoNs() throws Exception {
		ItemPath path = new ItemPath(UserType.F_NAME.getLocalPart());
		assertCanonical(path, null, "\\#name");
		assertCanonical(path, UserType.class, "\\" + COMMON + "#name");
	}

	@Test
	public void testCanonicalizationMulti() throws Exception {
		ItemPath path = new ItemPath(UserType.F_ASSIGNMENT, 1234, AssignmentType.F_ACTIVATION,
				ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null, "\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMultiNoNs() throws Exception {
		ItemPath path = new ItemPath(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_ACTIVATION.getLocalPart(),
				ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart());
		assertCanonical(path, null, "\\#assignment",
				"\\#assignment\\#activation", "\\#assignment\\#activation\\#administrativeStatus");
		assertCanonical(path, UserType.class, "\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMixedNs() throws Exception {
		ItemPath path = new ItemPath(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION,
				new QName("http://piracy.org/inventory", "store"),
				new QName("http://piracy.org/inventory", "shelf"),
				new QName("x"), ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null,
				"\\#assignment",
				"\\#assignment\\" + COMMON + "#extension",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
		assertCanonical(path, UserType.class,
				"\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMixedNs2() throws Exception {
		ItemPath path = new ItemPath(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION.getLocalPart(),
				new QName("http://piracy.org/inventory", "store"),
				new QName("http://piracy.org/inventory", "shelf"),
				AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null,
				"\\#assignment",
				"\\#assignment\\#extension",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation\\" + ONE + "#administrativeStatus");
		assertCanonical(path, UserType.class,
				"\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	// from IntegrationTestTools
	private static final String NS_RESOURCE_DUMMY_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	private static final QName RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME = new QName(NS_RESOURCE_DUMMY_CONFIGURATION ,"uselessString");

	@Test
	public void testCanonicalizationLong() throws Exception {
		ItemPath path = new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
				RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME);
		assertCanonical(path, null, "\\" + COMMON + "#connectorConfiguration",
				"\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties",
				"\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties\\" + ICF + "/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector#uselessString");
	}


	private void assertCanonical(ItemPath path, Class<? extends Containerable> clazz, String... representations) {
    	CanonicalItemPath canonicalItemPath = CanonicalItemPath.create(path, clazz, PrismTestUtil.getPrismContext());
		System.out.println(path + " => " + canonicalItemPath.asString() + "  (" + clazz + ")");
		for (int i = 0; i < representations.length; i++) {
    		String c = canonicalItemPath.allUpToIncluding(i).asString();
    		assertEquals("Wrong string representation of length " + (i+1), representations[i], c);
		}
		assertEquals("Wrong string representation ", representations[representations.length-1], canonicalItemPath.asString());
	}

}
