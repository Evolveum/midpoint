/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
