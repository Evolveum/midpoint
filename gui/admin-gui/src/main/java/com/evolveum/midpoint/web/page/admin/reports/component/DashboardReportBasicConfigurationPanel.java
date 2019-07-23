/*
 * Copyright (c) 2010-2019 Evolveum
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
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismReferencePanel;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author skublik
 *
 */
public class DashboardReportBasicConfigurationPanel extends BasePanel<ReportDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_LABEL_SIZE = "col-md-2";
    private static final String ID_INPUT_SIZE = "col-md-10";

    public DashboardReportBasicConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<>(getModel(), ID_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
            new PropertyModel<>(getModel(), ID_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(description);

        ValueChoosePanel<ObjectReferenceType> panel =
        		new ValueChoosePanel<ObjectReferenceType>(ID_DASHBOARD,
        				new PropertyModel<ObjectReferenceType>(getModel(), ReportDto.F_DASHBOARD_REF)) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public List<QName> getSupportedTypes() {
				return Arrays.asList(DashboardType.COMPLEX_TYPE);
			}
			

			@Override
			protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
				return (Class<O>) DashboardType.class;
			}

		};
		add(panel);
        
    }
}
