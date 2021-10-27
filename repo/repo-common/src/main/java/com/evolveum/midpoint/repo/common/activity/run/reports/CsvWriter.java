/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.activity.run.reports.formatters.Formatter;
import com.evolveum.midpoint.repo.common.activity.run.reports.formatters.FormatterRegistry;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Writes to the CSV report file.
 */
class CsvWriter {

    private static final String FIELD_DELIMITER = ","; // Must be comma because of the escape method used.

    /** Writer for the open file. */
    @NotNull private final PrintWriter printWriter;

    /** Definition of the records. */
    @NotNull private final ComplexTypeDefinition recordDefinition;

    /** TODO */
    @NotNull private final Collection<ItemName> itemsIncluded;

    CsvWriter(@NotNull PrintWriter printWriter, @NotNull ComplexTypeDefinition recordDefinition,
            @NotNull Collection<ItemName> itemsIncluded) {
        this.printWriter = printWriter;
        this.recordDefinition = recordDefinition;
        this.itemsIncluded = itemsIncluded;
    }

    void writeHeader() {
        writeRow(
                recordDefinition.getDefinitions().stream()
                        .filter(this::isIncluded)
                        .flatMap(def ->
                                FormatterRegistry.getFormatterFor(def).formatHeader(def).stream()));
        flush();
    }

    private boolean isIncluded(@NotNull ItemDefinition<?> itemDefinition) {
        return itemsIncluded.isEmpty() || itemsIncluded.contains(itemDefinition.getItemName());
    }

    void writeRecord(@NotNull Containerable record) {
        writeRecordNoFlush(record);
        flush();
    }

    void writeRecordNoFlush(@NotNull Containerable record) {
        PrismContainerValue<?> pcv = record.asPrismContainerValue();
        writeRow(
                recordDefinition.getDefinitions().stream()
                        .filter(this::isIncluded)
                        .flatMap(itemDef -> getFormattedValue(pcv, itemDef).stream()));
    }

    private List<String> getFormattedValue(@NotNull PrismContainerValue<?> pcv, ItemDefinition<?> itemDef) {
        Formatter formatter = FormatterRegistry.getFormatterFor(itemDef);
        //noinspection unchecked
        Item<?, ?> item = pcv.findItem(itemDef.getItemName(), Item.class);
        if (item == null) {
            return formatter.formatValue(null);
        } else if (item.size() == 1) {
            return formatter.formatValue(item.getRealValue());
        } else {
            return formatter.formatMultiValue(item.getRealValues());
        }
    }

    private void writeRow(Stream<String> columns) {
        printWriter.println(
                columns
                        .map(value -> StringEscapeUtils.escapeCsv(MiscUtil.emptyIfNull(value)))
                        .collect(Collectors.joining(FIELD_DELIMITER)));
    }

    void flush() {
        printWriter.flush();
    }

    void close() {
        printWriter.close();
    }
}
