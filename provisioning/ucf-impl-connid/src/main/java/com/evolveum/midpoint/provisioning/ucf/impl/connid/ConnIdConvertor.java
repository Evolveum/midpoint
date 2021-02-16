/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.provisioning.ucf.api.UcfObjectFound;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.util.MiscUtil;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;

import com.evolveum.midpoint.common.LocalizationService;
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
     *
     * All the attributes are mapped using the same way as they are mapped in
     * the schema (which is actually no mapping at all now).
     *
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
     * @param ucfErrorReportingMethod If EXCEPTIONS (the default), any exceptions are thrown as such. But if FETCH_RESULT,
     *                             exceptions are represented in fetchResult property of the returned resource object.
     *                             Generally, when called as part of "searchObjectsIterative" in the context of
     *                             a task, we might want to use the latter case to give the task handler a chance to report
     *                             errors to the user (and continue processing of the correct objects).
     *
     * @return new mapped ResourceObject instance.
     */
    @NotNull UcfObjectFound convertToUcfObject(@NotNull ConnectorObject co,
            @NotNull PrismObjectDefinition<ShadowType> objectDefinition, boolean full, boolean caseIgnoreAttributeNames,
            boolean legacySchema, UcfFetchErrorReportingMethod ucfErrorReportingMethod, OperationResult parentResult) throws SchemaException {

        // This is because of suspicion that this operation sometimes takes a long time.
        // If it will not be the case, we can safely remove subresult construction here.
        OperationResult result = parentResult.subresult(ConnIdConvertor.class.getName() + ".convertToResourceObject")
                .setMinor()
                .addArbitraryObjectAsParam("uid", co.getUid())
                .addArbitraryObjectAsParam("objectDefinition", objectDefinition)
                .addParam("full", full)
                .addParam("caseIgnoreAttributeNames", caseIgnoreAttributeNames)
                .addParam("legacySchema", legacySchema)
                .addArbitraryObjectAsParam("ucfErrorReportingMethod", ucfErrorReportingMethod)
                .build();
        try {
            // UID value is not null according to ConnId
            @NotNull String uidValue = co.getUid().getUidValue();

            ConnIdToMidPointConversion conversion = new ConnIdToMidPointConversion(co, objectDefinition.instantiate(), full,
                    caseIgnoreAttributeNames, legacySchema, this);

            try {
                conversion.execute();
                return new UcfObjectFound(conversion.getResourceObject(), uidValue, UcfErrorState.success());
            } catch (Throwable t) {
                if (ucfErrorReportingMethod == UcfFetchErrorReportingMethod.UCF_OBJECT) {
                    @NotNull PrismObject<ShadowType> incompleteResourceObject = conversion.getResourceObject(); // can be empty!
                    Throwable wrappedException = MiscUtil.createSame(t, createMessage(co, t));
                    return new UcfObjectFound(incompleteResourceObject, uidValue, UcfErrorState.error(wrappedException));
                } else {
                    throw t; // handled just below
                }
            }

        } catch (Throwable t) {
            // We have no resource object to return (e.g. because it couldn't be instantiated). So really the only option
            // is to throw an exception.
            String message = createMessage(co, t);
            result.recordFatalError(message, t);
            MiscUtil.throwAsSame(t, message);
            throw t; // just to make compiler happy
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    private String createMessage(@NotNull ConnectorObject co, Throwable t) {
        return "Couldn't convert resource object from ConnID to midPoint: uid=" + co.getUid() + ", name="
                + co.getName() + ", class=" + co.getObjectClass() + ": " + t.getMessage();
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
