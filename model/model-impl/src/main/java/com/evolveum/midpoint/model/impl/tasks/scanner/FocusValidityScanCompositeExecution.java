/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
public class FocusValidityScanCompositeExecution
        extends AbstractCompositeActivityExecution<
        FocusValidityScanWorkDefinition,
        FocusValidityScanActivityHandler,
        ScanWorkStateType> {

    protected FocusValidityScanCompositeExecution(
            ExecutionInstantiationContext<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> context) {
        super(context);
    }

}
