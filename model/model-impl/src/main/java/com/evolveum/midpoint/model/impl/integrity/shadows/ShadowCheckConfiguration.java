/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowIntegrityAspectType.*;

import java.lang.reflect.InvocationTargetException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

class ShadowCheckConfiguration {

    private final Trace logger;

    boolean checkIntents;
    boolean checkUniqueness;
    boolean checkNormalization;
    final boolean checkFetch;
    final boolean checkOwners;
    boolean checkExtraData;

    final boolean fixIntents;
    final boolean fixUniqueness;
    final boolean fixNormalization;
    final boolean fixExtraData;
    final boolean fixResourceRef;

    boolean checkDuplicatesOnPrimaryIdentifiersOnly;

    boolean dryRun;

    DuplicateShadowsResolver duplicateShadowsResolver;

    ShadowCheckConfiguration(@NotNull Trace logger, @NotNull ShadowIntegrityCheckWorkDefinition def,
            @NotNull ExecutionModeType executionMode) {
        this.logger = logger;

        checkIntents = def.diagnoses(INTENTS);
        checkUniqueness = def.diagnoses(UNIQUENESS);
        checkNormalization = def.diagnoses(NORMALIZATION);
        checkOwners = def.diagnoses(OWNERS);
        checkFetch = def.diagnoses(EXISTENCE_ON_RESOURCE);
        checkExtraData = def.diagnoses(EXTRA_DATA);

        fixIntents = def.fixes(INTENTS);
        fixUniqueness = def.fixes(UNIQUENESS);
        fixNormalization = def.fixes(NORMALIZATION);
        fixExtraData = def.fixes(EXTRA_DATA);
        fixResourceRef = def.fixes(RESOURCE_REF);

        if (def.fixes(OWNERS)) {
            logger.warn("It is currently not possible to fix the owner issues; will diagnose them only");
        }

        if (def.fixes(EXISTENCE_ON_RESOURCE)) {
            logger.warn("It is currently not possible to fix the existence ('fetch') of shadows; will diagnose that only");
        }

        if (fixUniqueness) {
            String duplicateShadowsResolverClassName = def.getDuplicateShadowsResolver();
            try {
                this.duplicateShadowsResolver = (DuplicateShadowsResolver) Class.forName(duplicateShadowsResolverClassName)
                        .getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException
                    | NoSuchMethodException | InvocationTargetException e) {
                throw new SystemException("Couldn't instantiate duplicate shadows resolver " + duplicateShadowsResolverClassName);
            }
        }

        checkDuplicatesOnPrimaryIdentifiersOnly = def.isCheckDuplicatesOnPrimaryIdentifiersOnly();

        dryRun = executionMode != ExecutionModeType.FULL;
    }

    void log(String state) {
        logger.info("{}\n" +
                        "- normalization       diagnose={},\tfix={}\n" +
                        "- uniqueness          diagnose={},\tfix={} (primary identifiers only = {})\n" +
                        "- intents             diagnose={},\tfix={}\n" +
                        "- extraData           diagnose={},\tfix={}\n" +
                        "- owners              diagnose={}\n" +
                        "- existenceOnResource diagnose={}\n" +
                        "- resourceRef         fix={}\n\n" +
                        "dryRun = {}\n",
                state,
                checkNormalization, fixNormalization,
                checkUniqueness, fixUniqueness, checkDuplicatesOnPrimaryIdentifiersOnly,
                checkIntents, fixIntents,
                checkExtraData, fixExtraData,
                checkOwners,
                checkFetch,
                fixResourceRef,
                dryRun);
    }
}
