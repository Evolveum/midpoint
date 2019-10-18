/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class BrowserPopupPanel<T extends Serializable> extends SearchPopupPanel<T> {

    private static final String ID_BROWSER_INPUT = "browserInput";
    private static final String ID_BROWSE = "browse";

    public BrowserPopupPanel(String id, IModel<DisplayableValue<T>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        IModel value = new PropertyModel(getModel(), SearchValue.F_LABEL);
        TextField input = new TextField(ID_BROWSER_INPUT, value);
        add(input);

        AjaxLink<Void> browse = new AjaxLink<Void>(ID_BROWSE) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                browsePerformed(target);
            }
        };
        add(browse);
    }

    protected void browsePerformed(AjaxRequestTarget target) {

    }
}
