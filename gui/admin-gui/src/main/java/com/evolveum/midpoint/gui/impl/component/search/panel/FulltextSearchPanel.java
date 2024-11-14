/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FulltextQueryWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.OidSearchItemWrapper;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class FulltextSearchPanel extends BasePanel<FulltextQueryWrapper> {

    private static final String ID_FULL_TEXT_CONTAINER = "fullTextContainer";
    private static final String ID_FULL_TEXT_FIELD = "fullTextField";
    public FulltextSearchPanel(String id, IModel<FulltextQueryWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer fullTextContainer = new WebMarkupContainer(ID_FULL_TEXT_CONTAINER);
        fullTextContainer.setOutputMarkupId(true);
        add(fullTextContainer);

        TextField<String> fullTextInput = new TextField<>(ID_FULL_TEXT_FIELD,
                new PropertyModel<>(getModel(), Search.F_FULL_TEXT));

        fullTextInput.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        fullTextInput.setOutputMarkupId(true);
        fullTextInput.add(new AttributeAppender("placeholder",
                createStringResource("SearchPanel.fullTextSearch")));
        fullTextInput.add(new AttributeAppender("title",
                createStringResource("SearchPanel.fullTextSearch")));
        fullTextContainer.add(fullTextInput);

    }

}
