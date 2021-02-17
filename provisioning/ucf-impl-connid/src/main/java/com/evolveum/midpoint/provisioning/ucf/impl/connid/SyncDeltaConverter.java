/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorInstanceConnIdImpl.toShadowDefinition;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.TokenUtil.createTokenProperty;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.Uid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
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
    private final PrismContext prismContext;
    private final ObjectClassComplexTypeDefinition requestObjectClass;

    SyncDeltaConverter(ConnectorInstanceConnIdImpl connectorInstance, ObjectClassComplexTypeDefinition requestObjectClass) {
        this.connectorInstance = connectorInstance;
        this.nameMapper = connectorInstance.connIdNameMapper;
        this.prismContext = connectorInstance.prismContext;
        this.connIdConvertor = connectorInstance.connIdConvertor;
        this.requestObjectClass = requestObjectClass;
    }

    @NotNull
    UcfLiveSyncChange createChange(int localSequenceNumber, SyncDelta connIdDelta, OperationResult result) {
        // The following should not throw any exception. And if it does, we are lost anyway, because
        // we need this information to create even "errored" change object.
        // For non-nullability of these variables see the code of SyncDelta.
        @NotNull SyncDeltaType icfDeltaType = connIdDelta.getDeltaType();
        @NotNull Uid uid = connIdDelta.getUid();
        @NotNull String uidValue = uid.getUidValue();
        @NotNull PrismProperty<Object> token = createTokenProperty(connIdDelta.getToken(), prismContext);

        Collection<ResourceAttribute<?>> identifiers = new ArrayList<>();
        ObjectClassComplexTypeDefinition actualObjectClass = null;
        ObjectDelta<ShadowType> objectDelta = null;
        PrismObject<ShadowType> resourceObject = null;

        LOGGER.trace("START creating delta of type {}", icfDeltaType);

        UcfErrorState errorState;
        try {
            actualObjectClass = getActualObjectClass(connIdDelta);
            assert actualObjectClass != null || icfDeltaType == SyncDeltaType.DELETE;

            if (icfDeltaType == SyncDeltaType.DELETE) {

                identifiers.addAll(ConnIdUtil.convertToIdentifiers(uid, actualObjectClass, connectorInstance.getResourceSchema()));
                objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.DELETE);

            } else if (icfDeltaType == SyncDeltaType.CREATE || icfDeltaType == SyncDeltaType.CREATE_OR_UPDATE || icfDeltaType == SyncDeltaType.UPDATE) {

                PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(actualObjectClass);
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
                    objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.ADD);
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

        UcfLiveSyncChange change = new UcfLiveSyncChange(localSequenceNumber, uidValue, identifiers, actualObjectClass,
                objectDelta, resourceObject, token, errorState);

        LOGGER.trace("END creating change of type {}:\n{}", icfDeltaType, change.debugDumpLazily());
        return change;
    }

    @Nullable
    private ObjectClassComplexTypeDefinition getActualObjectClass(SyncDelta connIdDelta) throws SchemaException {
        if (requestObjectClass != null) {
            return requestObjectClass;
        }

        ObjectClass deltaConnIdObjectClass = connIdDelta.getObjectClass();
        QName deltaObjectClassName = nameMapper.objectClassToQname(deltaConnIdObjectClass,
                connectorInstance.getSchemaNamespace(), connectorInstance.isLegacySchema());
        ObjectClassComplexTypeDefinition deltaObjectClass;
        if (deltaConnIdObjectClass != null) {
            deltaObjectClass = (ObjectClassComplexTypeDefinition) connectorInstance.getResourceSchema()
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
