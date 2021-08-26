/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * @author semancik
 */
public interface AuditService {

    int MAX_MESSAGE_SIZE = 1024;
    int MAX_PROPERTY_SIZE = 1024;

    String OP_AUDIT = "audit";
    String OP_CLEANUP_AUDIT_MAX_AGE = "cleanupAuditMaxAge";
    String OP_CLEANUP_AUDIT_MAX_RECORDS = "cleanupAuditMaxRecords";
    String OP_COUNT_OBJECTS = "countObjects";
    String OP_SEARCH_OBJECTS = "searchObjects";

    void audit(AuditEventRecord record, Task task, OperationResult result);

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
     */
    default void applyAuditConfiguration(SystemConfigurationAuditType configuration) {
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
}
