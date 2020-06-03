/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeDetailsTabPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author skublik
 *
 */
public class DashboardReportBasicConfigurationPanel extends BasePanel<ReportDto> {

    private static final Trace LOGGER = TraceManager.getTrace(DashboardReportBasicConfigurationPanel.class);

    private static final String ID_PANEL = "panel";
    private static final String ID_EXPORT = "export";
    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_COLLECTION = "collection";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
//    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_LABEL_SIZE = "col-md-2";
    private static final String ID_INPUT_SIZE = "col-md-10";

    public DashboardReportBasicConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Task task = getPageBase().createSimpleTask("Create report wrapper");
        PrismObjectWrapperFactory<ReportType> factory = getPageBase().findObjectWrapperFactory(getModel().getObject().getObject().getDefinition());

        WrapperContext context = new WrapperContext(task, task.getResult());
        context.setCreateIfEmpty(true);
        PrismObjectWrapper<ReportType> objectWrapper = null;
        try {
            objectWrapper = factory.createObjectWrapper(getModel().getObject().getObject(), ItemStatus.NOT_CHANGED, context);

            IModel<PrismObjectWrapper<ReportType>> objectWrapperModel = Model.of(objectWrapper);
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(this::getBasicTabVisibity);
            add (getPageBase().initItemPanel(ID_PANEL, ExportConfigurationType.COMPLEX_TYPE,
                    objectWrapperModel, builder.build()));
            getModel().getObject().setNewReportModel(objectWrapperModel);
            Panel export = getPageBase().initItemPanel(ID_EXPORT, ExportConfigurationType.COMPLEX_TYPE,
                PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel, ReportType.F_EXPORT), new ItemPanelSettingsBuilder().build());
            add(export);
            Panel dashboard = getPageBase().initItemPanel(ID_DASHBOARD, DashboardReportEngineConfigurationType.COMPLEX_TYPE,
                PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel, ItemPath.create(ReportType.F_DASHBOARD)), new ItemPanelSettingsBuilder().build());
            add(dashboard);
            Panel collection = getPageBase().initItemPanel(ID_COLLECTION, ObjectCollectionReportEngineConfigurationType.COMPLEX_TYPE,
                PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel, ItemPath.create(ReportType.F_OBJECT_COLLECTION)), new ItemPanelSettingsBuilder().build());
            add(collection);
        } catch (SchemaException e) {
            LOGGER.error("Could not create report details panel. Reason: {}", e.getMessage(), e);
        }
    }

//        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<>(getModel(), ID_NAME),
//                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
//        add(name);
//
//        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
//            new PropertyModel<>(getModel(), ID_DESCRIPTION),
//                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
//        add(description);
//
//        ValueChoosePanel<ObjectReferenceType> panel =
//                new ValueChoosePanel<ObjectReferenceType>(ID_DASHBOARD,
//                        new PropertyModel<ObjectReferenceType>(getModel(), ReportDto.F_DASHBOARD_REF)) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public List<QName> getSupportedTypes() {
//                return Arrays.asList(DashboardType.COMPLEX_TYPE);
//            }
//
//
//            @Override
//            protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
//                return (Class<O>) DashboardType.class;
//            }
//
//        };
//        add(panel);
//
//    }

    private ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper) {
        if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DESCRIPTION))
                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_NAME))
                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DOCUMENTATION))
                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_REPORT_ENGINE))) {

            return ItemVisibility.AUTO;
        }

        return ItemVisibility.HIDDEN;
    }
}
