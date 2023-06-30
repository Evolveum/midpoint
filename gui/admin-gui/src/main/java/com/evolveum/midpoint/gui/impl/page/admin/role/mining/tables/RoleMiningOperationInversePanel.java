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
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterUtils.generateFrequencyMap;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ProcessBusinessRolePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMiningOperationInversePanel extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";
    private static final String ID_FREQUENCY_FORM = "thresholds_form";

    private static final String ID_FREQUENCY_THRESHOLD_MIN_FRQ = "threshold_frequency";
    private static final String ID_FREQUENCY_THRESHOLD_MAX_FRQ = "threshold_frequency_max";

    private static final String ID_FREQUENCY_SUBMIT = "ajax_submit_link";
    private static final String ID_FREQUENCY_THRESHOLD_ITR = "threshold_intersection";

    private static final String ID_PROCESS_BUTTON = "process_selections_id";

    double minFrequency = 0.3;
    double maxFrequency = 1.0;
    Integer minIntersection = 10;
    List<IntersectionObject> mergedIntersection = new ArrayList<>();
    IntersectionObject selections;
    int fullRolesOccupation = 0;
    AjaxButton processButton;

    public RoleMiningOperationInversePanel(String id, List<ClusteringObjectMapped> users,
            List<String> occupiedRoles, boolean sortable) {
        super(id);

        HashMap<String, Double> frequencyMap = generateFrequencyMap(users, occupiedRoles);

        BoxedTablePanel<String> boxedTablePanel = generateRoleMiningTable(users,
                occupiedRoles, sortable, minFrequency, frequencyMap, null, maxFrequency);
        boxedTablePanel.setOutputMarkupId(true);
        boxedTablePanel.getDataTable().add(scaleModifier());
        add(boxedTablePanel);

        EmptyPanel tableIntersection = new EmptyPanel(ID_DATATABLE_INTERSECTIONS);
        tableIntersection.setOutputMarkupId(true);
        add(tableIntersection);

        add(frequencyForm(users, sortable, occupiedRoles, frequencyMap));
    }

    private TextFieldLabelPanel generateFieldPanel(String id, IModel<?> model, String stringResource) {
        TextFieldLabelPanel components = new TextFieldLabelPanel(id,
                model, stringResource);
        components.setOutputMarkupId(true);
        components.setOutputMarkupPlaceholderTag(true);
        components.setVisible(true);
        return components;
    }

    public Form<?> frequencyForm(List<ClusteringObjectMapped> usersMap, boolean sortable,
            List<String> occupiedRoles, HashMap<String, Double> frequencyMap) {
        Form<?> form = new Form<Void>(ID_FREQUENCY_FORM);

        TextFieldLabelPanel minFreqField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_MIN_FRQ,
                Model.of(minFrequency), getString("RoleMining.frequency.min.title"));
        form.add(minFreqField);

        TextFieldLabelPanel maxFreqField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_MAX_FRQ,
                Model.of(maxFrequency), getString("RoleMining.frequency.max.title"));
        form.add(maxFreqField);

        TextFieldLabelPanel minIntersectionField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_ITR,
                Model.of(minIntersection), getString("RoleMining.frequency.intersections.title"));
        form.add(minIntersectionField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_FREQUENCY_SUBMIT, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                minFrequency = (double) minFreqField.getBaseFormComponent().getModelObject();
                maxFrequency = (double) maxFreqField.getBaseFormComponent().getModelObject();
                minIntersection = (Integer) minIntersectionField.getBaseFormComponent().getModelObject();

                long startTime = startTimer("prepare intersections");

                mergedIntersection = generateIntersectionsMap(usersMap, minIntersection, minFrequency, frequencyMap,
                        maxFrequency);

                endTimer(startTime, "prepare intersections");

                getBoxedTableExtra().replaceWith(generateRoleMiningTable(usersMap,
                        occupiedRoles, sortable, minFrequency, frequencyMap, null, maxFrequency));
                getBoxedTableExtra().setOutputMarkupId(true);
                getBoxedTableExtra().getDataTable().add(scaleModifier());
                getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS, mergedIntersection,
                        sortable, occupiedRoles, usersMap, frequencyMap));

                target.appendJavaScript(getScaleScript());
                target.add(getBoxedTableExtra());
                target.add(getIntersectionTable().setOutputMarkupId(true));
                target.add(minFreqField);
                target.add(maxFreqField);
                target.add(minFreqField);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);

        processButton = new AjaxButton(ID_PROCESS_BUTTON, createStringResource("RoleMining.button.title.process")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                ProcessBusinessRolePanel detailsPanel = new ProcessBusinessRolePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("TO DO: details"), selections,null) {
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

    public BoxedTablePanel<String> generateRoleMiningTable(List<ClusteringObjectMapped> usersMap,
            List<String> occupiedRoles, boolean sortable, double frequency,
            HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        RoleMiningProvider<String> provider = new RoleMiningProvider<>(
                this, new ListModel<>(occupiedRoles) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<String> object) {
                super.setObject(object);
            }
        }, sortable);

        if (sortable) {
            provider.setSort(UserType.F_NAME.toString(), SortOrder.ASCENDING);
        }
        BoxedTablePanel<String> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRM(frequency, frequencyMap, intersection, maxFrequency, usersMap),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<String, String>> initColumnsRM(double frequency, HashMap<String,
            Double> frequencyMap, Set<String> intersection, double maxFrequency, List<ClusteringObjectMapped> clusteringObjectMapp) {

        List<IColumn<String, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<String> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("group")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public IModel<?> getDataModel(IModel<String> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<String>> item, String componentId,
                    IModel<String> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                //ROLES COLUMN HEADER
                Label label = new Label(componentId, rowModel.getObject());
                item.add(label);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("group")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<String, String> column;
        for (ClusteringObjectMapped clusterMap : clusteringObjectMapp) {
            List<String> roles = clusterMap.getPoints();

            boolean found = intersection != null && new HashSet<>(roles).containsAll(intersection);
            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<String>> cellItem,
                        String componentId, IModel<String> model) {
                    tableStyle(cellItem);
                    String role = model.getObject();

                    miningDetection(cellItem, componentId, role);

                }

                private void miningDetection(Item<ICellPopulator<String>> cellItem, String componentId, String role) {
                    if (found) {
                        filledCell(cellItem, componentId, "bg-success");
                    } else {
                        if (roles.contains(role)) {
                            Double fr = frequencyMap.get(role);
                            if (frequency > fr) {
                                filledCell(cellItem, componentId, "bg-danger");
                            } else if (maxFrequency < fr) {
                                filledCell(cellItem, componentId, "bg-info");
                            } else {
                                filledCell(cellItem, componentId, "table-dark");
                            }
                        } else {
                            emptyCell(cellItem, componentId);
                        }
                    }
                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> membersOids = clusterMap.getElements();

                    AjaxButton ajaxButton = new AjaxButton(componentId, Model.of(membersOids.size() + " member(s)")) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                            OperationResult operationResult = new OperationResult("getUsers");
                            List<PrismObject<FocusType>> users = new ArrayList<>();
                            for (String s : membersOids) {
                                users.add(getFocusObject((PageBase) getPage(), s, operationResult));
                            }
                            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Analyzed members details panel"), users, null) {
                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }
                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);

                        }
                    };
                    ajaxButton.setOutputMarkupId(true);

                    return ajaxButton;
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

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("metric")) {

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
                return new Label(componentId, getHeaderTitle("metric"));
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("type")) {

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
                return new Label(componentId, getHeaderTitle("type"));
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("occupancy.current")) {

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

                item.add(new Label(componentId, rowModel.getObject().getCurrentElements()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("occupancy.current"));
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("occupancy.total")) {

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

                AjaxButton ajaxButton = new AjaxButton(componentId,
                        createStringResource("RoleMining.button.title.compute")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        fullRolesOccupation = 0;

                        Set<String> rolesId = rowModel.getObject().getPoints();
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
                return new Label(componentId, getHeaderTitle("occupancy.total"));
            }

        });
        columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

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

                AjaxButton ajaxButton = new AjaxButton(componentId, createStringResource("RoleMining.button.title.load")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        Set<String> intersection = rowModel.getObject().getPoints();

                        selections = rowModel.getObject();
                        getBoxedTableExtra().replaceWith(generateRoleMiningTable(usersMap,
                                occupiedRoles, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                        getBoxedTableExtra().setOutputMarkupId(true);
                        getBoxedTableExtra().getDataTable().add(scaleModifier());

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
                return new Label(componentId, getHeaderTitle("display"));
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

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

}
