/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.upgrade.step.*;
import com.evolveum.midpoint.ninja.util.Log;

public class UpgradeAction extends Action<UpgradeOptions> {

    private static final Class<? extends UpgradeStep>[] STEPS = new Class[] {
            // todo upgrade initial objects, also all other objects that can be upgraded before midpoint version/DB/midpoint home was upgraded
            VersionCheckStep.class,
            VerifyStep.class,
            UpgradeObjectsBeforeShutdownStep.class,
            DownloadDistributionStep.class,
            DatabaseSchemaStep.class,
            UpgradeMidpointHomeStep.class,
            UpgradeObjectsAfterShutdownStep.class, // todo upgrade initial objects, also all other objects (changes that had to be done after DB upgrade)
            // todo what if recomputation/reconciliation/whatever task is needed?
    };

    @Override
    public void execute() throws Exception {
        UpgradeStepsContext ctx = new UpgradeStepsContext(context, options);

        final Log log = context.getLog();

        for (Class<? extends UpgradeStep> stepType : STEPS) {
            UpgradeStep step = createStepInstance(stepType, ctx);
            try {
                log.info("Starting step: " + step.getPresentableName());

                StepResult result = step.execute();
                ctx.addResult(step.getClass(), result);

                String identifier = step.getIdentifier();

                File tmpFolder = null; // todo fix
                File file = new File(tmpFolder, "step");    // todo check for existence
                FileUtils.write(file, identifier, StandardCharsets.UTF_8, false);

                log.info(step.getPresentableName() + " finished");

                if (!result.shouldContinue()) {
                    // todo print warning that processing was interrupted
                    break;
                }
            } catch (Exception ex) {
                // todo handle exception properly
                throw new RuntimeException("Exception occurred", ex);
            }
        }
    }

    private UpgradeStep<?> createStepInstance(Class<? extends UpgradeStep> stepType, UpgradeStepsContext ctx)
            throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException {
        try {
            return stepType.getConstructor(UpgradeStepsContext.class).newInstance(ctx);
        } catch (Exception ex) {
            return stepType.getConstructor().newInstance();
        }
    }
}
