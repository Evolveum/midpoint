/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowQueryConversionUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

/**
 * Verifies if suggested object type delineation filters are runnable.
 */
@Component
public class ObjectTypeFiltersValidator {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeFiltersValidator.class);

    private static final String ID_IS_FILTER_RUNNABLE = "isFilterRunnable";
    private static final String ID_IS_BASE_CONTEXT_FILTER_RUNNABLE = "isBaseContextFilterRunnable";

    private final ProvisioningService provisioningService;

    public ObjectTypeFiltersValidator(ProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    /**
     * Checks whether the suggested delineation is runnable for the resource and object class.
     */
    public void testObjectTypeFilter(
            String resourceOid,
            QName objectClassName,
            SearchFilterType filterBean,
            Task task,
            OperationResult parentResult) throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException, FilterValidationException {

        var result = parentResult.subresult(ID_IS_FILTER_RUNNABLE)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();

        var resourceObj = SmartIntegrationBeans.get().modelService.getObject(ResourceType.class, resourceOid, null, task, result);
        var resource = Resource.of(resourceObj);
        ResourceObjectDefinition objectDef = resource.getCompleteSchemaRequired().findObjectClassDefinitionRequired(objectClassName);

        try {
            ObjectFilter parsed = ShadowQueryConversionUtil.parseFilter(filterBean, objectDef);
            if (parsed == null) {
                throw new SchemaException("Cannot process suggested filter: " + filterBean);
            }

            ObjectQuery query = resource.queryFor(objectClassName).maxSize(1).build();
            query.addFilter(parsed);
            provisioningService.searchShadows(query, null, task, result);
        } catch (Exception e) {
            throw new FilterValidationException("Filter validation failed: " + e.getMessage());
        } finally {
            result.close();
        }
    }

    /**
     * Validates base context filter executability via provisioning layer.
     */
    public void testBaseContextFilter(
            String resourceOid,
            QName baseContextObjectClassName,
            SearchFilterType baseContextFilterBean,
            Task task,
            OperationResult parentResult) throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException, FilterValidationException {

        var result = parentResult.subresult(ID_IS_BASE_CONTEXT_FILTER_RUNNABLE)
                .addParam("resourceOid", resourceOid)
                .addParam("baseContextObjectClassName", baseContextObjectClassName)
                .build();

        var resourceObj = SmartIntegrationBeans.get().modelService.getObject(ResourceType.class, resourceOid, null, task, result);
        var resource = Resource.of(resourceObj);
        ResourceObjectDefinition objectDef = resource.getCompleteSchemaRequired().findObjectClassDefinitionRequired(baseContextObjectClassName);

        try {
            ObjectFilter parsed = ShadowQueryConversionUtil.parseFilter(baseContextFilterBean, objectDef);
            if (parsed == null) {
                throw new SchemaException("Cannot process base context filter: " + baseContextFilterBean);
            }

            ObjectQuery query = resource.queryFor(baseContextObjectClassName).maxSize(1).build();
            query.addFilter(parsed);
            provisioningService.searchShadows(query, null, task, result);
        } catch (Exception e) {
            throw new FilterValidationException("Filter validation failed: " + e.getMessage());
        } finally {
            result.close();
        }
    }
}

