/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.scripting.BulkActionsExecutor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.TaskManager;

import org.springframework.beans.factory.annotation.Autowired;

public class BaseExpressionEvaluator {

    @Autowired
    ModelService modelService;

    @Autowired
    TaskManager taskManager;

    @Autowired
    PrismContext prismContext;

    @Autowired
    BulkActionsExecutor bulkActionsExecutor;

}
