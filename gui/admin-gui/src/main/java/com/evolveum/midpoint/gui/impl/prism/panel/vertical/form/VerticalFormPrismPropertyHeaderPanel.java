/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 * @author lskublik
 *
 */
public class VerticalFormPrismPropertyHeaderPanel<T> extends PrismPropertyHeaderPanel<T> {

    private static final long serialVersionUID = 1L;


    /**
     * @param id
     * @param model
     */
    public VerticalFormPrismPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<T>> model) {
        super(id, model);
    }

    @Override
    protected void createRequired(String id) {
        WebMarkupContainer required = new WebMarkupContainer(id);
        required.add(new VisibleBehaviour(() -> false));
        add(required);
    }
}
