/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;

/**
 * @author katka
 */
public class PrismReferencePanelContext<R extends Referencable> extends ItemPanelContext<R, PrismReferenceWrapper<R>> {

    public PrismReferencePanelContext(IModel<PrismReferenceWrapper<R>> itemWrapper) {
        super(itemWrapper);
    }

    public ObjectFilter getFilter() {
        return unwrapWrapperModel().getFilter(getPageBase());
    }


}
