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
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Provides methods for suggesting parts of the integration solution, like inbound/outbound mappings.
 */
public interface SmartIntegrationService {

    /** Submits "suggest object types" request. Returns a token used to query the status. */
    String submitSuggestObjectTypesOperation(String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws CommonException;

    /** Checks the status of the "suggest object types" request. */
    StatusInformation<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Suggests a discrete focus type for the application (resource) object type. */
    QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Suggests a correlations. TODO specify this method more precisely */
    default Object suggestCorrelation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            QName focusTypeName,
            Object interactionMetadata)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /** Suggests inbound/outbound mappings for the given resource object type and focus type. */
    MappingsSuggestionType suggestMappings(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            QName focusTypeName,
            @Nullable MappingsSuggestionFiltersType filters,
            MappingsSuggestionInteractionMetadataType interactionMetadata,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Suggests association type definitions for the given resource. (Either for all object types, or with some restrictions.)
     *
     * NOTE: Interaction metadata will be added later.
     */
    AssociationsSuggestionType suggestAssociations(
            String resourceOid,
            Collection<ResourceObjectTypeIdentification> subjectTypeIdentifications,
            Collection<ResourceObjectTypeIdentification> objectTypeIdentifications,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * @param status Status of the operation, such as {@link OperationResultStatus#IN_PROGRESS} (must be set if the operation
     *               is still in progress), {@link OperationResultStatus#SUCCESS} (operation was successfully completed),
     *               {@link OperationResultStatus#FATAL_ERROR} (operation failed).
     * @param message Human-readable explanation of the status of the operation, if available.
     * @param result Final result of the operation, if available.
     * @param <T> Type of the result.
     */
    record StatusInformation<T>(
            OperationResultStatus status,
            @Nullable LocalizableMessage message,
            @Nullable T result) {
    }
}
