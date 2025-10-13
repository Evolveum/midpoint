/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl;

import java.util.ArrayList;
import java.util.List;

public class ExpectedTask {
    final String targetOid;
    final String processName;
    final List<ExpectedWorkItem> workItems;

    public ExpectedTask(String targetOid, String processName) {
        this.targetOid = targetOid;
        this.processName = processName;
        this.workItems = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ExpectedTask{" +
                "targetOid='" + targetOid + '\'' +
                ", processName='" + processName + '\'' +
                ", workItems: " + workItems.size() +
                '}';
    }
}
