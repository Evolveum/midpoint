/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * @author skublik
 */

public class EngineReportTabPanel extends ObjectBasicPanel<ReportType> {

    private static final String ID_PANEL = "panel";

    public EngineReportTabPanel(String id, IModel<PrismObjectWrapper<ReportType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        if (WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value())) {
            add(new SingleContainerPanel(ID_PANEL, PrismContainerWrapperModel.fromContainerWrapper(
                    getModel(), ItemPath.create(ReportType.F_DASHBOARD)), DashboardReportEngineConfigurationType.COMPLEX_TYPE) {
                @Override
                protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                    if (!ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_DASHBOARD_REF).equivalent(itemWrapper.getPath())
                            && !ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_SHOW_ONLY_WIDGETS_TABLE).equivalent(itemWrapper.getPath())) {
                        return ItemVisibility.HIDDEN;
                    }
                    return super.getVisibility(itemWrapper);
                }
            });
        } else if(WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value())) {
            add(new SingleContainerPanel(ID_PANEL, PrismContainerWrapperModel.fromContainerWrapper(
                    getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION)), ObjectCollectionReportEngineConfigurationType.COMPLEX_TYPE) {
                @Override
                protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                    if (!ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_CONDITION).equivalent(itemWrapper.getPath())
                            && !ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_USE_ONLY_REPORT_VIEW).equivalent(itemWrapper.getPath())) {
                        return ItemVisibility.HIDDEN;
                    }
                    return super.getVisibility(itemWrapper);
                }
            });
        } else {
            warn(getPageBase().createStringResource("PageReport.message.selectTypeOfReport").getString());
            add(new WebMarkupContainer(ID_PANEL));
        }
    }

    private ReportType getReport() {
        return getModelObject().getObject().asObjectable();
    }
}
