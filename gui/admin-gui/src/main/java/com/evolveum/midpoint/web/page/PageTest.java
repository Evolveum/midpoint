/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.home.PageAdminHome;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.lang.Bytes;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/test", action = {@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_DEVEL_URL)})
public class PageTest extends PageBase {

    public PageTest() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form("form");
        form.setMaxSize(Bytes.kilobytes(192));
//        form.setMultiPart(true);
        add(form);

        FileUploadField fileUpload = new FileUploadField("fileInput");
        form.add(fileUpload);

        AjaxSubmitButton save = new AjaxSubmitButton("save",
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                setResponsePage(PageUsers.class);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(save);
    }
}
