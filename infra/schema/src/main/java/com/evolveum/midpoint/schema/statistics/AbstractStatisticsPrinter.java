/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * Prints statistics in selected format.
 */
public abstract class AbstractStatisticsPrinter<T> {

    public enum Format {

        TEXT(AsciiTableFormatting::new),
        CSV(CsvFormatting::new),
        RAW(RawFormatting::new);

        @NotNull private final Supplier<Formatting> formattingSupplier;

        Format(@NotNull Supplier<Formatting> formattingSupplier) {
            this.formattingSupplier = formattingSupplier;
        }

        Formatting createFormatting() {
            return formattingSupplier.get();
        }
    }

    public enum SortBy {
        NAME, COUNT, TIME, OWN_TIME
    }

    /**
     * Information that is to be formatted.
     */
    @NotNull final T information;

    @NotNull final Options options;

    /**
     * Number of iterations, e.g. objects processed.
     * Null means that this option is not applicable, or we do not want to show per-iteration information.
     */
    final Integer iterations;

    /**
     * Number of seconds to which the data are related.
     * Null means that this option is not applicable, or we do not want to show time-related information.
     */
    final Integer seconds;

    /**
     * Data to be formatted.
     */
    Data data;

    /**
     * Formatting to be used.
     */
    private Formatting formatting;

    public AbstractStatisticsPrinter(@NotNull T information, Options options, Integer iterations, Integer seconds) {
        this.information = information;
        this.options = options != null ? options : new Options();
        this.iterations = iterations;
        this.seconds = seconds;
    }

    void initData() {
        if (data == null) {
            data = new Data();
        } else {
            throw new IllegalStateException("data already created");
        }
    }

    void initFormatting() {
        if (formatting == null) {
            formatting = options.format.createFormatting();
        } else {
            throw new IllegalStateException("formatting already created");
        }
    }

    public String print() {
        prepare();
        return applyFormatting();
    }

    public abstract void prepare();

    String applyFormatting() {
        return formatting.apply(data);
    }

    void addColumn(String label, Formatting.Alignment alignment, String format) {
        formatting.addColumn(label, alignment, format);
    }

    protected Number avg(Number total, Integer countObject) {
        int count = or0(countObject);
        return total != null && count > 0 ? total.floatValue() / count : null;
    }

    protected Number div(Number total, Integer countObject) {
        int count = or0(countObject);
        return total != null && count > 0 ? total.floatValue() / count : null;
    }

    protected Number percent(Long value, Long baseObject) {
        long base = or0(baseObject);
        if (value == null) {
            return null;
        } else if (base != 0) {
            return 100.0f * value / base;
        } else if (value == 0) {
            return 0.0f;
        } else {
            return Float.NaN;
        }
    }

    int zeroIfNull(Integer n) {
        return n != null ? n : 0;
    }

    long zeroIfNull(Long n) {
        return n != null ? n : 0;
    }

    <X> X nullIfFalse(boolean condition, X value) {
        return condition ? value : null;
    }

    public static class Options {
        @NotNull final Format format;
        @NotNull final SortBy sortBy;

        public Options() {
            this(null, null);
        }

        public Options(Format format, SortBy sortBy) {
            this.format = format != null ? format : Format.TEXT;
            this.sortBy = sortBy != null ? sortBy : SortBy.NAME;
        }
    }

    String formatString() {
        return "%s";
    }

    String formatInt() {
        return formatting.isNiceNumbersFormatting() ? "%,d" : "%d";
    }

    String formatFloat1() {
        return formatting.isNiceNumbersFormatting() ? "%,.1f" : "%f";
    }

    String formatPercent1() {
        return formatting.isNiceNumbersFormatting() ? "%.1f%%" : "%f%%";
    }

    String formatPercent2() {
        return formatting.isNiceNumbersFormatting() ? "%.2f%%" : "%f%%";
    }

    public Data getData() {
        return data;
    }

    public Stream<Object[]> getRawDataStream() {
        return data.getRawDataStream();
    }

    public Formatting getFormatting() {
        return formatting;
    }

    public String[] getColumnLabelsAsArray() {
        return formatting.getColumnLabelsAsArray();
    }
}
