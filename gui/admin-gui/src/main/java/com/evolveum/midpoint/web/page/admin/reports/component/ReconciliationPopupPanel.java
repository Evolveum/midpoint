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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ResourceItemDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ReconciliationPopupPanel extends SimplePanel<ReconciliationReportDto> {

    private static final String ID_FORM = "form";
    private static final String ID_RESOURCE = "resource";
    private static final String ID_RUN = "run";

    public ReconciliationPopupPanel(String id, IModel<ReconciliationReportDto> model,
                                    IModel<List<ResourceItemDto>> resources) {
        super(id, model);

        initLayout(resources);
    }

    private void initLayout(IModel<List<ResourceItemDto>> resources) {
        Form form = new Form(ID_FORM);
        add(form);

        IChoiceRenderer renderer = new IChoiceRenderer<ResourceItemDto>() {

            @Override
            public Object getDisplayValue(ResourceItemDto object) {
                return object.getName();
            }

            @Override
            public String getIdValue(ResourceItemDto object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice dropDown = new DropDownChoice(ID_RESOURCE, createModel(resources.getObject()),
                resources, renderer);
        form.add(dropDown);

        AjaxSubmitLinkButton run = new AjaxSubmitLinkButton(ID_RUN, ButtonType.POSITIVE,
                createStringResource("PageBase.button.run")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onRunPerformed(target);
            }
        };
        form.add(run);
    }

    private IModel<ResourceItemDto> createModel(final List<ResourceItemDto> resources) {
        return new IModel<ResourceItemDto>() {

            @Override
            public ResourceItemDto getObject() {
                ReconciliationReportDto dto = getModel().getObject();
                if (dto.getResourceOid() == null) {
                    return null;
                }

                for (ResourceItemDto item : resources) {
                    if (StringUtils.equals(item.getOid(), dto.getResourceOid())) {
                        return item;
                    }
                }

                return null;
            }

            @Override
            public void setObject(ResourceItemDto object) {
                ReconciliationReportDto dto = getModel().getObject();

                dto.setResourceOid(object != null ? object.getOid() : null);
                dto.setResourceName(object != null ? object.getName() : null);
            }

            @Override
            public void detach() {
            }
        };
    }

    protected void onRunPerformed(AjaxRequestTarget target) {

    }
}
