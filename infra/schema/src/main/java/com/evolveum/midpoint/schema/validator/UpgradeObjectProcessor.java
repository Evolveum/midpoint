/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

public interface UpgradeObjectProcessor<T extends Objectable> {

    String getIdentifier();

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    boolean isApplicable(PrismObject<?> object, ItemPath path);

    boolean process(PrismObject<T> object, ItemPath path);
}
