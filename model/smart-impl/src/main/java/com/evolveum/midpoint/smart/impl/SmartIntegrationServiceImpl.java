/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DelineationsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.function.Supplier;

@Service
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final String OP_SUGGEST_FOCUS_TYPE = "suggestFocusType";

    /** Supplies a mock service client for testing purposes. */
    @TestOnly
    @Nullable private Supplier<ServiceClient> serviceClientSupplier;

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelService modelService;

    @Override
    public DelineationsSuggestionType suggestDelineations(
            String resourceOid, QName objectClassName, Task task, OperationResult result) {
        return new DelineationsSuggestionType(); // TODO replace with real implementation
    }

    @Override
    public QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try {
            LOGGER.debug("Suggesting focus type for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
            try (var serviceClient = getServiceClient(result)) {
                var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
                var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
                var objectTypeDef = resourceSchema.getObjectTypeDefinitionRequired(typeIdentification);
                var objectClassDef = resourceSchema.findObjectClassDefinitionRequired(objectTypeDef.getObjectClassName());
                var type = serviceClient.suggestFocusType(
                        typeIdentification, objectClassDef, objectTypeDef.getDelineation(), task, result);
                LOGGER.debug("Suggested focus type: {}", type);
                return type;
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private ServiceClient getServiceClient(OperationResult result) throws SchemaException, ConfigurationException {
        if (serviceClientSupplier != null) {
            return serviceClientSupplier.get();
        }
        var smartIntegrationConfiguration =
                SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(
                        systemObjectCache.getSystemConfigurationBean(result));
        return new DefaultServiceClientImpl(smartIntegrationConfiguration);
    }

    public void setServiceClientSupplier(@Nullable Supplier<ServiceClient> serviceClientSupplier) {
        this.serviceClientSupplier = serviceClientSupplier;
    }
}
