/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.tools.gui;

/**
 * @author lazyman
 */
public class PropertiesStatistics {

    private int added;
    private int deleted;

    public int getAdded() {
        return added;
    }

    public void increment(PropertiesStatistics partial) {
        if (partial == null) {
            return;
        }

        incrementAdded(partial.getAdded());
        incrementDeleted(partial.getDeleted());
    }

    public void incrementAdded(int value) {
        added += value;
    }

    public void incrementDeleted(int value) {
        deleted += value;
    }

    public void incrementAdded() {
        added++;
    }

    public void incrementDeleted() {
        deleted++;
    }

    public int getDeleted() {
        return deleted;
    }

    @Override
    public String toString() {
        return "{" + "added=" + added + ", deleted=" + deleted + "}";
    }
}
