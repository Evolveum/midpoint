/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.ninja.util;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CountStatus {

    private int count;
    private int skipped;

    private long start;
    private long lastPrintout;

    public CountStatus() {
    }

    public int getCount() {
        return count;
    }

    public double getAvg() {
        if (count == 0) {
            return 0d;
        }

        long span = System.currentTimeMillis() - start;

        return span / count;
    }

    public long getStart() {
        return start;
    }

    public long getLastPrintout() {
        return lastPrintout;
    }

    public int getSkipped() {
        return skipped;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public void incrementCount() {
        this.count++;
    }

    public void start() {
        this.start = System.currentTimeMillis();
    }

    public void lastPrintoutNow() {
        this.lastPrintout = System.currentTimeMillis();
    }
}
