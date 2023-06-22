/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractIntersections.generateIntersectionsMap;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ProcessBusinessRolePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
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
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

public class SimilarGroupDetailsPanel extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";
    double minFrequency = 0.0;
    double maxFrequency = 1.0;
    Integer minIntersection = 0;

    List<IntersectionObject> mergedIntersection = new ArrayList<>();

    IntersectionObject selections;

    public boolean isSelectedBusinessRole(){
        return selections != null;
    }
    public SimilarGroupDetailsPanel(String id, List<PrismObject<MiningType>> miningTypeList,
            List<String> rolePrismObjectList, String targetOid, boolean sortable) {
        super(id);

        HashMap<String, Double> frequencyMap = getFrequencyMap(miningTypeList, rolePrismObjectList);

        BoxedTablePanel<PrismObject<MiningType>> components = generateTableRM(miningTypeList,
                rolePrismObjectList, targetOid, sortable, minFrequency, frequencyMap, null, maxFrequency);
        components.setOutputMarkupId(true);
        components.getDataTable().add(AttributeModifier.append("style", "transform: scale(0.3); transform-origin: 0 0;"));
        add(components);

        EmptyPanel tableIntersection = new EmptyPanel(ID_DATATABLE_INTERSECTIONS);
        tableIntersection.setOutputMarkupId(true);
        add(tableIntersection);

        add(frequencyForm(miningTypeList, targetOid, sortable, rolePrismObjectList, frequencyMap));

    }

    public Form<?> frequencyForm(List<PrismObject<MiningType>> miningTypeList, String targetOid, boolean sortable,
            List<String> rolePrismObjectList, HashMap<String, Double> frequencyMap) {

        Form<?> form = new Form<Void>("thresholds_form");

        TextFieldLabelPanel thresholdField = new TextFieldLabelPanel("threshold_frequency", Model.of(minFrequency), "Min frequency");
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        TextFieldLabelPanel thresholdFieldMax = new TextFieldLabelPanel("threshold_frequency_max", Model.of(maxFrequency), "Max frequency");
        thresholdFieldMax.setOutputMarkupId(true);
        thresholdFieldMax.setOutputMarkupPlaceholderTag(true);
        thresholdFieldMax.setVisible(true);
        form.add(thresholdFieldMax);

        TextFieldLabelPanel thresholdField2 = new TextFieldLabelPanel("threshold_frequency_2", Model.of(minIntersection), "Intersection");
        thresholdField2.setOutputMarkupId(true);
        thresholdField2.setOutputMarkupPlaceholderTag(true);
        thresholdField2.setVisible(true);
        form.add(thresholdField2);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_link_mn", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                minFrequency = (double) thresholdField.getBaseFormComponent().getModelObject();
                maxFrequency = (double) thresholdFieldMax.getBaseFormComponent().getModelObject();
                target.add(thresholdField);
                target.add(thresholdFieldMax);

                minIntersection = (Integer) thresholdField2.getBaseFormComponent().getModelObject();

                long startTime = System.currentTimeMillis();

                mergedIntersection = generateIntersectionsMap(miningTypeList, minIntersection, minFrequency, frequencyMap,
                        maxFrequency);

                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                double elapsedSeconds = elapsedTime / 1000.0;
                System.out.println("Elapsed time: " + elapsedSeconds + " seconds. (prepare intersections)");
                target.add(thresholdField);

                getBoxedTableExtra().replaceWith(generateTableRM(miningTypeList,
                        rolePrismObjectList, targetOid, sortable, minFrequency, frequencyMap, null, maxFrequency));
                getBoxedTableExtra().setOutputMarkupId(true);
                getBoxedTableExtra().getDataTable().add(AttributeModifier.append("style", "transform: scale(0.3);"
                        + " transform-origin: 0 0;"));

                target.appendJavaScript(getScaleScript());
                target.add(getBoxedTableExtra());

                getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS, mergedIntersection,
                        sortable, rolePrismObjectList, miningTypeList, targetOid, frequencyMap));
                target.add(getIntersectionTable().setOutputMarkupId(true));
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);



        AjaxButton ajaxButton = new AjaxButton("process_selections_id",Model.of("Process selections")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                ProcessBusinessRolePanel detailsPanel = new ProcessBusinessRolePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("TO DO: details"),selections) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);

            }

        };

        ajaxButton.setOutputMarkupId(true);
        ajaxButton.add(new VisibleBehaviour(this::isSelectedBusinessRole));

        form.add(ajaxButton);
        form.add(ajaxSubmitLink);

        return form;
    }


    public HashMap<String, Double> getFrequencyMap(List<PrismObject<MiningType>> miningTypeList,
            List<String> rolePrismObjectList) {

        HashMap<String, Double> roleFrequencyMap = new HashMap<>();
        HashSet<String> roleIds = new HashSet<>(rolePrismObjectList);

        HashMap<String, Integer> roleCountMap = new HashMap<>();
        for (PrismObject<MiningType> miningType : miningTypeList) {
            List<String> roles = miningType.asObjectable().getRoles();
            for (String roleId : roles) {
                if (roleIds.contains(roleId)) {
                    roleCountMap.put(roleId, roleCountMap.getOrDefault(roleId, 0) + 1);
                }
            }
        }

        int totalMiningTypeObjects = miningTypeList.size();
        for (Map.Entry<String, Integer> entry : roleCountMap.entrySet()) {
            String roleId = entry.getKey();
            int frequency = entry.getValue();
            double percentage = (double) frequency / totalMiningTypeObjects;
            roleFrequencyMap.put(roleId, percentage);
        }

        return roleFrequencyMap;
    }

    public BoxedTablePanel<PrismObject<MiningType>> generateTableRM(List<PrismObject<MiningType>> groupsOid,
            List<String> rolePrismObjectList, String targetOid, boolean sortable, double frequency,
            HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        RoleMiningProvider<PrismObject<MiningType>> provider = new RoleMiningProvider<>(
                this, new ListModel<>(groupsOid) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PrismObject<MiningType>> object) {
                super.setObject(object);
            }

        }, sortable);

        if (sortable) {
            provider.setSort(MiningType.F_ROLES.toString(), SortOrder.ASCENDING);
        }
        BoxedTablePanel<PrismObject<MiningType>> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRM(rolePrismObjectList, targetOid, frequency, frequencyMap, intersection, maxFrequency),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<PrismObject<MiningType>, String>> initColumnsRM(List<String> rolePrismObjectList,
            String targetOid, double frequency, HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        List<IColumn<PrismObject<MiningType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismObject<MiningType>> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Group")) {

            @Override
            public String getSortProperty() {
                return MiningType.F_OID.getLocalPart();
            }

            @Override
            public IModel<?> getDataModel(IModel<PrismObject<MiningType>> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PrismObject<MiningType>>> item, String componentId,
                    IModel<PrismObject<MiningType>> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                Label label = new Label(componentId, rowModel.getObject().getOid());
                String oid = rowModel.getObject().getValue().getOid();
                item.add(label);
                if (targetOid != null) {
                    if (oid.equals(targetOid)) {
                        item.add(new AttributeAppender("class", " table-primary"));
                    }
                }

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

        IColumn<PrismObject<MiningType>, String> column;
        for (String roleTypePrismObject : rolePrismObjectList) {
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
                public void populateItem(Item<ICellPopulator<PrismObject<MiningType>>> cellItem,
                        String componentId, IModel<PrismObject<MiningType>> model) {

                    tableStyle(cellItem);

                    if (intersection != null
                            && intersection.contains(roleTypePrismObject)
                            && new HashSet<>(model.getObject().asObjectable().getRoles()).containsAll(intersection)) {
                        filledCell(cellItem, componentId, "bg-success");
                    } else {
                        List<String> roleMembers = model.getObject().asObjectable().getRoles();
                        if (roleMembers.contains(roleTypePrismObject)) {
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
            boolean sortable, List<String> rolePrismObjectList, List<PrismObject<MiningType>> miningTypeList,
            String targetOid, HashMap<String, Double> frequencyMap) {

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
                id, provider, initColumnsIntersection(miningTypeList, targetOid, sortable, rolePrismObjectList, frequencyMap),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        return table;
    }

    int counter = 0;

    public List<IColumn<IntersectionObject, String>> initColumnsIntersection(List<PrismObject<MiningType>> miningTypeList,
            String targetOid, boolean sortable, List<String> rolePrismObjectList,
            HashMap<String, Double> frequencyMap) {

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

                        counter = 0;

                        Set<String> rolesId = rowModel.getObject().getRolesId();
                        OperationResult result = new OperationResult("Generate miningType object");

                        ResultHandler<UserType> handler = (object, parentResult) -> {

                            List<String> rolesOid = new ArrayList<>();
                            List<AssignmentType> assignment = object.asObjectable().getAssignment();
                            for (AssignmentType assignmentObject : assignment) {
                                ObjectReferenceType targetRef = assignmentObject.getTargetRef();
                                if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())) {
                                    rolesOid.add(targetRef.getOid());
                                }
                            }

                            if (new HashSet<>(rolesOid).containsAll(rolesId)) {
                                counter++;
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

                        this.setDefaultModel(Model.of(String.valueOf(counter)));
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
                        getBoxedTableExtra().replaceWith(generateTableRM(miningTypeList,
                                rolePrismObjectList, targetOid, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                        getBoxedTableExtra().setOutputMarkupId(true);
                        getBoxedTableExtra().getDataTable().add(AttributeModifier.append("style", "transform: scale(0.3);"
                                + " transform-origin: 0 0;"));

                        ajaxRequestTarget.appendJavaScript(getScaleScript());
                        ajaxRequestTarget.add(getBoxedTableExtra());

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

    private void tableStyle(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private void emptyCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new EmptyPanel(componentId));
    }

    private void filledCell(@NotNull Item<?> cellItem, String componentId, String color) {
        cellItem.add(new AttributeAppender("class", color));
        cellItem.add(new EmptyPanel(componentId));
    }

    protected Component getIntersectionTable() {
        return get(((PageBase) getPage()).createComponentPath(ID_DATATABLE_INTERSECTIONS));
    }

    protected BoxedTablePanel<?> getBoxedTableExtra() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

    private String getScaleScript() {
        return "let div = document.querySelector('#myTable');" +
                "let table = div.querySelector('table');" +
                "let scale = 1;" +
                "if (div && table) {" +
                "  div.onwheel = function(e) {" +
                "    e.preventDefault();" +
                "    let rectBefore = table.getBoundingClientRect();" +
                "    let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;" +
                "    let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;" +
                "    table.style.transformOrigin = 'left top';" +
                "    if (e.deltaY < 0) {" +
                "      console.log('Zooming in');" +
                "      scale += 0.03;" +
                "      let prevScale = scale - 0.1;" +
                "      let scaleFactor = scale / prevScale;" +
                "      let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);" +
                "      let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);" +
                "      table.style.transformOrigin = x + '%' + ' ' + y + '%';" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);" +
                "    } else if (e.deltaY > 0) {" +
                "      console.log('Zooming out');" +
                "      scale -= 0.03;" +
                "      scale = Math.max(0.1, scale);" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top);" +
                "    }" +
                "  };" +
                "} else {" +
                "  console.error('Div or table not found');" +
                "}";
    }

}
