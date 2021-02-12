/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Task part execution for scanner tasks.
 *
 * There is nothing specific here; the class exists mainly to define the type
 * parameters for related generic classes.
 */
public abstract class AbstractScannerTaskPartExecution
        <O extends ObjectType,
                TH extends AbstractScannerTaskHandler<TH, TE>,
                TE extends AbstractScannerTaskExecution<TH, TE>,
                E extends AbstractScannerTaskPartExecution<O, TH, TE, E, IP>,
                IP extends AbstractScannerItemProcessor<O, TH, TE, E, IP>>
    extends AbstractIterativeModelTaskPartExecution<O, TH, TE, E, IP> {

    public AbstractScannerTaskPartExecution(TE taskExecution) {
        super(taskExecution);
    }
}
