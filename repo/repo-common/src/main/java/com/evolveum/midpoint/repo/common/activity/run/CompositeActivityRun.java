/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

public final class CompositeActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends AbstractCompositeActivityRun<WD, AH, WS> {

    public CompositeActivityRun(ActivityRunInstantiationContext<WD, AH> context) {
        super(context);
        setInstanceReady();
    }
}
