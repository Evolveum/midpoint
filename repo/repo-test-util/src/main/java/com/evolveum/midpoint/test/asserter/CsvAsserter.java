/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;

/**
 * Asserts on CSV files, like reports. The first draft.
 *
 * TODO eliminate the need to call {@link #removeStandardFooter()} and {@link #parse()} explicitly
 */
public class CsvAsserter<RA> extends AbstractAsserter<RA> {

    private static final String STANDARD_FOOTER = "No active subscription. Please support midPoint by purchasing a subscription.";

    @NotNull private final List<String> lines;

    // The following are filled in after calling "parse" method.

    private List<String> headerNames;
    private List<CSVRecord> records;

    public CsvAsserter(@NotNull List<String> lines, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.lines = lines;
    }

    public CsvAsserter<RA> removeStandardFooter() {
        String last = lines.get(lines.size() - 1);
        assertThat(last).as("last line").isEqualTo(STANDARD_FOOTER);
        lines.remove(lines.size() - 1);
        return this;
    }

    public CsvAsserter<RA> parse(CSVFormat format) throws IOException {
        try (CSVParser parser = CSVParser.parse(
                String.join("\n", lines),
                format)) {
            headerNames = parser.getHeaderNames();
            records = parser.getRecords();
        }
        return this;
    }

    public CsvAsserter<RA> parse() throws IOException {
        return parse(
                CSVFormat.newFormat(';')
                        .builder()
                        .setQuote('"')
                        .setEscape('\\')
                        .setHeader()
                        .build());
    }

    public CsvAsserter<RA> display() {
        return display(desc());
    }

    public CsvAsserter<RA> display(String message) {
        IntegrationTestTools.display(message + ": header names", headerNames);
        IntegrationTestTools.display(message + ": records", records);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails(lines.size() + "-line CSV");
    }

    public RowAsserter row(int index) {
        var rowAsserter = new RowAsserter(index, this, descWithDetails("row #" + index + " in " + desc()));
        copySetupTo(rowAsserter);
        return rowAsserter;
    }

    public class RowAsserter extends AbstractAsserter<CsvAsserter<RA>> {

        int index;

        RowAsserter(int index, CsvAsserter<RA> returnAsserter, String details) {
            super(returnAsserter, details);
            this.index = index;
        }

        public RowAsserter assertValue(int column, String expected) {
            assertThat(getValue(column)).as("value in col " + column + " in " + desc()).isEqualTo(expected);
            return this;
        }

        public RowAsserter assertValues(int column, String... expected) {
            return assertValues(column, Set.of(expected));
        }

        @SuppressWarnings("WeakerAccess")
        public RowAsserter assertValues(int column, Set<String> expected) {
            assertThat(getValues(column))
                    .as("values in col " + column + " in " + desc())
                    .containsExactlyInAnyOrderElementsOf(expected);
            return this;
        }

        private String getValue(int column) {
            return records.get(index).get(column);
        }

        private Set<String> getValues(int column) {
            return Arrays.stream(getValue(column).split(","))
                    .collect(Collectors.toSet());
        }

        @Override
        protected String desc() {
            return "row #" + index + " in " + CsvAsserter.this.desc();
        }
    }
}
