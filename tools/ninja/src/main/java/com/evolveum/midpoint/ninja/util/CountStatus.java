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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CountStatus {

    private AtomicInteger count = new AtomicInteger(0);
    private AtomicInteger skipped = new AtomicInteger(0);

    private long start;

    private int lastCount;
    private long lastPrintout;

    public CountStatus() {
    }

    public int getCount() {
        return count.get();
    }

    public double getAvg() {
        if (count.get() == 0) {
            return 0d;
        }

        long span = System.currentTimeMillis() - start;

        return  ((double) span) / count.get();
    }

    public double getAvgReqSpeed() {
        if (lastCount == 0) {
            return 0d;
        }

        long span = (System.currentTimeMillis() - lastPrintout) / 1000;

        return  ((double) span) / lastCount;
    }

    public long getStart() {
        return start;
    }

    public long getLastPrintout() {
        return lastPrintout;
    }

    public int getSkipped() {
        return skipped.get();
    }

    public void incrementSkipped() {
        skipped.incrementAndGet();
    }

    public void incrementCount() {
        count.incrementAndGet();
    }

    public void start() {
        this.start = System.currentTimeMillis();
    }

    public void lastPrintoutNow() {
        this.lastPrintout = System.currentTimeMillis();
        this.lastCount = count.get() - lastCount;
    }
}
