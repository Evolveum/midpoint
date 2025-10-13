/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.Nullable;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface VisualizationDescriptionHandler {

    boolean match(VisualizationImpl visualization, @Nullable VisualizationImpl parentVisualization);

    void apply(VisualizationImpl visualization, @Nullable VisualizationImpl parentVisualization, Task task, OperationResult result);
}
