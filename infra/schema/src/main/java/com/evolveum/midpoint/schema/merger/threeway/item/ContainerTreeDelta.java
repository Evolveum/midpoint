/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;

public class ContainerTreeDelta<C extends Containerable>
        extends ItemTreeDelta<PrismContainerValue<C>, PrismContainerDefinition<C>, PrismContainer<C>, ContainerTreeDeltaValue<C>> {

    public ContainerTreeDelta( PrismContainerDefinition<C> definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "CTD";
    }

    public static <C extends Containerable> ContainerTreeDelta<C> from(@NotNull ContainerDelta<C> delta) {
        ContainerTreeDelta<C> result = new ContainerTreeDelta<>(delta.getDefinition());

        result.addDeltaValues(delta.getValuesToAdd(), ModificationType.ADD, ContainerTreeDeltaValue::from);
        result.addDeltaValues(delta.getValuesToReplace(), ModificationType.REPLACE, ContainerTreeDeltaValue::from);
        result.addDeltaValues(delta.getValuesToDelete(), ModificationType.DELETE, ContainerTreeDeltaValue::from);

        return result;
    }
}
