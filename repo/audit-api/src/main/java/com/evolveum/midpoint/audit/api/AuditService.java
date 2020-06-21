/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * @author semancik
 */
public interface AuditService {

    int MAX_MESSAGE_SIZE = 1024;
    int MAX_PROPERTY_SIZE = 1024;

    void audit(AuditEventRecord record, Task task);

    /**
     * Clean up audit records that are older than specified.
     *
     * @param policy Records will be deleted base on this policy.
     */
    void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult);

    /**
     * @throws UnsupportedOperationException if object retrieval is not supported
     */
    List<AuditEventRecord> listRecords(String query, Map<String, Object> params, OperationResult result);

    void listRecordsIterative(String query, Map<String, Object> params, AuditResultHandler auditResultHandler, OperationResult result);

    /**
     * Reindex items, e.g. if new columns were created for audit table according to which the search should be possible
     */
    void reindexEntry(AuditEventRecord record);

    /**
     * @throws UnsupportedOperationException if object retrieval is not supported
     */
    long countObjects(String query, Map<String, Object> params);

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

    // TODO MID-6319 cleanup after done
    // see com.evolveum.midpoint.repo.api.RepositoryService.countObjects for javadoc and rephrase it here
    <T extends ObjectType> long countObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException;

    @NotNull
    <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException;
}
