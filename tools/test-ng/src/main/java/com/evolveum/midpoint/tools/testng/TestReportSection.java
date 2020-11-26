/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents single test report section that also self-formats itself to escaped (not quoted) CSV.
 */
public class TestReportSection {

    public static final char SEPARATOR = ',';
    public static final char ESCAPE_CHAR = '\\';

    private final String sectionName;
    private final List<Object[]> rows = new ArrayList<>();

    private String[] columnNames;

    public TestReportSection(String sectionName) {
        this.sectionName = sectionName;
    }

    /**
     * Specifies column names - without implicit "test" that will be added during dump.
     */
    public TestReportSection withColumns(String... columnNames) {
        this.columnNames = columnNames;
        return this;
    }

    public void addRow(Object... row) {
        rows.add(row);
    }

    /**
     * Dumps the output as CSV including section header preceded by an empty line.
     *
     * @param testName common test name used as a first column value (named "test")
     */
    public void dump(String testName, PrintStream out) {
        out.print("\n[" + sectionName + "]\ntest");
        for (String columnName : columnNames) {
            out.print(SEPARATOR + format(columnName));
        }

        for (Object[] row : rows) {
            out.print('\n' + format(testName));
            for (Object value : row) {
                out.print(SEPARATOR + format(value));
            }
        }
        out.println();
    }

    /**
     * Null returns empty string, Strings are escaped if necessary, other values are to-stringed.
     */
    private String format(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String) {
            if (((String) value).indexOf(SEPARATOR) == -1) {
                return value.toString();
            }

            StringBuilder sb = new StringBuilder((String) value);
            int i = 0;
            while (i < sb.length()) {
                if (sb.charAt(i) == SEPARATOR || sb.charAt(i) == ESCAPE_CHAR) {
                    sb.insert(i, ESCAPE_CHAR);
                    i += 1;
                }
                i += 1;
            }
            return sb.toString();
        }

        return String.valueOf(value);
    }
}
