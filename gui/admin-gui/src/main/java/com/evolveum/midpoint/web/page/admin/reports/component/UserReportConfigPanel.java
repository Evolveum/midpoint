/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.JasperExportType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.DateFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

/**
 *  TODO - add proper Date validator
 *
 *  @author shood
 * */
public class UserReportConfigPanel extends BasePanel<UserReportDto>{

    private static final String ID_NAME = "name";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    public UserReportConfigPanel(String componentId, IModel<UserReportDto> model){
        super(componentId, model);
        initLayout(this);
    }

    private void initLayout(final Component component){

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<>(getModel(), UserReportDto.F_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(name);

        TextFormGroup description = new TextFormGroup(ID_DESCRIPTION, new PropertyModel<>(getModel(), UserReportDto.F_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(description);

        IModel choices = WebComponentUtil.createReadonlyModelFromEnum(JasperExportType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup exportType = new DropDownFormGroup(ID_EXPORT_TYPE, new
                PropertyModel<ExportType>(getModel(), UserReportDto.F_EXPORT_TYPE), choices, renderer,
                createStringResource("UserReportConfigPanel.exportFileType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(exportType);

        DateFormGroup dateFrom = new DateFormGroup(ID_DATE_FROM, new PropertyModel<>(getModel(), UserReportDto.F_FROM_GREG),
                createStringResource("UserReportConfigPanel.dateFrom"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(dateFrom);

        DateFormGroup dateTo = new DateFormGroup(ID_DATE_TO, new PropertyModel<>(getModel(), UserReportDto.F_TO_GREG),
                createStringResource("UserReportConfigPanel.dateTo"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(dateTo);
    }
}
