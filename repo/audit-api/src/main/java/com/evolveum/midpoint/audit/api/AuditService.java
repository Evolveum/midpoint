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

    /**
     * Reindex audit record - <b>currently does nothing</b>.
     * Previously it effectively created missing changed items detail entities,
     * which is less and less useful nowadays.
     * TODO: In the future we may consider reindexing of new columns, but the functionality
     * is currently not fully specified.
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

    /**
     * @see com.evolveum.midpoint.repo.api.RepositoryService#countObjects
     */
    int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @Nullable OperationResult parentResult);

    @NotNull
    SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @Nullable OperationResult parentResult)
            throws SchemaException;
}
