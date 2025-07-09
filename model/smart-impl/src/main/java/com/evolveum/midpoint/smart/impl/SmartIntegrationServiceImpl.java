/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DelineationsSuggestionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;

@Service
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    @Override
    public DelineationsSuggestionType suggestDelineations(
            String resourceOid, QName objectClassName, Task task, OperationResult result) {
        return new DelineationsSuggestionType(); // TODO replace with real implementation
    }

    @Override
    public QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result) {
        return UserType.COMPLEX_TYPE; // TODO replace with real implementation
    }
}
