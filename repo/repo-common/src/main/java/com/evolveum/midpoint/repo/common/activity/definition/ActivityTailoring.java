/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySubtaskDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class ActivityTailoring implements Cloneable {

    @NotNull private final List<ActivityTailoringType> changes = new ArrayList<>();

    /**
     * This is a very common kind of tailoring: execution of child activities in subtasks.
     * It is so common that it doesn't need to be defined via tailoring, but can be
     * specified simply in parent task distribution section.
     */
    private ActivitySubtaskDefinitionType subtasksForChildren;

    ActivityTailoring() {
    }

    private ActivityTailoring(ActivityTailoring prototype) {
        changes.addAll(CloneUtil.cloneCollectionMembers(prototype.changes));
        subtasksForChildren = CloneUtil.clone(prototype.subtasksForChildren);
    }


    void addFrom(ActivityDefinitionType activityDefinitionBean) {
        if (activityDefinitionBean == null) {
            return;
        }
        if (activityDefinitionBean.getDistribution() != null && activityDefinitionBean.getDistribution().getSubtasks() != null) {
            stateCheck(subtasksForChildren == null, "Subtasks for children are already defined");
            subtasksForChildren = activityDefinitionBean.getDistribution().getSubtasks().clone();
        }
        ActivitiesTailoringType tailoringBean = activityDefinitionBean.getTailoring();
        if (tailoringBean != null) {
            changes.addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getChange()));
//        this.bean.getInsertBefore().addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getInsertBefore()));
//        this.bean.getInsertAfter().addAll(CloneUtil.cloneCollectionMembers(tailoringBean.getInsertAfter()));
        }
    }

    public boolean isEmpty() {
        return changes.isEmpty() && subtasksForChildren == null;
    }

    public @NotNull Collection<ActivityTailoringType> getChanges() {
        return changes;
    }

    public ActivitySubtaskDefinitionType getSubtasksForChildren() {
        return subtasksForChildren;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityTailoring clone() {
        return new ActivityTailoring(this);
    }
}
