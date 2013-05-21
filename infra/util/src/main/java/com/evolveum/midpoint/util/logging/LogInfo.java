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

package com.evolveum.midpoint.util.logging;

import java.beans.ConstructorProperties;

/**
 *
 * @author Vilo Repan
 */
public class LogInfo {

    private String packageName;
    private int level;

    @ConstructorProperties({"packageName", "level"})
    public LogInfo(String packageName, int level) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name can't be null nor empty.");
        }
        this.packageName = packageName;
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public int hashCode() {
       return packageName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LogInfo)) {
            return false;
        }

        LogInfo info = (LogInfo) obj;
        return packageName == null ? info.getPackageName() == null
                : packageName.equals(info.getPackageName());
    }
}
