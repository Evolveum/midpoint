/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Data to be displayed. This is an abstract form suitable for outputting in various formats (ascii, csv, ...).
 */
@Experimental
public class Data {

    private final List<Record> records = new ArrayList<>();

    public Record createRecord() {
        Record record = new Record();
        records.add(record);
        return record;
    }

    public int size() {
        return records.size();
    }

    public List<Record> getRecords() {
        return records;
    }

    public Stream<Object[]> getRawDataStream() {
        return records.stream()
                .map(record -> record.values.toArray());
    }

    static class Record {
        private final List<Object> values = new ArrayList<>();

        public void add(Object value) {
            values.add(value);
        }

        public Object getValue(int col) {
            return values.get(col);
        }
    }
}
