/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface VisualizationDescriptionHandler {

    boolean match(VisualizationImpl visualization);

    void apply(VisualizationImpl visualization, Task task, OperationResult result);
}
