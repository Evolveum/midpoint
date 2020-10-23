/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.stream.Collectors;

/**
 * Formats data as CSV file.
 */
public class CsvFormatting extends Formatting {

    private static final String DELIMITER = ";";

    /**
     * Raw data to be formatted.
     */
    private Data data;

    /**
     * Output of the formatter.
     */
    private StringBuilder sb;

    public String apply(Data data) {
        this.data = data;
        this.sb = new StringBuilder();
        formatHeader();
        formatData();
        return sb.toString();
    }

    private void formatHeader() {
        sb
                .append(columns.stream()
                        .map(c -> "\"" + c.label + "\"")
                        .collect(Collectors.joining(DELIMITER)))
                .append("\n");
    }

    private void formatData() {
        for (Data.Record record : data.getRecords()) {
            for (int col = 0; col < columns.size(); col++) {
                if (col > 0) {
                    sb.append(DELIMITER);
                }
                Column column = columns.get(col);
                Object value = record.getValue(col);
                if (value instanceof String) {
                    sb.append("\"");
                }
                sb.append(column.format(value));
                if (value instanceof String) {
                    sb.append("\"");
                }
            }
            sb.append("\n");
        }
    }
}
