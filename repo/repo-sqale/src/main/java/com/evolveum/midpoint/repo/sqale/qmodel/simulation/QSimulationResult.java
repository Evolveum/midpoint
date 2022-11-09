/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

public class QSimulationResult extends QObject<MSimulationResult> {

    public static final String TABLE_NAME = "m_simulation_result";

    public QSimulationResult(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSimulationResult(String variable, String schema, String table) {
        super(MSimulationResult.class, variable, schema, table);
    }

}
