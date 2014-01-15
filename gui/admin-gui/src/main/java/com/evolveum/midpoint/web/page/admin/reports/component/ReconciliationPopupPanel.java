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

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ResourceItemDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserReportDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ReconciliationPopupPanel extends SimplePanel<ReconciliationReportDto> {

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_FORM = "form";
    private static final String ID_RESOURCE = "resource";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";
    private static final String ID_BUTTON_RUN = "runSave";
    private static final String ID_BUTTON_SAVE = "save";
    private static final String ID_BUTTON_CANCEL = "cancel";

    public ReconciliationPopupPanel(String id, IModel<ReconciliationReportDto> model,
                                    IModel<List<ResourceItemDto>> resources) {
        super(id, model);

        initLayout(resources, this);
    }

    private void initLayout(IModel<List<ResourceItemDto>> resources,
                            final Component component) {
        Form form = new Form(ID_FORM);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        TextField<String> description = new TextField<String>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), UserReportDto.F_DESCRIPTION));
        description.setRequired(true);
        form.add(description);

        IChoiceRenderer<ExportType> exportTypeRenderer = new IChoiceRenderer<ExportType>() {

            @Override
            public Object getDisplayValue(ExportType object) {
                return WebMiscUtil.createLocalizedModelForEnum(object, component).getObject();
            }

            @Override
            public String getIdValue(ExportType object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice exportDropDown = new DropDownChoice(ID_EXPORT_TYPE, new PropertyModel<ExportType>(getModel(),
                UserReportDto.F_EXPORT_TYPE), WebMiscUtil.createReadonlyModelFromEnum(ExportType.class), exportTypeRenderer){

            @Override
            protected CharSequence getDefaultChoice(final String selectedValue){
                return ExportType.PDF.toString();
            }
        };
        form.add(exportDropDown);

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

        AjaxSubmitButton saveAndRun = new AjaxSubmitButton(ID_BUTTON_RUN, createStringResource("PageBase.button.saveAndRun")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSaveAndRunPerformed(target);
            }
        };
        form.add(saveAndRun);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSavePerformed(target);
            }
        };
        form.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL, createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onCancelPerformed(target);
            }
        };
        form.add(cancel);
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

    protected void onSaveAndRunPerformed(AjaxRequestTarget target) {}
    protected void onSavePerformed(AjaxRequestTarget target) {}
    protected void onCancelPerformed(AjaxRequestTarget target) {}
}
