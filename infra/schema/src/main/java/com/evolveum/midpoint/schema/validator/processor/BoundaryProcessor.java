/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BoundarySpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@SuppressWarnings("unused")
public class BoundaryProcessor implements UpgradeObjectProcessor<TaskType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(
                object, path, StringWorkSegmentationType.class, StringWorkSegmentationType.F_BOUNDARY_CHARACTERS);
    }

    @Override
    public boolean process(PrismObject<TaskType> object, ItemPath path) throws Exception {
        StringWorkSegmentationType segmentation = getItemParent(object, path);
        if (segmentation == null) {
            return false;
        }

        List<BoundarySpecificationType> boundaries = segmentation.getBoundary();
        List<String> chars = segmentation.getBoundaryCharacters();
        int i = 1;
        for (String c : chars) {
            BoundarySpecificationType boundary = new BoundarySpecificationType()
                    .characters(c)
                    .position(i++);
            boundaries.add(boundary);
        }

        chars.clear();

        return true;
    }
}
