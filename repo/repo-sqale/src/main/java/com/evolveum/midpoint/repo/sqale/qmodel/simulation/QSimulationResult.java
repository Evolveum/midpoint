/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.sql.Types;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.sql.ColumnMetadata;

public class QSimulationResult extends QObject<MSimulationResult> {

    public static final String TABLE_NAME = "m_simulation_result";

    public static final ColumnMetadata PARTITIONED =
            ColumnMetadata.named("partitioned").ofType(Types.BOOLEAN);

    public QSimulationResult(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSimulationResult(String variable, String schema, String table) {
        super(MSimulationResult.class, variable, schema, table);
    }

    public final BooleanPath partitioned = createBoolean("partitioned", PARTITIONED);

}
