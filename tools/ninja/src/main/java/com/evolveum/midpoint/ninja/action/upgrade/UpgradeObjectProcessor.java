/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface UpgradeObjectProcessor<T extends ObjectType> {

    String getIdentifier();

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    boolean isApplicable(PrismObject<?> object, ItemPath path);

    boolean process(PrismObject<T> object);
}
