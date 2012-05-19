/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class SimpleSearchPanel extends Panel {

    private LoadableModel<SimpleSearchDto> model;

    public SimpleSearchPanel(String id) {
        super(id);

        model = new LoadableModel<SimpleSearchDto>(false) {

            @Override
            protected SimpleSearchDto load() {
                return new SimpleSearchDto();
            }
        };

        initLayout();
    }

    private void initLayout() {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("fullNameCheck", new PropertyModel<Boolean>(model, "fullName"));
        add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox("givenNameCheck", new PropertyModel<Boolean>(model, "givenName"));
        add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox("familyNameCheck", new PropertyModel<Boolean>(model, "familyName"));
        add(familyNameCheck);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                new StringResourceModel("simpleSearchPanel.button.clear", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clearPerformed(target);
            }
        };
        add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                new StringResourceModel("simpleSearchPanel.button.search", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        add(searchButton);
    }

    public LoadableModel<SimpleSearchDto> getModel() {
        return model;
    }

    public void clearPerformed(AjaxRequestTarget target) {

    }

    public void searchPerformed(AjaxRequestTarget target) {

    }
}
