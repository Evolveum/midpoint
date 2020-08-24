/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querydsl;

import java.sql.Types;

import com.querydsl.sql.OracleTemplates;

public class MidpointOracleTemplates extends OracleTemplates {

    public static final MidpointOracleTemplates DEFAULT = new MidpointOracleTemplates();

    public MidpointOracleTemplates() {
        super('\\', false);
        addTypeNameToCode("varchar2", Types.VARCHAR, true);
        // We expect properly set Oracle DB where nationalized strings are also VARCHAR2.
        addTypeNameToCode("varchar2", Types.NVARCHAR, true);

        // While supported it produces constraint violation with auto-generated IDs.
        // We would need sequences + row-level trigger added to them, it's not worth the hassle.
        // Batching still works, it will just produce x separate insert statements. It's OK.
        setBatchToBulkSupported(false);
    }
}
