/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.inamik.text.tables.Cell.Functions.LEFT_ALIGN;
import static com.inamik.text.tables.Cell.Functions.RIGHT_ALIGN;
import static java.util.Collections.singleton;

import com.inamik.text.tables.Cell;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.grid.Border;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats data as "nice" ASCII table.
 */
public class AsciiTableFormatting extends Formatting {

    /**
     * Raw data to be formatted.
     */
    private Data data;

    /**
     * Table that draws the data nicely.
     */
    private GridTable table;

    public String apply(Data data) {
        this.data = data;
        createTable();
        formatHeader();
        formatData();
        finishTable();
        return getTableAsString();
    }

    private void createTable() {
        table = GridTable.of(2, columns.size());
    }

    private void formatHeader() {
        for (int i = 0; i < columns.size(); i++) {
            table.put(0, i, singleton(wrap(columns.get(i).label)));
        }
    }

    private String wrap(String s) {
        return StringUtils.wrap(s, ' ');
    }

    private void formatData() {
        for (int col = 0; col < columns.size(); col++) {
            Column column = columns.get(col);
            List<String> lines = new ArrayList<>();
            for (Data.Record record : data.getRecords()) {
                lines.add(wrap(column.format(record.getValue(col))));
            }
            table.put(1, col, lines);
        }
    }

    private void finishTable() {
        applyAlignment();
        createBorder();
    }

    private void applyAlignment() {
        table.apply(Cell.Functions.TOP_ALIGN);
        for (int i = 0; i < columns.size(); i++) {
            Alignment alignment = columns.get(i).alignment;
            if (alignment == null || alignment == Alignment.LEFT) {
                table.applyToCol(i, LEFT_ALIGN);
            } else {
                table.applyToCol(i, RIGHT_ALIGN);
            }
        }
    }

    private void createBorder() {
        table = Border.of(Border.Chars.of('+', '-', '|')).apply(table);
    }

    @NotNull
    private String getTableAsString() {
        StringBuilder sb = new StringBuilder();
        for (String line: table.toCell()) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
