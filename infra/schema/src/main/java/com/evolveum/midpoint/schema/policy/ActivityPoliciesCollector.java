/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

/**
 * Collects the policies of all the activities defined in an activity definition tree.*Policies can sit in the activity itself, in any activity of its `composition`, in the activities
 * inserted by `tailoring/insertBefore` and `tailoring/insertAfter` and in `tailoring/change` - all of that
 * recursively. The collector returns them as a flat list; where exactly the policies were found is not preserved.
 */
public class ActivityPoliciesCollector {

    private ActivityPoliciesCollector() {
    }
    public static @NotNull List<ActivityPoliciesType> collect(@Nullable ActivityDefinitionType activity) {
        List<ActivityPoliciesType> result = new ArrayList<>();
        collect(activity, result);
        return result;
    }

    private static void collect(ActivityDefinitionType activity, List<ActivityPoliciesType> result) {
        if (activity == null) {
            return;
        }
        add(activity.getPolicies(), result);

        ActivityCompositionType composition = activity.getComposition();
        if (composition != null) {
            for (ActivityDefinitionType child : composition.getActivity()) {
                collect(child, result);
            }
        }

        ActivitiesTailoringType tailoring = activity.getTailoring();
        if (tailoring != null) {
            for (ActivityTailoringType change : tailoring.getChange()) {
                add(change.getPolicies(), result);
            }
        }
    }

    private static void add(ActivityPoliciesType policies, List<ActivityPoliciesType> result) {
        if (policies != null) {
            result.add(policies);
        }
    }

}
