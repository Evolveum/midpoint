/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Verifies if suggested object type delineation filters are runnable.
 *
 * - Parses suggestion filters into Prism filters.
 * - Combines them using logical OR.
 * - Executes an iterative search on ShadowType and stops after the first hit.
 *
 * - This class does not compute coverage or quality metrics.
 * - It is intended as a lightweight sanity check only.
 */
@Component
public class ObjectTypesQualityAssessor {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesQualityAssessor.class);

    private static final String ID_IS_FILTER_RUNNABLE = "isFilterRunnable";

    /**
     * Checks whether the suggested delineation is runnable for the resource and object class.
     */
    public boolean isFilterRunnable(
            String resourceOid,
            QName objectClassName,
            ResourceObjectTypeDelineationType delineation,
            Task task,
            OperationResult parentResult) throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        var result = parentResult.subresult(ID_IS_FILTER_RUNNABLE)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var resourceObj = SmartIntegrationBeans.get().modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            var resource = Resource.of(resourceObj);
            ResourceObjectDefinition objectDef = resource.getCompleteSchemaRequired().findObjectClassDefinitionRequired(objectClassName);

            List<SearchFilterType> filterBeans = delineation.getFilter();
            if (filterBeans == null || filterBeans.isEmpty()) {
                throw new SchemaException("No suggested filters.");
            }

            List<ObjectFilter> parts = new ArrayList<>();
            for (SearchFilterType f : filterBeans) {
                ObjectFilter parsed = ShadowQueryConversionUtil.parseFilter(f, objectDef);
                if (parsed == null) {
                    throw new SchemaException("Cannot process suggested filter: " + f);
                }
                parts.add(parsed);
            }

            ObjectQuery query = resource.queryFor(objectClassName).build();
            query.addFilter(PrismContext.get().queryFactory().createAndOptimized(parts));

            SmartIntegrationBeans.get().modelService.searchObjectsIterative(
                    ShadowType.class,
                    query,
                    (obj, lres) -> {
                        return false; // stop after first hit
                    },
                    null, task, result);

            return true;
        } finally {
            result.close();
        }
    }
}

