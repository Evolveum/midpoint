/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferenceHeaderPanel;
import com.evolveum.midpoint.prism.Referencable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.io.Serial;

/**
 * @author lskublik
 *
 */
public class VerticalFormPrismReferenceHeaderPanel<R extends Referencable> extends PrismReferenceHeaderPanel<R> {

    @Serial private static final long serialVersionUID = 1L;

    private boolean isRequiredTagVisibleInHeaderPanel = false;

    public VerticalFormPrismReferenceHeaderPanel(String id, IModel<PrismReferenceWrapper<R>> model, ItemPanelSettings itemPanelSettings) {
        super(id, model, itemPanelSettings);
    }

    @Override
    protected void createRequired(String id) {
        WebMarkupContainer required = new WebMarkupContainer(id);
        required.add(new VisibleBehaviour(() -> isRequiredTagVisibleInHeaderPanel));
        add(required);
    }

    public void setRequiredTagVisibleInHeaderPanel(boolean requiredTagVisibleInHeaderPanel) {
        isRequiredTagVisibleInHeaderPanel = requiredTagVisibleInHeaderPanel;
    }
}
