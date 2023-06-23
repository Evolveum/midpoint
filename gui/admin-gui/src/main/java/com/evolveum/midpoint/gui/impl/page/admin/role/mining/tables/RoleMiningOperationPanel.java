/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.getRolesOid;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractIntersections.generateIntersectionsMap;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterUtils.generateFrequencyMap;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ProcessBusinessRolePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMiningOperationPanel extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";
    double minFrequency = 0.3;
    double maxFrequency = 1.0;
    Integer minIntersection = 10;
    List<IntersectionObject> mergedIntersection = new ArrayList<>();
    IntersectionObject selections;
    int fullRolesOccupation = 0;
    AjaxButton processButton;

    public RoleMiningOperationPanel(String id, List<ClusteringObjectMapped> users,
            List<String> occupiedRoles, boolean sortable) {
        super(id);

        HashMap<String, Double> frequencyMap = generateFrequencyMap(users, occupiedRoles);

        BoxedTablePanel<ClusteringObjectMapped> boxedTablePanel = generateRoleMiningTable(users,
                occupiedRoles, sortable, minFrequency, frequencyMap, null, maxFrequency);
        boxedTablePanel.setOutputMarkupId(true);
        boxedTablePanel.getDataTable().add(scaleModifier());
        add(boxedTablePanel);

        EmptyPanel tableIntersection = new EmptyPanel(ID_DATATABLE_INTERSECTIONS);
        tableIntersection.setOutputMarkupId(true);
        add(tableIntersection);

        add(frequencyForm(users, sortable, occupiedRoles, frequencyMap));
    }

    public Form<?> frequencyForm(List<ClusteringObjectMapped> usersMap, boolean sortable,
            List<String> occupiedRoles, HashMap<String, Double> frequencyMap) {

        Form<?> form = new Form<Void>("thresholds_form");

        TextFieldLabelPanel thresholdField = new TextFieldLabelPanel("threshold_frequency",
                Model.of(minFrequency), "Min frequency");
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        TextFieldLabelPanel thresholdFieldMax = new TextFieldLabelPanel("threshold_frequency_max",
                Model.of(maxFrequency), "Max frequency");
        thresholdFieldMax.setOutputMarkupId(true);
        thresholdFieldMax.setOutputMarkupPlaceholderTag(true);
        thresholdFieldMax.setVisible(true);
        form.add(thresholdFieldMax);

        TextFieldLabelPanel minIntersectionField = new TextFieldLabelPanel("threshold_intersection",
                Model.of(minIntersection), "Intersection");
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_link", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                minFrequency = (double) thresholdField.getBaseFormComponent().getModelObject();
                maxFrequency = (double) thresholdFieldMax.getBaseFormComponent().getModelObject();

                minIntersection = (Integer) minIntersectionField.getBaseFormComponent().getModelObject();

                long startTime = startTimer("prepare intersections");

                mergedIntersection = generateIntersectionsMap(usersMap, minIntersection, minFrequency, frequencyMap,
                        maxFrequency);

                endTimer(startTime, "prepare intersections");

                getBoxedTableExtra().replaceWith(generateRoleMiningTable(usersMap,
                        occupiedRoles, sortable, minFrequency, frequencyMap, null, maxFrequency));
                getBoxedTableExtra().setOutputMarkupId(true);
                getBoxedTableExtra().getDataTable().add(AttributeModifier.append("style", "transform: scale(0.3);"
                        + " transform-origin: 0 0;"));
                getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS, mergedIntersection,
                        sortable, occupiedRoles, usersMap, frequencyMap));

                target.appendJavaScript(getScaleScript());
                target.add(getBoxedTableExtra());
                target.add(getIntersectionTable().setOutputMarkupId(true));
                target.add(thresholdField);
                target.add(thresholdFieldMax);
                target.add(thresholdField);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);

        processButton = new AjaxButton("process_selections_id", Model.of("Process selections")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                ProcessBusinessRolePanel detailsPanel = new ProcessBusinessRolePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("TO DO: details"), selections) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);

            }

        };

        processButton.setOutputMarkupId(true);
        processButton.setOutputMarkupPlaceholderTag(true);
        processButton.setVisible(false);

        form.add(processButton);
        form.add(ajaxSubmitLink);

        return form;
    }

    public BoxedTablePanel<ClusteringObjectMapped> generateRoleMiningTable(List<ClusteringObjectMapped> usersMap,
            List<String> occupiedRoles, boolean sortable, double frequency,
            HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        RoleMiningProvider<ClusteringObjectMapped> provider = new RoleMiningProvider<>(
                this, new ListModel<>(usersMap) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<ClusteringObjectMapped> object) {
                super.setObject(object);
            }

        }, sortable);

        if (sortable) {
            provider.setSort(UserType.F_NAME.toString(), SortOrder.ASCENDING);
        }
        BoxedTablePanel<ClusteringObjectMapped> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRM(occupiedRoles, frequency, frequencyMap, intersection, maxFrequency),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<ClusteringObjectMapped, String>> initColumnsRM(List<String> occupiedRoles, double frequency, HashMap<String,
            Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        List<IColumn<ClusteringObjectMapped, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<ClusteringObjectMapped> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Group")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public IModel<?> getDataModel(IModel<ClusteringObjectMapped> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ClusteringObjectMapped>> item, String componentId,
                    IModel<ClusteringObjectMapped> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                List<String> usersGroup = rowModel.getObject().getMembers();
                String joinedGroup = String.join(", ", usersGroup);
                Label label = new Label(componentId, "count: " + usersGroup.size() + " | " + joinedGroup);
                item.add(label);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Group")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<ClusteringObjectMapped, String> column;
        for (String roleTypePrismObject : occupiedRoles) {
            String name = "" + roleTypePrismObject;
            String cellColor = "table-dark";
            Double fr = frequencyMap.get(roleTypePrismObject);
            if (frequency > fr) {
                cellColor = "bg-danger";
            } else if (maxFrequency < fr) {
                cellColor = "bg-info";
            }

            String finalCellColor = cellColor;
            column = new AbstractColumn<>(createStringResource(name)) {

                @Override
                public void populateItem(Item<ICellPopulator<ClusteringObjectMapped>> cellItem,
                        String componentId, IModel<ClusteringObjectMapped> model) {

                    tableStyle(cellItem);

                    List<String> rolesOid = model.getObject().getRoles();
                    if (intersection != null
                            && intersection.contains(roleTypePrismObject)
                            && new HashSet<>(rolesOid).containsAll(intersection)) {
                        filledCell(cellItem, componentId, "bg-success");
                    } else {
                        if (rolesOid.contains(roleTypePrismObject)) {
                            filledCell(cellItem, componentId, finalCellColor);
                        } else {
                            emptyCell(cellItem, componentId);
                        }
                    }
                }

                @Override
                public Component getHeader(String componentId) {

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(name), createStringResource(name), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, roleTypePrismObject);
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        return columns;
    }

    public BoxedTablePanel<IntersectionObject> generateTableIntersection(String id, List<IntersectionObject> miningSets,
            boolean sortable, List<String> occupiedRoles, List<ClusteringObjectMapped> usersMap,
            HashMap<String, Double> frequencyMap) {

        RoleMiningProvider<IntersectionObject> provider = new RoleMiningProvider<>(
                this, new ListModel<>(miningSets) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<IntersectionObject> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(IntersectionObject.F_METRIC, SortOrder.DESCENDING);

        BoxedTablePanel<IntersectionObject> table = new BoxedTablePanel<>(
                id, provider, initColumnsIntersection(usersMap, sortable, occupiedRoles, frequencyMap),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        return table;
    }

    public List<IColumn<IntersectionObject, String>> initColumnsIntersection(List<ClusteringObjectMapped> usersMap,
            boolean sortable, List<String> occupiedRoles, HashMap<String, Double> frequencyMap) {

        List<IColumn<IntersectionObject, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<IntersectionObject> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Metric")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getMetric()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Metric"));
            }

        });

        columns.add(new AbstractExportableColumn<>(Model.of("Type")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getType()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Type"));
            }

        });

        columns.add(new AbstractExportableColumn<>(Model.of("Current occupancy")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getMembers()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Current occupancy"));
            }

        });

        columns.add(new AbstractExportableColumn<>(Model.of("Total occupancy")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of("Compute")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        fullRolesOccupation = 0;

                        Set<String> rolesId = rowModel.getObject().getRolesId();
                        OperationResult result = new OperationResult("Generate miningType object");

                        ResultHandler<UserType> handler = (object, parentResult) -> {

                            List<String> rolesOid = getRolesOid(object.asObjectable());

                            if (new HashSet<>(rolesOid).containsAll(rolesId)) {
                                fullRolesOccupation++;
                            }
                            return true;
                        };

                        GetOperationOptionsBuilder optionsBuilder = ((PageBase) getPage()).getSchemaService()
                                .getOperationOptionsBuilder();
                        RepositoryService repositoryService = ((PageBase) getPage()).getRepositoryService();
                        try {
                            repositoryService.searchObjectsIterative(UserType.class, null, handler, optionsBuilder.build(),
                                    true, result);
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }

                        this.setDefaultModel(Model.of(String.valueOf(fullRolesOccupation)));
                        item.setOutputMarkupId(true);
                        ajaxRequestTarget.add(item);
                    }
                };
                ajaxButton.setOutputMarkupId(true);
                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                item.add(ajaxButton);

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Total occupancy"));
            }

        });
        columns.add(new AbstractExportableColumn<>(Model.of("Display")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of("Load")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        Set<String> intersection = rowModel.getObject().getRolesId();

                        selections = rowModel.getObject();
                        getBoxedTableExtra().replaceWith(generateRoleMiningTable(usersMap,
                                occupiedRoles, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                        getBoxedTableExtra().setOutputMarkupId(true);
                        getBoxedTableExtra().getDataTable().add(AttributeModifier.append("style",
                                "transform: scale(0.3);"
                                        + " transform-origin: 0 0;"));

                        ajaxRequestTarget.appendJavaScript(getScaleScript());
                        ajaxRequestTarget.add(getBoxedTableExtra());
                        processButton.setVisible(true);
                        ajaxRequestTarget.add(processButton);

                    }
                };

                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                ajaxButton.setOutputMarkupId(true);
                item.add(ajaxButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Display"));
            }

        });

        return columns;
    }

    protected Component getIntersectionTable() {
        return get(((PageBase) getPage()).createComponentPath(ID_DATATABLE_INTERSECTIONS));
    }

    protected BoxedTablePanel<?> getBoxedTableExtra() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

}
