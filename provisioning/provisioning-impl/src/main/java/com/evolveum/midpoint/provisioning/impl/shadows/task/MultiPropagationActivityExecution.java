/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Execution of a multi-propagation task.
 */
@ItemProcessorClass(MultiPropagationItemProcessor.class)
@HandledObjectType(ResourceType.class)
public class MultiPropagationActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ResourceType,
                MultiPropagationTaskHandler,
                MultiPropagationTaskHandler.TaskExecution,
                MultiPropagationActivityExecution,
                MultiPropagationItemProcessor> {

    public MultiPropagationActivityExecution(MultiPropagationTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
    }
}
