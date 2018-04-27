/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointOracleDialect extends Oracle12cDialect {

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
            public String[] getSqlCreateStrings(Index exportable, Metadata metadata) {
                String[] data = exporter.getSqlCreateStrings(exportable, metadata);
                String[] transformed = new String[data.length];

                for (int i = 0; i < data.length; i++) {
                    transformed[i] = data[i] + INITRANS;
                }

                return transformed;
            }

            @Override
            public String[] getSqlDropStrings(Index exportable, Metadata metadata) {
                return exporter.getSqlDropStrings(exportable, metadata);
            }
        };
    }
}
