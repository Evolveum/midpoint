/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
