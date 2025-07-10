/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DelineationsSuggestionType;

/**
 * Provides methods for suggesting parts of the integration solution, like inbound/outbound mappings.
 */
public interface SmartIntegrationService {

    /** Suggests delineations for the given resource and object class. */
    DelineationsSuggestionType suggestDelineations(String resourceOid, QName objectClassName, Task task, OperationResult result);

    /** Suggests a discrete focus type for the application (resource) object type. */
    QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    //void suggestCorrelation();

    //void suggestAttributeMapping();

    //void suggestAssociations();
}
