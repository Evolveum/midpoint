/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

/**
 * Generic interface for a client that communicates with a remote service.
 * Can point to a real remote service or to a mock for testing purposes.
 */
public interface ServiceClient extends AutoCloseable {

    /** Calls the `suggestObjectTypes` method on the remote service. */
    ObjectTypesSuggestionType suggestObjectTypes(
            ResourceObjectClassDefinition objectClassDef,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            ResourceSchema resourceSchema,
            Task task,
            OperationResult result) throws SchemaException;

    /** Calls the `suggestFocusType` method on the remote service. */
    QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            Task task,
            OperationResult result) throws SchemaException;

    @Override
    void close();
}
