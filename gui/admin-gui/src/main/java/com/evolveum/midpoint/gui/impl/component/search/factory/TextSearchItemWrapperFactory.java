/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.TextSearchItemWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;

public class TextSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<String, TextSearchItemWrapper> {

    @Override
    protected TextSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        ItemPath path = ctx.getPath();
        ItemDefinition<?> itemDef = ctx.getItemDef();
        PrismReferenceValue valueEnumerationRef = ctx.getValueEnumerationRef();
        if (valueEnumerationRef != null) {
            return new TextSearchItemWrapper(
                    path,
                    itemDef,
                    valueEnumerationRef.getOid(),
                    valueEnumerationRef.getTargetType());
        }

        if (path != null) {
            return new TextSearchItemWrapper(path, itemDef);
        }
        return new TextSearchItemWrapper();

    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return true;
    }
}
