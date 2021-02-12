/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavol Mederly
 */
public class DefaultDuplicateShadowsResolver implements DuplicateShadowsResolver {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultDuplicateShadowsResolver.class);

    @Override
    public DuplicateShadowsTreatmentInstruction determineDuplicateShadowsTreatment(Collection<PrismObject<ShadowType>> shadows) {
        if (shadows.size() <= 1) {
            return null;
        }
        LOGGER.trace("Determining duplicate shadows treatment: conflicting set with {} members", shadows.size());
        // prefer shadows with intent and linked ones
        int bestScore = 0;
        PrismObject<ShadowType> bestShadow = null;
        for (PrismObject<ShadowType> shadow : shadows) {
            ShadowType shadowType = shadow.asObjectable();
            int score = 0;
            if (shadowType.getSynchronizationSituation() == SynchronizationSituationType.LINKED) {
                score += 10;
            }
            List owners = (List) shadow.getUserData(ShadowIntegrityCheckItemProcessor.KEY_OWNERS);
            if (owners != null && !owners.isEmpty()) {
                score += 10;
            }
            if (shadowType.getIntent() != null && !shadowType.getIntent().isEmpty()) {
                score += 8;
            }
            if (shadow.getUserData(ShadowIntegrityCheckItemProcessor.KEY_EXISTS_ON_RESOURCE) != null) { // filled in only if checkFetch is true
                score += 50;
            }
            LOGGER.trace("Shadow {} has score of {}; best is {}", ObjectTypeUtil.toShortString(shadow), score, bestScore);
            if (bestShadow == null || score > bestScore) {
                bestScore = score;
                bestShadow = shadow;
            }
        }
        DuplicateShadowsTreatmentInstruction instruction = new DuplicateShadowsTreatmentInstruction();
        instruction.setShadowOidToReplaceDeletedOnes(bestShadow.getOid());
        instruction.setShadowsToDelete(new ArrayList<>());
        for (PrismObject<ShadowType> shadow : shadows) {
            if (!shadow.getOid().equals(bestShadow.getOid())) {
                instruction.getShadowsToDelete().add(shadow);
            }
        }
        return instruction;
    }
}
