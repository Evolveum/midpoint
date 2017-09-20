/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.xml.xsom.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.namespace.QName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static com.evolveum.midpoint.prism.PrismConstants.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Parser for DOM-represented XSD, creates midPoint Schema representation.
 *
 * It will parse schema in several passes. It has special handling if the schema
 * is "resource schema", which will create ResourceObjectDefinition and
 * ResourceObjectAttributeDefinition instead of PropertyContainer and Property.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
class DomToSchemaPostProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(DomToSchemaPostProcessor.class);

	private final XSSchemaSet xsSchemaSet;
	private final EntityResolver entityResolver;
	private final PrismContext prismContext;
	private PrismSchemaImpl schema;
	private String shortDescription;
	private boolean isRuntime;
	private boolean allowDelayedItemDefinitions;

	DomToSchemaPostProcessor(XSSchemaSet xsSchemaSet, EntityResolver entityResolver,
			PrismContext prismContext) {
		this.xsSchemaSet = xsSchemaSet;
		this.entityResolver = entityResolver;
		this.prismContext = prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		return this.prismContext.getSchemaRegistry();
	}

	private SchemaDefinitionFactory getDefinitionFactory() {
		return ((PrismContextImpl) prismContext).getDefinitionFactory();
	}

	private String getNamespace() {
		return schema.getNamespace();
	}

	private boolean isMyNamespace(QName qname) {
		return getNamespace().equals(qname.getNamespaceURI());
	}

	/**
	 * Main entry point.
	 */
	void postprocessSchema(PrismSchemaImpl prismSchema, boolean isRuntime, boolean allowDelayedItemDefinitions, String shortDescription) throws SchemaException {
		this.schema = prismSchema;
		this.isRuntime = isRuntime;
		this.allowDelayedItemDefinitions = allowDelayedItemDefinitions;
		this.shortDescription = shortDescription;

		// Create ComplexTypeDefinitions from all top-level complexType
		// definition in the XSD
		processComplexTypeDefinitions(xsSchemaSet);

		// Create SimpleTypeDefinitions from all top-level simpleType
		// definition in the XSD
		processSimpleTypeDefinitions(xsSchemaSet);

		// Create PropertyContainer (and possibly also Property) definition from
		// the top-level elements in XSD
		// This also creates ResourceObjectDefinition in some cases
		createDefinitionsFromElements(xsSchemaSet);
	}

	/**
	 * Create ComplexTypeDefinitions from all top-level complexType definitions
	 * in the XSD.
	 *
	 * These definitions are the reused later to fit inside PropertyContainer
	 * definitions.
	 *
	 * @param set
	 *            XS Schema Set
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

	private ComplexTypeDefinition getOrProcessComplexType(QName typeName) throws SchemaException {
		ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinition(typeName);
		if (complexTypeDefinition != null) {
			return complexTypeDefinition;
		}
		// The definition is not yet processed (or does not exist). Let's try to
		// process it.
		XSComplexType complexType = xsSchemaSet.getComplexType(typeName.getNamespaceURI(),
				typeName.getLocalPart());
		return processComplexTypeDefinition(complexType);
	}

	/**
	 * Creates ComplexTypeDefinition object from a single XSD complexType
	 * definition.
	 *
	 * @param complexType
	 *            XS complex type definition
	 */
	private ComplexTypeDefinition processComplexTypeDefinition(XSComplexType complexType)
			throws SchemaException {

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		ComplexTypeDefinitionImpl ctd = (ComplexTypeDefinitionImpl) definitionFactory.createComplexTypeDefinition(complexType, prismContext,
				complexType.getAnnotation());

		ComplexTypeDefinition existingComplexTypeDefinition = schema
				.findComplexTypeDefinition(ctd.getTypeName());
		if (existingComplexTypeDefinition != null) {
			// We already have this in schema. So avoid redundant work and
			// infinite loops;
			return existingComplexTypeDefinition;
		}
		// Add to the schema right now to avoid loops - even if it is not
		// complete yet
		// The definition may reference itself
		schema.add(ctd);

		XSContentType content = complexType.getContentType();
		XSContentType explicitContent = complexType.getExplicitContent();
		if (content != null) {
			XSParticle particle = content.asParticle();
			if (particle != null) {
				XSTerm term = particle.getTerm();

				if (term.isModelGroup()) {
					Boolean inherited = null;
					if (explicitContent == null || content == explicitContent) {
						inherited = false;
					}
					addPropertyDefinitionListFromGroup(term.asModelGroup(), ctd, inherited, explicitContent);
				}
			}

			XSAnnotation annotation = complexType.getAnnotation();
			Element extensionAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation,
					A_EXTENSION);
			if (extensionAnnotationElement != null) {
				QName extensionType = DOMUtil.getQNameAttribute(extensionAnnotationElement,
						A_EXTENSION_REF.getLocalPart());
				if (extensionType == null) {
					throw new SchemaException("The " + A_EXTENSION + "annontation on " + ctd.getTypeName()
							+ " complex type does not have " + A_EXTENSION_REF.getLocalPart() + " attribute",
							A_EXTENSION_REF);
				}
				ctd.setExtensionForType(extensionType);
			}
		}

		markRuntime(ctd);

		if (complexType.isAbstract()) {
			ctd.setAbstract(true);
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

		ctd.setDefaultNamespace(getDefaultNamespace(complexType));
		ctd.setIgnoredNamespaces(getIgnoredNamespaces(complexType));

		if (isAny(complexType)) {
			ctd.setXsdAnyMarker(true);
		}

		if (isList(complexType)) {
			ctd.setListMarker(true);
		}

		extractDocumentation(ctd, complexType.getAnnotation());

		if (getSchemaRegistry() != null) {
			Class<?> compileTimeClass = getSchemaRegistry().determineCompileTimeClass(ctd.getTypeName());
			ctd.setCompileTimeClass(compileTimeClass);
		}

		definitionFactory.finishComplexTypeDefinition(ctd, complexType, prismContext,
				complexType.getAnnotation());

		// Attempt to create object or container definition from this complex
		// type

		PrismContainerDefinition<?> defFromComplexType = getDefinitionFactory()
				.createExtraDefinitionFromComplexType(complexType, ctd, prismContext,
						complexType.getAnnotation());

		if (defFromComplexType != null) {
			markRuntime(defFromComplexType);
			schema.add(defFromComplexType);
		}

		return ctd;

	}

	private void processSimpleTypeDefinitions(XSSchemaSet set) throws SchemaException {
		Iterator<XSSimpleType> iterator = set.iterateSimpleTypes();
		while (iterator.hasNext()) {
			XSSimpleType simpleType = iterator.next();
			if (simpleType.getTargetNamespace().equals(schema.getNamespace())) {
				processSimpleTypeDefinition(simpleType);
			}
		}
	}

	private SimpleTypeDefinition processSimpleTypeDefinition(XSSimpleType simpleType)
			throws SchemaException {

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		SimpleTypeDefinitionImpl std = (SimpleTypeDefinitionImpl) definitionFactory.createSimpleTypeDefinition(simpleType, prismContext,
				simpleType.getAnnotation());

		SimpleTypeDefinition existingSimpleTypeDefinition = schema.findSimpleTypeDefinitionByType(std.getTypeName());
		if (existingSimpleTypeDefinition != null) {
			// We already have this in schema. So avoid redundant work
			return existingSimpleTypeDefinition;
		}
		markRuntime(std);

		QName superType = determineSupertype(simpleType);
		if (superType != null) {
			std.setSuperType(superType);
		}

		extractDocumentation(std, simpleType.getAnnotation());

		if (getSchemaRegistry() != null) {
			Class<?> compileTimeClass = getSchemaRegistry().determineCompileTimeClass(std.getTypeName());
			std.setCompileTimeClass(compileTimeClass);
		}

		schema.add(std);
		return std;
	}

	private void extractDocumentation(Definition definition, XSAnnotation annotation) {
		if (annotation == null) {
			return;
		}
		Element documentationElement = SchemaProcessorUtil.getAnnotationElement(annotation,
				DOMUtil.XSD_DOCUMENTATION_ELEMENT);
		if (documentationElement != null) {
			// The documentation may be HTML-formatted. Therefore we want to
			// keep the formatting and tag names
			String documentationText = DOMUtil.serializeElementContent(documentationElement);
			((DefinitionImpl) definition).setDocumentation(documentationText);
		}
	}

	/**
	 * Creates ComplexTypeDefinition object from a XSModelGroup inside XSD
	 * complexType definition. This is a recursive method. It can create
	 * "anonymous" internal PropertyContainerDefinitions. The definitions will
	 * be added to the ComplexTypeDefinition provided as parameter.
	 *
	 * @param group
	 *            XSD XSModelGroup
	 * @param ctd
	 *            ComplexTypeDefinition that will hold the definitions
	 * @param inherited
	 *            Are these properties inherited? (null means we don't know and
	 *            we'll determine that from explicitContent)
	 * @param explicitContent
	 *            Explicit (i.e. non-inherited) content of the type being parsed
	 *            - filled-in only for subtypes!
	 */
	private void addPropertyDefinitionListFromGroup(XSModelGroup group, ComplexTypeDefinition ctd,
			Boolean inherited, XSContentType explicitContent) throws SchemaException {

		XSParticle[] particles = group.getChildren();
		for (XSParticle p : particles) {
			boolean particleInherited = inherited != null ? inherited : (p != explicitContent);
			XSTerm pterm = p.getTerm();
			if (pterm.isModelGroup()) {
				addPropertyDefinitionListFromGroup(pterm.asModelGroup(), ctd, particleInherited,
						explicitContent);
			}

			// xs:element inside complex type
			if (pterm.isElementDecl()) {
				XSAnnotation annotation = selectAnnotationToUse(p.getAnnotation(), pterm.getAnnotation());

				XSElementDecl elementDecl = pterm.asElementDecl();
				QName elementName = new QName(elementDecl.getTargetNamespace(), elementDecl.getName());
				QName typeFromAnnotation = getTypeAnnotation(p.getAnnotation());

				XSType xsType = elementDecl.getType();

				if (isObjectReference(xsType, annotation)) {

					processObjectReferenceDefinition(xsType, elementName, annotation, ctd, p,
							particleInherited);

				} else if (isObjectDefinition(xsType)) {
					// This is object reference. It also has its *Ref equivalent
					// which will get parsed.
					// therefore it is safe to ignore

				} else if (xsType.getName() == null && typeFromAnnotation == null) {

					if (isAny(xsType)) {
						if (isPropertyContainer(elementDecl)) {
							XSAnnotation containerAnnotation = xsType.getAnnotation();
							PrismContainerDefinition<?> containerDefinition = createPropertyContainerDefinition(
									xsType, p, null, containerAnnotation, false);
							((PrismContainerDefinitionImpl) containerDefinition).setInherited(particleInherited);
							((ComplexTypeDefinitionImpl) ctd).add(containerDefinition);
						} else {
							PrismPropertyDefinitionImpl propDef = createPropertyDefinition(xsType, elementName,
									DOMUtil.XSD_ANY, ctd, annotation, p);
							propDef.setInherited(particleInherited);
							((ComplexTypeDefinitionImpl) ctd).add(propDef);
						}
					}

				} else if (isPropertyContainer(elementDecl)) {

					// Create an inner PropertyContainer. It is assumed that
					// this is a XSD complex type
					XSComplexType complexType = (XSComplexType) xsType;
					ComplexTypeDefinition complexTypeDefinition = null;
					if (typeFromAnnotation != null && complexType != null
							&& !typeFromAnnotation.equals(getType(xsType))) {
						// There is a type override annotation. The type that
						// the schema parser determined is useless
						// We need to locate our own complex type definition
						if (isMyNamespace(typeFromAnnotation)) {
							complexTypeDefinition = getOrProcessComplexType(typeFromAnnotation);
						} else {
							complexTypeDefinition = prismContext.getSchemaRegistry()
									.findComplexTypeDefinition(typeFromAnnotation);
						}
						if (complexTypeDefinition == null) {
							throw new SchemaException(
									"Cannot find definition of complex type " + typeFromAnnotation
											+ " as specified in type override annotation at " + elementName);
						}
					} else {
						complexTypeDefinition = processComplexTypeDefinition(complexType);
					}
					XSAnnotation containerAnnotation = complexType.getAnnotation();
					PrismContainerDefinition<?> containerDefinition = createPropertyContainerDefinition(
							xsType, p, complexTypeDefinition, containerAnnotation, false);
					if (isAny(xsType)) {
						((PrismContainerDefinitionImpl) containerDefinition).setRuntimeSchema(true);
						((PrismContainerDefinitionImpl) containerDefinition).setDynamic(true);
					}
					((PrismContainerDefinitionImpl) containerDefinition).setInherited(particleInherited);
					((ComplexTypeDefinitionImpl) ctd).add(containerDefinition);

				} else {

					// Create a property definition (even if this is a XSD
					// complex type)
					QName typeName = new QName(xsType.getTargetNamespace(), xsType.getName());

					PrismPropertyDefinitionImpl propDef = createPropertyDefinition(xsType, elementName, typeName,
							ctd, annotation, p);
					propDef.setInherited(particleInherited);
					((ComplexTypeDefinitionImpl) ctd).add(propDef);
				}
			}
		}
	}

	private PrismReferenceDefinitionImpl processObjectReferenceDefinition(XSType xsType, QName elementName,
			XSAnnotation annotation, ComplexTypeDefinition containingCtd, XSParticle elementParticle,
			boolean inherited) throws SchemaException {
		QName typeName = new QName(xsType.getTargetNamespace(), xsType.getName());
		QName primaryElementName = elementName;
		Element objRefAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation,
				A_OBJECT_REFERENCE);
		boolean hasExplicitPrimaryElementName = (objRefAnnotationElement != null
				&& !StringUtils.isEmpty(objRefAnnotationElement.getTextContent()));
		if (hasExplicitPrimaryElementName) {
			primaryElementName = DOMUtil.getQNameValue(objRefAnnotationElement);
		}
		PrismReferenceDefinitionImpl definition = null;
		if (containingCtd != null) {
			definition = (PrismReferenceDefinitionImpl) containingCtd.findItemDefinition(primaryElementName, PrismReferenceDefinition.class);
		}
		if (definition == null) {
			SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
			definition = (PrismReferenceDefinitionImpl) definitionFactory.createReferenceDefinition(primaryElementName, typeName,
					containingCtd, prismContext, annotation, elementParticle);
			definition.setInherited(inherited);
			if (containingCtd != null) {
				((ComplexTypeDefinitionImpl) containingCtd).add(definition);
			}
		}
		if (hasExplicitPrimaryElementName) {
			// The elements that have explicit type name determine the target
			// type name (if not yet set)
			if (definition.getTargetTypeName() == null) {
				definition.setTargetTypeName(typeName);
			}
			if (definition.getCompositeObjectElementName() == null) {
				definition.setCompositeObjectElementName(elementName);
			}
		} else {
			// The elements that use default element names override type
			// definition
			// as there can be only one such definition, therefore the behavior
			// is deterministic
			definition.setTypeName(typeName);
		}
		Element targetTypeAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation,
				A_OBJECT_REFERENCE_TARGET_TYPE);
		if (targetTypeAnnotationElement != null
				&& !StringUtils.isEmpty(targetTypeAnnotationElement.getTextContent())) {
			// Explicit definition of target type overrides previous logic
			QName targetType = DOMUtil.getQNameValue(targetTypeAnnotationElement);
			definition.setTargetTypeName(targetType);
		}
		setMultiplicity(definition, elementParticle, annotation, false);
		Boolean composite = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_COMPOSITE);
		if (composite != null) {
			definition.setComposite(composite);
		}

		parseItemDefinitionAnnotations(definition, annotation);
		// extractDocumentation(definition, annotation);
		return definition;
	}

	private void setMultiplicity(ItemDefinition itemDef, XSParticle particle, XSAnnotation annotation,
			boolean topLevel) {
		if (topLevel || particle == null) {
			((ItemDefinitionImpl) itemDef).setMinOccurs(0);
			Element maxOccursAnnotation = SchemaProcessorUtil.getAnnotationElement(annotation, A_MAX_OCCURS);
			if (maxOccursAnnotation != null) {
				String maxOccursString = maxOccursAnnotation.getTextContent();
				int maxOccurs = XsdTypeMapper.multiplicityToInteger(maxOccursString);
				((ItemDefinitionImpl) itemDef).setMaxOccurs(maxOccurs);
			} else {
				((ItemDefinitionImpl) itemDef).setMaxOccurs(-1);
			}
		} else {
			// itemDef.setMinOccurs(particle.getMinOccurs());
			// itemDef.setMaxOccurs(particle.getMaxOccurs());
			((ItemDefinitionImpl) itemDef).setMinOccurs(particle.getMinOccurs().intValue());
			((ItemDefinitionImpl) itemDef).setMaxOccurs(particle.getMaxOccurs().intValue());
		}
	}

	/**
	 * Create PropertyContainer (and possibly also Property) definition from the
	 * top-level elements in XSD. Each top-level element will be interpreted as
	 * a potential PropertyContainer. The element name will be set as name of
	 * the PropertyContainer, element type will become type (indirectly through
	 * ComplexTypeDefinition).
	 *
	 * No need to recurse here. All the work was already done while creating
	 * ComplexTypeDefinitions.
	 *
	 * @param set
	 *            XS Schema Set
	 * @throws SchemaException
	 */
	private void createDefinitionsFromElements(XSSchemaSet set) throws SchemaException {
		Iterator<XSElementDecl> iterator = set.iterateElementDecls();
		while (iterator.hasNext()) {
			XSElementDecl xsElementDecl = iterator.next();
			if (isDeprecated(xsElementDecl)) {
				// Safe to ignore. We want it in the XSD schema only. The real
				// definition will be
				// parsed from the non-deprecated variant
			}

			if (xsElementDecl.getTargetNamespace().equals(schema.getNamespace())) {

				QName elementName = new QName(xsElementDecl.getTargetNamespace(), xsElementDecl.getName());
				XSType xsType = xsElementDecl.getType();
				if (xsType == null) {
					throw new SchemaException("Found element " + elementName + " without type definition");
				}
				QName typeQName = determineType(xsElementDecl);
				if (typeQName == null) {
					// No type defined, safe to skip
					continue;
					// throw new SchemaException("Found element "+elementName+"
					// with incomplete type name:
					// {"+xsType.getTargetNamespace()+"}"+xsType.getName());
				}
				XSAnnotation annotation = xsElementDecl.getAnnotation();
				ItemDefinitionImpl definition;

				if (isPropertyContainer(xsElementDecl) || isObjectDefinition(xsType)) {
					ComplexTypeDefinition complexTypeDefinition = findComplexTypeDefinition(typeQName);
					if (complexTypeDefinition == null) {
						if (!allowDelayedItemDefinitions) {
							throw new SchemaException("Couldn't parse prism container " + elementName + " of type " + typeQName
								+ " because complex type definition couldn't be found and delayed item definitions are not allowed.");
						}
						definition = null;
						schema.addDelayedItemDefinition(() -> {
							ComplexTypeDefinition ctd = findComplexTypeDefinition(typeQName);
							// here we take the risk that ctd is null
							return createPropertyContainerDefinition(xsType, xsElementDecl, ctd, annotation, null, true);
						});
					} else {
						definition = createPropertyContainerDefinition(
								xsType, xsElementDecl, complexTypeDefinition, annotation, null, true);
					}
				} else if (isObjectReference(xsElementDecl, xsType)) {
					definition = processObjectReferenceDefinition(xsType, elementName,
							annotation, null, null, false);
				} else {
					// Create a top-level property definition (even if this is a XSD complex type)
					definition = createPropertyDefinition(xsType, elementName, typeQName,
							null, annotation, null);
				}
				if (definition != null) {
					definition.setSubstitutionHead(getSubstitutionHead(xsElementDecl));
					schema.add(definition);
				}

			} else { //if (xsElementDecl.getTargetNamespace().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
				// This is OK to ignore. These are imported elements from other
				// schemas
				// } else {
				// throw new SchemaException("Found element
				// "+xsElementDecl.getName()+" with wrong namespace
				// "+xsElementDecl.getTargetNamespace()+" while expecting
				// "+schema.getNamespace());
			}
		}
	}

	// We first try to find the definition locally, because in schema registry we don't have the current schema yet.
	private ComplexTypeDefinition findComplexTypeDefinition(QName typeQName) {
		ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinitionByType(typeQName);
		if (complexTypeDefinition == null) {
			complexTypeDefinition = getSchemaRegistry().findComplexTypeDefinitionByType(typeQName);
		}
		return complexTypeDefinition;
	}

	private QName getSubstitutionHead(XSElementDecl element) {
		XSElementDecl head = element.getSubstAffiliation();
		if (head == null) {
			return null;
		} else {
			return new QName(head.getTargetNamespace(), head.getName());
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
			XSComplexType complexType = (XSComplexType) xsType;
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

	/**
	 * Determine whether the definition contains "list" attribute (directly or indirectly)
	 */
	private boolean isList(XSComplexType complexType) {
		Collection<? extends XSAttributeUse> attributeUses = complexType.getAttributeUses();
		return attributeUses != null && attributeUses.stream()
				.anyMatch(au -> au.getDecl() != null && DOMUtil.IS_LIST_ATTRIBUTE_NAME.equals(au.getDecl().getName()));
	}

	// not much tested
	private void applyToDeclarations(XSComponent component, Consumer<XSDeclaration> consumer) {
		if (component == null) {
			return;
		}
		if (component instanceof XSDeclaration) {
			consumer.accept((XSDeclaration) component);
		}
		// recursion (if needed)
		if (component instanceof XSParticle) {
			applyToDeclarations(((XSParticle) component).getTerm(), consumer);
		} else if (component instanceof XSModelGroup) {
			for (XSParticle particle : ((XSModelGroup) component).getChildren()) {
				applyToDeclarations(particle, consumer);
			}
		} else if (component instanceof XSModelGroupDecl) {
			applyToDeclarations(((XSModelGroupDecl) component).getModelGroup(), consumer);
		}
	}

	private QName determineSupertype(XSType type) {
		XSType baseType = type.getBaseType();
		if (baseType == null) {
			return null;
		}
		if (baseType.getName().equals("anyType")) {
			return null;
		}
		return new QName(baseType.getTargetNamespace(), baseType.getName());
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
				for (XSParticle childParticle : children) {
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
		Element annoElement = SchemaProcessorUtil.getAnnotationElement(xsElementDecl.getAnnotation(),
				A_PROPERTY_CONTAINER);
		if (annoElement != null) {
			return true;
		}
		return isPropertyContainer(xsElementDecl.getType());
	}

	/**
	 * Returns true if provides XSD type is a property container. It looks for
	 * annotations.
	 */
	private boolean isPropertyContainer(XSType xsType) {
		Element annoElement = SchemaProcessorUtil.getAnnotationElement(xsType.getAnnotation(),
				A_PROPERTY_CONTAINER);
		if (annoElement != null) {
			return true;
		}
		if (xsType.getBaseType() != null && !xsType.getBaseType().equals(xsType)) {
			return isPropertyContainer(xsType.getBaseType());
		}
		return false;
	}

	private String getDefaultNamespace(XSType xsType) {
		Element annoElement = SchemaProcessorUtil.getAnnotationElement(xsType.getAnnotation(), A_DEFAULT_NAMESPACE);
		if (annoElement != null) {
			return annoElement.getTextContent();
		}
		if (xsType.getBaseType() != null && !xsType.getBaseType().equals(xsType)) {
			return getDefaultNamespace(xsType.getBaseType());
		}
		return null;
	}

	@NotNull
	private List<String> getIgnoredNamespaces(XSType xsType) {
		List<String> rv = new ArrayList<>();
		List<Element> annoElements = SchemaProcessorUtil.getAnnotationElements(xsType.getAnnotation(), A_IGNORED_NAMESPACE);
		for (Element annoElement : annoElements) {
			rv.add(annoElement.getTextContent());
		}
		if (xsType.getBaseType() != null && !xsType.getBaseType().equals(xsType)) {
			rv.addAll(getIgnoredNamespaces(xsType.getBaseType()));
		}
		return rv;
	}

	private boolean isObjectReference(XSElementDecl xsElementDecl, XSType xsType) {
		XSAnnotation annotation = xsType.getAnnotation();
		return isObjectReference(xsType, annotation);
	}

	private boolean isObjectReference(XSType xsType, XSAnnotation annotation) {
		if (isObjectReference(annotation)) {
			return true;
		}
		return isObjectReference(xsType);
	}

	private boolean isObjectReference(XSAnnotation annotation) {
		Element objRefAnnotationElement = SchemaProcessorUtil.getAnnotationElement(annotation,
				A_OBJECT_REFERENCE);
		return (objRefAnnotationElement != null);
	}

	private boolean isObjectReference(XSType xsType) {
		return SchemaProcessorUtil.hasAnnotation(xsType, A_OBJECT_REFERENCE);
	}

	/**
	 * Returns true if provides XSD type is an object definition. It looks for a
	 * ObjectType supertype.
	 */
	private boolean isObjectDefinition(XSType xsType) {
		return SchemaProcessorUtil.hasAnnotation(xsType, A_OBJECT);
	}

	/**
	 * Creates appropriate instance of PropertyContainerDefinition. It may be
	 * PropertyContainerDefinition itself or one of its subclasses
	 * (ResourceObjectDefinition). This method also takes care of parsing all
	 * the annotations and similar fancy stuff.
	 *
	 * We need to pass createResourceObject flag explicitly here. Because even
	 * if we are in resource schema, we want PropertyContainers inside
	 * ResourceObjects, not ResourceObjects inside ResouceObjects.
	 */
	private PrismContainerDefinition<?> createPropertyContainerDefinition(XSType xsType,
			XSParticle elementParticle, ComplexTypeDefinition complexTypeDefinition, XSAnnotation annotation,
			boolean topLevel) throws SchemaException {
		XSTerm elementTerm = elementParticle.getTerm();
		XSElementDecl elementDecl = elementTerm.asElementDecl();

		PrismContainerDefinition<?> pcd = createPropertyContainerDefinition(xsType, elementDecl,
				complexTypeDefinition, annotation, elementParticle, topLevel);
		return pcd;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private PrismContainerDefinitionImpl<?> createPropertyContainerDefinition(XSType xsType,
			XSElementDecl elementDecl, ComplexTypeDefinition complexTypeDefinition,
			XSAnnotation annotation, XSParticle elementParticle, boolean topLevel)
					throws SchemaException {

		QName elementName = new QName(elementDecl.getTargetNamespace(), elementDecl.getName());
		PrismContainerDefinitionImpl<?> pcd;

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();

		Class compileTimeClass = null;
		if (getSchemaRegistry() != null && complexTypeDefinition != null) {
			compileTimeClass = getSchemaRegistry().determineCompileTimeClass(complexTypeDefinition.getTypeName());
		}
		if (isObjectDefinition(xsType)) {
			pcd = definitionFactory.createObjectDefinition(elementName, complexTypeDefinition, prismContext, compileTimeClass);
			// Multiplicity is fixed to a single-value here
			pcd.setMinOccurs(1);
			pcd.setMaxOccurs(1);
		} else {
			pcd = definitionFactory.createContainerDefinition(elementName, complexTypeDefinition, prismContext, compileTimeClass);
			setMultiplicity(pcd, elementParticle, elementDecl.getAnnotation(), topLevel);
		}

		markRuntime(pcd);

		parseItemDefinitionAnnotations(pcd, annotation);
		parseItemDefinitionAnnotations(pcd, elementDecl.getAnnotation());
		if (elementParticle != null) {
			parseItemDefinitionAnnotations(pcd, elementParticle.getAnnotation());
		}

		return pcd;
	}

	/**
	 * Creates appropriate instance of PropertyDefinition. It creates either
	 * PropertyDefinition itself or one of its subclasses
	 * (ResourceObjectAttributeDefinition). The behavior depends of the "mode"
	 * of the schema. This method is also processing annotations and other fancy
	 * property-relates stuff.
	 */
	private <T> PrismPropertyDefinitionImpl<T> createPropertyDefinition(XSType xsType, QName elementName,
			QName typeName, ComplexTypeDefinition ctd, XSAnnotation annotation, XSParticle elementParticle)
					throws SchemaException {
		PrismPropertyDefinitionImpl<T> propDef;

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();

		Collection<? extends DisplayableValue<T>> allowedValues = parseEnumAllowedValues(typeName, ctd,
				xsType);

		Object defaultValue = parseDefaultValue(elementParticle, typeName);

		propDef = (PrismPropertyDefinitionImpl) definitionFactory.createPropertyDefinition(elementName, typeName, ctd, prismContext,
				annotation, elementParticle, allowedValues, null);
		setMultiplicity(propDef, elementParticle, annotation, ctd == null);

		// Process generic annotations
		parseItemDefinitionAnnotations(propDef, annotation);

		List<Element> accessElements = SchemaProcessorUtil.getAnnotationElements(annotation, A_ACCESS);
		if (accessElements.isEmpty()) {
			// Default access is read-write-create
			propDef.setCanAdd(true);
			propDef.setCanModify(true);
			propDef.setCanRead(true);
		} else {
			propDef.setCanAdd(false);
			propDef.setCanModify(false);
			propDef.setCanRead(false);
			for (Element e : accessElements) {
				String access = e.getTextContent();
				if (access.equals(A_ACCESS_CREATE)) {
					propDef.setCanAdd(true);
				}
				if (access.equals(A_ACCESS_UPDATE)) {
					propDef.setCanModify(true);
				}
				if (access.equals(A_ACCESS_READ)) {
					propDef.setCanRead(true);
				}
			}
		}

		markRuntime(propDef);

		Element indexableElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_INDEXED);
		if (indexableElement != null) {
			Boolean indexable = XmlTypeConverter.toJavaValue(indexableElement, Boolean.class);
			propDef.setIndexed(indexable);
		}

		Element matchingRuleElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_MATCHING_RULE);
		if (matchingRuleElement != null) {
			QName matchingRule = XmlTypeConverter.toJavaValue(matchingRuleElement, QName.class);
			propDef.setMatchingRuleQName(matchingRule);
		}

		Element valueEnumerationRefElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_VALUE_ENUMERATION_REF);
		if (valueEnumerationRefElement != null) {
			String oid = valueEnumerationRefElement.getAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME);
			if (oid != null) {
				QName targetType = DOMUtil.getQNameAttribute(valueEnumerationRefElement, PrismConstants.ATTRIBUTE_REF_TYPE_LOCAL_NAME);
				PrismReferenceValue valueEnumerationRef = new PrismReferenceValue(oid, targetType);
				propDef.setValueEnumerationRef(valueEnumerationRef);
			}
		}

		return propDef;
	}

	private Object parseDefaultValue(XSParticle elementParticle, QName typeName) {
		if (elementParticle == null) {
			return null;
		}
		XSTerm term = elementParticle.getTerm();
		if (term == null) {
			return null;
		}

		XSElementDecl elementDecl = term.asElementDecl();
		if (elementDecl == null) {
			return null;
		}
		if (elementDecl.getDefaultValue() != null) {
			if (XmlTypeConverter.canConvert(typeName)) {
				return XmlTypeConverter.toJavaValue(elementDecl.getDefaultValue().value, typeName);
			}
			return elementDecl.getDefaultValue().value;
		}
		return null;
	}

	private <T> Collection<? extends DisplayableValue<T>> parseEnumAllowedValues(QName typeName,
			ComplexTypeDefinition ctd, XSType xsType) {
		if (xsType.isSimpleType()) {
			if (xsType.asSimpleType().isRestriction()) {
				XSRestrictionSimpleType restriction = xsType.asSimpleType().asRestriction();
				List<XSFacet> enumerations = restriction.getDeclaredFacets(XSFacet.FACET_ENUMERATION);
				List<DisplayableValueImpl<T>> enumValues = new ArrayList<DisplayableValueImpl<T>>(
						enumerations.size());
				for (XSFacet facet : enumerations) {
					String value = facet.getValue().value;
					Element descriptionE = SchemaProcessorUtil.getAnnotationElement(facet.getAnnotation(),
							SCHEMA_DOCUMENTATION);
					Element appInfo = SchemaProcessorUtil.getAnnotationElement(facet.getAnnotation(),
							SCHEMA_APP_INFO);
					Element valueE = null;
					if (appInfo != null) {
						NodeList list = appInfo.getElementsByTagNameNS(
								PrismConstants.A_LABEL.getNamespaceURI(),
								PrismConstants.A_LABEL.getLocalPart());
						if (list.getLength() != 0) {
							valueE = (Element) list.item(0);
						}
					}
					String label = null;
					if (valueE != null) {
						label = valueE.getTextContent();
					} else {
						label = value;
					}
					DisplayableValueImpl<T> edv = null;
					Class compileTimeClass = prismContext.getSchemaRegistry().getCompileTimeClass(typeName);
					if (ctd != null && !ctd.isRuntimeSchema() && compileTimeClass != null) {

						String fieldName = null;
						for (Field field : compileTimeClass.getDeclaredFields()) {
							XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
							if (xmlEnumValue != null && xmlEnumValue.value() != null
									&& xmlEnumValue.value().equals(value)) {
								fieldName = field.getName();
							}

						}
						if (fieldName != null) {
							T enumValue = (T) Enum.valueOf((Class<Enum>) compileTimeClass, fieldName);
							edv = new DisplayableValueImpl(enumValue, label,
									descriptionE != null ? descriptionE.getTextContent() : null);
						} else {
							edv = new DisplayableValueImpl(value, label,
									descriptionE != null ? descriptionE.getTextContent() : null);
						}


					} else {
					edv = new DisplayableValueImpl(value, label,
							descriptionE != null ? descriptionE.getTextContent() : null);
					}
					enumValues.add(edv);

				}
				if (enumValues != null && !enumValues.isEmpty()) {
					return enumValues;
				}

			}
		}
		return null;
	}

	private void parseItemDefinitionAnnotations(ItemDefinitionImpl itemDef, XSAnnotation annotation)
			throws SchemaException {
		if (annotation == null || annotation.getAnnotation() == null) {
			return;
		}

		// ignore
		Boolean ignore = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_IGNORE);
		if (ignore != null) {
			itemDef.setIgnored(ignore);
		}

		// deprecated
		Boolean deprecated = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_DEPRECATED);
		if (deprecated != null) {
			itemDef.setDeprecated(deprecated);
		}

		// operational
		Boolean operational = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_OPERATIONAL);
		if (operational != null) {
			itemDef.setOperational(operational);
		}

		// displayName
		Element attributeDisplayName = SchemaProcessorUtil.getAnnotationElement(annotation, A_DISPLAY_NAME);
		if (attributeDisplayName != null) {
			itemDef.setDisplayName(attributeDisplayName.getTextContent());
		}

		// displayOrder
		Element displayOrderElement = SchemaProcessorUtil.getAnnotationElement(annotation, A_DISPLAY_ORDER);
		if (displayOrderElement != null) {
			Integer displayOrder = DOMUtil.getIntegerValue(displayOrderElement);
			itemDef.setDisplayOrder(displayOrder);
		}

		// help
		Element help = SchemaProcessorUtil.getAnnotationElement(annotation, A_HELP);
		if (help != null) {
			itemDef.setHelp(help.getTextContent());
		}

		// emphasized
		Boolean emphasized = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_EMPHASIZED);
		if (emphasized != null) {
			itemDef.setEmphasized(emphasized);
		}

		// documentation
		extractDocumentation(itemDef, annotation);

		Boolean heterogeneousListItem = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_HETEROGENEOUS_LIST_ITEM);
		if (heterogeneousListItem != null) {
			itemDef.setHeterogeneousListItem(heterogeneousListItem);
		}
	}

	private boolean isDeprecated(XSElementDecl xsElementDecl) throws SchemaException {
		XSAnnotation annotation = xsElementDecl.getAnnotation();
		Boolean deprecated = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, A_DEPRECATED);
		return (deprecated != null && deprecated);
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
		Element appinfo = SchemaProcessorUtil.getAnnotationElement(annotation,
				new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		if (appinfo != null) {
			return true;
		}

		return false;
	}

	private void markRuntime(Definition def) {
		if (isRuntime) {
			((DefinitionImpl) def).setRuntimeSchema(true);
		}
	}

}
