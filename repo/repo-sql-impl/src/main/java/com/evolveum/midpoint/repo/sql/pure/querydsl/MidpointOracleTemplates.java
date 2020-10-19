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
        // varchar2 is the type we want to generate for missing varchar columns
        addTypeNameToCode("varchar2", Types.VARCHAR, true);

        // While supported it produces constraint violation with auto-generated IDs.
        // We would need sequences + row-level trigger added to them, it's not worth the hassle.
        // Batching still works, it will just produce x separate insert statements. It's OK.
        setBatchToBulkSupported(false);
    }
}
