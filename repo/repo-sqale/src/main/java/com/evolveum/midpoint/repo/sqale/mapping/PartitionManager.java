/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.qmodel.shadow.MShadow;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.result.OperationResult;

public interface PartitionManager<M> {


    boolean isPartitionCreationOnAdd();

    void setPartitionCreationOnAdd(boolean value);

    /**
     *
     * If partitioning before add is enabled ensures that partition for particular
     * row exists (creates partition if it missing)
     *
     * @param row row object, which must at least contain all partitioning keys
     * @param jdbcSession
     */
    default void ensurePartitionExistsBeforeAdd(M row, JdbcSession jdbcSession) {
        if (isPartitionCreationOnAdd()) {
            ensurePartitionExists(row, jdbcSession);
        }
    }

    /**
     * Ensures that partition for particular row exists (creates partition if it missing)
     *
     * @param row row object, which must at least contain all partitioning keys
     * @param jdbcSession
     */
    void ensurePartitionExists(M row, JdbcSession jdbcSession);

    /** Analyze existing records,creates partitions for them and migrates them from default partitions
     * to specific partition.
     *
     * @param parentResult
     */
    void createMissingPartitions(OperationResult parentResult);

    static <R> void ensurePartitionExistsBeforeAdd(SqaleTableMapping<?,?, R> table, R row, JdbcSession jdbcSession) {
        var partitionManager = table.getPartitionManager();
        if (partitionManager != null) {
            partitionManager.ensurePartitionExistsBeforeAdd(row, jdbcSession);
        }
    }
}
