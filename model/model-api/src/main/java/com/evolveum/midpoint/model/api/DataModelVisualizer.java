/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.Collection;

/**
 * EXPERIMENTAL
 */
public interface DataModelVisualizer {

    enum Target {
        DOT, CYTOSCAPE
    }

    String visualize(Collection<String> resourceOids, Target target, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    String visualize(ResourceType resource, Target target, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException;
}
