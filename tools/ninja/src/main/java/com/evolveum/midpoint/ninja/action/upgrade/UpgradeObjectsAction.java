/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;

import java.util.HashMap;
import java.util.Map;

public class UpgradeObjectsAction extends Action<Void> {

    private Map<Class<?>, Object> context = new HashMap<>();

    private static final Class<? extends UpgradeStep>[] STEPS = new Class[] {
            DownloadDistributionStep.class,
            DatabaseSchemaStep.class,
            UpgradeMidpointHomeStep.class
    };

    @Override
    public void execute() throws Exception {
        for (Class<? extends UpgradeStep> stepType : STEPS) {
            UpgradeStep step = stepType.getConstructor().newInstance();
            Object result = step.execute();

            context.put(result.getClass(), result);
        }
    }
}
