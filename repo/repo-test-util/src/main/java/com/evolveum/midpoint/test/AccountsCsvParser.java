/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Parses the CSV with accounts and provides the set of attributes names and collection of accounts.
 *
 * The CSV format is currently set to:
 * * Delimiter: `|`
 * * Comment: `#`
 * * Surrounding spaces are ignored
 * * ID column: `id`
 * * Ignored columns starts with: `_`
 */
public final class AccountsCsvParser {

    private static final char COMMENT_MARKER = '#';
    private static final char DELIMITER = '|';
    private static final String IGNORED_COLUMN_PREFIX = "_";
    private static final String ID_COLUMN = "id";

    private final File accountsCsv;
    private Collection<Account> accounts;
    private Set<String> attributeNames;

    public AccountsCsvParser(File accountsCsv) {
        this.accountsCsv = accountsCsv;
    }

    private void parseAccountsFile() throws IOException {
        final List<CSVRecord> records;
        final RelevantColumnsIndexes relevantColumnsIndexes;

        try (final CSVParser parser = CSVParser.parse(
                this.accountsCsv,
                StandardCharsets.UTF_8,
                CSVFormat.Builder.create(CSVFormat.DEFAULT)
                        .setCommentMarker(COMMENT_MARKER)
                        .setHeader().setSkipHeaderRecord(true)
                        .setDelimiter(DELIMITER)
                        .setIgnoreSurroundingSpaces(true)
                        .build())) {

            relevantColumnsIndexes = filterRelevantHeaders(parser.getHeaderMap());
            records = parser.getRecords();
        }

        this.attributeNames = relevantColumnsIndexes.attributesNames();
        this.accounts = parseAccounts(records, relevantColumnsIndexes);
    }

    /**
     * Read the account attributes names from.
     *
     * @return The set of attributes names as defined in the columns of CSV header row.
     */
    public Set<String> readAttributesNames() throws IOException {
        if (this.attributeNames == null) {
            parseAccountsFile();
        }

        return Collections.unmodifiableSet(this.attributeNames);
    }

    /**
     * Collection of parsed accounts with their IDs and map of attributes with values.
     *
     * @return The collection of parsed accounts.
     */
    public Collection<Account> readAccounts() throws IOException {
        if (this.accounts == null) {
            parseAccountsFile();
        }

        return Collections.unmodifiableCollection(this.accounts);
    }

    private static RelevantColumnsIndexes filterRelevantHeaders(Map<String, Integer> headersIndexes) {
        int idColumnIndex = -1;
        final Map<String, Integer> attributesIndexes = new HashMap<>();
        for (Map.Entry<String, Integer> headerIndex : headersIndexes.entrySet()) {
            final String headerName = headerIndex.getKey();
            if (headerName.equals(ID_COLUMN)) {
                idColumnIndex = headerIndex.getValue();
            } else if (!headerName.startsWith(IGNORED_COLUMN_PREFIX)) {
                attributesIndexes.put(headerName, headerIndex.getValue());
            }
        }
        return new RelevantColumnsIndexes(idColumnIndex, attributesIndexes);
    }

    private static Collection<Account> parseAccounts(List<CSVRecord> records,
            RelevantColumnsIndexes relevantColumnsIndexes) {
        return records.stream()
                .map(record -> {
                    final Map<String, String> attributes = relevantColumnsIndexes.attributesWithIndexesSet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> record.get(entry.getValue())));
                    final String accountId = record.get(relevantColumnsIndexes.idColumnIndex());
                    return new Account(accountId, attributes);
                })
                .toList();
    }

    public record Account(String id, Map<String, String> attributes) { }

    private record RelevantColumnsIndexes(int idColumnIndex, Map<String, Integer> attributesWithIndexes) {

        Set<String> attributesNames() {
            return this.attributesWithIndexes.keySet();
        }

        Set<Map.Entry<String, Integer>> attributesWithIndexesSet() {
            return this.attributesWithIndexes.entrySet();
        }
    }

}
