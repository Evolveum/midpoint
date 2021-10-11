/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.impl.marshaller.PathHolderSegment;
import com.evolveum.midpoint.prism.impl.marshaller.TrivialItemPathParser;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This is low-level ItemPath parsing/serialization test.
 *
 * @author semancik
 */
public class ItemPathTest extends AbstractPrismTest {

    private static final String FILENAME_STRANGECHARS = "src/test/resources/path/strange.txt";
    private static final String FILENAME_DATA_XML = "src/test/resources/path/data.xml";
    private static final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    private static final String NS_FOO = "http://foo.com/";
    private static final String NS_BAR = "http://bar.com/";

    private static final String FILENAME_CHANGETYPE = "src/test/resources/path/changetype-1.xml";

    public ItemPathTest() {
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
        PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
    }

    @Test
    public void xPathFromDomNode1() throws ParserConfigurationException, SAXException, IOException {

        // Given
        Element el1 = parseDataGetEl1();
        String xpathString = "/root/x:el1[100]";
        el1.setTextContent(xpathString);

        // When

        ItemPathHolder xpath = ItemPathHolder.createForTesting(el1);

        // Then

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

        File file = new File(FILENAME_DATA_XML);
        Document doc = builder.parse(file);

        //NodeList childNodes = doc.getChildNodes();

        NodeList rootNodes = doc.getElementsByTagName("root");
        Node rootNode = rootNodes.item(0);

        NodeList nodes = ((Element) rootNode).getElementsByTagNameNS("http://xx.com/", "el1");

        Node el1 = nodes.item(0);

        return (Element)el1;
    }

    @Test
    public void variableTest() {

        String xpathStr =
                "declare namespace v='http://vvv.com';" +
                "declare namespace x='http://www.xxx.com';" +
                "$v:var/x:xyz[10]";

        ItemPathHolder xpath = ItemPathHolder.createForTesting(xpathStr);

        AssertJUnit.assertEquals("$v:var/x:xyz[10]", xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("http://vvv.com", xpath.getNamespaceMap().get("v"));
        AssertJUnit.assertEquals("http://www.xxx.com", xpath.getNamespaceMap().get("x"));
    }

    @Test
    public void dotTest() {

        ItemPathHolder dotPath = ItemPathHolder.createForTesting(".");

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

        ItemPathHolder xpath = ItemPathHolder.createForTesting(xpathStr);

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

        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("foo", "http://foo");
        namespaceMap.put("bar", "http://bar");

        String xpathStr = "foo:foo/bar:bar";

        ItemPathHolder xpath = ItemPathHolder.createForTesting(xpathStr, namespaceMap);

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

        try (FileInputStream stream = new FileInputStream(file)) {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            xpathStr = StandardCharsets.UTF_8.decode(bb).toString();
        }

        ItemPathHolder xpath = ItemPathHolder.createForTesting(xpathStr);

        System.out.println("Stragechars Pure XPath: "+xpath.getXPathWithoutDeclarations());
        AssertJUnit.assertEquals("$i:user/i:extension/ri:foobar", xpath.getXPathWithoutDeclarations());

        System.out.println("Stragechars ROUND TRIP: "+xpath.getXPathWithDeclarations());

    }

    @Test
    public void xpathFromQNameTest() {
        // GIVEN
        QName qname = new QName(NS_FOO, "foo");
        ItemPathHolder xpath = ItemPathHolder.createForTesting(qname);
        QName elementQName = new QName(NS_BAR, "bar");

        // WHEN
        Element element = xpath.toElement(elementQName, DOMUtil.getDocument());

        // THEN
        System.out.println("XPath from Qname:");
        System.out.println(DOMUtil.serializeDOMToString(element));

        assertEquals("Wrong element name", "bar", element.getLocalName());
        assertEquals("Wrong element namespace", NS_BAR, element.getNamespaceURI());
        Map<String, String> nsdecls = DOMUtil.getNamespaceDeclarations(element);
//        assertEquals("Wrong declaration for prefix "+XPathHolder.DEFAULT_PREFIX, NS_FOO, nsdecls.get(XPathHolder.DEFAULT_PREFIX));
        String prefix = nsdecls.keySet().iterator().next();
        assertEquals("Wrong element content", prefix+":foo", element.getTextContent());
    }

    @Test
    public void testXPathSerializationToDom() {
        // GIVEN
        QName qname1 = new QName(NS_C, "extension");
        QName qname2 = new QName(NS_FOO, "foo");
        ItemPathHolder itemPathHolder1 = ItemPathHolder.createForTesting(qname1, qname2);
        QName elementQName = new QName(NS_BAR, "bar");

        // WHEN
        Element element = itemPathHolder1.toElement(elementQName, DOMUtil.getDocument());
        ItemPathHolder itemPathHolder2 = ItemPathHolder.createForTesting(element);

        // THEN
        System.out.println("XPath from QNames:");
        System.out.println(DOMUtil.serializeDOMToString(element));

        UniformItemPath xpath1 = itemPathHolder1.toItemPath();
        UniformItemPath xpath2 = itemPathHolder2.toItemPath();
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

    /*
     * The following is unfinished test. It was moved here from the schema module, because after migration of prism
     * into prism-api/prism-impl it would need to access some of the internal structures.
     *
     * If needed, it has to be finished, probably by eliminating schema-related artifacts.
     */

//    /**
//     * This is not a proper test yet.
//     * It does some operations with XPath. If it does not die, then the
//     * code some somehow consistent.
//     *
//     * It should be improved later.
//     */
//    @Test
//    public void xpathTest() throws IOException, ParserConfigurationException, SchemaException {
//
//        ObjectModificationType objectModification = PrismTestUtil.parseAtomicValue(new File(FILENAME_CHANGETYPE),
//                ObjectModificationType.COMPLEX_TYPE);
//
//        for (ItemDeltaType change : objectModification.getItemDelta()) {
//            ItemPathType pathType = change.getPath();
//            System.out.println("  path=" + pathType + " (" + pathType.getClass().getName() + ") " + pathType.toString());
//            UniformItemPath path = pathType.getUniformItemPath();
//
//            AssertJUnit.assertEquals("c:extension/piracy:ship[2]/c:name", path.serializeWithoutDeclarations());
//
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true);
//            DocumentBuilder loader = factory.newDocumentBuilder();
//            Document doc = loader.newDocument();
//
//            Element xpathElement = path.xpath.toElement("http://elelel/", "path", doc);
//
//            Attr nsC = xpathElement.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "c");
//            Attr nsPiracy = xpathElement.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "piracy");
//
//            System.out.println("c: "+nsC);
//            System.out.println("piracy: "+nsPiracy);
//
//            //            AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/public/common/common-3", nsC.getValue());
//            //            AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/samples/piracy", nsPiracy.getValue());
//
//            System.out.println("XPATH Element: " + xpathElement);
//
//            ItemPathHolderTestWrapper xpathFromElement = ItemPathHolderTestWrapper.createForTesting(xpathElement);
//            ItemPathHolderTestWrapper.assertEquals(xpath, xpathFromElement);
//
//            //            attributes = xpathElement.getAttributes();
//            //            for (int i = 0; i < attributes.getLength(); i++) {
//            //                Node n = attributes.item(i);
//            //                System.out.println(" A: " + n.getNodeName() + "(" + n.getPrefix() + " : " + n.getLocalName() + ") = " + n.getNodeValue());
//            //            }
//
//            List<PathHolderSegment> segments = xpath.toSegments();
//
//            System.out.println("XPATH segments: " + segments);
//
//            ItemPathHolderTestWrapper xpathFromSegments = ItemPathHolderTestWrapper.createForTesting(segments);
//
//            System.out.println("XPath from segments: " + xpathFromSegments);
//
//            AssertJUnit.assertEquals("c:extension/piracy:ship[2]/c:name", xpathFromSegments.getXPathWithoutDeclarations());
//        }
//
//    }

}
