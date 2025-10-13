/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 *
 */
public class PrismContainerWrapperHeaderModel<C extends Containerable, T extends Containerable> extends ItemWrapperModel<C, PrismContainerWrapper<T>>{

    private static final long serialVersionUID = 1L;
    private PageBase pageBase;

    public PrismContainerWrapperHeaderModel(IModel<?> parent, ItemPath path, PageBase pageBase) {
        super(parent, path, false);
        this.pageBase = pageBase;
    }

    @Override
    public PrismContainerWrapper<T> getObject() {
        PrismContainerWrapper<T> ret = (PrismContainerWrapper<T>) getItemWrapperForHeader(PrismContainerDefinition.class, pageBase);
        return ret;
    }

}
