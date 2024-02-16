/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisOutlierPropertyTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    public RoleAnalysisOutlierPropertyTable(String id,
            LoadableDetachableModel<List<RoleAnalysisOutlierDescriptionType>> outlierResults) {
        super(id);

        RoleMiningProvider<RoleAnalysisOutlierDescriptionType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(outlierResults.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleAnalysisOutlierDescriptionType> object) {
                super.setObject(object);
            }

        }, false);

        BoxedTablePanel<RoleAnalysisOutlierDescriptionType> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns()) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<RoleAnalysisOutlierDescriptionType, String>> initColumns() {

        List<IColumn<RoleAnalysisOutlierDescriptionType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "black", "");
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("Name")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType ref = result.getObject();
                PolyStringType targetName = ref.getTargetName();
                String oid = ref.getOid();
                QName type = ref.getType();
                Task task = getPageBase().createSimpleTask("Load object");
                String objectName = "";
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                if (targetName == null) {
                    if (type.equals(UserType.COMPLEX_TYPE)) {
                        PrismObject<UserType> object = roleAnalysisService.getObject(UserType.class, oid, task, task.getResult());
                        if (object != null) {
                            objectName = object.getName().getOrig();
                        }

                    } else if (type.equals(RoleType.COMPLEX_TYPE)) {
                        PrismObject<RoleType> object = roleAnalysisService.getObject(RoleType.class, oid, task, task.getResult());
                        if (object != null) {
                            objectName = object.getName().getOrig();
                        }
                    }
                } else {
                    objectName = targetName.getOrig();
                }

                item.add(new AjaxLinkPanel(componentId, Model.of(objectName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                        if (type.equals(UserType.COMPLEX_TYPE)) {
                            getPageBase().navigateToNext(PageUser.class, parameters);
                        } else if (type.equals(RoleType.COMPLEX_TYPE)) {
                            getPageBase().navigateToNext(PageRole.class, parameters);
                        }

                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.name.header"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Session")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType ref = result.getSession();

                Task task = getPageBase().createSimpleTask("Load object");
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                String objectName = "unknown";
                PrismObject<RoleAnalysisSessionType> object = roleAnalysisService
                        .getObject(RoleAnalysisSessionType.class, ref.getOid(), task, task.getResult());
                if (object != null) {
                    objectName = object.getName().getOrig();
                }

                item.add(new AjaxLinkPanel(componentId, Model.of(objectName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, ref.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class, parameters);
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.session.header"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Cluster")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType ref = result.getCluster();

                Task task = getPageBase().createSimpleTask("Load object");
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                String objectName = "unknown";
                PrismObject<RoleAnalysisClusterType> object = roleAnalysisService
                        .getObject(RoleAnalysisClusterType.class, ref.getOid(), task, task.getResult());
                if (object != null) {
                    objectName = object.getName().getOrig();
                }

                item.add(new AjaxLinkPanel(componentId, Model.of(objectName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, ref.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.cluster.header"));
            }

        });


        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public String getSortProperty() {
                return RoleAnalysisOutlierDescriptionType.F_CONFIDENCE.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {
                if (rowModel.getObject() != null) {
                    Double confidence = rowModel.getObject().getConfidence();
                    if (confidence != null) {
                        double confidencePercentage = confidence * 100.0;
                        confidencePercentage = Math.round(confidencePercentage * 100.0) / 100.0;
                        String formattedConfidence = String.format("%.2f", confidencePercentage);
                        item.add(new Label(componentId, formattedConfidence + " (%)"));
                    } else {
                        item.add(new Label(componentId, "N/A"));
                    }
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.confidence.header"));

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                OutlierCategory category = rowModel.getObject().getCategory();
                item.add(new Label(componentId, createStringResource(category != null ? category.value() : "")));

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.category.header"));
            }

        });

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }
}
