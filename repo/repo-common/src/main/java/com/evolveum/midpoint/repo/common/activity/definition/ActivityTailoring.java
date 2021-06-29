/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ActivityTailoring {

    @NotNull private final List<ActivityTailoringType> changes = new ArrayList<>();

    public void add(ActivitiesTailoringType tailoringBean) {
        if (tailoringBean == null) {
            return;
        }
        changes.addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getChange()));
//        this.bean.getInsertBefore().addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getInsertBefore()));
//        this.bean.getInsertAfter().addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getInsertAfter()));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public @NotNull Collection<ActivityTailoringType> getChanges() {
        return changes;
    }
}
