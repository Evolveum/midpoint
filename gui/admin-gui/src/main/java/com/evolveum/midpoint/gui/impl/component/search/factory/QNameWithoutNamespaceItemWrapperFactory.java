/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
