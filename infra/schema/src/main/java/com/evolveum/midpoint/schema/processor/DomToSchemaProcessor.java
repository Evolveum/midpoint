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

package com.evolveum.midpoint.schema.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;

import static com.evolveum.midpoint.schema.processor.ProcessorConstants.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * @author lazyman
 */
class DomToSchemaProcessor {

	Schema parseDom(Element xsdSchema) throws SchemaProcessorException {
		Schema schema = initSchema(xsdSchema);

		XSSchemaSet set = parseSchema(xsdSchema);
		if (set == null) {
			return schema;
		}
		Iterator<XSComplexType> iterator = set.iterateComplexTypes();
		while (iterator.hasNext()) {
			XSComplexType complexType = iterator.next();
			if (complexType.getTargetNamespace().equals(schema.getNamespace())) {
				addPropertyContainerDefinition(complexType, xsdSchema, schema);
			}
		}

		return schema;
	}

	private Schema initSchema(Element xsdSchema) {
		String targetNamespace = xsdSchema.getAttribute("targetNamespace");
		return new Schema(targetNamespace);
	}

	private void addPropertyContainerDefinition(XSComplexType complexType, Element document, Schema schema) {
		QName qname = new QName(complexType.getTargetNamespace(), complexType.getName());

		XSAnnotation annotation = complexType.getAnnotation();
		// nativeObjectClass
		Element nativeAttrElement = getAnnotationElement(annotation, A_NATIVE_OBJECT_CLASS);
		String nativeObjectClass = nativeAttrElement == null ? qname.getLocalPart() : nativeAttrElement
				.getTextContent();
		ResourceObjectDefinition definition = new ResourceObjectDefinition(schema, qname, null, new QName(
				schema.getNamespace(), complexType.getName()));
		definition.setNativeObjectClass(nativeObjectClass);
		if (isAccountObject(annotation)) {
			definition.setAccountType(true);
		}

		List<PropertyDefinition> list = null;
		XSContentType content = complexType.getContentType();
		if (content != null) {
			XSParticle particle = content.asParticle();
			if (particle != null) {
				XSTerm term = particle.getTerm();

				if (term.isModelGroup()) {
					list = createResourceAttributeListFromGroup(term.asModelGroup());
				}
			}
		}

		if (list != null) {
			Set<PropertyDefinition> set = definition.getDefinitions();
			for (PropertyDefinition property : list) {
				if (property instanceof ResourceObjectAttributeDefinition) {
					ResourceObjectAttributeDefinition attrDefinition = (ResourceObjectAttributeDefinition) property;
					attrDefinition.setObjectDefinition(definition);
				}
				set.add(property);
			}
		}

		parseAnnotationForObjectClass(annotation, definition, document);

		schema.getDefinitions().add(definition);
	}

	private List<PropertyDefinition> createResourceAttributeListFromGroup(XSModelGroup group) {
		List<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>();

		XSParticle[] particles = group.getChildren();
		for (XSParticle p : particles) {
			XSTerm pterm = p.getTerm();
			if (pterm.isModelGroup()) {
				definitions.addAll(createResourceAttributeListFromGroup(pterm.asModelGroup()));
			}

			// xs:element inside complex type
			if (pterm.isElementDecl()) {
				XSAnnotation annotation = selectAnnotationToUse(p.getAnnotation(), pterm.getAnnotation());

				XSElementDecl element = pterm.asElementDecl();
				QName qname = new QName(element.getTargetNamespace(), element.getName());
				// nativeAttributeName
				Element nativeAttrElement = getAnnotationElement(annotation, A_NATIVE_ATTRIBUTE_NAME);
				String nativeAttributeName = nativeAttrElement == null ? qname.getLocalPart()
						: nativeAttrElement.getTextContent();

				XSType type = element.getType();
				if (qname.getLocalPart().contains("NAME")) {
					System.out.println(type.getName());
				}

				ResourceObjectAttributeDefinition attribute = new ResourceObjectAttributeDefinition(qname,
						null, new QName(type.getTargetNamespace(), type.getName()));
				if (!StringUtils.isEmpty(nativeAttributeName)) {
					attribute.setNativeAttributeName(nativeAttributeName);
				}

				attribute.setMinOccurs(p.getMinOccurs());
				attribute.setMaxOccurs(p.getMaxOccurs());
				parseAnnotationForAttribute(annotation, attribute);

				definitions.add(attribute);
			}
		}

		return definitions;
	}

	private void parseAnnotationForObjectClass(XSAnnotation annotation, ResourceObjectDefinition objectClass,
			Element document) {
		if (annotation == null || annotation.getAnnotation() == null) {
			return;
		}

		// displayName
		ResourceObjectAttributeDefinition attrDefinition = getAnnotationReference(annotation, A_DISPLAY_NAME,
				document, objectClass);
		if (attrDefinition != null) {
			objectClass.setDisplayNameAttribute(attrDefinition);
		}
		// descriptionAttribute
		attrDefinition = getAnnotationReference(annotation, A_DESCRIPTION_ATTRIBUTE, document, objectClass);
		if (attrDefinition != null) {
			objectClass.setDescriptionAttribute(attrDefinition);
		}
		// identifier
		attrDefinition = getAnnotationReference(annotation, A_IDENTIFIER, document, objectClass);
		if (attrDefinition != null) {
			objectClass.getIdentifiers().add(attrDefinition);
		}
		// secondaryIdentifier
		attrDefinition = getAnnotationReference(annotation, A_SECONDARY_IDENTIFIER, document, objectClass);
		if (attrDefinition != null) {
			objectClass.getSecondaryIdentifiers().add(attrDefinition);
		}

		// check if it's default account object class...
		Element accountType = getAnnotationElement(annotation, A_ACCOUNT_TYPE);
		if (accountType != null) {
			String defaultValue = accountType.getAttribute("default");
			if (defaultValue != null) {
				objectClass.setDefaultAccountType(Boolean.parseBoolean(defaultValue));
			}
		}
	}

	private void parseAnnotationForAttribute(XSAnnotation annotation,
			ResourceObjectAttributeDefinition attribute) {
		if (annotation == null || annotation.getAnnotation() == null) {
			return;
		}

		// attributeDisplayName
		Element attributeDisplayName = getAnnotationElement(annotation, A_ATTRIBUTE_DISPLAY_NAME);
		if (attributeDisplayName != null) {
			attribute.setDisplayName(attributeDisplayName.getTextContent());
		}

		// help
		Element help = getAnnotationElement(annotation, A_HELP);
		if (help != null) {
			attribute.setHelp(help.getTextContent());
		}

		// flagList
		// TODO: canCreate/canUpdate/canRead

		// encryption, classification
		// TODO: encryption
		// Element encryption = getAnnotationElement(annotation,
		// A_CA_ENCRYPTION);
		// if (encryption != null && encryption.getTextContent() != null) {
		// Element classificationLevel = getAnnotationElement(annotation,
		// A_CA_CLASSIFICATION_LEVEL);
		// String classification = classificationLevel == null ? null :
		// classificationLevel.getTextContent();
		// attribute.makeClassified(
		// ResourceAttributeDefinition.Encryption.valueOf(encryption.getTextContent()),
		// classification);
		// }
	}

	private ResourceObjectAttributeDefinition getAnnotationReference(XSAnnotation annotation, QName qname,
			Element document, ResourceObjectDefinition objectClass) {
		Element element = getAnnotationElement(annotation, qname);
		if (element != null) {
			String reference = element.getAttribute("ref");
			if (reference != null && !reference.isEmpty()) {
				PropertyDefinition definition = objectClass.findPropertyDefinition(getQNameForReference(
						reference, document));
				if (definition instanceof ResourceObjectAttributeDefinition) {
					return (ResourceObjectAttributeDefinition) definition;
				}
			}
		}

		return null;
	}

	private QName getQNameForReference(String reference, Element element) {
		String[] array = reference.split(":");

		String namespace = element.getOwnerDocument().lookupPrefix(array[0]);
		if (namespace == null) {
			namespace = element.getAttribute("xmlns:" + array[0]);
		}

		return new QName(namespace, array[1]);
	}

	private boolean isAccountObject(XSAnnotation annotation) {
		if (annotation == null || annotation.getAnnotation() == null) {
			return false;
		}

		Element accountType = getAnnotationElement(annotation, A_ACCOUNT_TYPE);
		if (accountType != null) {
			return true;
		}

		return false;
	}

	private Element getAnnotationElement(XSAnnotation annotation, QName qname) {
		if (annotation == null) {
			return null;
		}

		Element xsdAnnotation = (Element) annotation.getAnnotation();
		NodeList list = xsdAnnotation.getElementsByTagNameNS(qname.getNamespaceURI(), qname.getLocalPart());
		if (list != null && list.getLength() > 0) {
			return (Element) list.item(0);
		}

		return null;
	}

	private XSAnnotation selectAnnotationToUse(XSAnnotation particleAnnotation, XSAnnotation termAnnotation) {
		boolean useParticleAnnotation = false;
		if (particleAnnotation != null && particleAnnotation.getAnnotation() != null) {
			if (testAnnotationAppinfo(particleAnnotation)) {
				useParticleAnnotation = true;
			}
		}

		boolean useTermAnnotation = false;
		if (termAnnotation != null && termAnnotation.getAnnotation() != null) {
			if (testAnnotationAppinfo(termAnnotation)) {
				useTermAnnotation = true;
			}
		}

		if (useParticleAnnotation) {
			return particleAnnotation;
		}

		if (useTermAnnotation) {
			return termAnnotation;
		}

		return null;
	}

	private boolean testAnnotationAppinfo(XSAnnotation annotation) {
		Element appinfo = getAnnotationElement(annotation, new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		if (appinfo != null) {
			return true;
		}

		return false;
	}

	private XSOMParser createSchemaParser() {
		XSOMParser parser = new XSOMParser();
		parser.setErrorHandler(new SchemaErrorHandler());
		parser.setAnnotationParser(new DomAnnotationParserFactory());
		parser.setEntityResolver(SchemaConstants.getEntityResolver());

		return parser;
	}

	private XSSchemaSet parseSchema(Element schema) throws SchemaProcessorException {
		XSSchemaSet xss = null;
		try {
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(schema);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(out);

			trans.transform(source, result);

			XSOMParser parser = createSchemaParser();
			InputSource inSource = new InputSource(new ByteArrayInputStream(out.toByteArray()));
			inSource.setSystemId("SystemId"); // HACK: it's here to make entity
												// resolver work...
			inSource.setEncoding("utf-8");
			parser.parse(inSource);
			xss = parser.getResult();
		} catch (Exception ex) {
			throw new SchemaProcessorException("Uknown error during resource xsd schema parsing: "
					+ ex.getMessage(), ex);
		}

		return xss;
	}

	@Deprecated
	public static void main(String[] args) throws SchemaProcessorException, IOException {
		File file = new File("./src/main/java/a.xsd");
		
		InputStream stream = new FileInputStream(file);
		Schema schema = Schema.parse(parseDocument(stream));
		Document document = Schema.parseSchema(schema);
		String str = SchemaToDomProcessor.printDom(document).toString();		
		System.out.println(str);
		System.out.println("---------------------------");
		stream = new ByteArrayInputStream(str.getBytes("utf-8"));		
		schema = Schema.parse(parseDocument(stream));
		document = Schema.parseSchema(schema);
		str = SchemaToDomProcessor.printDom(document).toString();		
		System.out.println(str);
	}

	@Deprecated
	private static Element parseDocument(InputStream doc) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.parse(doc).getDocumentElement();
		} catch (SAXException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage());
		} catch (IOException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage());
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage());
		}
	}
}
