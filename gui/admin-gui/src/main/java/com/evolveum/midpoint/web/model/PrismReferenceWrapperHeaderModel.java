/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 *
 */
public class PrismReferenceWrapperHeaderModel<C extends Containerable, R extends Referencable> extends ItemWrapperModel<C, PrismReferenceWrapper<R>>{

    private static final long serialVersionUID = 1L;
    private PageBase pageBase;

    public PrismReferenceWrapperHeaderModel(IModel<?> parent, ItemPath path, PageBase pageBase) {
        super(parent, path, false);
        this.pageBase = pageBase;
    }

    @Override
    public PrismReferenceWrapper<R> getObject() {
        PrismReferenceWrapper<R> ret = (PrismReferenceWrapper<R>) getItemWrapperForHeader(PrismReferenceDefinition.class, pageBase);
        return ret;
    }

}
