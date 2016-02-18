/*
 * Copyright (c) 2010-2016 Evolveum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.ObjectPolicyConfigurationEditor;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AdminGuiConfigurationDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.admin.roles.component.MultiplicityPolicyDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * Created by Honchar.
 */
public class AdminGuiConfigPanel extends BasePanel<List<RichHyperlinkType>> {

    private static final String ID_DASHBOARD_LINK_EDITOR = "dashboardLinkEditor";
    private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-6";

    public AdminGuiConfigPanel(String id, IModel<List<RichHyperlinkType>> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        GenericMultiValueLabelEditPanel dashboardLinkEditor = new GenericMultiValueLabelEditPanel<RichHyperlinkType>(ID_DASHBOARD_LINK_EDITOR,
                getModel(), createStringResource("AdminGuiConfigPanel.dashboardLinksConfig"), LABEL_SIZE, INPUT_SIZE){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new DashboardLinkDialog(ID_MODAL_EDITOR, null){

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        closeModalWindow(target);
                        target.add(getDashboardLinkEditorContainer());
                    }
                };
                add(dialog);
            }

            @Override
            protected IModel<String> createTextModel(final IModel<RichHyperlinkType> model) {
                return new PropertyModel<String>(model, "label");
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<RichHyperlinkType> rowModel) {
                DashboardLinkDialog window = (DashboardLinkDialog) get(ID_MODAL_EDITOR);
                window.updateModel(target, rowModel.getObject());
                window.show(target);
            }

            @Override
            protected RichHyperlinkType createNewEmptyItem() {
                RichHyperlinkType link = new RichHyperlinkType();
                link.getAuthorization().add("");
                return link;
            }
        };
        dashboardLinkEditor.setOutputMarkupId(true);
        add(dashboardLinkEditor);

    }

    private WebMarkupContainer getDashboardLinkEditorContainer(){
        return (WebMarkupContainer) get(ID_DASHBOARD_LINK_EDITOR);
    }
}
