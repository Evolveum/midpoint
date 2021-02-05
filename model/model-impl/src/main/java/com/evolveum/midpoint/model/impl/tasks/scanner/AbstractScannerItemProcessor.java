/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Result handler for all scanner tasks.
 *
 * There is nothing specific here; the class exists mainly to define the type
 * parameters for related generic classes.
 */
public abstract class AbstractScannerItemProcessor
        <O extends ObjectType,
                TH extends AbstractScannerTaskHandler<TH, TE>,
                TE extends AbstractScannerTaskExecution<TH, TE>,
                E extends AbstractScannerTaskPartExecution<O, TH, TE, E, RH>,
                RH extends AbstractScannerItemProcessor<O, TH, TE, E, RH>>
        extends AbstractSearchIterativeItemProcessor<O, TH, TE, E, RH> {

    protected AbstractScannerItemProcessor(E taskExecution) {
        super(taskExecution);
    }
}
