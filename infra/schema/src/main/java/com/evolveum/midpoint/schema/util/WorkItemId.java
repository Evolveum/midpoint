/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 *  Uniquely identifies a work item.
 */
public class WorkItemId implements Serializable {

    @NotNull public final String caseOid;
    public final long id;

    public WorkItemId(@NotNull String caseOid, long id) {
        this.caseOid = caseOid;
        this.id = id;
    }

    public static WorkItemId create(@NotNull String caseOid, long id) {
        return new WorkItemId(caseOid, id);
    }

    public static WorkItemId create(@NotNull String compound) {
        String[] components = parseWorkItemId(compound);
        return new WorkItemId(components[0], Long.parseLong(components[1]));
    }

    public static String createWorkItemId(String caseOid, Long workItemId) {
        return caseOid + ":" + workItemId;
    }

    public static String getCaseOidFromWorkItemId(String workItemId) {
        return parseWorkItemId(workItemId)[0];
    }

    public static long getIdFromWorkItemId(String workItemId) {
        return Long.parseLong(parseWorkItemId(workItemId)[1]);
    }

    private static String[] parseWorkItemId(@NotNull String workItemId) {
        String[] components = workItemId.split(":");
        if (components.length != 2) {
            throw new IllegalStateException("Illegal work item ID: " + workItemId);
        } else {
            return components;
        }
    }

    public static WorkItemId of(@NotNull CaseWorkItemType workItem) {
        return create(CaseWorkItemUtil.getCaseRequired(workItem).getOid(), workItem.getId());
    }

    @NotNull
    public String getCaseOid() {
        return caseOid;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "WorkItemId{" +
                "caseOid='" + caseOid + '\'' +
                ", id=" + id +
                '}';
    }

    public String asString() {
        return caseOid + ":" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof WorkItemId))
            return false;
        WorkItemId that = (WorkItemId) o;
        return id == that.id &&
                caseOid.equals(that.caseOid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseOid, id);
    }
}
