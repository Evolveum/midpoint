/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.impl.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.impl.schema.SchemaToDomProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSParticle;

import java.util.Collection;

import static com.evolveum.midpoint.prism.impl.schema.SchemaProcessorUtil.*;

/**
 * @author semancik
 *
 */
public class MidPointSchemaDefinitionFactory extends SchemaDefinitionFactory {

    @Override
    public MutableComplexTypeDefinition createComplexTypeDefinition(XSComplexType complexType,
            PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
        if (isResourceObject(annotation)) {
            return createObjectClassDefinition(complexType, annotation);
        }
        return super.createComplexTypeDefinition(complexType, prismContext, annotation);
    }

    private MutableComplexTypeDefinition createObjectClassDefinition(XSComplexType complexType, XSAnnotation annotation)
            throws SchemaException {

        ResourceObjectClassDefinitionImpl ocDef =
                ResourceObjectClassDefinitionImpl.raw(
                        new QName(complexType.getTargetNamespace(), complexType.getName()));

        ocDef.setNativeObjectClass(
                getAnnotationString(annotation, MidPointConstants.RA_NATIVE_OBJECT_CLASS));

        ocDef.setDefaultAccountDefinition(
                Boolean.TRUE.equals(
                        getAnnotationBooleanMarker(annotation, MidPointConstants.RA_DEFAULT)));

        ocDef.setAuxiliary(
                Boolean.TRUE.equals(
                        getAnnotationBooleanMarker(annotation, MidPointConstants.RA_AUXILIARY)));

        return ocDef;
    }

    @Override
    public void finishComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition, XSComplexType complexType,
            PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
        super.finishComplexTypeDefinition(complexTypeDefinition, complexType, prismContext, annotation);
        if (complexTypeDefinition instanceof ResourceObjectClassDefinition) {
            // TODO is this safe?
            finishObjectClassDefinition((ResourceObjectClassDefinitionImpl)complexTypeDefinition, annotation);
        }
    }

    private void finishObjectClassDefinition(ResourceObjectClassDefinitionImpl ocDef, XSAnnotation annotation)
            throws SchemaException {

        // displayNameAttribute
        ResourceAttributeDefinition<?> attrDefinition =
                getAnnotationReference(annotation, MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE, ocDef);
        if (attrDefinition != null) {
            ocDef.setDisplayNameAttributeName(attrDefinition.getItemName());
        }
        // namingAttribute
        attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_NAMING_ATTRIBUTE, ocDef);
        if (attrDefinition != null) {
            ocDef.setNamingAttributeName(attrDefinition.getItemName());
        }
        // descriptionAttribute
        attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_DESCRIPTION_ATTRIBUTE, ocDef);
        if (attrDefinition != null) {
            ocDef.setDescriptionAttributeName(attrDefinition.getItemName());
        }
        // identifier
        attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_IDENTIFIER, ocDef);
        if (attrDefinition != null) {
            ocDef.addPrimaryIdentifierName(attrDefinition.getItemName());
        }
        // secondaryIdentifier
        attrDefinition = getAnnotationReference(annotation, MidPointConstants.RA_SECONDARY_IDENTIFIER, ocDef);
        if (attrDefinition != null) {
            ocDef.addSecondaryIdentifierName(attrDefinition.getItemName());
        }
    }

    @Override
    public void addExtraComplexTypeAnnotations(ComplexTypeDefinition definition, Element appinfo, SchemaToDomProcessor schemaToDomProcessor) {
        super.addExtraComplexTypeAnnotations(definition, appinfo, schemaToDomProcessor);
        if (definition instanceof ResourceObjectClassDefinition) {
            addExtraObjectClassAnnotations((ResourceObjectClassDefinition)definition, appinfo, schemaToDomProcessor);
        }
    }

    private void addExtraObjectClassAnnotations(ResourceObjectClassDefinition definition, Element appinfo, SchemaToDomProcessor processor) {
        processor.addAnnotation(MidPointConstants.RA_RESOURCE_OBJECT, appinfo);

        // displayName, identifier, secondaryIdentifier
        for (ResourceAttributeDefinition<?> identifier : definition.getPrimaryIdentifiers()) {
            processor.addRefAnnotation(MidPointConstants.RA_IDENTIFIER, identifier.getItemName(), appinfo);
        }
        for (ResourceAttributeDefinition<?> identifier : definition.getSecondaryIdentifiers()) {
            processor.addRefAnnotation(MidPointConstants.RA_SECONDARY_IDENTIFIER,identifier.getItemName(),appinfo);
        }
        if (definition.getDisplayNameAttribute() != null) {
            processor.addRefAnnotation(MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE, definition.getDisplayNameAttribute().getItemName(), appinfo);
        }
        if (definition.getDescriptionAttribute() != null) {
            processor.addRefAnnotation(MidPointConstants.RA_DESCRIPTION_ATTRIBUTE, definition.getDescriptionAttribute().getItemName(), appinfo);
        }
        if (definition.getNamingAttribute() != null) {
            processor.addRefAnnotation(MidPointConstants.RA_NAMING_ATTRIBUTE, definition.getNamingAttribute().getItemName(), appinfo);
        }
        // TODO: what to do with native object class, composite
        // // nativeObjectClass
        if (!StringUtils.isEmpty(definition.getNativeObjectClass())) {
            processor.addAnnotation(MidPointConstants.RA_NATIVE_OBJECT_CLASS, definition.getNativeObjectClass(), appinfo);
        }

        if (definition.isDefaultAccountDefinition()) {
            processor.addAnnotation(MidPointConstants.RA_DEFAULT, true, appinfo);
        }
        if (definition.isAuxiliary()) {
            processor.addAnnotation(MidPointConstants.RA_AUXILIARY, true, appinfo);
        }
    }

    @Override
    public <C extends Containerable> PrismContainerDefinition<C> createExtraDefinitionFromComplexType(XSComplexType complexType,
            ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
//        if (complexTypeDefinition instanceof ResourceObjectClassDefinition) {
//            return createResourceAttributeContainerDefinition(complexType, (ResourceObjectClassDefinition)complexTypeDefinition,
//                    prismContext, annotation);
//        }

        return super.createExtraDefinitionFromComplexType(complexType, complexTypeDefinition, prismContext, annotation);
    }

    @Override
    public <T> PrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName,
            ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation,
            XSParticle elementParticle) throws SchemaException {
        if (complexTypeDefinition instanceof ResourceObjectClassDefinition) {
            //noinspection unchecked
            return (PrismPropertyDefinition<T>)
                    createResourceAttributeDefinition(elementName, typeName, annotation);
        }

        return super.createPropertyDefinition(elementName, typeName, complexTypeDefinition, prismContext, annotation, elementParticle);
    }

    @Override
    public <T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName,
            ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, XSAnnotation annotation,
            XSParticle elementParticle, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) throws SchemaException {
        if (complexTypeDefinition instanceof ResourceObjectClassDefinition) {
            //noinspection unchecked
            return (MutablePrismPropertyDefinition<T>)
                    createResourceAttributeDefinition(elementName, typeName, annotation);
        }

        return super.createPropertyDefinition(elementName, typeName, complexTypeDefinition, prismContext, annotation, elementParticle, allowedValues, defaultValue);
    }

    private MutablePrismPropertyDefinition<?> createResourceAttributeDefinition(
            QName elementName, QName typeName, XSAnnotation annotation) throws SchemaException {
        RawResourceAttributeDefinitionImpl<?> attrDef = new RawResourceAttributeDefinitionImpl<>(elementName, typeName);

        // nativeAttributeName
        Element nativeAttrElement = getAnnotationElement(annotation, MidPointConstants.RA_NATIVE_ATTRIBUTE_NAME);
        String nativeAttributeName = nativeAttrElement == null ? null : nativeAttrElement.getTextContent();
        if (!StringUtils.isEmpty(nativeAttributeName)) {
            attrDef.setNativeAttributeName(nativeAttributeName);
        }

        // frameworkAttributeName
        Element frameworkAttrElement = getAnnotationElement(annotation, MidPointConstants.RA_FRAMEWORK_ATTRIBUTE_NAME);
        String frameworkAttributeName = frameworkAttrElement == null ? null : frameworkAttrElement.getTextContent();
        if (!StringUtils.isEmpty(frameworkAttributeName)) {
            attrDef.setFrameworkAttributeName(frameworkAttributeName);
        }

        // returnedByDefault
        attrDef.setReturnedByDefault(getAnnotationBoolean(annotation, MidPointConstants.RA_RETURNED_BY_DEFAULT_NAME));

        return attrDef;
    }

    @Override
    public void addExtraPropertyAnnotations(
            PrismPropertyDefinition<?> definition,
            Element appinfo,
            SchemaToDomProcessor schemaToDomProcessor) {
        super.addExtraPropertyAnnotations(definition, appinfo, schemaToDomProcessor);

        if (definition instanceof ResourceAttributeDefinition) {
            ResourceAttributeDefinition<?> rad = (ResourceAttributeDefinition<?>) definition;
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
        return getAnnotationElement(annotation, MidPointConstants.RA_RESOURCE_OBJECT) != null;
    }

    private ResourceAttributeDefinition<?> getAnnotationReference(
            XSAnnotation annotation, QName qname, ResourceObjectClassDefinition objectClass) throws SchemaException {
        Element element = getAnnotationElement(annotation, qname);
        if (element != null) {
            String reference = element.getTextContent();
            if (reference == null || reference.isEmpty()) {
                // Compatibility
                reference = element.getAttribute("ref");
            }
            if (!reference.isEmpty()) {
                QName referenceItemName = DOMUtil.resolveQName(element, reference);
                PrismPropertyDefinition<?> definition = objectClass.findPropertyDefinition(ItemName.fromQName(referenceItemName));
                if (definition == null) {
                    throw new SchemaException("The annotation "+qname+" in "+objectClass+" is pointing to "+referenceItemName+" which does not exist");
                }
                if (definition instanceof ResourceAttributeDefinition) {
                    return (ResourceAttributeDefinition<?>) definition;
                } else {
                    throw new SchemaException("The annotation "+qname+" in "+objectClass+" is pointing to "+referenceItemName+" which is not an attribute, it is "+definition);
                }
            }
        }
        return null;
    }

}
