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
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provides methods for suggesting parts of the integration solution, like inbound/outbound mappings.
 */
public interface SmartIntegrationService {

    /**
     * Creates a new resource with the given connector and the given connector configuration.
     *
     * @return OID of the created resource (if the resource was created successfully)
     */
    @Nullable String createNewResource(
            PolyStringType name,
            ObjectReferenceType connectorRef,
            ConnectorConfigurationType connectorConfiguration,
            Task task,
            OperationResult result);

    /**
     * Estimates the number of objects of the given class on the given resource.
     *
     * NOTE: Maybe this method could reside right in `ModelService`.
     *
     * @param resourceOid OID of the resource to estimate the size for
     * @param objectClassName Name of the object class to estimate the size for
     * @param maxSizeForEstimation When trying to estimate the size, the implementation may use a sample of objects - at most
     * this many objects.
     */
    ObjectClassSizeEstimationType estimateObjectClassSize(
            String resourceOid, QName objectClassName, int maxSizeForEstimation, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Returns the object holding last known statistics for the given resource and object class. */
    GenericObjectType getLatestStatistics(
            String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws SchemaException;

    /** Submits "suggest object types" request. Returns a token used to query the status. */
    String submitSuggestObjectTypesOperation(String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws CommonException;

    /**
     * List statuses of all relevant "suggest object types" requests (for given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<ObjectTypesSuggestionType>> listSuggestObjectTypesOperationStatuses(
            String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest object types" request. */
    StatusInfo<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Suggests a discrete focus type for the application (resource) object type. */
    QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Suggests correlation rules for the given resource object type and focus type.
     * The method returns the correlation rules along with any missing mappings that are needed for the rules to work.
     *
     * The ability to find correlation rules is limited by the information available. In particular:
     *
     * . TODO
     */
    CorrelationSuggestionType suggestCorrelation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable Object interactionMetadata,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Suggests inbound/outbound mappings for the given resource object type and focus type.
     *
     * The ability to find mappings is limited by the information available. In particular, if there are
     * no correlated data (between resource and midPoint), it cannot suggest any mappings other than "as-is" ones.
     *
     * Hence, mappings and correlation suggestions should be created hand in hand. These two methods can be called
     * in alternation, most probably in a loop, until the suggestions stabilize. User should review the suggestions
     * during the process.
     */
    MappingsSuggestionType suggestMappings(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable MappingsSuggestionFiltersType filters,
            @Nullable MappingsSuggestionInteractionMetadataType interactionMetadata,
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

}
