/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class PageXmlDataReview extends PageAdmin {

    private static final String ID_ACE_EDITOR_CONTAINER = "aceEditorContainer";
    private static final String ID_ACE_EDITOR_PANEL = "aceEditorPanel";
    private static final String ID_BUTTON_BACK = "back";

    public PageXmlDataReview(IModel<String> title, IModel<String> data){
        initLayout(title, data);
    }

    private void initLayout(IModel<String> title, IModel<String> data){
        WebMarkupContainer container = new WebMarkupContainer(ID_ACE_EDITOR_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        AceEditorPanel aceEditorPanel = new AceEditorPanel(ID_ACE_EDITOR_PANEL, title, data);
        aceEditorPanel.getEditor().setReadonly(true);
        aceEditorPanel.setOutputMarkupId(true);
        container.add(aceEditorPanel);

        AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        add(back);
    }

}
