/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonIterativeChangeExecutionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executes a set of deltas bound to specific objects. No iteration here.
 *
 * This activity is used to execute changes in background.
 */
@Component
public class NonIterativeChangeExecutionActivityHandler
        extends ModelActivityHandler<
        NonIterativeChangeExecutionActivityHandler.MyWorkDefinition,
        NonIterativeChangeExecutionActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.EXECUTE_DELTAS_TASK_HANDLER_URI;

    @PostConstruct
    public void register() {
        handlerRegistry.register(NonIterativeChangeExecutionWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(NonIterativeChangeExecutionWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MyWorkDefinition.class);
    }

    @Override
    public String getIdentifierPrefix() {
        return "non-iterative-change-execution";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, NonIterativeChangeExecutionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, NonIterativeChangeExecutionActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    final static class MyActivityRun
            extends LocalActivityRun<MyWorkDefinition, NonIterativeChangeExecutionActivityHandler, AbstractActivityWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<MyWorkDefinition, NonIterativeChangeExecutionActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .statisticsSupported(false); // no iterations, no statistics
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result)
                throws CommonException {

            getActivityHandler().beans.modelService.executeChanges(
                    getWorkDefinition().getParsedDeltas(),
                    getWorkDefinition().getExecutionOptions(),
                    getRunningTask(), result);

            return autoComputeRunResult();
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        @NotNull private final Collection<ObjectDeltaType> deltas;
        private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                deltas = getLegacyDeltas(legacy);
                executionOptions = ModelImplUtils.getModelExecuteOptions(legacy.getTaskExtension());
            } else {
                NonIterativeChangeExecutionWorkDefinitionType typedDefinition = (NonIterativeChangeExecutionWorkDefinitionType)
                        ((WorkDefinitionWrapper.TypedWorkDefinitionWrapper) source).getTypedDefinition();
                deltas = typedDefinition.getDelta();
                executionOptions = fromModelExecutionOptionsType(typedDefinition.getExecutionOptions());
            }

            argCheck(!deltas.isEmpty(), "No deltas specified");
        }

        private @NotNull Collection<ObjectDeltaType> getLegacyDeltas(LegacyWorkDefinitionSource legacyDef) {
            Collection<ObjectDeltaType> deltas =
                    legacyDef.getExtensionItemRealValues(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS, ObjectDeltaType.class);
            if (!deltas.isEmpty()) {
                return deltas;
            } else {
                return legacyDef.getExtensionItemRealValues(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA, ObjectDeltaType.class);
            }
        }

        private Collection<ObjectDelta<? extends ObjectType>> getParsedDeltas() throws SchemaException {
            List<ObjectDelta<? extends ObjectType>> parsedDeltas = new ArrayList<>();
            for (ObjectDeltaType deltaBean : deltas) {
                parsedDeltas.add(
                        DeltaConvertor.createObjectDelta(deltaBean));
            }
            return parsedDeltas;
        }

        public ModelExecuteOptions getExecutionOptions() {
            return executionOptions;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "deltas", deltas, indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "executionOptions", String.valueOf(executionOptions), indent+1);
        }
    }
}
