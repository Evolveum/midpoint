/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.testng.Assert.assertTrue;

import java.util.List;

import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QProcessedObjectMapping;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.querydsl.core.types.dsl.Expressions;

public class SimulationsPartitionedTest extends SimulationsBaselineTest {

    @Override
    protected boolean getPartitioned() {
        return true;
    }

    @Override
    protected void assertProcessedEmptyOrNoPartition(String oid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        // Check if partition exists
        try (var session = sqlRepoContext.newJdbcSession()) {
            var relName = Expressions.stringPath("relname");
            List<String> partitionList = session.newQuery().select(relName)
                    .from(Expressions.stringTemplate("pg_class"))
                    .where(relName.eq(QProcessedObjectMapping .partitionName(oid)))
                    .fetch();
            assertTrue(partitionList.isEmpty(), "Partition was not removed.");
        }
        super.assertProcessedEmptyOrNoPartition(oid, result);
    }
}
