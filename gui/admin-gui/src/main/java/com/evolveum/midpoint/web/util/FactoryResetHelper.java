/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.*;

import java.lang.reflect.Modifier;
import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@SuppressWarnings("unused")
public class FactoryResetHelper {

    private static final String DOT_CLASS = FactoryResetHelper.class.getName() + ".";

    private static final String OPERATION_INITIAL_IMPORT = DOT_CLASS + "initialImport";

    private ModelServiceLocator locator;

    public FactoryResetHelper(@NotNull ModelServiceLocator locator) {
        this.locator = locator;
    }

    public void createAndRunDeleteAllTask(Task task) {
        OperationResult result = task.getResult();

        final String taskOid = UUID.randomUUID().toString();
        try {
            // @formatter:off
            ActivityDefinitionType definition = new ActivityDefinitionType()
                    .identifier("Delete all")
                    .beginComposition()
                    .<ActivityDefinitionType>end()
                    .beginDistribution()
                    .workerThreads(4)
                    .end();
            // @formatter:on

            List<ActivityDefinitionType> activities = definition.getComposition().getActivity();
            // delete all objects (indestructible objects will be skipped)
            for (ObjectTypes type : createSortedTypesForDeleteAll()) {
                activities.add(createDeleteActivityForType(type.getTypeQName(), createAllQuery(type.getClassDefinition()), activities.size() + 1));
            }

            // delete indestructible objects (all but currently executing task)
            for (ObjectTypes type : createSortedTypesForDeleteAllIndestructible()) {
                QueryType query = TASK == type ? createTaskQuery(taskOid) : null;

                activities.add(createDeleteIndestructibleActivityForType(type.getTypeQName(), query, activities.size() + 1));
            }

            // run initial object import + model restart/post-init
            activities.add(createInitialImportActivity(activities.size() + 1));

            locator.getModelInteractionService().submit(
                    definition,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .oid(taskOid)
                                    .name("Factory reset " + WebComponentUtil.formatDate(new Date()))
                                    .indestructible(true)
                                    .cleanupAfterCompletion(XmlTypeConverter.createDuration("P1D")))
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);
        } catch (Exception ex) {
            result.computeStatusIfUnknown();
            result.recordFatalError("Couldn't create delete all task", ex);
        }
    }

    private ActivityDefinitionType createDeleteActivityForType(QName type, QueryType query, int order) {
        // @formatter:off
        return new ActivityDefinitionType()
                .order(order)
                .identifier(order + ": Delete all " + type.getLocalPart())
                .beginDistribution()
                .<ActivityDefinitionType>end()
                .beginWork()
                    .beginDeletion()
                        .beginObjects()
                            .type(type)
                            .query(query)
                            .useRepositoryDirectly(true)
                        .<DeletionWorkDefinitionType>end()
                    .<WorkDefinitionsType>end()
                .end();
        // @formatter:on
    }

    private ActivityDefinitionType createDeleteIndestructibleActivityForType(QName type, QueryType query, int order) {
        // @formatter:off
        ActivityDefinitionType activity = new ActivityDefinitionType()
                .order(order)
                .identifier(order + ": Delete all " + type.getLocalPart())
                .beginDistribution()
                .<ActivityDefinitionType>end()
                .beginWork()
                    .beginIterativeScripting()
                        .beginObjects()
                            .type(type)
                            .query(query)
                            .useRepositoryDirectly(true)
                        .<IterativeScriptingWorkDefinitionType>end()
                    .<WorkDefinitionsType>end()
                .end();
        // @formatter:on

        ScriptExpressionEvaluatorType script = new ScriptExpressionEvaluatorType();
        script.setCode("\n"
                + "if (org.apache.commons.lang3.BooleanUtils.isTrue(input.isIndestructible())) {\n"
                + "    com.evolveum.midpoint.prism.delta.ObjectDelta delta = midpoint.prismContext\n"
                + "            .deltaFor(input.getClass())\n"
                + "            .item(input.F_INDESTRUCTIBLE).replace()\n"
                + "            .asObjectDelta(input.getOid())\n"
                + "    midpoint.modifyObject(delta, com.evolveum.midpoint.model.api.ModelExecuteOptions.create().raw(true))\n"
                + "}\n"
                + "\n"
                + "midpoint.deleteObject(input.getClass(), input.getOid(), com.evolveum.midpoint.model.api.ModelExecuteOptions.create().raw(true))\n");

        ExecuteScriptActionExpressionType execute = new ExecuteScriptActionExpressionType();
        execute.setScript(script);

        ExecuteScriptType request = new ExecuteScriptType();
        request.scriptingExpression(new ObjectFactory().createExecute(execute));
        activity.getWork().getIterativeScripting().setScriptExecutionRequest(request);

        return activity;
    }

    private <T extends ObjectType> QueryType createAllQuery(Class<T> type) throws SchemaException {
        PrismContext prismContext = locator.getPrismContext();

        return prismContext.getQueryConverter().createQueryType(
                prismContext.queryFor(type)
                        .all()
                        .build());
    }

    private QueryType createTaskQuery(String taskOid) throws SchemaException {
        PrismContext prismContext = locator.getPrismContext();
        // @formatter:off
        final ObjectQuery query = prismContext.queryFor(TaskType.class)
                .not().ownerId(taskOid).build();
        // @formatter:on
        return prismContext.getQueryConverter().createQueryType(query);
    }

    private List<ObjectTypes> createSortedTypesForDeleteAllIndestructible() {
        return createSortedTypes(Arrays.asList(), Arrays.asList(ARCHETYPE, SYSTEM_CONFIGURATION, USER), Arrays.asList(NODE));
    }

    private List<ObjectTypes> createSortedTypesForDeleteAll() {
        final List<ObjectTypes> head = Arrays.asList(SHADOW, USER, ROLE, ORG, SERVICE);

        final List<ObjectTypes> tail = Arrays.asList(
                RESOURCE, CONNECTOR, MARK, OBJECT_TEMPLATE, OBJECT_COLLECTION, ARCHETYPE, SECURITY_POLICY, PASSWORD_POLICY,
                SYSTEM_CONFIGURATION);

        return createSortedTypes(head, tail, Arrays.asList(NODE, ARCHETYPE));
    }

    private List<ObjectTypes> createSortedTypes(List<ObjectTypes> head, List<ObjectTypes> tail, List<ObjectTypes> skip) {
        final List<ObjectTypes> result = new ArrayList<>();

        result.addAll(head);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (head.contains(type) || tail.contains(type) || skip.contains(type)) {
                continue;
            }

            if (Modifier.isAbstract(type.getClassDefinition().getModifiers())) {
                continue;
            }

            result.add(type);
        }

        result.addAll(tail);

        return result;
    }

    private ActivityDefinitionType createInitialImportActivity(int order) {
        ExecuteScriptActionExpressionType execute = new ExecuteScriptActionExpressionType();
        ScriptExpressionEvaluatorType script = new ScriptExpressionEvaluatorType();
        script.setCode("\n"
                + FactoryResetHelper.class.getName() + ".runInitialDataImport(\n"
                + "\tcom.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder.getApplicationContext(),\n"
                + "\tmidpoint.getCurrentTask().getResult())\n"
                + "log.info(\"Repository factory reset finished\")\n"
        );
        execute.setScript(script);
        execute.setForWholeInput(true);

        ExecuteScriptType executeScript = new ExecuteScriptType()
                .scriptingExpression(new ObjectFactory().createExecute(execute));

        // @formatter:off
        return new ActivityDefinitionType()
                .identifier("Initial import")
                .order(order)
                .beginWork()
                    .beginNonIterativeScripting()
                        .scriptExecutionRequest(executeScript)
                    .<WorkDefinitionsType>end()
                .end();
        // @formatter:on
    }

    /**
     * Used in factory reset task as last activity. Do not remove!
     */
    public static void runInitialDataImport(ApplicationContext context, OperationResult parent) {
        OperationResult result = parent.createSubresult(OPERATION_INITIAL_IMPORT);

        ModelService modelService = context.getBean(ModelService.class);
        CacheDispatcher cacheDispatcher = context.getBean(CacheDispatcher.class);
        TaskManager taskManager = context.getBean(TaskManager.class);
        PrismContext prismContext = context.getBean(PrismContext.class);
        MidpointConfiguration midpointConfiguration = context.getBean(MidpointConfiguration.class);

        try {
            InitialDataImport initialDataImport = new InitialDataImport();
            initialDataImport.setModel(modelService);
            initialDataImport.setTaskManager(taskManager);
            initialDataImport.setPrismContext(prismContext);
            initialDataImport.setConfiguration(midpointConfiguration);
            initialDataImport.init(true);

            // TODO consider if we need to go clusterwide here
            cacheDispatcher.dispatchInvalidation(null, null, true, null);

            modelService.shutdown();

            modelService.postInit(result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't run initial data import", ex);
        }
    }
}
