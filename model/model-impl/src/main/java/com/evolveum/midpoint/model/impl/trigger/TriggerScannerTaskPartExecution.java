/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.trigger;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_TRIGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskPartExecution;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.DefaultHandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Single execution of a trigger scanner task part.
 */
@ItemProcessorClass(TriggerScannerItemProcessor.class)
@DefaultHandledObjectType(ObjectType.class)
public class TriggerScannerTaskPartExecution
        extends AbstractScannerTaskPartExecution
        <ObjectType,
                TriggerScannerTaskHandler,
                TriggerScannerTaskHandler.TaskExecution,
                TriggerScannerTaskPartExecution,
                TriggerScannerItemProcessor> {

    public TriggerScannerTaskPartExecution(TriggerScannerTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) {
        return getPrismContext().queryFor(ObjectType.class)
                .item(F_TRIGGER, F_TIMESTAMP).le(taskExecution.getThisScanTimestamp())
                .build();
    }
}
