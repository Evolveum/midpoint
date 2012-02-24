/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaProcessorUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSParticle;

/**
 * @author semancik
 *
 */
public class MidPointSchemaDefinitionFactory extends SchemaDefinitionFactory {
	
	@Override
	public ComplexTypeDefinition createComplexTypeDefinition(XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		if (isResourceObject(annotation)) {
			return createObjectClassDefinition(complexType, prismContext, annotation);
		}
		return super.createComplexTypeDefinition(complexType, prismContext, annotation);
	}

	private ComplexTypeDefinition createObjectClassDefinition(XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) {
		QName typeName = new QName(complexType.getTargetNamespace(),complexType.getName());
		
		ObjectClassComplexTypeDefinition ocDef = new ObjectClassComplexTypeDefinition(null, typeName, prismContext);
		
		// nativeObjectClass
		Element nativeAttrElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_NATIVE_OBJECT_CLASS);
		String nativeObjectClass = nativeAttrElement == null ? null : nativeAttrElement.getTextContent();
		ocDef.setNativeObjectClass(nativeObjectClass);
		
		// accountType
		if (isAccountObject(annotation)) {
			ocDef.setAccountType(true);
		}
		
		// TODO: change
		// check if it's default account object class...
		Element accountType = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT_TYPE);
		if (accountType != null) {
			String defaultValue = accountType.getAttribute("default");
			if (defaultValue != null) {
				ocDef.setDefaultAccountType(Boolean.parseBoolean(defaultValue));
			}
		}
				
		return ocDef;

	}
	
	@Override
	public void finishComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition, XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		super.finishComplexTypeDefinition(complexTypeDefinition, complexType, prismContext, annotation);
		if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			finishObjectClassDefinition((ObjectClassComplexTypeDefinition)complexTypeDefinition, complexType, prismContext, annotation);
		}
	}

	private void finishObjectClassDefinition(ObjectClassComplexTypeDefinition ocDef,
			XSComplexType complexType, PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		
		// displayNameAttribute
		ResourceAttributeDefinition attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE, ocDef);
		if (attrDefinition != null) {
			ocDef.setDisplayNameAttribute(attrDefinition);
		}
		// namingAttribute
		attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_NAMING_ATTRIBUTE, ocDef);
		if (attrDefinition != null) {
			ocDef.setNamingAttribute(attrDefinition);
		}
		// descriptionAttribute
		attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_DESCRIPTION_ATTRIBUTE, ocDef);
		if (attrDefinition != null) {
			ocDef.setDescriptionAttribute(attrDefinition);
		}
		// identifier
		attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_IDENTIFIER, ocDef);
		if (attrDefinition != null) {
			ocDef.getIdentifiers().add(attrDefinition);
		}
		// secondaryIdentifier
		attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_SECONDARY_IDENTIFIER, ocDef);
		if (attrDefinition != null) {
			ocDef.getSecondaryIdentifiers().add(attrDefinition);
		}

		
	}

	@Override
	public PrismContainerDefinition createExtraDefinitionFromComplexType(XSComplexType complexType,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation) throws SchemaException {		
		if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			return createResourceAttributeContainerDefinition(complexType, (ObjectClassComplexTypeDefinition)complexTypeDefinition, 
					prismContext, annotation);
		}
		
		return super.createExtraDefinitionFromComplexType(complexType, complexTypeDefinition, prismContext, annotation);
	}

	private PrismContainerDefinition createResourceAttributeContainerDefinition(XSComplexType complexType,
			ObjectClassComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation) {
		
		ResourceAttributeContainerDefinition attrContDef = new ResourceAttributeContainerDefinition(null, complexTypeDefinition, prismContext);
		// TODO annotations
		return attrContDef;

	}

	@Override
	public PrismPropertyDefinition createPropertyDefinition(QName elementName, QName typeName,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation,
			XSParticle elementParticle) throws SchemaException {
		if (complexTypeDefinition != null && complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			return createResourceAttributeDefinition(elementName, typeName, prismContext, annotation);
		}

		return super.createPropertyDefinition(elementName, typeName, complexTypeDefinition, prismContext, annotation, elementParticle);
	}
				
	private PrismPropertyDefinition createResourceAttributeDefinition(QName elementName, QName typeName,
			PrismContext prismContext, XSAnnotation annotation) {
		ResourceAttributeDefinition attrDef = new ResourceAttributeDefinition(elementName, elementName, typeName, prismContext);
		return attrDef;
	}

	private boolean isResourceObject(XSAnnotation annotation) {
		// annotation: resourceObject
		if (SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.MA_RESOURCE_OBJECT) != null) {
			return true;
		}
		// annotation: accountType
		if (SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.MA_ACCOUNT_TYPE) != null) {
			// <accountType> implies <resourceObject> ... at least for now (compatibility)
			return true;
		}
		return false;
	}
	
	private boolean isAccountObject(XSAnnotation annotation) {
		if (annotation == null || annotation.getAnnotation() == null) {
			return false;
		}
	
		Element accountType = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT_TYPE);
		if (accountType != null) {
			return true;
		}
	
		return false;
	}
	
	private ResourceAttributeDefinition getAnnotationReference(XSAnnotation annotation, QName qname, ObjectClassComplexTypeDefinition objectClass) throws SchemaException {
		Element element = SchemaProcessorUtil.getAnnotationElement(annotation, qname);
		if (element != null) {
			String reference = element.getAttribute("ref");
			if (reference != null && !reference.isEmpty()) {
				QName referenceItemName = DOMUtil.resolveQName(element, reference);
				PrismPropertyDefinition definition = objectClass.findPropertyDefinition(referenceItemName);
				if (definition == null) {
					throw new SchemaException("The annotation "+qname+" in "+objectClass+" is pointing to "+referenceItemName+" which does not exist");
				}
				if (definition instanceof ResourceAttributeDefinition) {
					return (ResourceAttributeDefinition) definition;
				} else {
					throw new SchemaException("The annotation "+qname+" in "+objectClass+" is pointing to "+referenceItemName+" which is not an attribute, it is "+definition);
				}
			}
		}
		return null;
	}

}
