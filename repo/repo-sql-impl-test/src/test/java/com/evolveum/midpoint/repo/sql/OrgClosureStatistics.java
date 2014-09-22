/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.util.logging.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public class OrgClosureStatistics {

    static class Key {
        int level;
        boolean isAdd;

        Key(int level, boolean isAdd) {
            this.level = level;
            this.isAdd = isAdd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (isAdd != key.isAdd) return false;
            if (level != key.level) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = level;
            result = 31 * result + (isAdd ? 1 : 0);
            return result;
        }
    }

    static class Record {
        Key key;
        long time;

        Record(int level, boolean isAdd, long time) {
            this.key = new Key(level, isAdd);
            this.time = time;
        }
    }

    static class AggregateRecord {
        int records;
        long totalTime;
    }

    private List<Record> records = new ArrayList<>();

    public void record(int level, boolean isAdd, long time) {
        records.add(new Record(level, isAdd, time));
    }

    private static final long BUCKETS[] = new long[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 250, 300, 350, 400, 450, 500,
                                  600, 700, 800, 900, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, Long.MAX_VALUE };

    private int maxLevel;

    private Map<Key, AggregateRecord[]> aggregates = new HashMap<>();

    public void dump(Trace logger) {
        aggregate();
        AggregateRecord totalForDelete = new AggregateRecord();
        AggregateRecord totalForAdd = new AggregateRecord();
        for (int level = 0; level <= maxLevel; level++) {
            logger.info("-------------------------------");
            logger.info("Level = " + level);
            AggregateRecord[] dataForDelete = aggregates.get(new Key(level, false));
            AggregateRecord[] dataForAdd = aggregates.get(new Key(level, true));
            dump(logger, "DELETE", dataForDelete, totalForDelete);
            dump(logger, "ADD", dataForAdd, totalForAdd);
        }
        logger.info("=================== Totals for delete/add: {} / {}",
                totalForDelete.records > 0 ? ((float) totalForDelete.totalTime)/totalForDelete.records : 0,
                totalForAdd.records > 0 ? ((float) totalForAdd.totalTime)/totalForAdd.records : 0);
    }

    private void dump(Trace logger, String label, AggregateRecord[] data, AggregateRecord total) {
        long totalTime = 0;
        int totalRecords = 0;
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                totalTime += data[i].totalTime;
                totalRecords += data[i].records;
            }
            total.totalTime += totalTime;
            total.records += totalRecords;
        }
        logger.info(" ---> {}: avg {} ms", label, totalRecords > 0 ? ((float) totalTime)/totalRecords : 0);
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                logger.info("up to {}: {}", BUCKETS[i], data[i].records);
            }
        }
    }

    private void aggregate() {
        aggregates = new HashMap<>();
        maxLevel = -1;
        for (Record record : records) {
            Key key = record.key;
            if (key.level > maxLevel) {
                maxLevel = key.level;
            }
            AggregateRecord[] data = aggregates.get(key);
            if (data == null) {
                data = new AggregateRecord[BUCKETS.length];
                for (int i = 0; i < data.length; i++) {
                    data[i] = new AggregateRecord();
                }
                aggregates.put(key, data);
            }
            int i = 0;
            while (i < BUCKETS.length && BUCKETS[i] < record.time) {
                i++;
            }
            data[i].records++;
            data[i].totalTime += record.time;
        }
    }
}
