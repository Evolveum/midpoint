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

public class TreeContainerDelta<C extends Containerable>
        extends TreeItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>, PrismContainer<C>, TreeContainerDeltaValue<C>> {

    public TreeContainerDelta( PrismContainerDefinition<C> definition) {
        super(definition);
    }

    public static <C extends Containerable> TreeContainerDelta<C> from(@NotNull ContainerDelta<C> delta) {
        TreeContainerDelta<C> result = new TreeContainerDelta<>(delta.getDefinition());

        result.addDeltaValues(delta.getValuesToAdd(), ModificationType.ADD, TreeContainerDeltaValue::from);
        result.addDeltaValues(delta.getValuesToReplace(), ModificationType.REPLACE, TreeContainerDeltaValue::from);
        result.addDeltaValues(delta.getValuesToDelete(), ModificationType.DELETE, TreeContainerDeltaValue::from);

        return result;
    }
}
