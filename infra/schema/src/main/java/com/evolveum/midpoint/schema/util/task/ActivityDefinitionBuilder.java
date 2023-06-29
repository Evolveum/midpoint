/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps with creating {@link ActivityDefinitionType} objects.
 *
 * Currently, assumes there's exactly one work definition. This is the most typical scenario. If you need more complex,
 * consider creating the activity yourself.
 *
 * This class maintains interesting parts of the activity definition; and, at the very end, it combines them into
 * the final definition.
 */
@Experimental // to be seen how this class will evolve
public class ActivityDefinitionBuilder {

    @NotNull private final WorkDefinitionsType works;

    private ActivityExecutionModeDefinitionType executionModeDef;

    private SimulationDefinitionType simulationDefinition;

    private ActivityDefinitionBuilder(@NotNull WorkDefinitionsType works) {
        this.works = works;
    }

    public static ActivityDefinitionBuilder create(@NotNull WorkDefinitionsType works) {
        return new ActivityDefinitionBuilder(works);
    }

    public static ActivityDefinitionBuilder create(@NotNull ExplicitChangeExecutionWorkDefinitionType definition) {
        return new ActivityDefinitionBuilder(
                new WorkDefinitionsType()
                        .explicitChangeExecution(definition));
    }

    public static ActivityDefinitionBuilder create(@NotNull CleanupWorkDefinitionType definition) {
        return new ActivityDefinitionBuilder(
                new WorkDefinitionsType()
                        .cleanup(definition));
    }

    public static ActivityDefinitionBuilder create(@NotNull IterativeScriptingWorkDefinitionType definition) {
        return new ActivityDefinitionBuilder(
                new WorkDefinitionsType()
                        .iterativeScripting(definition));
    }

    public static ActivityDefinitionBuilder create(@NotNull ReindexingWorkDefinitionType definition) {
        return new ActivityDefinitionBuilder(
                new WorkDefinitionsType()
                        .reindexing(definition));
    }

    public ActivityDefinitionBuilder withExecutionMode(@Nullable ExecutionModeType value) {
        if (executionModeDef == null) {
            if (value == null) {
                return this;
            }
            executionModeDef = new ActivityExecutionModeDefinitionType();
        }
        executionModeDef.setMode(value);
        return this;
    }

    public ActivityDefinitionBuilder withPredefinedConfiguration(@Nullable PredefinedConfigurationType value) {
        if (executionModeDef == null) {
            if (value == null) {
                return this;
            }
            executionModeDef = new ActivityExecutionModeDefinitionType();
        }
        var configurationSpec = executionModeDef.getConfigurationToUse();
        if (configurationSpec == null) {
            configurationSpec = executionModeDef.beginConfigurationToUse();
        }
        configurationSpec.setPredefined(value);
        return this;
    }

    public ActivityDefinitionBuilder withSimulationDefinition(@Nullable SimulationDefinitionType simulationDefinition) {
        this.simulationDefinition = simulationDefinition;
        return this;
    }

    public @NotNull ActivityDefinitionType build() {
        var def = new ActivityDefinitionType()
                .work(works)
                .execution(executionModeDef);
        if (simulationDefinition != null) {
            def.reporting(new ActivityReportingDefinitionType()
                    .simulationResult(new ActivitySimulationResultDefinitionType()
                            .definition(simulationDefinition)));
        }
        return def;
    }
}
