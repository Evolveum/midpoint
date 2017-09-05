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
package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaProcessorUtil;
import com.evolveum.midpoint.prism.schema.SchemaToDomProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSParticle;

import java.util.Collection;

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
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		QName typeName = new QName(complexType.getTargetNamespace(),complexType.getName());

		ObjectClassComplexTypeDefinitionImpl ocDef = new ObjectClassComplexTypeDefinitionImpl(typeName, prismContext);

		// nativeObjectClass
		Element nativeAttrElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_NATIVE_OBJECT_CLASS);
		String nativeObjectClass = nativeAttrElement == null ? null : nativeAttrElement.getTextContent();
		ocDef.setNativeObjectClass(nativeObjectClass);

		ShadowKindType kind = null;
		Element kindElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_KIND);
		if (kindElement != null) {
			String kindString = kindElement.getTextContent();
			if (StringUtils.isEmpty(kindString)) {
				throw new SchemaException("The definition of kind is empty in the annotation of object class "+typeName);
			}
			try {
				kind = ShadowKindType.fromValue(kindString);
			} catch (IllegalArgumentException e) {
				throw new SchemaException("Definition of unknown kind '"+kindString+"' in the annotation of object class "+typeName);
			}
			ocDef.setKind(kind);
		}

		Boolean defaultInAKind = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, MidPointConstants.RA_DEFAULT);
		if (defaultInAKind == null) {
			ocDef.setDefaultInAKind(false);
		} else {
			ocDef.setDefaultInAKind(defaultInAKind);
		}

		Boolean auxiliary = SchemaProcessorUtil.getAnnotationBooleanMarker(annotation, MidPointConstants.RA_AUXILIARY);
		if (auxiliary == null) {
			ocDef.setAuxiliary(false);
		} else {
			ocDef.setAuxiliary(auxiliary);
		}

		String intent = null;
		Element intentElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_INTENT);
		if (intentElement != null) {
			intent = intentElement.getTextContent();
			if (StringUtils.isEmpty(intent)) {
				throw new SchemaException("The definition of intent is empty in the annotation of object class "+typeName);
			}
			ocDef.setIntent(intent);
		}

		// accountType: DEPRECATED
		if (isAccountObject(annotation)) {
			if (kind != null && kind != ShadowKindType.ACCOUNT) {
				throw new SchemaException("Conflicting definition of kind and legacy account annotation in the annotation of object class "+typeName);
			}
			ocDef.setKind(ShadowKindType.ACCOUNT);
			Element account = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT);
			if (account != null && !defaultInAKind) {
				String defaultValue = account.getAttribute("default");
				// Compatibility (DEPRECATED)
				if (defaultValue != null) {
					ocDef.setDefaultInAKind(Boolean.parseBoolean(defaultValue));
				}
			}
			Element accountTypeElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT_TYPE);
			if (accountTypeElement != null) {
				String accountType = accountTypeElement.getTextContent();
				if (intent != null && !intent.equals(accountType)) {
					throw new SchemaException("Conflicting definition of intent and legacy accountType annotation in the annotation of object class "+typeName);
				}
				ocDef.setIntent(accountType);
			}
		}

		return ocDef;

	}

	@Override
	public void finishComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition, XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		super.finishComplexTypeDefinition(complexTypeDefinition, complexType, prismContext, annotation);
		if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			// TODO is this safe?
			finishObjectClassDefinition((ObjectClassComplexTypeDefinitionImpl)complexTypeDefinition, complexType, prismContext, annotation);
		}
	}

	private void finishObjectClassDefinition(ObjectClassComplexTypeDefinitionImpl ocDef,
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
			((Collection)ocDef.getPrimaryIdentifiers()).add(attrDefinition);
		}
		// secondaryIdentifier
		attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_SECONDARY_IDENTIFIER, ocDef);
		if (attrDefinition != null) {
			((Collection)ocDef.getSecondaryIdentifiers()).add(attrDefinition);
		}
	}

	@Override
	public void addExtraComplexTypeAnnotations(ComplexTypeDefinition definition, Element appinfo, SchemaToDomProcessor schemaToDomProcessor) {
		super.addExtraComplexTypeAnnotations(definition, appinfo, schemaToDomProcessor);
		if (definition instanceof ObjectClassComplexTypeDefinition) {
			addExtraObjectClassAnnotations((ObjectClassComplexTypeDefinition)definition, appinfo, schemaToDomProcessor);
		}
	}

	private void addExtraObjectClassAnnotations(ObjectClassComplexTypeDefinition definition, Element appinfo, SchemaToDomProcessor processor) {
		processor.addAnnotation(MidPointConstants.RA_RESOURCE_OBJECT, appinfo);

		// displayName, identifier, secondaryIdentifier
		for (ResourceAttributeDefinition identifier : definition.getPrimaryIdentifiers()) {
			processor.addRefAnnotation(MidPointConstants.RA_IDENTIFIER, identifier.getName(), appinfo);
		}
		for (ResourceAttributeDefinition identifier : definition.getSecondaryIdentifiers()) {
			processor.addRefAnnotation(MidPointConstants.RA_SECONDARY_IDENTIFIER,identifier.getName(),appinfo);
		}
		if (definition.getDisplayNameAttribute() != null) {
			processor.addRefAnnotation(MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE, definition.getDisplayNameAttribute().getName(), appinfo);
		}
		if (definition.getDescriptionAttribute() != null) {
			processor.addRefAnnotation(MidPointConstants.RA_DESCRIPTION_ATTRIBUTE, definition.getDescriptionAttribute().getName(), appinfo);
		}
		if (definition.getNamingAttribute() != null) {
			processor.addRefAnnotation(MidPointConstants.RA_NAMING_ATTRIBUTE, definition.getNamingAttribute().getName(), appinfo);
		}
		// TODO: what to do with native object class, composite
		// // nativeObjectClass
		if (!StringUtils.isEmpty(definition.getNativeObjectClass())) {
			processor.addAnnotation(MidPointConstants.RA_NATIVE_OBJECT_CLASS, definition.getNativeObjectClass(), appinfo);
		}

		// kind
		if (definition.getKind() != null) {
			processor.addAnnotation(MidPointConstants.RA_KIND, definition.getKind().value(), appinfo);
		}
		if (definition.isDefaultInAKind()) {
			processor.addAnnotation(MidPointConstants.RA_DEFAULT, true, appinfo);
		}
		if (definition.isAuxiliary()) {
			processor.addAnnotation(MidPointConstants.RA_AUXILIARY, true, appinfo);
		}
		if (definition.getIntent() != null) {
			processor.addAnnotation(MidPointConstants.RA_INTENT, definition.getIntent(), appinfo);
		}
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> createExtraDefinitionFromComplexType(XSComplexType complexType,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
//		if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
//			return createResourceAttributeContainerDefinition(complexType, (ObjectClassComplexTypeDefinition)complexTypeDefinition,
//					prismContext, annotation);
//		}

		return super.createExtraDefinitionFromComplexType(complexType, complexTypeDefinition, prismContext, annotation);
	}

	private PrismContainerDefinition createResourceAttributeContainerDefinition(XSComplexType complexType,
			ObjectClassComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation) {

		ResourceAttributeContainerDefinition attrContDef = new ResourceAttributeContainerDefinitionImpl(null, complexTypeDefinition, prismContext);

		return attrContDef;

	}

	@Override
	public <T> PrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation,
			XSParticle elementParticle) throws SchemaException {
		if (complexTypeDefinition != null && complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			return createResourceAttributeDefinition(elementName, typeName, prismContext, annotation);
		}

		return super.createPropertyDefinition(elementName, typeName, complexTypeDefinition, prismContext, annotation, elementParticle);
	}

	@Override
	public <T> PrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation,
			XSParticle elementParticle, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) throws SchemaException {
		if (complexTypeDefinition != null && complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			return createResourceAttributeDefinition(elementName, typeName, prismContext, annotation);
		}

		return super.createPropertyDefinition(elementName, typeName, complexTypeDefinition, prismContext, annotation, elementParticle, allowedValues, defaultValue);
	}

	private PrismPropertyDefinition createResourceAttributeDefinition(QName elementName, QName typeName,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		ResourceAttributeDefinitionImpl attrDef = new ResourceAttributeDefinitionImpl(elementName, typeName, prismContext);

		// nativeAttributeName
		Element nativeAttrElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_NATIVE_ATTRIBUTE_NAME);
		String nativeAttributeName = nativeAttrElement == null ? null : nativeAttrElement.getTextContent();
		if (!StringUtils.isEmpty(nativeAttributeName)) {
			attrDef.setNativeAttributeName(nativeAttributeName);
		}

		// frameworkAttributeName
		Element frameworkAttrElement = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_FRAMEWORK_ATTRIBUTE_NAME);
		String frameworkAttributeName = frameworkAttrElement == null ? null : frameworkAttrElement.getTextContent();
		if (!StringUtils.isEmpty(frameworkAttributeName)) {
			attrDef.setFrameworkAttributeName(frameworkAttributeName);
		}

		// returnedByDefault
		attrDef.setReturnedByDefault(SchemaProcessorUtil.getAnnotationBoolean(annotation, MidPointConstants.RA_RETURNED_BY_DEFAULT_NAME));

		return attrDef;
	}

	@Override
	public void addExtraPropertyAnnotations(PrismPropertyDefinition definition, Element appinfo,
			SchemaToDomProcessor schemaToDomProcessor) {
		super.addExtraPropertyAnnotations(definition, appinfo, schemaToDomProcessor);

		if (definition instanceof ResourceAttributeDefinition) {
			ResourceAttributeDefinition rad = (ResourceAttributeDefinition)definition;
			if (rad.getNativeAttributeName() != null) {
				schemaToDomProcessor.addAnnotation(MidPointConstants.RA_NATIVE_ATTRIBUTE_NAME, rad.getNativeAttributeName(), appinfo);
			}
			if (rad.getFrameworkAttributeName() != null) {
				schemaToDomProcessor.addAnnotation(MidPointConstants.RA_FRAMEWORK_ATTRIBUTE_NAME, rad.getFrameworkAttributeName(), appinfo);
			}
			if (rad.getReturnedByDefault() != null) {
				schemaToDomProcessor.addAnnotation(MidPointConstants.RA_RETURNED_BY_DEFAULT_NAME, rad.getReturnedByDefault().toString(), appinfo);
			}
		}
	}

	private boolean isResourceObject(XSAnnotation annotation) {
		// annotation: resourceObject
		if (SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_RESOURCE_OBJECT) != null) {
			return true;
		}
		// annotation: accountType DEPRECATED
		if (SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT) != null) {
			// <accountType> implies <resourceObject> ... at least for now (compatibility)
			return true;
		}
		return false;
	}

	private boolean isAccountObject(XSAnnotation annotation) {
		if (annotation == null || annotation.getAnnotation() == null) {
			return false;
		}

		Element accountType = SchemaProcessorUtil.getAnnotationElement(annotation, MidPointConstants.RA_ACCOUNT);
		if (accountType != null) {
			return true;
		}

		return false;
	}

	private ResourceAttributeDefinition getAnnotationReference(XSAnnotation annotation, QName qname, ObjectClassComplexTypeDefinition objectClass) throws SchemaException {
		Element element = SchemaProcessorUtil.getAnnotationElement(annotation, qname);
		if (element != null) {
			String reference = element.getTextContent();
			if (reference == null || reference.isEmpty()) {
				// Compatibility
				reference = element.getAttribute("ref");
			}
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
