/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeSuggestion;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.NOT_APPLICABLE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.SchemaMatchService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class SchemaMatchPreloadActivityRun
        extends LocalActivityRun<
        SchemaMatchPreloadWorkDefinition,
        SchemaMatchPreloadActivityHandler,
        AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaMatchPreloadActivityRun.class);

    private final SchemaMatchService schemaMatchService;
    private final TaskManager taskManager;

    SchemaMatchPreloadActivityRun(
            ActivityRunInstantiationContext<SchemaMatchPreloadWorkDefinition, SchemaMatchPreloadActivityHandler> context,
            SchemaMatchService schemaMatchService,
            TaskManager taskManager) {
        super(context);
        this.schemaMatchService = schemaMatchService;
        this.taskManager = taskManager;
        setInstanceReady();
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
        var task = getRunningTask();
        var workDefinition = getWorkDefinition();
        var resourceOid = workDefinition.getResourceOid();
        var objectClassName = workDefinition.getObjectClassName();
        var permissions = workDefinition.getPermissions();
        var sourceTaskOid = workDefinition.getSourceTaskOid();

        if (sourceTaskOid == null) {
            throw new ConfigurationException("No sourceTaskRef in SchemaMatchPreload work definition");
        }

        if (!permissions.contains(DataAccessPermissionType.SCHEMA_ACCESS)) {
            LOGGER.debug("Skipping schema matching pre-load - SCHEMA_ACCESS permission not granted");
            return ActivityRunResult.finished(NOT_APPLICABLE);
        }

        var suggestedTypesOpt = loadSuggestedTypes(sourceTaskOid, result);
        if (suggestedTypesOpt.isEmpty() || suggestedTypesOpt.get().getObjectType().isEmpty()) {
            LOGGER.debug("No suggested object types to pre-load schema matching for");
            return ActivityRunResult.finished(NOT_APPLICABLE);
        }
        var suggestedTypes = suggestedTypesOpt.get();

        var typesByFocusType = groupByFocusType(suggestedTypes.getObjectType());
        if (typesByFocusType.isEmpty()) {
            LOGGER.debug("No suggested object types have a focus type assigned, skipping schema matching pre-load");
            return ActivityRunResult.finished(NOT_APPLICABLE);
        }

        LOGGER.info("Pre-loading schema matching for {} unique focus type(s) on resource {} (object class {})",
                typesByFocusType.size(), resourceOid, objectClassName);

        for (var entry : typesByFocusType.entrySet()) {
            var focusTypeName = entry.getKey();
            var typesWithThisFocus = entry.getValue();
            LOGGER.debug("Computing schema match for object class {} with focus type {}", objectClassName, focusTypeName);
            try {
                SchemaMatchResultType match = schemaMatchService
                        .computeSchemaMatchByObjectClass(resourceOid, objectClassName, focusTypeName, true, task, result);

                for (var objectTypeBean : typesWithThisFocus) {
                    var kind = objectTypeBean.getKind().value();
                    var intent = objectTypeBean.getIntent();
                    var schemaMatchOid = schemaMatchService
                            .saveSchemaMatch(resourceOid, kind, intent, match, result);
                    LOGGER.debug("Schema match cached with OID {} for type kind={} intent={}", schemaMatchOid, kind, intent);
                }
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER,
                        "Failed to pre-load schema matching for resource {} object class {} focus type {}. "
                        + "Schema matching will be computed on-demand if needed later.",
                        e, resourceOid, objectClassName, focusTypeName);
            }
        }

        LOGGER.info("Schema matching pre-load completed for resource {} (object class {})", resourceOid, objectClassName);
        return ActivityRunResult.success();
    }

    private Optional<ObjectTypesSuggestionType> loadSuggestedTypes(String sourceTaskOid, OperationResult result) {
        try {
            var options = GetOperationOptionsBuilder.create()
                    .noFetch()
                    .item(TaskType.F_RESULT).retrieve()
                    .item(TaskType.F_ACTIVITY_STATE).retrieve()
                    .build();
            var sourceTask = taskManager
                    .getObject(TaskType.class, sourceTaskOid, options, result)
                    .asObjectable();
            var resultItem = sourceTask.asPrismObject().findItem(
                    ItemPath.create(
                            TaskType.F_ACTIVITY_STATE,
                            TaskActivityStateType.F_ACTIVITY,
                            ActivityStateType.F_WORK_STATE,
                            ObjectTypesSuggestionWorkStateType.F_RESULT));
            if (resultItem == null) {
                LOGGER.warn("No objectTypesSuggestion result found in source task {}", sourceTaskOid);
                return Optional.empty();
            }
            return Optional.ofNullable(resultItem.getRealValue(ObjectTypesSuggestionType.class));
        } catch (Exception e) {
            throw new SystemException("Failed to load suggested types from source task " + sourceTaskOid, e);
        }
    }

    private Map<QName, List<ResourceObjectTypeDefinitionType>> groupByFocusType(
            List<ResourceObjectTypeDefinitionType> objectTypes) {
        Map<QName, List<ResourceObjectTypeDefinitionType>> result = new LinkedHashMap<>();
        for (var objectTypeBean : objectTypes) {
            Optional.ofNullable(objectTypeBean.getFocus())
                    .map(ResourceObjectFocusSpecificationType::getType)
                    .ifPresentOrElse(
                            focus -> result.computeIfAbsent(focus, k -> new ArrayList<>()).add(objectTypeBean),
                            () -> LOGGER.debug("Skipping object type kind={} intent={} - no focus type assigned",
                                    objectTypeBean.getKind(), objectTypeBean.getIntent()));
        }
        return result;
    }
}
