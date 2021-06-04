/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.definition.CompositeWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.PureCompositeActivityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PureCompositeWorkStateType;

public class PureCompositeActivityExecution
        extends AbstractCompositeActivityExecution<CompositeWorkDefinition, PureCompositeActivityHandler, PureCompositeWorkStateType> {

    public PureCompositeActivityExecution(
            ExecutionInstantiationContext<CompositeWorkDefinition, PureCompositeActivityHandler> context) {
        super(context);
    }
}
