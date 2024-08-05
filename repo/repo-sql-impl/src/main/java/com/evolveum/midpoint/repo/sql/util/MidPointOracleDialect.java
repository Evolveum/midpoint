/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointOracleDialect extends OracleDialect {

    private static final String INITRANS = " initrans 30";

    @Override
    public String getTableTypeString() {
        return super.getTableTypeString() + INITRANS;
    }

    @Override
    public Exporter<Index> getIndexExporter() {
        Exporter<Index> exporter = super.getIndexExporter();

        return new Exporter<Index>() {

            @Override
            public String[] getSqlCreateStrings(Index exportable, Metadata metadata, SqlStringGenerationContext context) {
                String[] data = exporter.getSqlCreateStrings(exportable, metadata, context);
                String[] transformed = new String[data.length];

                for (int i = 0; i < data.length; i++) {
                    transformed[i] = data[i] + INITRANS;
                }

                return transformed;
            }

            @Override
            public String[] getSqlDropStrings(Index exportable, Metadata metadata, SqlStringGenerationContext context) {
                return exporter.getSqlDropStrings(exportable, metadata, context);
            }
        };
    }
}
