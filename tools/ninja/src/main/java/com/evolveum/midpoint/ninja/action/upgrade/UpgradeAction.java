/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;

public class UpgradeAction extends Action<UpgradeOptions> {

    private static final Class<? extends UpgradeStep>[] STEPS = new Class[] {
            // todo upgrade initial objects, also all other objecst that can be upgraded before midpoint version/DB/midpoint home was upgraded
            VersionCheckStep.class,
            DownloadDistributionStep.class,
            DatabaseSchemaStep.class,
            UpgradeMidpointHomeStep.class,
            // todo upgrade initial objects, also all other objects (changes that had to be done after DB upgrade)
            // todo what if recomputation/reconciliation/whatever task is needed?
    };

    @Override
    public void execute() throws Exception {
        UpgradeStepsContext ctx = new UpgradeStepsContext(context);

        for (Class<? extends UpgradeStep> stepType : STEPS) {
            UpgradeStep step;
            try {
                step = stepType.getConstructor(UpgradeStepsContext.class).newInstance(ctx);
            } catch (Exception ex) {
                step = stepType.getConstructor().newInstance();
            }

            Object result = step.execute();

            ctx.addResult(step.getClass(), result);
        }
    }
}
