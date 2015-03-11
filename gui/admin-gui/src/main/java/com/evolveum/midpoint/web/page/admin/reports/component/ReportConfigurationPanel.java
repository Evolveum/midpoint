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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author shood
 * @author lazyman
 *
 */
public class ReportConfigurationPanel extends SimplePanel<ReportDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_USE_HIBERNATE_SESSION = "useHibernateSession";
    private static final String ID_ORIENTATION = "orientation";

    private static final String ID_SEARCH_ON_RESOURCE = "searchOnResource";
    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    public ReportConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(getModel(), ID_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), ID_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(description);

        CheckBox searchOnResourceCheckbox = new CheckBox(ID_SEARCH_ON_RESOURCE, new PropertyModel<Boolean>(getModel(), ID_SEARCH_ON_RESOURCE));
        add(searchOnResourceCheckbox);
        
        IModel choices = WebMiscUtil.createReadonlyModelFromEnum(ExportType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
//        ReportDto dto = new ReportDto();
//        dto.setExportType(getModel().getObject().asObjectable().getExport());
        DropDownFormGroup exportType = new DropDownFormGroup(ID_EXPORT_TYPE, new
                PropertyModel<ExportType>(getModel(), ReportDto.F_EXPORT_TYPE), choices, renderer,
                createStringResource("ReportType.export"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(exportType);
//
//        //todo useHibernateSession and orientation
//
//        IModel<ObjectWrapper> wrapper = new LoadableModel<ObjectWrapper>(false) {
//
//            @Override
//            protected ObjectWrapper load() {
//                PrismObject<ReportType> report = getModel().getObject().getObject();
//
//                return new ObjectWrapper(null, null, report, null, ContainerStatus.MODIFYING);
//            }
//        };
//        PrismObjectPanel properties = new PrismObjectPanel(ID_PROPERTIES, wrapper, null, null);
//        add(properties);
    }
}
