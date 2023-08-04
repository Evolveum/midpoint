/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import java.util.Collection;

import com.evolveum.midpoint.schema.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * Service contract for audit - this can actually represent multiple audit services.
 * This is implementation independent, but some features may not be supported by all implementations.
 * For instance, {@link #supportsRetrieval()} indicates whether audit supports searching, or just storing.
 */
public interface AuditService {

    int MAX_MESSAGE_SIZE = 1024;
    int MAX_PROPERTY_SIZE = 1024;

    String OP_AUDIT = "audit";
    String OP_CLEANUP_AUDIT_MAX_AGE = "cleanupAuditMaxAge";
    String OP_CLEANUP_AUDIT_MAX_RECORDS = "cleanupAuditMaxRecords";
    String OP_COUNT_OBJECTS = "countObjects";
    String OP_SEARCH_OBJECTS = "searchObjects";
    String OP_SEARCH_OBJECTS_ITERATIVE = "searchObjectsIterative";
    String OP_SEARCH_OBJECTS_ITERATIVE_PAGE = "searchObjectsIterativePage";

    /**
     * Emits audit event record, e.g. writes it in the database or logs it to a file.
     * If audit is recorded to the repository, {@link AuditEventRecord#repoId} will be set,
     * it should not be provided by the client code except for import reasons.
     * This is high-level audit method that also tries to complete the audit event record,
     * e.g. filling in missing task information, current timestamp if none is provided, etc.
     */
    void audit(AuditEventRecord record, Task task, OperationResult parentResult);

    /**
     * Emits audit event record provided as a generated Prism bean.
     * Used for audit import functionality.
     * This is a low-level audit method that does not process provided record at all.
     */
    @Experimental
    void audit(AuditEventRecordType record, OperationResult parentResult);

    /**
     * Clean up audit records that are older than specified.
     *
     * @param policy Records will be deleted base on this policy.
     */
    void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult);

    /**
     * Returns true if retrieval of objects from the audit trail is supported.
     * This applies to listRecords, countObjects, reconstructObject and similar
     * operations.
     */
    boolean supportsRetrieval();

    /**
     * Called when audit configuration is established or changed.
     * Setting to null means to use default values for the settings.
     */
    default void applyAuditConfiguration(@Nullable SystemConfigurationAuditType configuration) {
    }

    /**
     * Returns the number of audit events that match the specified query.
     * If query is null or no filter in query is specified, count of all audit events is returned.
     * Ignores any paging and ordering from the query.
     *
     * @param query search query
     * @param parentResult parent operation result (in/out)
     * @return count of audit events of specified type that match the search criteria
     */
    int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult);

    /**
     * Returns the result list of audit events that match the specified query.
     * If query is null or no filter in query is specified, all audit events are returned,
     * subject to paging or internal sanity limit.
     *
     * @param query search query
     * @param parentResult parent operation result (in/out)
     * @return list of audit events of specified type that match the search criteria
     */
    @NotNull
    SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException;

    /**
     * Executes iterative search using the provided `handler` to process each object.
     *
     * @param query search query
     * @param handler result handler
     * @param options get options to use for the search
     * @param parentResult parent OperationResult (in/out)
     * @return summary information about the search result
     */
    @Experimental
    SearchResultMetadata searchObjectsIterative(
            @Nullable ObjectQuery query,
            @NotNull AuditResultHandler handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException;

    @NotNull RepositoryDiag getRepositoryDiag();
}
