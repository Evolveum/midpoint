/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.prism.PrismObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class RoleAnalysisDetectedAnomalyTable extends BasePanel<String> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisDetectedAnomalyTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    IModel<RoleAnalysisOutlierType> outlierModel;
    IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel;
    IModel<ListMultimap<String, DetectedAnomalyResult>> anomalyResultMap;
    IModel<ListMultimap<String, RoleAnalysisOutlierPartitionType>> anomalyPartitionMap;
    boolean partitionAnalysis = false;

    public RoleAnalysisDetectedAnomalyTable(String id,
            @NotNull RoleAnalysisOutlierType outlier, RoleAnalysisOutlierPartitionType partition) {
        super(id);

        boolean partitionAnalysis = true;
        initModels(outlier, partition);

        createTable();
    }

    public RoleAnalysisDetectedAnomalyTable(String id,
            @NotNull RoleAnalysisOutlierType outlier) {
        super(id);

        partitionAnalysis = false;
        initModels(outlier, null);

        createTable();
    }

    private void initModels(@NotNull RoleAnalysisOutlierType outlier, RoleAnalysisOutlierPartitionType partition) {
        outlierModel = new LoadableModel<>() {
            @Override
            protected RoleAnalysisOutlierType load() {
                return outlier;
            }
        };

        if (partitionAnalysis && partition != null) {
            partitionModel = new LoadableModel<>() {
                @Override
                protected List<RoleAnalysisOutlierPartitionType> load() {
                    return Collections.singletonList(partition);
                }
            };
        } else {
            partitionModel = new LoadableModel<>() {
                @Override
                protected List<RoleAnalysisOutlierPartitionType> load() {
                    RoleAnalysisOutlierType outlier = getOutlierModelObject();
                    return outlier.getOutlierPartitions();
                }
            };
        }

    }

    private void createTable() {

        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull @Unmodifiable List<Component> createToolbarButtonsList(String buttonId) {
                return List.of(RoleAnalysisDetectedAnomalyTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createRecertifyInlineMenu());
                return menuItems;

            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createIconColumn() {
                return super.createIconColumn();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return RoleAnalysisDetectedAnomalyTable.this.createProvider(getPartitionModelObject());
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisDetectedAnomalyTable.header.confidence.title"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();
                        List<DetectedAnomalyResult> anomalyResult = getAnomalyResultMapModelObject().get(oid);
                        double averageConfidence = 0.0;
                        for (DetectedAnomalyResult detectedAnomalyResult : anomalyResult) {
                            double confidence = detectedAnomalyResult.getStatistics().getConfidence();
                            averageConfidence += confidence;
                        }
                        averageConfidence = averageConfidence / anomalyResult.size();

                        initDensityProgressPanel(cellItem, componentId, averageConfidence);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisDetectedAnomalyTable.header.identification.title"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();
                        List<RoleAnalysisOutlierPartitionType> partitions = getAnomalyPartitionMapModelObject().get(oid);
                        Label label = new Label(componentId, String.valueOf(partitions.size()));
                        label.setOutputMarkupId(true);
                        cellItem.add(label);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisDetectedAnomalyTable.header.unreliability.title"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        Label label = new Label(componentId, "TBA");
                        label.setOutputMarkupId(true);
                        cellItem.add(label);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);
                return columns;
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
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

    private SelectableBeanObjectDataProvider<RoleType> createProvider(List<RoleAnalysisOutlierPartitionType> partition) {

        anomalyResultMap = new LoadableModel<>() {
            @Override
            protected ListMultimap<String, DetectedAnomalyResult> load() {
                return ArrayListMultimap.create();
            }
        };

        anomalyPartitionMap = new LoadableModel<>() {
            @Override
            protected ListMultimap<String, RoleAnalysisOutlierPartitionType> load() {
                return ArrayListMultimap.create();
            }
        };

        Set<String> anomalyOidSet = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : partition) {
            List<DetectedAnomalyResult> partitionAnalysis = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResult anomalyResult : partitionAnalysis) {
                String oid = anomalyResult.getTargetObjectRef().getOid();
                anomalyOidSet.add(oid);
                anomalyResultMap.getObject().put(oid, anomalyResult);
                anomalyPartitionMap.getObject().put(oid, outlierPartition);
            }
        }

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
        OperationResult result = task.getResult();

        List<RoleType> roles = new ArrayList<>();
        for (String oid : anomalyOidSet) {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
            if (rolePrismObject != null) {
                roles.add(rolePrismObject.asObjectable());
            }
        }

        return new SelectableBeanObjectDataProvider<>(
                RoleAnalysisDetectedAnomalyTable.this, Set.of()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleType> searchObjects(Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return roles.subList(offset, offset + maxSize);
            }

            @Override
            protected Integer countObjects(Class<RoleType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {
                return roles.size();
            }
        };
    }

    @Contract(" -> new")
    private @NotNull InlineMenuItem createRecertifyInlineMenu() {
        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.recertify.title")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        onRefresh(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                return RoleAnalysisDetectedAnomalyTable.this.getConfirmationMessageModel(action);
            }
        };
    }

    private IModel<String> getConfirmationMessageModel(@NotNull ColumnMenuAction<?> action) {
        if (action.getRowModel() == null) {
            return createStringResource("RoleAnalysisCandidateRoleTable.message.deleteActionAllObjects");
        } else {
            return createStringResource("RoleAnalysisCandidateRoleTable.message.deleteAction");
        }
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleType> getTable() {
        return (MainObjectListPanel<RoleType>) get(ID_DATATABLE);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    protected boolean isDeleteOperationEnabled() {
        return true;
    }

    private void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
            @NotNull String componentId,
            @NotNull Double density) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColor(pointsDensity);

        ProgressBar progressBar = new ProgressBar(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return pointsDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeAppender.append("style", "width: 170px"));
        cellItem.add(progressBar);
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    public IModel<List<RoleAnalysisOutlierPartitionType>> getPartitionModel() {
        return partitionModel;
    }

    public List<RoleAnalysisOutlierPartitionType> getPartitionModelObject() {
        return partitionModel.getObject();
    }

    public RoleAnalysisOutlierType getOutlierModelObject() {
        return outlierModel.getObject();
    }

    public RoleAnalysisOutlierPartitionType getPartitionSingleModelObject() {
        return getPartitionModelObject().get(0);
    }

    public ListMultimap<String, DetectedAnomalyResult> getAnomalyResultMapModelObject() {
        return anomalyResultMap.getObject();
    }

    public ListMultimap<String, RoleAnalysisOutlierPartitionType> getAnomalyPartitionMapModelObject() {
        return anomalyPartitionMap.getObject();
    }

}
