/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

public record Conflict<
        PV extends PrismValue,
        ID extends ItemDefinition<I>,
        I extends Item<PV, ID>,
        V extends ItemTreeDeltaValue<PV, ITD>,
        ITD extends ItemTreeDelta<PV, ID, I, V>,
        ITDV extends ItemTreeDeltaValue<PV, ITD>>
        (ITDV first, ITDV second) {
}
