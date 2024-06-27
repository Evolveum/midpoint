/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure.extractAttributeAnalysis;
import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateAssignmentOutlierResultModel;

import java.io.Serial;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.outlier.RoleAnalysisOutlierTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.outlier.RoleAnalysisOutlierTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTile;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisOutlierPropertyTileTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";
    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;

    public RoleAnalysisOutlierPropertyTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull LoadableDetachableModel<List<RoleAnalysisOutlierDescriptionType>> outlierDescriptionList,
            @NotNull RoleAnalysisOutlierType outlierParent) {
        super(id);
        this.pageBase = pageBase;
        this.items = new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = getTable().getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
        add(initTable(outlierDescriptionList, outlierParent));
    }

    public TileTablePanel<RoleAnalysisOutlierTileModel<RoleAnalysisOutlierDescriptionType>, RoleAnalysisOutlierDescriptionType> initTable(
            @NotNull LoadableDetachableModel<List<RoleAnalysisOutlierDescriptionType>> outlierDescriptionList,
            @NotNull RoleAnalysisOutlierType outlierParent) {

        RoleMiningProvider<RoleAnalysisOutlierDescriptionType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(outlierDescriptionList.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleAnalysisOutlierDescriptionType> object) {
                super.setObject(object);
            }

        }, false);

        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                UserProfileStorage.TableId.PANEL_OUTLIER_PROPERTIES) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<RoleAnalysisOutlierDescriptionType, String>> createColumns() {
                return RoleAnalysisOutlierPropertyTileTable.this.initColumns(outlierParent);
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisOutlierPropertyTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"),
                        Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace("title",
                        createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);
                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
//                        RoleAnalysisSessionTileTable.this.getTable().refresh();
                        target.add(RoleAnalysisOutlierPropertyTileTable.this);
                    }
                };

                viewToggle.add(AttributeModifier.replace("title", createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisOutlierPropertyTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(RoleAnalysisOutlierPropertyTileTable.this);
                    }
                };

                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "card-footer";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return " m-0";
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                return provider;
            }

            @Override
            protected PageableListView<?, ?> createTilesPanel(String tilesId, ISortableDataProvider<RoleAnalysisOutlierDescriptionType, String> provider1) {
                return super.createTilesPanel(tilesId, provider1);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisOutlierTileModel createTileObject(RoleAnalysisOutlierDescriptionType object) {
                ObjectReferenceType ref = object.getObject();
                String name = ref.getTargetName().toString();
                return new RoleAnalysisOutlierTileModel<>(object, name, "TODO", outlierParent, getPageBase());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-3 p-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left ";
            }

            @Override
            protected Component createTile(String id, IModel<RoleAnalysisOutlierTileModel<RoleAnalysisOutlierDescriptionType>> model) {
                return new RoleAnalysisOutlierTilePanel<>(id, model);
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    public List<IColumn<RoleAnalysisOutlierDescriptionType, String>> initColumns(@NotNull RoleAnalysisOutlierType outlierParent) {

        List<IColumn<RoleAnalysisOutlierDescriptionType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType object = result.getObject();
                if (object.getType().equals(UserType.COMPLEX_TYPE)) {
                    return createDisplayType(GuiStyleConstants.CLASS_OBJECT_USER_ICON, "black", "");
                }

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
                PrismObject<RoleAnalysisClusterType> cluster = roleAnalysisService
                        .getObject(RoleAnalysisClusterType.class, ref.getOid(), task, task.getResult());
                if (cluster != null) {
                    objectName = cluster.getName().getOrig();
                }

                item.add(new AjaxLinkPanel(componentId, Model.of(objectName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, ref.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                });

                //TODO - add similar aspect analysis
//                PrismObject<UserType> user = roleAnalysisService.getUserTypeObject(targetObjectOid, task, task.getResult());
//                if (cluster != null && user != null) {
//                    RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService.resolveSimilarAspect(cluster.asObjectable(), user);
//                }

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
                return RoleAnalysisOutlierDescriptionType.F_CONFIDENCE_DEVIATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {
                if (rowModel.getObject() != null) {
                    Double confidence = rowModel.getObject().getConfidenceDeviation();
                    if (confidence != null) {
                        double confidencePercentage = confidence * 100.0;
                        confidencePercentage = confidencePercentage * 100.0 / 100.0;
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        decimalFormat.setGroupingUsed(false);
                        decimalFormat.setRoundingMode(RoundingMode.DOWN);
                        String formattedConfidence = decimalFormat.format(confidencePercentage);
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

        columns.add(new AbstractColumn<>(createStringResource("Cluster analysis")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                Task task = getPageBase().createSimpleTask("Load object");
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType clusterRef = result.getCluster();

                PrismObject<RoleAnalysisClusterType> object = roleAnalysisService
                        .getObject(RoleAnalysisClusterType.class, clusterRef.getOid(), task, task.getResult());

                if (object == null) {
                    return;
                }

                RoleAnalysisClusterType cluster = object.asObjectable();

                item.add(new AjaxLinkPanel(componentId, Model.of("Cluster attributes")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        ObjectReferenceType propertyObjectRef = result.getObject();
                        QName type = propertyObjectRef.getType();
                        ObjectReferenceType targetObjectRef = outlierParent.getTargetObjectRef();

                        PrismObject<UserType> userTypeObject;
                        PrismObject<RoleType> roleTypeObject;
                        if (type.equals(RoleType.COMPLEX_TYPE)) {
                            userTypeObject = roleAnalysisService.getUserTypeObject(targetObjectRef.getOid(), task, task.getResult());
                            roleTypeObject = roleAnalysisService.getRoleTypeObject(propertyObjectRef.getOid(), task, task.getResult());
                        } else {
                            userTypeObject = roleAnalysisService.getUserTypeObject(propertyObjectRef.getOid(), task, task.getResult());
                            roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, task.getResult());
                        }

                        if (userTypeObject == null || roleTypeObject == null) {
                            return;
                        }

                        ObjectReferenceType targetSessionRef = outlierParent.getTargetSessionRef();
                        PrismObject<RoleAnalysisSessionType> targetSession = roleAnalysisService
                                .getObject(RoleAnalysisSessionType.class, targetSessionRef.getOid(), task, task.getResult());

                        if (targetSession == null) {
                            return;
                        }

                        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                                targetSession.asObjectable(), UserType.COMPLEX_TYPE);

                        List<RoleAnalysisAttributeDef> attributesForRoleAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                                targetSession.asObjectable(), RoleType.COMPLEX_TYPE);

                        if (attributesForUserAnalysis == null && attributesForRoleAnalysis == null) {
                            return;
                        }

                        Set<String> userPathToMark = null;
                        if (attributesForUserAnalysis != null) {
                            userPathToMark = roleAnalysisService.resolveUserValueToMark(userTypeObject, attributesForUserAnalysis);
                        }

                        Set<String> rolePathToMark = null;
                        if (attributesForRoleAnalysis != null) {
                            rolePathToMark = roleAnalysisService.resolveRoleValueToMark(roleTypeObject, attributesForRoleAnalysis);
                        }

                        RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
                        if (attributesForUserAnalysis != null) {
                            RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                                    .resolveUserAttributes(userTypeObject, attributesForUserAnalysis);
                            if (cluster.getClusterStatistics() == null) {
                                return;
                            }

                            RoleAnalysisAttributeAnalysisResult clusterAttributes = cluster.getClusterStatistics()
                                    .getUserAttributeAnalysisResult();

                            if (clusterAttributes == null) {
                                return;
                            }

                            compareAttributeResult = roleAnalysisService
                                    .resolveSimilarAspect(userAttributes, clusterAttributes);
                        }

                        if (compareAttributeResult == null) {
                            return;
                        }
                        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

                        Set<String> finalRolePathToMark = rolePathToMark;
                        Set<String> finalUserPathToMark = userPathToMark;
                        RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"),
                                cluster) {

                            @Override
                            public List<AttributeAnalysisStructure> getStackedNegativeValue() {
                                return extractAttributeAnalysis(attributeAnalysis, UserType.COMPLEX_TYPE);
                            }

                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }

                            @Override
                            protected Set<String> getRolePathToMark() {
                                return finalRolePathToMark;
                            }

                            @Override
                            protected Set<String> getUserPathToMark() {
                                return finalUserPathToMark;
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, Model.of("Cluster analysis"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Member analysis")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {

                RoleAnalysisOutlierDescriptionType result = rowModel.getObject();
                ObjectReferenceType ref = result.getObject();
                QName type = ref.getType();

                if (type.equals(RoleType.COMPLEX_TYPE)) {
                    roleAnalysisPanel(item, componentId, ref);
                } else {
                    userAnalysisPanel(item, componentId, ref);
                }

            }

            private void roleAnalysisPanel(
                    @NotNull Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item,
                    @NotNull String componentId,
                    @NotNull ObjectReferenceType roleRef) {
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                String title = "Member attributes";

                item.add(new AjaxLinkPanel(componentId, Model.of(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismObject<RoleType> prismRole = roleAnalysisService
                                .getRoleTypeObject(roleRef.getOid(), task, operationResult);
                        if (prismRole == null) {
                            return;
                        }

                        ObjectReferenceType targetSessionRef = outlierParent.getTargetSessionRef();
                        PrismObject<RoleAnalysisSessionType> targetSession = roleAnalysisService
                                .getObject(RoleAnalysisSessionType.class, targetSessionRef.getOid(), task, task.getResult());

                        if (targetSession == null) {
                            return;
                        }

                        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                                targetSession.asObjectable(), UserType.COMPLEX_TYPE);

                        if (attributesForUserAnalysis == null) {
                            return;
                        }

                        List<AttributeAnalysisStructure> attributeAnalysisStructures = roleAnalysisService
                                .roleMembersAttributeAnalysis(attributesForUserAnalysis, prismRole.getOid(), task, operationResult);

                        ObjectReferenceType targetObjectRef = outlierParent.getTargetObjectRef();

                        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(
                                targetObjectRef.getOid(), task, task.getResult());

                        if (userTypeObject == null) {
                            return;
                        }

                        Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(
                                userTypeObject, attributesForUserAnalysis);

                        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                                .resolveRoleMembersAttribute(prismRole.getOid(), task, operationResult, attributesForUserAnalysis);
                        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                                .resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

                        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                                .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

                        if (compareAttributeResult == null) {
                            return;
                        }
                        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

                        RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"),
                                attributeAnalysisStructures, RoleAnalysisProcessModeType.USER) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }

                            @Override
                            public List<AttributeAnalysisStructure> getStackedNegativeValue() {
                                return extractAttributeAnalysis(attributeAnalysis, UserType.COMPLEX_TYPE);
                            }

                            @Override
                            protected Set<String> getUserPathToMark() {
                                return userPathToMark;
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                });
            }

            private void userAnalysisPanel(@NotNull Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item,
                    @NotNull String componentId,
                    @NotNull ObjectReferenceType userRef) {
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                String title = "Member attributes";

                item.add(new AjaxLinkPanel(componentId, Model.of(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ObjectReferenceType targetObjectRef = outlierParent.getTargetObjectRef();
                        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, task.getResult());

                        if (roleTypeObject == null) {
                            return;
                        }

                        ObjectReferenceType targetSessionRef = outlierParent.getTargetSessionRef();
                        PrismObject<RoleAnalysisSessionType> targetSession = roleAnalysisService
                                .getObject(RoleAnalysisSessionType.class, targetSessionRef.getOid(), task, task.getResult());

                        if (targetSession == null) {
                            return;
                        }

                        List<RoleAnalysisAttributeDef> attributesForRoleAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                                targetSession.asObjectable(), RoleType.COMPLEX_TYPE);

                        if (attributesForRoleAnalysis == null) {
                            return;
                        }

                        List<AttributeAnalysisStructure> attributeAnalysisStructures = roleAnalysisService
                                .userRolesAttributeAnalysis(attributesForRoleAnalysis, userRef.getOid(), task, operationResult);

                        Set<String> rolePathToMark = roleAnalysisService.resolveRoleValueToMark(roleTypeObject, attributesForRoleAnalysis);

                        RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"),
                                attributeAnalysisStructures, RoleAnalysisProcessModeType.ROLE) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }

                            @Override
                            protected Set<String> getRolePathToMark() {
                                return rolePathToMark;
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, Model.of("Member analysis"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Result")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierDescriptionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierDescriptionType> rowModel) {
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                Task task = getPageBase().createSimpleTask("Load object");

                ObjectReferenceType ref = rowModel.getObject().getObject();
                QName type = ref.getType();

                if (type.equals(UserType.COMPLEX_TYPE)) {
                    item.add(new Label(componentId, "TODO"));
                    return;
                }

                ObjectReferenceType targetObjectRef = outlierParent.getTargetObjectRef();
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(targetObjectRef.getOid(), task, task.getResult());

                if (userTypeObject == null) {
                    return;
                }

                OutlierObjectModel outlierObjectModel = generateAssignmentOutlierResultModel(
                        roleAnalysisService, rowModel.getObject(), task, task.getResult(), userTypeObject, outlierParent);

                String outlierName = outlierObjectModel.getOutlierName();
                Double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                String description = outlierObjectModel.getOutlierDescription();
                String timestamp = outlierObjectModel.getTimeCreated();

                item.add(new AjaxLinkPanel(componentId, Model.of("Result")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        OutlierResultPanel detailsPanel = new OutlierResultPanel(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel")) {

                            @Override
                            public StringResourceModel getTitle() {
                                return createStringResource("Outlier assignment description");
                            }

                            @Override
                            public Component getCardHeaderBody(String componentId) {
                                OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                                        description, String.valueOf(outlierConfidence), timestamp);
                                components.setOutputMarkupId(true);
                                return components;
                            }

                            @Override
                            public Component getCardBodyComponent(String componentId) {
                                //TODO just for testing
                                RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                                outlierObjectModel.getOutlierItemModels().forEach(outlierItemModel -> {
                                    cardBodyComponent.add(new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel));
                                });
                                return cardBodyComponent;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }

                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                });

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, Model.of("Result"));
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

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTile<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTile<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

}
