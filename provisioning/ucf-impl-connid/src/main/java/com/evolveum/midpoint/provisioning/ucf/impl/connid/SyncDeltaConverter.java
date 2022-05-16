/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.TokenUtil.toUcf;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorInstanceConnIdImpl.toShadowDefinition;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.*;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.Uid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
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
    private final ConnIdNameMapper nameMapper;
    private final ConnIdConvertor connIdConvertor;
    private final ResourceObjectDefinition requestedObjectDefinition;

    SyncDeltaConverter(ConnectorInstanceConnIdImpl connectorInstance, ResourceObjectDefinition requestedObjectDefinition) {
        this.connectorInstance = connectorInstance;
        this.nameMapper = connectorInstance.connIdNameMapper;
        this.connIdConvertor = connectorInstance.connIdConvertor;
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

        Collection<ResourceAttribute<?>> identifiers = new ArrayList<>();
        ResourceObjectDefinition actualObjectDefinition = null;
        ObjectDelta<ShadowType> objectDelta = null;
        PrismObject<ShadowType> resourceObject = null;

        LOGGER.trace("START creating delta of type {}", icfDeltaType);

        UcfErrorState errorState;
        try {
            actualObjectDefinition = getActualObjectDefinition(connIdDelta);
            assert actualObjectDefinition != null || icfDeltaType == SyncDeltaType.DELETE;

            if (icfDeltaType == SyncDeltaType.DELETE) {

                identifiers.addAll(
                        ConnIdUtil.convertToIdentifiers(
                                uid, actualObjectDefinition, connectorInstance.getRawResourceSchema()));
                objectDelta = PrismContext.get().deltaFactory().object().create(ShadowType.class, ChangeType.DELETE);

            } else if (icfDeltaType == SyncDeltaType.CREATE || icfDeltaType == SyncDeltaType.CREATE_OR_UPDATE || icfDeltaType == SyncDeltaType.UPDATE) {

                PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(actualObjectDefinition);
                LOGGER.trace("Object definition: {}", objectDefinition);

                // We can consider using "fetch result" error reporting method here, and go along with a partial object.
                resourceObject = connIdConvertor
                        .convertToUcfObject(
                                connIdDelta.getObject(), objectDefinition, false,
                                connectorInstance.isCaseIgnoreAttributeNames(), connectorInstance.isLegacySchema(),
                                UcfFetchErrorReportingMethod.EXCEPTION, result)
                        .getResourceObject();

                LOGGER.trace("Got (current) resource object: {}", resourceObject.debugDumpLazily());
                identifiers.addAll(emptyIfNull(ShadowUtil.getAllIdentifiers(resourceObject)));

                if (icfDeltaType == SyncDeltaType.CREATE) {
                    objectDelta = PrismContext.get().deltaFactory().object().create(ShadowType.class, ChangeType.ADD);
                    objectDelta.setObjectToAdd(resourceObject);
                }

            } else {
                throw new GenericFrameworkException("Unexpected sync delta type " + icfDeltaType);
            }

            if (identifiers.isEmpty()) {
                throw new SchemaException("No identifiers in sync delta " + connIdDelta);
            }

            errorState = UcfErrorState.success();

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
        QName deltaObjectClassName = nameMapper.objectClassToQname(deltaConnIdObjectClass, connectorInstance.isLegacySchema());
        ResourceObjectDefinition deltaObjectClass;
        if (deltaConnIdObjectClass != null) {
            deltaObjectClass = (ResourceObjectClassDefinition) connectorInstance.getRawResourceSchema()
                    .findComplexTypeDefinitionByType(deltaObjectClassName);
        } else {
            deltaObjectClass = null;
        }
        if (deltaObjectClass == null) {
            if (connIdDelta.getDeltaType() == SyncDeltaType.DELETE) {
                // tolerate this. E.g. LDAP changelogs do not have objectclass in delete deltas.
            } else {
                throw new SchemaException("Got delta with object class " + deltaObjectClassName + " (" +
                        deltaConnIdObjectClass + ") that has no definition in resource schema");
            }
        }
        return deltaObjectClass;
    }
}
