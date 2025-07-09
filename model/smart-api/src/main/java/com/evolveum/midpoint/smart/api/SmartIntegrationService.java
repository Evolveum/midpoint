/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import javax.xml.namespace.QName;

/**
 * Provides methods for suggesting parts of the integration solution, like inbound/outbound mappings.
 */
public interface SmartIntegrationService {

    /** TODO */
    DelineationSuggestions suggestDelineation(String resourceOid, QName objectClassName, Task task, OperationResult result);

    /** TODO */
    FocusTypeSuggestions suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result);

    //void suggestCorrelation();

    //void suggestAttributeMapping();

    //void suggestAssociations();
}
