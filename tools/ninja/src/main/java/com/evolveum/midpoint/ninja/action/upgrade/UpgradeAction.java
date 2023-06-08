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

import com.evolveum.midpoint.ninja.action.upgrade.step.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.util.Log;

public class UpgradeAction extends Action<UpgradeOptions> {

    private static final Class<? extends UpgradeStep<?>>[] STEPS = new Class[] {
//            UpgradePreCheckStep.class,
//            VerifyStep.class,
//            UpgradeObjectsBeforeShutdownStep.class,   // todo upgrade initial objects, also all other objects that can be upgraded before midpoint version/DB/midpoint home was upgraded
//            UpgradeMidpointInstallationStep.class,
//            UpgradeObjectsAfterShutdownStep.class, // todo upgrade initial objects, also all other objects (changes that had to be done after DB upgrade)
            // todo what if recomputation/reconciliation/whatever task is needed?
    };

    @Override
    public void execute() throws Exception {
        final Log log = context.getLog();

        UpgradeStepsContext ctx = new UpgradeStepsContext(context, options);
        File tempDirectory = ctx.getTempDirectory();

        log.info("Using '" + tempDirectory.getAbsolutePath() + "' as temp directory");

        FileUtils.forceMkdir(tempDirectory);

        for (Class<? extends UpgradeStep> stepType : STEPS) {
            long startTime = System.currentTimeMillis();

            UpgradeStep step = createStepInstance(stepType, ctx);
            try {
                log.info("Starting " + step.getPresentableName());

                StepResult result = step.execute();
                ctx.addResult(step.getClass(), result);

                String identifier = step.getIdentifier();

                File file = new File(tempDirectory, "step");
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileUtils.write(file, identifier, StandardCharsets.UTF_8, false);

                long duration = System.currentTimeMillis() - startTime;
                log.info(StringUtils.capitalize(step.getPresentableName()) + " finished in " +
                        DurationFormatUtils.formatDurationWords(duration, true, true));

                if (!result.shouldContinue()) {
                    // todo print warning that processing was interrupted
                    break;
                }
            } catch (Exception ex) {
                log.error("Unknown error occurred during upgrade, reason: {}", ex, ex.getMessage());
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
