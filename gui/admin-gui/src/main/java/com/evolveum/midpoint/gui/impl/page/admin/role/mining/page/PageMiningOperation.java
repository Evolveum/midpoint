/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.getRolesOid;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractIntersections.generateIntersectionsMap;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterUtils.generateFrequencyMap;

import java.io.Serial;
import java.util.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ProcessBusinessRolePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/miningOperation", matchUrlForSecurity = "/admin/miningOperation")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageMiningOperation extends PageAdmin {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";
    private static final String ID_FREQUENCY_FORM = "thresholds_form";

    private static final String ID_FREQUENCY_THRESHOLD_MIN_FRQ = "threshold_frequency";
    private static final String ID_FREQUENCY_THRESHOLD_MAX_FRQ = "threshold_frequency_max";

    private static final String ID_FREQUENCY_SUBMIT = "ajax_submit_link";
    private static final String ID_FREQUENCY_THRESHOLD_ITR = "threshold_intersection";

    private static final String ID_PROCESS_BUTTON = "process_selections_id";

    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_MODE = "mode";
    double minFrequency = 0.3;
    double maxFrequency = 1.0;
    Integer minIntersection = 10;
    List<IntersectionObject> mergedIntersection = new ArrayList<>();
    IntersectionObject selectionsPoints;

    int totalPointsOccupation = 0;
    AjaxButton processButton;
    Set<String> intersection;
    boolean inverse = true;

    OperationResult result = new OperationResult("GetObject");

    String getPageParameterOid() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_OID).toString();
    }

    String getPageParameterMode() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_MODE).toString();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    public PageMiningOperation() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        String pageParameterMode = getPageParameterMode();

        ClusterType cluster = getCluster(getPageBase(), getPageParameterOid()).asObjectable();
        List<ClusteringObjectMapped> clusteringObjectMapped = null;
        if (pageParameterMode.equals(Mode.ROLE.getDisplayString())) {
            clusteringObjectMapped = generateClusterMappedStructureRoleMode(cluster, getPageBase());
        } else if (pageParameterMode.equals(Mode.USER.getDisplayString())) {
            clusteringObjectMapped = generateClusterMappedStructure(cluster, getPageBase(), result);
        }

        List<String> clusterPoints = cluster.getPoints();

        assert clusteringObjectMapped != null;
        HashMap<String, Double> frequencyMap = generateFrequencyMap(clusteringObjectMapped, clusterPoints);
        if (inverse) {
            BoxedTablePanel<String> boxedTablePanel = generateRoleMiningTable(clusteringObjectMapped,
                    clusterPoints, false, minFrequency, frequencyMap, null, maxFrequency);
            boxedTablePanel.setOutputMarkupId(true);
            boxedTablePanel.getDataTable().add(scaleModifier());

            boxedTablePanel.getDataTable().add(new AjaxEventBehavior("change") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    target.appendJavaScript(getScaleScript());
                }
            });
            add(boxedTablePanel);
        } else {

            BoxedTablePanel<ClusteringObjectMapped> boxedTablePanel = generateRoleMiningTableReverse(clusteringObjectMapped,
                    clusterPoints, false, minFrequency, frequencyMap, null, maxFrequency);
            boxedTablePanel.setOutputMarkupId(true);
            boxedTablePanel.getDataTable().add(scaleModifier());

            boxedTablePanel.getDataTable().add(new AjaxEventBehavior("change") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    target.appendJavaScript(getScaleScript());
                }
            });
            add(boxedTablePanel);
        }

        EmptyPanel tableIntersection = new EmptyPanel(ID_DATATABLE_INTERSECTIONS);
        tableIntersection.setOutputMarkupId(true);
        add(tableIntersection);

        add(frequencyForm(clusteringObjectMapped, false, clusterPoints, frequencyMap));

        List<ClusteringObjectMapped> finalClusteringObjectMapped = clusteringObjectMapped;
        List<ClusteringObjectMapped> finalClusteringObjectMapped1 = clusteringObjectMapped;
        AjaxLinkPanel ajaxButton = new AjaxLinkPanel("table_style", Model.of("Invert table")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (inverse) {
                    inverse = false;
                    getBoxedTableExtra().replaceWith(generateRoleMiningTableReverse(finalClusteringObjectMapped,
                            clusterPoints, false, minFrequency, frequencyMap, intersection, maxFrequency));
                } else {
                    inverse = true;
                    getBoxedTableExtra().replaceWith(generateRoleMiningTable(finalClusteringObjectMapped1,
                            clusterPoints, false, minFrequency, frequencyMap, intersection, maxFrequency));

                }

                getBoxedTableExtra().getDataTable().add(scaleModifier());
                ajaxRequestTarget.appendJavaScript(getScaleScript());
                ajaxRequestTarget.add(getBoxedTableExtra());
            }
        };
        ajaxButton.setOutputMarkupId(true);
        add(ajaxButton);
    }

    private TextFieldLabelPanel generateFieldPanel(String id, IModel<?> model, String stringResource) {
        TextFieldLabelPanel components = new TextFieldLabelPanel(id,
                model, stringResource);
        components.setOutputMarkupId(true);
        components.setOutputMarkupPlaceholderTag(true);
        components.setVisible(true);
        return components;
    }

    public Form<?> frequencyForm(List<ClusteringObjectMapped> clusteringObjectMapped, boolean sortable,
            List<String> occupiedPoints, HashMap<String, Double> frequencyMap) {
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

                mergedIntersection = generateIntersectionsMap(clusteringObjectMapped, minIntersection, minFrequency, frequencyMap,
                        maxFrequency);

                endTimer(startTime, "prepare intersections");

                if (inverse) {
                    getBoxedTableExtra().replaceWith(generateRoleMiningTable(clusteringObjectMapped,
                            occupiedPoints, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                } else {
                    getBoxedTableExtra().replaceWith(generateRoleMiningTableReverse(clusteringObjectMapped,
                            occupiedPoints, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                }

                getBoxedTableExtra().setOutputMarkupId(true);
                getBoxedTableExtra().getDataTable().add(scaleModifier());
                getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS, mergedIntersection,
                        sortable, occupiedPoints, clusteringObjectMapped, frequencyMap));

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
                        Model.of("TO DO: details"), selectionsPoints, getPageParameterMode()) {
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

    public BoxedTablePanel<String> generateRoleMiningTable(List<ClusteringObjectMapped> clusteringObjectMapped,
            List<String> occupiedPoints, boolean sortable, double frequency,
            HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        RoleMiningProvider<String> provider = new RoleMiningProvider<>(
                this, new ListModel<>(occupiedPoints) {

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
                ID_DATATABLE, provider, initColumnsRM(frequency, frequencyMap, intersection, maxFrequency, clusteringObjectMapped),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public BoxedTablePanel<ClusteringObjectMapped> generateRoleMiningTableReverse(List<ClusteringObjectMapped> clusteringObjectMapped,
            List<String> occupiedPoints, boolean sortable, double frequency,
            HashMap<String, Double> frequencyMap, Set<String> intersection, double maxFrequency) {

        RoleMiningProvider<ClusteringObjectMapped> provider = new RoleMiningProvider<>(
                this, new ListModel<>(clusteringObjectMapped) {

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
                ID_DATATABLE, provider, initColumnsReverse(frequency, frequencyMap, intersection, maxFrequency, occupiedPoints),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<ClusteringObjectMapped, String>> initColumnsReverse(double frequency, HashMap<String,
            Double> frequencyMap, Set<String> intersection, double maxFrequency, List<String> occupiedPoints) {

        List<IColumn<ClusteringObjectMapped, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<ClusteringObjectMapped> rowModel) {
                DisplayType displayType;
                if (getPageParameterMode().equals("ROLE")) {
                    displayType = GuiDisplayTypeUtil
                            .createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
                } else {
                    displayType = GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
                }
                return displayType;
            }
        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("group")) {

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

                List<String> elements = rowModel.getObject().getElements();

                String title = elements.size() + " element(s)";
                AjaxLinkPanel components = new AjaxLinkPanel(componentId,
                        createStringResource(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<PrismObject<FocusType>> objects = new ArrayList<>();
                        for (String s : elements) {
                            objects.add(getFocusObject((PageBase) getPage(), s, result));
                        }
                        MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"), objects, getPageParameterMode()) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }

                };
                item.add(components.setOutputMarkupId(true));
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

        if (selectionsPoints == null) {
            selectionsPoints = new IntersectionObject(new HashSet<>(), 0, null, 0, null, new HashSet<>());
        }

        IColumn<ClusteringObjectMapped, String> column;
        for (String points : occupiedPoints) {
            String cellColor = "table-dark";
            Double fr = frequencyMap.get(points);
            if (frequency > fr) {
                cellColor = "bg-danger";
            } else if (maxFrequency < fr) {
                cellColor = "bg-info";
            }

            String finalCellColor = cellColor;
            column = new AbstractColumn<>(createStringResource(points)) {

                @Override
                public void populateItem(Item<ICellPopulator<ClusteringObjectMapped>> cellItem,
                        String componentId, IModel<ClusteringObjectMapped> model) {

                    tableStyle(cellItem);

                    List<String> rolesOid = model.getObject().getPoints();
                    if (intersection != null
                            && intersection.contains(points)
                            && new HashSet<>(rolesOid).containsAll(intersection)) {
                        filledCell(cellItem, componentId, "bg-success");
                        selectionsPoints.getElements().addAll(model.getObject().getElements());
                        selectionsPoints.getPoints().add(points);
                    } else {
                        if (rolesOid.contains(points)) {
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

                    if (getPageParameterMode().equals("ROLE")) {
                        displayType = GuiDisplayTypeUtil.createDisplayType(
                                WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
                    }
                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(points), createStringResource(points), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, points);
                            if (getPageParameterMode().equals("ROLE")) {
                                ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                            } else {
                                ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);

                            }
                        }
                    };

                }

            };
            columns.add(column);
        }

        return columns;
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
                DisplayType displayType;
                if (getPageParameterMode().equals("ROLE")) {
                    displayType = GuiDisplayTypeUtil
                            .createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
                } else {
                    displayType = GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
                }
                return displayType;
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

                String name = rowModel.getObject();

                AjaxLinkPanel components = new AjaxLinkPanel(componentId,
                        Model.of(name)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject());
                        if (getPageParameterMode().equals("ROLE")) {
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                        } else {
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);

                        }
                    }

                };
                components.setOutputMarkupId(true);
                item.add(components);
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

        if (selectionsPoints == null) {
            selectionsPoints = new IntersectionObject(new HashSet<>(), 0, null, 0, null, new HashSet<>());
        }
        IColumn<String, String> column;
        for (ClusteringObjectMapped clusterMap : clusteringObjectMapp) {
            List<String> points = clusterMap.getPoints();

            boolean found = intersection != null && new HashSet<>(points).containsAll(intersection);
            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<String>> cellItem,
                        String componentId, IModel<String> model) {
                    tableStyle(cellItem);
                    String role = model.getObject();

                    miningDetection(cellItem, componentId, role);

                }

                private void miningDetection(Item<ICellPopulator<String>> cellItem, String componentId, String role) {

                    if (points.contains(role)) {

                        if (found && intersection.contains(role)) {
                            filledCell(cellItem, componentId, "bg-success");
                            selectionsPoints.getElements().addAll(clusterMap.getElements());
                            selectionsPoints.getPoints().add(role);
                        } else {
                            Double fr = frequencyMap.get(role);
                            if (frequency > fr) {
                                filledCell(cellItem, componentId, "bg-danger");
                            } else if (maxFrequency < fr) {
                                filledCell(cellItem, componentId, "bg-info");
                            } else {
                                filledCell(cellItem, componentId, "table-dark");
                            }
                        }

                    } else {
                        emptyCell(cellItem, componentId);
                    }
                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> elements = clusterMap.getElements();

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    if (getPageParameterMode().equals("ROLE")) {
                        displayType = GuiDisplayTypeUtil.createDisplayType(
                                WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
                    }
                    String title = elements.size() + " element(s)";
                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(title), createStringResource(title), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            for (String s : elements) {
                                objects.add(getFocusObject((PageBase) getPage(), s, result));
                            }
                            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Analyzed members details panel"), objects, getPageParameterMode()) {
                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }
                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }

                    };
                }

            };
            columns.add(column);
        }

        return columns;
    }

    public BoxedTablePanel<IntersectionObject> generateTableIntersection(String id, List<IntersectionObject> miningSets,
            boolean sortable, List<String> occupiedPoints, List<ClusteringObjectMapped> clusteringObjectMapped,
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
                id, provider, initColumnsIntersection(clusteringObjectMapped, sortable, occupiedPoints, frequencyMap),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        return table;
    }

    public List<IColumn<IntersectionObject, String>> initColumnsIntersection(List<ClusteringObjectMapped> clusteringObjectMapped,
            boolean sortable, List<String> occupiedPoints, HashMap<String, Double> frequencyMap) {

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

                PageBase pageBase = (PageBase) getPage();
                AjaxButton ajaxButton = new AjaxButton(componentId,
                        createStringResource("RoleMining.button.title.compute")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        totalPointsOccupation = 0;
                        Set<String> points = rowModel.getObject().getPoints();
                        OperationResult result = new OperationResult("Generate miningType object");

                        if (getPageParameterMode().equals("ROLE")) {

                            ResultHandler<RoleType> resultHandler = (object, parentResult) -> {
                                try {

                                    String point = object.getOid();
                                    List<String> element = getMembersOidList(pageBase, point);

                                    if (new HashSet<>(element).containsAll(points)) {
                                        totalPointsOccupation++;
                                    }

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return true;
                            };

                            GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService()
                                    .getOperationOptionsBuilder();
                            RepositoryService repositoryService = pageBase.getRepositoryService();

                            try {
                                repositoryService.searchObjectsIterative(RoleType.class, null, resultHandler,
                                        optionsBuilder.build(), true, result);
                            } catch (SchemaException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            ResultHandler<UserType> handler = (object, parentResult) -> {

                                List<String> rolesOid = getRolesOid(object.asObjectable());

                                if (new HashSet<>(rolesOid).containsAll(points)) {
                                    totalPointsOccupation++;
                                }
                                return true;
                            };

                            GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService()
                                    .getOperationOptionsBuilder();
                            RepositoryService repositoryService = pageBase.getRepositoryService();
                            try {
                                repositoryService.searchObjectsIterative(UserType.class, null, handler,
                                        optionsBuilder.build(), true, result);
                            } catch (SchemaException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        this.setDefaultModel(Model.of(String.valueOf(totalPointsOccupation)));
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
                        intersection = rowModel.getObject().getPoints();

                        if (inverse) {
                            getBoxedTableExtra().replaceWith(generateRoleMiningTable(clusteringObjectMapped,
                                    occupiedPoints, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                        } else {
                            getBoxedTableExtra().replaceWith(generateRoleMiningTableReverse(clusteringObjectMapped,
                                    occupiedPoints, sortable, minFrequency, frequencyMap, intersection, maxFrequency));
                        }

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

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}

