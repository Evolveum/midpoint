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
import com.evolveum.midpoint.schema.validator.processor.ProcessorMixin;

public interface UpgradeObjectProcessor<T extends Objectable> extends ProcessorMixin {

    /**
     * @return Unique identifier of the processor. By default it is class name without "Processor" suffix.
     */
    default String getIdentifier() {
        return getIdentifier(getClass());
    }

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    /**
     * Checks if the processor is applicable for the object and path.
     * Most often whether object is instance of proper ObjectType and item at the path exists.
     */
    boolean isApplicable(PrismObject<?> object, ItemPath path);

    /**
     * Executes upgrade of item defined by path argument by modifying the object to correct state.
     */
    boolean process(PrismObject<T> object, ItemPath path);

}
