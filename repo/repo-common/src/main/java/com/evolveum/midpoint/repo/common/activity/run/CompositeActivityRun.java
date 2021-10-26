/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
