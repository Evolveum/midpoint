/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;

public class ActivityPoliciesDefinition {

    @NotNull private ActivityPoliciesType bean;

    public ActivityPoliciesDefinition(ActivityPoliciesType bean) {
        this.bean = bean;
    }

    public static @NotNull ActivityPoliciesDefinition create(@Nullable ActivityDefinitionType activityDefinitionBean) {
        if (activityDefinitionBean == null) {
            return new ActivityPoliciesDefinition(new ActivityPoliciesType());
        }

        ActivityPoliciesType policies = activityDefinitionBean.getPolicies();
        if (policies == null) {
            policies = new ActivityPoliciesType();
        } else {
            policies = policies.clone();
        }

        return new ActivityPoliciesDefinition(policies);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityPoliciesDefinition clone() {
        return new ActivityPoliciesDefinition(bean.clone());
    }

    public ActivityPoliciesType getPolicies() {
        return bean;
    }

    void applyChangeTailoring(ActivityTailoringType tailoring) {
        if (tailoring.getPolicies() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getPolicies());
        } else {
            // null means we do not want it to change.
        }
    }
}
