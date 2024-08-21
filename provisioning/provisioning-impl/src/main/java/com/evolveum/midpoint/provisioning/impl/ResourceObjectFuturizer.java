/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTION_PENDING;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ShadowDefinitionApplicator;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Computes the expected future state of a resource object or repo shadow by applying pending operations (from the repo shadow).
 *
 * @see #futurizeResourceObject(ProvisioningContext, RepoShadow, ExistingResourceObjectShadow, boolean, XMLGregorianCalendar)
 * @see #futurizeRepoShadow(ProvisioningContext, RepoShadow, XMLGregorianCalendar)
 */
public class ResourceObjectFuturizer {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFuturizer.class);

    @NotNull private final ProvisioningContext shadowCtx;
    @NotNull private final RepoShadow repoShadow;
    private final boolean skipExecutionPendingOperations;
    @NotNull private final XMLGregorianCalendar now;
    private final ExistingResourceObjectShadow originalResourceObject;

    private ResourceObjectFuturizer(
            @NotNull ProvisioningContext shadowCtx,
            @NotNull RepoShadow repoShadow,
            ExistingResourceObjectShadow originalResourceObject,
            boolean skipExecutionPendingOperations,
            @NotNull XMLGregorianCalendar now) {
        this.shadowCtx = shadowCtx;
        this.repoShadow = repoShadow;
        this.originalResourceObject = originalResourceObject;
        this.skipExecutionPendingOperations = skipExecutionPendingOperations;
        this.now = now;
    }

    public static @NotNull ExistingResourceObjectShadow futurizeResourceObject(
            @NotNull ProvisioningContext shadowCtx,
            @NotNull RepoShadow repoShadow,
            @NotNull ExistingResourceObjectShadow resourceObject,
            boolean skipExecutionPendingOperations,
            XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        return (ExistingResourceObjectShadow)
                new ResourceObjectFuturizer(shadowCtx, repoShadow, resourceObject, skipExecutionPendingOperations, now)
                        .futurize();
    }

    public static @NotNull ResourceObjectShadow futurizeRepoShadow(
            @NotNull ProvisioningContext shadowCtx,
            @NotNull RepoShadow repoShadow,
            XMLGregorianCalendar now)
            throws SchemaException {
        return new ResourceObjectFuturizer(shadowCtx, repoShadow, null, false, now)
                .futurize();
    }

    private @NotNull ResourceObjectShadow futurize()
            throws SchemaException {
        LOGGER.trace("Starting to futurize {} / {}", repoShadow, originalResourceObject);
        ResourceObjectShadow currentResourceObject =
                originalResourceObject != null ?
                        originalResourceObject : repoShadow.asResourceObject();
        if (currentResourceObject.isDead()) {
            return currentResourceObject;
        }

        var sortedOperations = repoShadow.getPendingOperationsSorted();
        if (sortedOperations.isEmpty()) {
            return currentResourceObject;
        }
        var shadowDefinitionApplicator = ShadowDefinitionApplicator.strict(shadowCtx.getObjectDefinitionRequired());
        var gracePeriod = shadowCtx.getGracePeriod();
        var resourceReadIsCachingOnly = shadowCtx.isReadingCachingOnly();
        for (var pendingOperation : sortedOperations) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
            if (resultStatus == NOT_APPLICABLE) {
                // Not applicable means: "no point trying this, will not retry". Therefore it will not change future state.
                continue;
            }
            if (executionStatus == COMPLETED && pendingOperation.isCompletedAndOverPeriod(now, gracePeriod)) {
                // Completed operations over grace period. They have already affected current state. They are already "applied".
                continue;
            }
            // Note: We still want to process errors, even fatal errors. As long as they are in executing state then they
            // are going to be retried and they still may influence future state
            if (skipExecutionPendingOperations && executionStatus == EXECUTION_PENDING) {
                continue;
            }
            if (resourceReadIsCachingOnly) {
                // We are getting the data from our own cache. So we know that all completed operations are already applied
                // in the cache. Re-applying them will mean additional risk of corrupting the data.
                if (resultStatus != null && resultStatus != IN_PROGRESS && resultStatus != UNKNOWN) {
                    continue;
                }
            } else {
                // We want to apply all the deltas, even those that are already completed. They might not be reflected
                // on the resource yet. E.g. they may be not be present in the CSV export until the next export cycle is scheduled
            }

            ObjectDelta<ShadowType> pendingDelta =
                    DeltaConvertor.createObjectDelta(
                            pendingOperation.getDeltaBeanRequired(),
                            shadowCtx.getObjectDefinitionRequired().getPrismObjectDefinition(),
                            false);

            shadowDefinitionApplicator.applyToDelta(pendingDelta);
            if (pendingDelta.isAdd()) {
                if (originalResourceObject != null) {
                    // If we have the resource object, we need to ignore the ADD operation.
                    // In that case the object was obviously already created. The data that we have from the resource
                    // are going to be more precise than the pending ADD delta (which might not have been applied completely).
                } else {
                    // But if we have no resource object, we need to take the data from the ADD operation.
                    ShadowType newBean = pendingDelta.getObjectToAdd().clone().asObjectable();
                    newBean.setOid(repoShadow.getOid());
                    newBean.setName(repoShadow.getName());
                    newBean.setShadowLifecycleState(repoShadow.getShadowLifecycleState());
                    // Here we transfer pending operations from repo shadow to the new bean, so that they will be returned
                    // to the caller as part of that object.
                    newBean.getPendingOperation().addAll(
                            CloneUtil.cloneCollectionMembers(
                                    repoShadow.getBean().getPendingOperation()));
                    // There are some metadata in the pending ADD object; but they may be out of date, and definitely
                    // not useful, as they have no PCV ID as they did not see the repository yet. So, let's take them from
                    // the actual repo shadow.
                    newBean.asPrismContainerValue().setValueMetadata(
                            repoShadow.getBean().asPrismContainerValue().getValueMetadata().clone());
                    shadowCtx.applyCurrentDefinition(newBean);
                    currentResourceObject = ResourceObjectShadow.fromBean(newBean, true, shadowCtx.getObjectDefinitionRequired());
                    // We also ignore the fact that there may be multiple pending ADD operations. We just take the last one.
                }
            } else if (pendingDelta.isModify()) {
                // FIXME the data here may be incomplete, for example if we start with repo shadow only (the "ADD" operation
                //  may be long forgotten or not written as pending at all), and then we apply the full-shadow MODIFY operation.
                //  We live with that for now. What could we do about it? Maybe to strip down extra (not cached) attributes?

                // Attribute values get their definitions here (assuming the shadow has the refined definition).
                // Association values do not.
                currentResourceObject.updateWith(pendingDelta.getModifications());
            } else if (pendingDelta.isDelete()) {
                currentResourceObject.getBean().setDead(true);
                currentResourceObject.getBean().setExists(false);
                currentResourceObject.getBean().setPrimaryIdentifierValue(null);
            }
        }

        // I am not sure why there is still PrismContainerDefinition in associated objects.
        shadowDefinitionApplicator.applyToAssociationValues(currentResourceObject.getBean());

        // TODO: check schema, remove non-readable attributes, activation, password, etc.
//        CredentialsType creds = resultShadowType.getCredentials();
//        if (creds != null) {
//            PasswordType passwd = creds.getPassword();
//            if (passwd != null) {
//                passwd.setValue(null);
//            }
//        }
        return currentResourceObject;
    }
}
