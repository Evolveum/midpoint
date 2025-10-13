/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ProcessedObjectTypeSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.QNameWithoutNamespaceItemWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Search wrapper for search only in memory for multivalue containers.
 */
public class QNameWithoutNamespaceItemWrapperFactory extends AbstractSearchItemWrapperFactory<QName, QNameWithoutNamespaceItemWrapper> {

    @Override
    protected QNameWithoutNamespaceItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new QNameWithoutNamespaceItemWrapper();
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        if (ctx.getPath() == null) {
            return false;
        }
        if (!GuiObjectListViewType.class.equals(ctx.getContainerClassType())){
            return false;
        }

        return GuiObjectListViewType.F_TYPE.equivalent(ctx.getPath());
    }
}
