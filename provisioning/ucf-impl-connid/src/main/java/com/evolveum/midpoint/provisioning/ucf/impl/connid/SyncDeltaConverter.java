/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.connIdObjectClassNameToUcf;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.TokenUtil.toUcf;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.*;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.Uid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Converts SyncDelta objects to UcfLiveSyncChange objects (i.e. ICF -> UCF conversion).
 */
class SyncDeltaConverter {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceConnIdImpl.class);

    private final ConnectorInstanceConnIdImpl connectorInstance;
    private final ConnIdObjectConvertor connIdObjectConvertor;
    private final ResourceObjectDefinition requestedObjectDefinition;

    SyncDeltaConverter(ConnectorInstanceConnIdImpl connectorInstance, ResourceObjectDefinition requestedObjectDefinition) {
        this.connectorInstance = connectorInstance;
        this.connIdObjectConvertor = connectorInstance.connIdObjectConvertor;
        this.requestedObjectDefinition = requestedObjectDefinition;
    }

    @NotNull
    UcfLiveSyncChange createChange(int localSequenceNumber, SyncDelta connIdDelta, OperationResult result) {
        // The following should not throw any exception. And if it does, we are lost anyway, because
        // we need this information to create even "errored" change object.
        // For non-nullability of these variables see the code of SyncDelta.
        @NotNull SyncDeltaType icfDeltaType = connIdDelta.getDeltaType();
        @NotNull Uid uid = connIdDelta.getUid();
        @NotNull String uidValue = uid.getUidValue();
        @NotNull UcfSyncToken token = toUcf(connIdDelta.getToken());

        Collection<ShadowSimpleAttribute<?>> identifiers = new ArrayList<>();
        ResourceObjectDefinition actualObjectDefinition = null;
        ObjectDelta<ShadowType> objectDelta = null;
        UcfResourceObject resourceObject = null;

        LOGGER.trace("START creating delta of type {}", icfDeltaType);

        @NotNull UcfErrorState errorState;
        try {
            actualObjectDefinition = getActualObjectDefinition(connIdDelta);
            assert actualObjectDefinition != null || icfDeltaType == SyncDeltaType.DELETE;

            if (icfDeltaType == SyncDeltaType.DELETE) {

                identifiers.addAll(
                        ConnIdUtil.convertToIdentifiers(
                                uid, actualObjectDefinition, connectorInstance.getResourceSchema()));
                objectDelta = PrismContext.get().deltaFactory().object().create(ShadowType.class, ChangeType.DELETE);

            } else if (icfDeltaType == SyncDeltaType.CREATE || icfDeltaType == SyncDeltaType.CREATE_OR_UPDATE || icfDeltaType == SyncDeltaType.UPDATE) {

                // We use the "object" error reporting method here, to be able to proceed with partial object.
                // This is to avoid having to artificially add e.g. identifiers to the object later. If not done here,
                // we have no possibility to guess the identifiers later! This is the best place to do that.
                resourceObject = connIdObjectConvertor
                        .convertToUcfObject(
                                connIdDelta.getObject(),
                                actualObjectDefinition,
                                UcfFetchErrorReportingMethod.UCF_OBJECT,
                                result);

                LOGGER.trace("Conversion result: {}", resourceObject.getErrorState());

                LOGGER.trace("Got (current) resource object: {}", resourceObject.debugDumpLazily());
                identifiers.addAll(ShadowUtil.getAllIdentifiers(resourceObject.getPrismObject()));

                if (icfDeltaType == SyncDeltaType.CREATE) {
                    objectDelta = PrismContext.get().deltaFactory().object().create(ShadowType.class, ChangeType.ADD);
                    objectDelta.setObjectToAdd(resourceObject.getPrismObject());
                }

            } else {
                throw new GenericFrameworkException("Unexpected sync delta type " + icfDeltaType);
            }

            errorState = resourceObject != null ? resourceObject.getErrorState() : UcfErrorState.success();

        } catch (Exception e) {
            result.recordFatalError(e);
            errorState = UcfErrorState.error(e);
        }

        UcfLiveSyncChange change = new UcfLiveSyncChange(
                localSequenceNumber, uidValue, identifiers, actualObjectDefinition,
                objectDelta, resourceObject, token, errorState);

        LOGGER.trace("END creating change of type {}:\n{}", icfDeltaType, change.debugDumpLazily());
        return change;
    }

    @Nullable
    private ResourceObjectDefinition getActualObjectDefinition(SyncDelta connIdDelta) throws SchemaException {
        if (requestedObjectDefinition != null) {
            return requestedObjectDefinition;
        }

        ObjectClass deltaConnIdObjectClass = connIdDelta.getObjectClass();
        QName deltaObjectClassName = connIdObjectClassNameToUcf(deltaConnIdObjectClass, connectorInstance.isLegacySchema());
        ResourceObjectDefinition deltaObjectClass;
        if (deltaConnIdObjectClass != null) {
            deltaObjectClass = connectorInstance.getResourceSchemaRequired().findDefinitionForObjectClass(deltaObjectClassName);
        } else {
            deltaObjectClass = null;
        }
        if (deltaObjectClass == null) {
            if (connIdDelta.getDeltaType() == SyncDeltaType.DELETE) {
                // tolerate this. E.g. LDAP changelogs do not have objectclass in delete deltas.
            } else {
                throw new SchemaException(
                        "Got delta with object class %s (%s) that has no definition in resource schema".formatted(
                                deltaObjectClassName, deltaConnIdObjectClass));
            }
        }
        return deltaObjectClass;
    }
}
