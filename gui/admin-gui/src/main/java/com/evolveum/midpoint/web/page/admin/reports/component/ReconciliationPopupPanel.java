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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ResourceItemDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ReconciliationPopupPanel extends BasePanel<ReconciliationReportDto> {

    private static final String ID_NAME = "name";
    private static final String ID_RESOURCE = "resource";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    public ReconciliationPopupPanel(String id, IModel<ReconciliationReportDto> model,
                                    IModel<List<ResourceItemDto>> resources) {
        super(id, model);
        initLayout(resources, this);
    }

    private void initLayout(IModel<List<ResourceItemDto>> resources,
                            final Component component) {

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<>(getModel(), ReconciliationReportDto.F_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(name);

        TextFormGroup description = new TextFormGroup(ID_DESCRIPTION, new PropertyModel<>(getModel(), ReconciliationReportDto.F_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(description);

        IModel choices = WebComponentUtil.createReadonlyModelFromEnum(ExportType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup exportType = new DropDownFormGroup(ID_EXPORT_TYPE, new
                PropertyModel<ExportType>(getModel(), ReconciliationReportDto.F_EXPORT_TYPE), choices, renderer,
                createStringResource("ReconciliationPopupPanel.exportFileType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(exportType);

        DropDownFormGroup resourceDropDown = new DropDownFormGroup(ID_RESOURCE, createModel(resources.getObject()),
                resources, renderer, createStringResource("ReconciliationPopupPanel.resource"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(resourceDropDown);
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
}
