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

package com.evolveum.midpoint.prism.schema;

import static com.evolveum.midpoint.prism.PrismConstants.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.aspectj.weaver.bcel.TypeAnnotationAccessVar;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

/**
 * Parser for DOM-represented XSD, creates midPoint Schema representation.
 * 
 * It will parse schema in several passes. It has special handling if the schema is "resource schema", which will
 * create ResourceObjectDefinition and ResourceObjectAttributeDefinition instead of PropertyContainer and Property. 
 * 
 * @author lazyman
 * @author Radovan Semancik
 */
class DomToSchemaProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(DomToSchemaProcessor.class);
	
	private PrismSchema schema;
	private EntityResolver entityResolver;
	private PrismContext prismContext;
	private XSSchemaSet xsSchemaSet;
	
	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	public SchemaRegistry getSchemaRegistry() {
		return this.prismContext.getSchemaRegistry();
	}

	private SchemaDefinitionFactory getDefinitionFactory() {
		return prismContext.getDefinitionFactory();
	}
	
	private String getNamespace() {
		return schema.getNamespace();
	}
	
	private boolean isMyNamespace(QName qname) {
		return getNamespace().equals(qname.getNamespaceURI());
	}

	/**
	 * Main entry point.
	 * 
	 * 
	 * @param xsdSchema DOM-represented XSD schema
	 * @param isResourceSchema true if this is "resource schema"
	 * @return parsed midPoint schema
	 * @throws SchemaException in case of any error
	 */
	PrismSchema parseDom(PrismSchema prismSchema, Element xsdSchema) throws SchemaException {
		Validate.notNull(xsdSchema, "XSD schema element must not be null.");
		
		schema = prismSchema;
		
		initSchema(xsdSchema);

		xsSchemaSet = parseSchema(xsdSchema);
		if (xsSchemaSet == null) {
			return schema;
		}
		
		// Create ComplexTypeDefinitions from all top-level complexType definition in the XSD
		processComplexTypeDefinitions(xsSchemaSet);

		// Create PropertyContainer (and possibly also Property) definition from the top-level elements in XSD
		// This also creates ResourceObjectDefinition in some cases
		createDefinitionsFromElements(xsSchemaSet);
				
		return schema;
	}

	private void initSchema(Element xsdSchema) throws SchemaException {
		String targetNamespace = xsdSchema.getAttributeNS(DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getNamespaceURI(),
				DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getLocalPart());
		if (StringUtils.isEmpty(targetNamespace)) {
			// also try without the namespace
			targetNamespace = xsdSchema.getAttribute(DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getLocalPart());
		}
		if (StringUtils.isEmpty(targetNamespace)) {
			throw new SchemaException("Schema does not have targetNamespace specification");
		}
		schema.setNamespace(targetNamespace);
	}
	
	private XSSchemaSet parseSchema(Element schema) throws SchemaException {
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
			// XXX: hack: it's here to make entity resolver work...
			inSource.setSystemId("SystemId");
			// XXX: end hack
			inSource.setEncoding("utf-8");
			
			parser.parse(inSource);
			
			xss = parser.getResult();
		} catch (Exception ex) {
			throw new SchemaException("Uknown error during resource xsd schema parsing: "
					+ ex.getMessage(), ex);
		}

		return xss;
	}
	
	private XSOMParser createSchemaParser() {
		XSOMParser parser = new XSOMParser();
		if (entityResolver == null) {
			entityResolver = prismContext.getSchemaRegistry().getBuiltinSchemaResolver();
			if (entityResolver == null) {
				throw new IllegalStateException("Entity resolver is not set (even tried to pull it from prism context)");
			}
		}
		SchemaHandler errorHandler = new SchemaHandler(entityResolver);
		parser.setErrorHandler(errorHandler);
		parser.setAnnotationParser(new DomAnnotationParserFactory());
		parser.setEntityResolver(errorHandler);

		return parser;
	}
	
	/**
	 * Create ComplexTypeDefinitions from all top-level complexType definitions in the XSD.
	 * 
	 * These definitions are the reused later to fit inside PropertyContainer definitions.
	 * 
	 * @param set XS Schema Set
	 */
	private void processComplexTypeDefinitions(XSSchemaSet set) throws SchemaException {
		Iterator<XSComplexType> iterator = set.iterateComplexTypes();
		while (iterator.hasNext()) {
			XSComplexType complexType = iterator.next();
			if (complexType.getTargetNamespace().equals(schema.getNamespace())) {
				processComplexTypeDefinition(complexType);
			}
		}
	}
	
	private ComplexTypeDefinition processComplexTypeDefinition(XSComplexType complexType) throws SchemaException {
		// Create the actual definition. This is potentially recursive call
		ComplexTypeDefinition complexTypeDefinition = createComplexTypeDefinition(complexType);
		ComplexTypeDefinition existingComplexTypeDefinition = schema.findComplexTypeDefinition(complexTypeDefinition.getTypeName());
		if (existingComplexTypeDefinition != null) {
			// Already processed
			return existingComplexTypeDefinition;
		}
		schema.getDefinitions().add(complexTypeDefinition);

		// As this is a top-level complex type definition, attempt to create object or container definition
		// from it
		
		PrismContainerDefinition defFromComplexType = getDefinitionFactory().createExtraDefinitionFromComplexType(complexType, complexTypeDefinition, prismContext, 
				complexType.getAnnotation());
		
		if (defFromComplexType != null) {
			schema.getDefinitions().add(defFromComplexType);
		}
		
		return complexTypeDefinition;
	}

	private ComplexTypeDefinition getOrProcessComplexType(QName typeName) throws SchemaException {
		ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinition(typeName);
		if (complexTypeDefinition != null) {
			return complexTypeDefinition;
		}
		// The definition is not yet processed (or does not exist). Let's try to process it.
		XSComplexType complexType = xsSchemaSet.getComplexType(typeName.getNamespaceURI(), typeName.getLocalPart());
		return processComplexTypeDefinition(complexType);
	}


	/**
	 * Creates ComplexTypeDefinition object from a single XSD complexType definition.
	 * @param complexType XS complex type definition
	 */
	private ComplexTypeDefinition createComplexTypeDefinition(XSComplexType complexType) throws SchemaException {
		
		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		ComplexTypeDefinition ctd = definitionFactory.createComplexTypeDefinition(complexType, prismContext, complexType.getAnnotation());
				
		XSContentType content = complexType.getContentType();
		if (content != null) {
			XSParticle particle = content.asParticle();
			if (particle != null) {
				XSTerm term = particle.getTerm();

				if (term.isModelGroup()) {
					addPropertyDefinitionListFromGroup(term.asModelGroup(), ctd);
				}
			}
			
			XSAnnotation annotation = complexType.getAnnotation();
			Element extensionAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_EXTENSION);
			if (extensionAnnotationElement != null) {
				QName extensionType = DOMUtil.getQNameAttribute(extensionAnnotationElement, A_EXTENSION_REF.getLocalPart());
				if (extensionType == null) {
					throw new SchemaException("The "+A_EXTENSION+"annontation on "+ctd.getTypeName()+" complex type does not have "+A_EXTENSION_REF.getLocalPart()+" attribute",A_EXTENSION_REF);
				}
				ctd.setExtensionForType(extensionType);
			}
		}
		
		QName superType = determineSupertype(complexType);
		if (superType != null) {
			ctd.setSuperType(superType);
		}
		
		if (isObjectDefinition(complexType)) {
			ctd.setObjectMarker(true);
		}
		if (isPropertyContainer(complexType)) {
			ctd.setContainerMarker(true);
		}
		
		if (isAny(complexType)) {
			ctd.setXsdAnyMarker(true);
		}
		
		definitionFactory.finishComplexTypeDefinition(ctd, complexType, prismContext, complexType.getAnnotation());
		
		return ctd;

	}

	/**
	 * Creates ComplexTypeDefinition object from a XSModelGroup inside XSD complexType definition.
	 * This is a recursive method. It can create "anonymous" internal PropertyContainerDefinitions.
	 * The definitions will be added to the ComplexTypeDefinition provided as parameter.
	 * @param group XSD XSModelGroup
	 * @param ctd ComplexTypeDefinition that will hold the definitions
	 */
	private void addPropertyDefinitionListFromGroup(XSModelGroup group, ComplexTypeDefinition ctd) throws SchemaException {

		XSParticle[] particles = group.getChildren();
		for (XSParticle p : particles) {
			XSTerm pterm = p.getTerm();
			if (pterm.isModelGroup()) {
				addPropertyDefinitionListFromGroup(pterm.asModelGroup(), ctd);
			}

			// xs:element inside complex type
			if (pterm.isElementDecl()) {
				XSAnnotation annotation = selectAnnotationToUse(p.getAnnotation(), pterm.getAnnotation());

				XSElementDecl elementDecl = pterm.asElementDecl();
				QName elementName = new QName(elementDecl.getTargetNamespace(), elementDecl.getName());
				QName typeFromAnnotation = getTypeAnnotation(p.getAnnotation());
				
				XSType xsType = elementDecl.getType();
			
				if (isObjectReference(xsType, annotation)) {
				
					PrismReferenceDefinition propDef = processObjectReferenceDefinition(xsType, elementName, annotation, ctd, p);					
					setMultiplicity(propDef, p);
				
				} else if (isObjectDefinition(xsType)) {					
					// This is object reference. It also has its *Ref equivalent which will get parsed.
					// therefore it is safe to ignore

				} else if (xsType.getName() == null && typeFromAnnotation == null) {					
					
					if (isAny(xsType)) {
						// This is a element with xsd:any type. It has to be property container
						XSAnnotation containerAnnotation = xsType.getAnnotation();
						PrismContainerDefinition containerDefinition = createPropertyContainerDefinition(xsType, p,
								null, containerAnnotation);
						ctd.getDefinitions().add(containerDefinition);
					}

				} else if (isPropertyContainer(elementDecl)) {
					
					// Create an inner PropertyContainer. It is assumed that this is a XSD complex type
					// TODO: check cast
					XSComplexType complexType = (XSComplexType)xsType;
					ComplexTypeDefinition complexTypeDefinition = null;
					if (typeFromAnnotation != null && complexType != null && !typeFromAnnotation.equals(getType(xsType))) {
						// There is a type override annotation. The type that the schema parser determined is useless
						// We need to locate our own complex type definition
						if (isMyNamespace(typeFromAnnotation)) {
							complexTypeDefinition = getOrProcessComplexType(typeFromAnnotation);
						} else {
							complexTypeDefinition = getPrismContext().getSchemaRegistry().findComplexTypeDefinition(typeFromAnnotation);
						}
						if (complexTypeDefinition == null) {
							throw new SchemaException("Cannot find definition of complex type "+typeFromAnnotation+
									" as specified in type override annotation at "+elementName);
						}
					} else {
						complexTypeDefinition = createComplexTypeDefinition(complexType);
					}
					XSAnnotation containerAnnotation = complexType.getAnnotation();
					PrismContainerDefinition containerDefinition = createPropertyContainerDefinition(xsType, p, 
							complexTypeDefinition, containerAnnotation);
					if (isAny(xsType)) {
						containerDefinition.setRuntimeSchema(true);
					}
					ctd.getDefinitions().add(containerDefinition);
										
				} else {
										
					// Create a property definition (even if this is a XSD complex type)
					QName typeName = new QName(xsType.getTargetNamespace(), xsType.getName());

					PrismPropertyDefinition propDef = createPropertyDefinition(xsType, elementName, typeName, ctd, annotation, p);
					
					setMultiplicity(propDef, p);
					
					ctd.add(propDef);
				}
			}
		}
	}
	
	private PrismReferenceDefinition processObjectReferenceDefinition(XSType xsType, QName elementName,
			XSAnnotation annotation, ComplexTypeDefinition ctd, XSParticle elementParticle) throws SchemaException {
		// Create a property definition (even if this is a XSD complex type)
		QName typeName = new QName(xsType.getTargetNamespace(), xsType.getName());
		QName primaryElementName = elementName;
		Element objRefAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_OBJECT_REFERENCE);
		boolean hasExplicitPrimaryElementName = (objRefAnnotationElement != null && !StringUtils.isEmpty(objRefAnnotationElement.getTextContent()));
		if (hasExplicitPrimaryElementName) {
			primaryElementName = DOMUtil.getQNameValue(objRefAnnotationElement);
		}
		PrismReferenceDefinition definition = ctd.findItemDefinition(primaryElementName, PrismReferenceDefinition.class);
		if (definition == null) {
			SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
			definition = definitionFactory.createReferenceDefinition(primaryElementName, typeName, ctd, prismContext, annotation, elementParticle);
			ctd.add(definition);
		}
		if (hasExplicitPrimaryElementName) {
			// The elements that have explicit type name determine the target type name (if not yet set)
			if (definition.getTargetTypeName() == null) {
				definition.setTargetTypeName(typeName);
			}
			if (definition.getCompositeObjectElementName() == null) {
				definition.setCompositeObjectElementName(elementName);
			}
		} else {
			// The elements that use default element names override type definition
			// as there can be only one such definition, therefore the behavior is deterministic
			definition.setTypeName(typeName);
		}
		Element targetTypeAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_OBJECT_REFERENCE_TARGET_TYPE);
		if (targetTypeAnnotationElement != null && !StringUtils.isEmpty(targetTypeAnnotationElement.getTextContent())) {
			// Explicit definition of target type overrides previous logic
			QName targetType = DOMUtil.getQNameValue(targetTypeAnnotationElement);
			definition.setTargetTypeName(targetType);
		}
		return definition;
	}

	private void setMultiplicity(ItemDefinition propDef, XSParticle p) {
		propDef.setMinOccurs(p.getMinOccurs());
		propDef.setMaxOccurs(p.getMaxOccurs());
	}

	/**
	 * Create PropertyContainer (and possibly also Property) definition from the top-level elements in XSD.
	 * Each top-level element will be interpreted as a potential PropertyContainer. The element name will be set as
	 * name of the PropertyContainer, element type will become type (indirectly through ComplexTypeDefinition).
	 * 
	 * No need to recurse here. All the work was already done while creating ComplexTypeDefinitions.
	 * 
	 * @param set XS Schema Set
	 * @throws SchemaException
	 */
	private void createDefinitionsFromElements(XSSchemaSet set) throws SchemaException {
		Iterator<XSElementDecl> iterator = set.iterateElementDecls();
		while (iterator.hasNext()) {
			XSElementDecl xsElementDecl = iterator.next();
			if (xsElementDecl.getTargetNamespace().equals(schema.getNamespace())) {
				
				QName elementName = new QName(xsElementDecl.getTargetNamespace(),xsElementDecl.getName());
				XSType xsType = xsElementDecl.getType();
				if (xsType == null) {
					throw new SchemaException("Found element "+elementName+" without type definition");
				}
				QName typeQName = determineType(xsElementDecl);
				if (typeQName == null) {
					// No type defined, safe to skip
					continue;
					//throw new SchemaException("Found element "+elementName+" with incomplete type name: {"+xsType.getTargetNamespace()+"}"+xsType.getName());
				}
				XSAnnotation annotation = xsType.getAnnotation();
				
				if (isPropertyContainer(xsElementDecl) || isObjectDefinition(xsType)) {
					
					ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinition(typeQName);
					PrismContainerDefinition propertyContainerDefinition = createPropertyContainerDefinition(xsType, xsElementDecl,
							complexTypeDefinition, annotation, null);
					schema.getDefinitions().add(propertyContainerDefinition);
					
				} else {
					
					// Create a top-level property definition (even if this is a XSD complex type)
					// This is not really useful, just for the sake of completeness
					PrismPropertyDefinition propDef = createPropertyDefinition(xsType, elementName, typeQName, null, annotation, null);
					schema.getDefinitions().add(propDef);
				}
				
			} else if (xsElementDecl.getTargetNamespace().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
				// This is OK to ignore. These are imported elements from other schemas
//				} else {
//					throw new SchemaException("Found element "+xsElementDecl.getName()+" with wrong namespace "+xsElementDecl.getTargetNamespace()+" while expecting "+schema.getNamespace());
			}
		}
	}
	
	private QName determineType(XSElementDecl xsElementDecl) {
		// Check for a:type annotation. If present, this overrides the type
		QName type = getTypeAnnotation(xsElementDecl);
		if (type != null) {
			return type;
		}
		XSType xsType = xsElementDecl.getType();
		if (xsType == null) {
			return null;
		}
		return getType(xsType);		
	}
	
	private QName getType(XSType xsType) {
		if (xsType.getName() == null) {
			return null;
		}
		return new QName(xsType.getTargetNamespace(), xsType.getName());
	}

	private QName getTypeAnnotation(XSElementDecl xsElementDecl) {
		XSAnnotation annotation = xsElementDecl.getAnnotation();
		return getTypeAnnotation(annotation);
	}
	
	private QName getTypeAnnotation(XSAnnotation annotation) {
		return SchemaProcessorUtil.getAnnotationQName(annotation, A_TYPE);
	}
	
	/**
	 * Determine whether the definition contains xsd:any (directly or indirectly)
	 */
	private boolean isAny(XSType xsType) {
		if (xsType instanceof XSComplexType) {
			XSComplexType complexType = (XSComplexType)xsType;
			XSContentType contentType = complexType.getContentType();
			if (contentType != null) {
				XSParticle particle = contentType.asParticle();
				if (particle != null) {
					XSTerm term = particle.getTerm();
					if (term != null) {
						return isAny(term);
					}
				}
			}
		}		
		return false;
	}
	
	private QName determineSupertype(XSComplexType complexType) {
		XSType baseType = complexType.getBaseType();
		if (baseType == null) {
			return null;
		}
		if (baseType.getName().equals("anyType")) {
			return null;
		}
		return new QName(baseType.getTargetNamespace(),baseType.getName());
	}

	/**
	 * Determine whether the definition contains xsd:any (directly or indirectly)
	 */
	private boolean isAny(XSTerm term) {
		if (term.isWildcard()) {
			return true;
		}
		if (term.isModelGroup()) {
			XSParticle[] children = term.asModelGroup().getChildren();
			if (children != null) {
				for (XSParticle childParticle: children) {
					XSTerm childTerm = childParticle.getTerm();
					if (childTerm != null) {
						if (isAny(childTerm)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean isPropertyContainer(XSElementDecl xsElementDecl) {
		Element annoElement = SchemaProcessorUtil.getAnnotationElement(xsElementDecl.getAnnotation(), A_PROPERTY_CONTAINER);
		if (annoElement != null) {
			return true;
		}
		return isPropertyContainer(xsElementDecl.getType());
	}

	/**
	 * Returns true if provides XSD type is a property container. It looks for annotations.
	 */
	private boolean isPropertyContainer(XSType xsType) {
		Element annoElement = SchemaProcessorUtil.getAnnotationElement(xsType.getAnnotation(), A_PROPERTY_CONTAINER);
		if (annoElement != null) {
			return true;
		}
		if (xsType.getBaseType() != null && !xsType.getBaseType().equals(xsType)) {
			return isPropertyContainer(xsType.getBaseType());
		}
		return false;
	}
	
	private boolean isObjectReference(XSType xsType, XSAnnotation annotation) {
		if (isObjectReference(annotation)) {
			return true;
		}
		return isObjectReference(xsType);
	}

	private boolean isObjectReference(XSAnnotation annotation) {
		Element objRefAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_OBJECT_REFERENCE);
		return (objRefAnnotationElement != null);
	}

	private boolean isObjectReference(XSType xsType) {
		return SchemaProcessorUtil.hasAnnotation(xsType, A_OBJECT_REFERENCE);
	}

	/**
	 * Returns true if provides XSD type is an object definition. It looks for a ObjectType supertype.
	 */
	private boolean isObjectDefinition(XSType xsType) {
		return SchemaProcessorUtil.hasAnnotation(xsType, A_OBJECT);
	}

	/**
	 * Creates appropriate instance of PropertyContainerDefinition.
	 * It may be PropertyContainerDefinition itself or one of its subclasses (ResourceObjectDefinition). This method also takes care of parsing
	 * all the annotations and similar fancy stuff.
	 * 
	 * We need to pass createResourceObject flag explicitly here. Because even if we are in resource schema, we want PropertyContainers inside
	 * ResourceObjects, not ResourceObjects inside ResouceObjects.
	 * 
	 * @param xsType
	 * @param elementName name of the created definition
	 * @param complexTypeDefinition complex type to use for the definition.
	 * @param annotation XS annotation to apply.
	 * @param createResourceObject true if ResourceObjectDefinition should be created
	 * @return appropriate PropertyContainerDefinition instance
	 */
	private PrismContainerDefinition createPropertyContainerDefinition(XSType xsType, XSParticle elementParticle, 
			ComplexTypeDefinition complexTypeDefinition, XSAnnotation annotation) throws SchemaException {	
		XSTerm elementTerm = elementParticle.getTerm();
		XSElementDecl elementDecl = elementTerm.asElementDecl();
		
		PrismContainerDefinition pcd = createPropertyContainerDefinition(xsType, elementDecl, complexTypeDefinition, 
				annotation, elementParticle);
		pcd.setMinOccurs(elementParticle.getMinOccurs());
		pcd.setMaxOccurs(elementParticle.getMaxOccurs());
		return pcd;
	}
	
	private PrismContainerDefinition createPropertyContainerDefinition(XSType xsType, XSElementDecl elementDecl, 
			ComplexTypeDefinition complexTypeDefinition, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		
		QName elementName = new QName(elementDecl.getTargetNamespace(), elementDecl.getName());
		PrismContainerDefinition pcd = null;
		
		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		
		if (isObjectDefinition(xsType)) {
			Class compileTimeClass = null;
			if (getSchemaRegistry() != null) {
				compileTimeClass = getSchemaRegistry().determineCompileTimeClass(elementName, complexTypeDefinition);
			}
			pcd = definitionFactory.createObjectDefinition(elementName, complexTypeDefinition, prismContext, 
					compileTimeClass, annotation, elementParticle);
		} else {
			pcd = definitionFactory.createContainerDefinition(elementName, complexTypeDefinition, prismContext, 
					annotation, elementParticle);
		}
		
		// TODO: parse generic annotations
		
		// ignore		
		List<Element> ignore = SchemaProcessorUtil.getAnnotationElements(annotation, A_IGNORE);
		if (ignore != null && !ignore.isEmpty()) {
			if (ignore.size() != 1) {
				// TODO: error!
			}
			String ignoreString = ignore.get(0).getTextContent();
			if (StringUtils.isEmpty(ignoreString)) {
				// Element is present but no content: defaults to "true"
				pcd.setIgnored(true);
			} else {
				pcd.setIgnored(Boolean.parseBoolean(ignoreString));
			}
		}
		
		return pcd;
	}

	/**
	 * Creates appropriate instance of PropertyDefinition.
	 * It creates either PropertyDefinition itself or one of its subclasses (ResourceObjectAttributeDefinition). The behavior
	 * depends of the "mode" of the schema. This method is also processing annotations and other fancy property-relates stuff.
	 */
	private PrismPropertyDefinition createPropertyDefinition(XSType xsType, QName elementName, QName typeName, 
			ComplexTypeDefinition ctd, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		PrismPropertyDefinition prodDef;
		
		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		
		prodDef = definitionFactory.createPropertyDefinition(elementName, typeName, ctd, prismContext, annotation, elementParticle);
		
		// Process generic annotations
		
		if (annotation == null || annotation.getAnnotation() == null) {
			return prodDef;
		}
		
		// ignore		
		List<Element> ignore = SchemaProcessorUtil.getAnnotationElements(annotation, A_IGNORE);
		if (ignore != null && !ignore.isEmpty()) {
			if (ignore.size() != 1) {
				// TODO: error!
			}
			String ignoreString = ignore.get(0).getTextContent();
			if (StringUtils.isEmpty(ignoreString)) {
				// Element is present but no content: defaults to "true"
				prodDef.setIgnored(true);
			} else {
				prodDef.setIgnored(Boolean.parseBoolean(ignoreString));
			}
		}
		
		// access
		List<Element> accessList = SchemaProcessorUtil.getAnnotationElements(annotation, A_ACCESS);
		if (accessList != null && !accessList.isEmpty()) {
			prodDef.setCreate(containsAccessFlag("create", accessList));
			prodDef.setRead(containsAccessFlag("read", accessList));
			prodDef.setUpdate(containsAccessFlag("update", accessList));
		}
		
		// attributeDisplayName
		Element attributeDisplayName = SchemaProcessorUtil.getAnnotationElement(annotation, A_DISPLAY_NAME);
		if (attributeDisplayName != null) {
			prodDef.setDisplayName(attributeDisplayName.getTextContent());
		}
		
		// help
		Element help = SchemaProcessorUtil.getAnnotationElement(annotation, A_HELP);
		if (help != null) {
			prodDef.setHelp(help.getTextContent());
		}
		
		List<Element> accessElements = SchemaProcessorUtil.getAnnotationElements(annotation, A_ACCESS);
		if (accessElements == null || accessElements.isEmpty()) {
			// Default access is read-write-create
			prodDef.setCreate(true);
			prodDef.setUpdate(true);
			prodDef.setRead(true);
		} else {
			prodDef.setCreate(false);
			prodDef.setUpdate(false);
			prodDef.setRead(false);
			for (Element e : accessElements) {
				String access = e.getTextContent();
				if (access.equals(A_ACCESS_CREATE)) {
					prodDef.setCreate(true);
				}
				if (access.equals(A_ACCESS_UPDATE)) {
					prodDef.setUpdate(true);
				}
				if (access.equals(A_ACCESS_READ)) {
					prodDef.setRead(true);
				}
			}
		}
		
		return prodDef;
	}


	private boolean containsAccessFlag(String flag, List<Element> accessList) {
		for (Element element : accessList) {
			if (flag.equals(element.getTextContent())) {
				return true;
			}
		}

		return false;
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
		Element appinfo = SchemaProcessorUtil.getAnnotationElement(annotation, new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		if (appinfo != null) {
			return true;
		}

		return false;
	}


	
}
