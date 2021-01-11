/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FETCH_RESULT;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Converts from ConnId connector objects to midPoint resource objects (by delegating
 * to {@link ConnIdToMidPointConversion}) and back from midPoint to ConnId (by itself).
 *
 * @author semancik
 */
class ConnIdConvertor {

    final String resourceSchemaNamespace;
    final Protector protector;
    final LocalizationService localizationService;
    final ConnIdNameMapper connIdNameMapper;

    ConnIdConvertor(Protector protector, String resourceSchemaNamespace, LocalizationService localizationService,
            ConnIdNameMapper connIdNameMapper) {
        this.protector = protector;
        this.resourceSchemaNamespace = resourceSchemaNamespace;
        this.localizationService = localizationService;
        this.connIdNameMapper = connIdNameMapper;
    }

    /**
     * Converts ICF ConnectorObject to the midPoint ResourceObject.
     * <p/>
     * All the attributes are mapped using the same way as they are mapped in
     * the schema (which is actually no mapping at all now).
     * <p/>
     * If an optional ResourceObjectDefinition was provided, the resulting
     * ResourceObject is schema-aware (getDefinition() method works). If no
     * ResourceObjectDefinition was provided, the object is schema-less. TODO:
     * this still needs to be implemented.
     *
     * @param co ICF ConnectorObject to convert
     *
     * @param full If true it describes if the returned resource object should
     *             contain all of the attributes defined in the schema, if false
     *             the returned resource object will contain only attributed with
     *             the non-null values.
     *
     * @param errorReportingMethod If EXCEPTIONS (the default), any exceptions are thrown as such. But if FETCH_RESULT,
     *                             exceptions are represented in fetchResult property of the returned resource object.
     *                             Generally, when called as part of "searchObjectsIterative" in the context of
     *                             a task, we might want to use the latter case to give the task handler a chance to report
     *                             errors to the user (and continue processing of the correct objects).
     *
     * @return new mapped ResourceObject instance.
     */
    @NotNull PrismObject<ShadowType> convertToResourceObject(@NotNull ConnectorObject co,
            PrismObjectDefinition<ShadowType> objectDefinition, boolean full, boolean caseIgnoreAttributeNames,
            boolean legacySchema, FetchErrorReportingMethodType errorReportingMethod, OperationResult parentResult) throws SchemaException {

        // This is because of suspicion that this operation sometimes takes a long time.
        // If it will not be the case, we can safely remove subresult construction here.
        OperationResult result = parentResult.subresult(ConnIdConvertor.class.getName() + ".convertToResourceObject")
                .setMinor()
                .addArbitraryObjectAsParam("uid", co.getUid())
                .addArbitraryObjectAsParam("objectDefinition", objectDefinition)
                .addParam("full", full)
                .addParam("caseIgnoreAttributeNames", caseIgnoreAttributeNames)
                .addParam("legacySchema", legacySchema)
                .addArbitraryObjectAsParam("errorReportingMethod", errorReportingMethod)
                .build();
        ConnIdToMidPointConversion conversion = null;
        try {
            if (objectDefinition == null) {
                throw new SchemaException("No definition");
            }
            conversion = new ConnIdToMidPointConversion(co, objectDefinition.instantiate(), full,
                    caseIgnoreAttributeNames, legacySchema, this);
            return conversion.execute();
        } catch (Throwable t) {
            String message = "Couldn't convert resource object from ConnID to midPoint: uid=" + co.getUid() + ", name="
                    + co.getName() + ", class=" + co.getObjectClass() + ": " + t.getMessage();
            result.recordFatalError(message, t);
            if (errorReportingMethod == FETCH_RESULT && conversion != null) {
                return conversion.reportErrorInFetchResult(result);
            } else {
                MiscUtil.throwAsSame(t, message);
                throw t; // just to make compiler happy
            }
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    Set<Attribute> convertFromResourceObjectToConnIdAttributes(ResourceAttributeContainer attributesPrism,
            ObjectClassComplexTypeDefinition ocDef) throws SchemaException {
        Collection<ResourceAttribute<?>> resourceAttributes = attributesPrism.getAttributes();
        return convertFromResourceObjectToConnIdAttributes(resourceAttributes, ocDef);
    }

    private Set<Attribute> convertFromResourceObjectToConnIdAttributes(Collection<ResourceAttribute<?>> mpResourceAttributes,
            ObjectClassComplexTypeDefinition ocDef) throws SchemaException {
        Set<Attribute> attributes = new HashSet<>();
        for (ResourceAttribute<?> attribute : emptyIfNull(mpResourceAttributes)) {
            attributes.add(convertToConnIdAttribute(attribute, ocDef));
        }
        return attributes;
    }

    private Attribute convertToConnIdAttribute(ResourceAttribute<?> mpAttribute, ObjectClassComplexTypeDefinition ocDef) throws SchemaException {
        QName midPointAttrQName = mpAttribute.getElementName();
        if (midPointAttrQName.equals(SchemaConstants.ICFS_UID)) {
            throw new SchemaException("ICF UID explicitly specified in attributes");
        }

        String connIdAttrName = connIdNameMapper.convertAttributeNameToConnId(mpAttribute, ocDef);

        Set<Object> connIdAttributeValues = new HashSet<>();
        for (PrismPropertyValue<?> pval : mpAttribute.getValues()) {
            connIdAttributeValues.add(ConnIdUtil.convertValueToConnId(pval, protector, mpAttribute.getElementName()));
        }

        try {
            return AttributeBuilder.build(connIdAttrName, connIdAttributeValues);
        } catch (IllegalArgumentException e) {
            throw new SchemaException(e.getMessage(), e);
        }
    }
}
