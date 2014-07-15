/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.configuration.dto.BulkActionDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/bulk", action = {
//        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
//                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
//        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#bulkAction",
//                label = "PageBulkAction.auth.bulkAction.label", description = "PageBulkAction.auth.bulkAction.description")
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_DEVEL_URL)
})
public class PageBulkAction extends PageAdminConfiguration {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_START = "start";
    private static final String ID_EDITOR = "editor";
    private static final String ID_ASYNC = "async";

    private IModel<BulkActionDto> model = new Model<>(new BulkActionDto());

    public PageBulkAction() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        CheckBox async = new CheckBox(ID_ASYNC, new PropertyModel<Boolean>(model, BulkActionDto.F_ASYNC));
        mainForm.add(async);

        AceEditor editor = new AceEditor(ID_EDITOR, new PropertyModel<String>(model, BulkActionDto.F_SCRIPT));
        mainForm.add(editor);

        AjaxSubmitButton start = new AjaxSubmitButton(ID_START, createStringResource("PageBulkUsers.button.start")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                startPerformed(target);
            }
        };
        mainForm.add(start);
    }

    private void startPerformed(AjaxRequestTarget target) {
        model.getObject();

        //todo implement
    }
}
