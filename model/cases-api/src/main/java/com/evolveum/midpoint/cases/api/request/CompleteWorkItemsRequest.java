/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.request;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Request to complete a set of work items (on given case).
 *
 * Each {@link SingleCompletion} may have its own {@link SingleCompletion#output}.
 */
public class CompleteWorkItemsRequest extends Request {

    public static class SingleCompletion {

        private final long workItemId;

        @NotNull private final AbstractWorkItemOutputType output;

        public SingleCompletion(long workItemId, @NotNull AbstractWorkItemOutputType output) {
            this.workItemId = workItemId;
            this.output = output;
        }

        public long getWorkItemId() {
            return workItemId;
        }

        public @NotNull AbstractWorkItemOutputType getOutput() {
            return output;
        }

        @Override
        public String toString() {
            return "SingleCompletion{" +
                    "workItemId=" + workItemId +
                    ", output='" + output + '\'' +
                    '}';
        }
    }

    @NotNull private final Collection<SingleCompletion> completions = new ArrayList<>();

    public CompleteWorkItemsRequest(@NotNull String caseOid, WorkItemEventCauseInformationType causeInformation) {
        super(caseOid, causeInformation);
    }

    @NotNull
    public Collection<SingleCompletion> getCompletions() {
        return completions;
    }

    @Override
    public String toString() {
        return "CompleteWorkItemsRequest{" +
                "completions=" + completions +
                ", caseOid='" + caseOid + '\'' +
                ", causeInformation=" + causeInformation +
                '}';
    }
}
