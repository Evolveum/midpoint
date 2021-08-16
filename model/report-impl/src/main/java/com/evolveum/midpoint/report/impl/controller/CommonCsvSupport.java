/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CsvFileFormatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.QuoteModeType;

import com.google.common.base.MoreObjects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generally useful methods for dealing with CSV files.
 * To be used in CSV report readers and writers.
 */
class CommonCsvSupport {

    @NotNull private final CsvFileFormatType configuration;

    CommonCsvSupport(@Nullable FileFormatConfigurationType formatsConfiguration) {
        this.configuration = formatsConfiguration != null && formatsConfiguration.getCsv() != null ?
                formatsConfiguration.getCsv() : new CsvFileFormatType(PrismContext.get());
    }

    CSVFormat createCsvFormat() {
        return CSVFormat.newFormat(toCharacter(getFieldDelimiter()))
                .withAllowDuplicateHeaderNames(true)
                .withAllowMissingColumnNames(false)
                .withEscape(toCharacter(getEscape()))
                .withIgnoreEmptyLines(true)
                .withIgnoreHeaderCase(false)
                .withIgnoreSurroundingSpaces(true)
                .withQuote(toCharacter(getQuote()))
                .withQuoteMode(QuoteMode.valueOf(getQuoteMode().name()))
                .withRecordSeparator(getRecordSeparator())
                .withTrailingDelimiter(isTrailingDelimiter())
                .withTrim(isTrim());
    }

    private Character toCharacter(String value) {
        if (value == null) {
            return null;
        }

        if (value.length() != 1) {
            throw new IllegalArgumentException("Can't cast to character of " + value + ", illegal string size: "
                    + value.length() + ", should be 1");
        }

        return value.charAt(0);
    }

    private String getFieldDelimiter() {
        return configuration.getFieldDelimiter() == null ? ";" : configuration.getFieldDelimiter();
    }

    boolean isHeader() {
        return !Boolean.FALSE.equals(configuration.isHeader());
    }

    private boolean isTrim() {
        return Boolean.TRUE.equals(configuration.isTrim());
    }

    private boolean isTrailingDelimiter() {
        return Boolean.TRUE.equals(configuration.isTrailingDelimiter());
    }

    private String getRecordSeparator() {
        return MoreObjects.firstNonNull(configuration.getRecordSeparator(), "\r\n");
    }

    private QuoteModeType getQuoteMode() {
        return MoreObjects.firstNonNull(configuration.getQuoteMode(), QuoteModeType.NON_NUMERIC);
    }

    private String getQuote() {
        return MoreObjects.firstNonNull(configuration.getQuote(), "\"");
    }

    private String getEscape() {
        return MoreObjects.firstNonNull(configuration.getEscape(), "\\");
    }

    String getEncoding() {
        return MoreObjects.firstNonNull(configuration.getEncoding(), "utf-8");
    }

    String getMultivalueDelimiter() {
        return MoreObjects.firstNonNull(configuration.getMultivalueDelimiter(), ",");
    }

    String removeNewLines(String value) {
        return value
                .replace("\n\t", " ")
                .replace("\n", " ");
    }
}
