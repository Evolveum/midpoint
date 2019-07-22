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
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportEngineSelectionType;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 *
 */
public class ReportConfigurationPanel extends BasePanel<ReportDto> {

    private static final String ID_BASIC_PANEL = "basicPanel";

    public ReportConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
    	ReportEngineSelectionType reportEngineType = getModel().getObject().getReportEngineType();
    	
    	if(ReportEngineSelectionType.DASHBOARD.equals(reportEngineType)) {
    		add(new DashboardReportBasicConfigurationPanel(ID_BASIC_PANEL, getModel()));
    	} else {
    		add(new JasperReportBasicConfigurationPanel(ID_BASIC_PANEL, getModel()));
    	}
    }
}
