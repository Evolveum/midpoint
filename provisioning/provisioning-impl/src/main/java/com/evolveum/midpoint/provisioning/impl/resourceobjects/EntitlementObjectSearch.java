/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowItemsToReturn;
import com.evolveum.midpoint.provisioning.util.ShadowItemsToReturnProvider;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.processor.SimulatedShadowReferenceTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.DelineationProcessor.determineQueryWithConstraints;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.createEntitlementQuery;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.getSingleValue;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Searches for entitlement objects (targets) on the resource.
 *
 * Used when reading associations and cleaning them up on subject deletion.
 */
class EntitlementObjectSearch<T> {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementObjectSearch.class);

    @NotNull private final ProvisioningContext subjectCtx;
    @NotNull private final SimulatedShadowReferenceTypeDefinition simulationDefinition;
    @NotNull private final AttributeBinding attributeBinding;
    @NotNull private final ShadowType subject;

    /**
     * The name of the attribute (on the subject) according to the value of which we are searching the entitlements.
     * Typically the account DN.
     */
    @NotNull private final QName subjectAttrName;

    /**
     * The value of {@link #subjectAttrName} according to which we are searching the entitlements.
     * Determined during construction. If null, the search cannot be done.
     */
    @Nullable private final PrismPropertyValue<T> subjectAttrValue;

    EntitlementObjectSearch(
            @NotNull ProvisioningContext subjectCtx,
            @NotNull SimulatedShadowReferenceTypeDefinition simulationDefinition,
            @NotNull AttributeBinding attributeBinding,
            @NotNull ShadowType subject) throws SchemaException {

        var errorCtx = subjectCtx.getExceptionDescriptionLazy();

        this.subjectCtx = subjectCtx;
        this.simulationDefinition = simulationDefinition;
        this.attributeBinding = attributeBinding;
        this.subject = subject;

        this.subjectAttrName = attributeBinding.subjectSide(); // e.g. ri:dn
        var subjectAssocName = simulationDefinition.getLocalSubjectItemName();

        this.subjectAttrValue = getSingleValue(subject, subjectAttrName, subjectAssocName, errorCtx);
    }

    public void execute(@NotNull ResourceObjectHandler foundObjectsHandler, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {

        argCheck(subjectAttrValue != null, "No subject attr value (should be checked by the caller)");

        // We have to search each object participant individually, as they have separate delineations.
        for (var participantDef : simulationDefinition.getObjects()) {

            var objectAttrDef = participantDef.getObjectAttributeDefinition(attributeBinding); // e.g. ri:members
            var query = createEntitlementQuery(objectAttrDef, subjectAttrValue);

            var objectDefinition = participantDef.getObjectDefinition();

            ProvisioningContext wildcardCtx = subjectCtx.toWildcard();

            var queryWithConstraints = determineQueryWithConstraints(
                    wildcardCtx, objectDefinition, participantDef.getDelineation(), query, result);

            LOGGER.trace("Searching for object-to-subject association objects for subject {}: query {}",
                    ShadowUtil.getHumanReadableNameLazily(subject.asPrismObject()), queryWithConstraints);
            try {
                ResourceObjectSearchOperation.execute(
                        wildcardCtx,
                        objectDefinition,
                        foundObjectsHandler,
                        queryWithConstraints,
                        getDefaultAttributesToReturn(objectDefinition), // We hope that default "attributes to return" are OK.
                        false, // ...and that this is OK as well.
                        result);
            } catch (LocalTunnelException e) {
                throw e.schemaException;
            }
        }
    }

    @NotNull QName getSubjectAttrName() {
        return subjectAttrName;
    }

    @Nullable PrismPropertyValue<T> getSubjectAttrValue() {
        return subjectAttrValue;
    }

    private ShadowItemsToReturn getDefaultAttributesToReturn(ResourceObjectDefinition objectDefinition) {
        return new ShadowItemsToReturnProvider(subjectCtx.getResource(), objectDefinition, null)
                .createAttributesToReturn();
    }

    static class LocalTunnelException extends RuntimeException {
        SchemaException schemaException;
        LocalTunnelException(SchemaException cause) {
            super(cause);
            this.schemaException = cause;
        }
    }
}
