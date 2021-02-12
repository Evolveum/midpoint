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

import java.util.Collection;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
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
    UcfLiveSyncChange prepareChange(int localSequenceNumber, SyncDelta connIdDelta, OperationResult result)
            throws SchemaException, GenericFrameworkException {

        SyncDeltaType icfDeltaType = connIdDelta.getDeltaType();
        LOGGER.trace("START creating delta of type {}", icfDeltaType);

        ObjectClassComplexTypeDefinition actualObjectClass = getActualObjectClass(connIdDelta);
        assert actualObjectClass != null || icfDeltaType == SyncDeltaType.DELETE;

        @NotNull Uid uid = connIdDelta.getUid(); // for non-nullability see the code of SyncDelta
        Collection<ResourceAttribute<?>> identifiers;
        ObjectDelta<ShadowType> objectDelta;
        PrismObject<ShadowType> resourceObject;

        if (icfDeltaType == SyncDeltaType.DELETE) {

            identifiers = ConnIdUtil.convertToIdentifiers(uid, actualObjectClass, connectorInstance.getResourceSchema());
            resourceObject = null;
            objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.DELETE);

        } else if (icfDeltaType == SyncDeltaType.CREATE || icfDeltaType == SyncDeltaType.CREATE_OR_UPDATE || icfDeltaType == SyncDeltaType.UPDATE) {

            PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(actualObjectClass);
            LOGGER.trace("Object definition: {}", objectDefinition);

            // TODO error reporting method
            resourceObject = connIdConvertor.convertToResourceObject(connIdDelta.getObject(),
                    objectDefinition, false, connectorInstance.isCaseIgnoreAttributeNames(), connectorInstance.isLegacySchema(),
                    FetchErrorReportingMethodType.DEFAULT, result);
            LOGGER.trace("Got (current) resource object: {}", resourceObject.debugDumpLazily());
            identifiers = ShadowUtil.getAllIdentifiers(resourceObject);

            if (icfDeltaType == SyncDeltaType.CREATE) {
                objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.ADD);
                objectDelta.setObjectToAdd(resourceObject);
            } else {
                objectDelta = null;
            }

        } else {
            throw new GenericFrameworkException("Unexpected sync delta type " + icfDeltaType);
        }

        PrismProperty<Object> tokenProperty = createTokenProperty(connIdDelta.getToken(), prismContext);
        UcfLiveSyncChange change = new UcfLiveSyncChange(localSequenceNumber, uid.getUidValue(), emptyIfNull(identifiers),
                actualObjectClass, objectDelta, resourceObject, tokenProperty);

        LOGGER.trace("END creating change of type {}:\n{}", icfDeltaType, change.debugDumpLazily());
        return change;
    }

    @Nullable
    private ObjectClassComplexTypeDefinition getActualObjectClass(SyncDelta connIdDelta) throws SchemaException {
        ObjectClassComplexTypeDefinition deltaObjectClass;
        if (requestObjectClass != null) {
            return requestObjectClass;
        }

        ObjectClass deltaConnIdObjectClass = connIdDelta.getObjectClass();
        QName deltaObjectClassName = nameMapper.objectClassToQname(deltaConnIdObjectClass,
                connectorInstance.getSchemaNamespace(), connectorInstance.isLegacySchema());
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
