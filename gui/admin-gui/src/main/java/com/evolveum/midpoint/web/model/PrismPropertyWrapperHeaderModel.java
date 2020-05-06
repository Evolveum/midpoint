/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 *
 */
public class PrismPropertyWrapperHeaderModel<C extends Containerable, T> extends ItemWrapperModel<C, PrismPropertyWrapper<T>>{

    private static final long serialVersionUID = 1L;
    private PageBase pageBase;

    public PrismPropertyWrapperHeaderModel(IModel<?> parent, ItemPath path, PageBase pageBase) {
        super(parent, path, false);
        this.pageBase = pageBase;
    }

    @Override
    public PrismPropertyWrapper<T> getObject() {
        PrismPropertyWrapper<T> ret = (PrismPropertyWrapper<T>) getItemWrapperForHeader(PrismPropertyDefinition.class, pageBase);
        return ret;
    }

}
