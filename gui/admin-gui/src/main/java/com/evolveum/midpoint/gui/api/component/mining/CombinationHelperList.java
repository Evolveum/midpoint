/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import java.util.List;

public class CombinationHelperList {

    int count;
    List<String> combinations;

    public CombinationHelperList(int count, List<String> combinations) {
        this.count = count;
        this.combinations = combinations;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getCombinations() {
        return combinations;
    }

    public void setCombinations(List<String> combinations) {
        this.combinations = combinations;
    }

}
