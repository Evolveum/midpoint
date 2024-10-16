/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisMultiplePartitionAnomalyResultTabPopup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisSinglePartitionAnomalyResultTabPopup;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedAnomalyTable extends BasePanel<String> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisDetectedAnomalyTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    IModel<RoleAnalysisOutlierType> outlierModel;
    IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel;
    IModel<ListMultimap<String, DetectedAnomalyResult>> anomalyResultMap;
    IModel<ListMultimap<String, RoleAnalysisOutlierPartitionType>> anomalyPartitionMap;

    Set<String> anomalyOidSet;
    AnomalyTableCategory category;

    public RoleAnalysisDetectedAnomalyTable(String id,
            @NotNull RoleAnalysisOutlierType outlier, RoleAnalysisOutlierPartitionType partition, AnomalyTableCategory category) {
        super(id);

        this.category = category;
        initModels(outlier, partition);

        createTable();
    }

    private void initModels(@NotNull RoleAnalysisOutlierType outlier, RoleAnalysisOutlierPartitionType partition) {
        outlierModel = new LoadableModel<>() {
            @Override
            protected RoleAnalysisOutlierType load() {
                return outlier;
            }
        };

        if (category == AnomalyTableCategory.PARTITION_ANOMALY && partition != null) {
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
                    return outlier.getPartition();
                }
            };
        }

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

        anomalyOidSet = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : getPartitionModelObject()) {
            List<DetectedAnomalyResult> partitionAnalysis = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResult anomalyResult : partitionAnalysis) {
                String oid = anomalyResult.getTargetObjectRef().getOid();
                anomalyOidSet.add(oid);
                anomalyResultMap.getObject().put(oid, anomalyResult);
                anomalyPartitionMap.getObject().put(oid, outlierPartition);
            }
        }
    }

    private void createTable() {

        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0 elevation-0";
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull @Unmodifiable List<Component> createToolbarButtonsList(String buttonId) {
                if (category.equals(AnomalyTableCategory.OUTLIER_OVERVIEW)) {
                    return List.of();
                }
                return List.of(RoleAnalysisDetectedAnomalyTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            protected void addBasicActions(List<InlineMenuItem> menuItems) {
                //TODO TBD
            }

            @Override
            protected String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm";
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected boolean isPagingVisible() {
                if (category == AnomalyTableCategory.OUTLIER_OVERVIEW) {
                    return false;
                }
                return super.isPagingVisible();
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();

                if (category == AnomalyTableCategory.PARTITION_ANOMALY || category == AnomalyTableCategory.OUTLIER_OVERVIEW) {
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsMenu());
                    return menuItems;
                }
//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createMarkInlineMenu());
//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createRecertifyInlineMenu());
//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createDeleteInlineMenu());

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
            protected @NotNull ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return RoleAnalysisDetectedAnomalyTable.this.createProvider();
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {
                return category.generateConfiguration(getOutlierModel(),
                        getPageBase(), getAnomalyResultMapModelObject(), getAnomalyPartitionMapModelObject());
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private @NotNull AjaxIconButton createRefreshButton(String buttonId) {
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

    private SelectableBeanObjectDataProvider<RoleType> createProvider() {

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
        OperationResult result = task.getResult();

        List<RoleType> roles = new ArrayList<>();
        if (category.equals(AnomalyTableCategory.OUTLIER_ACESS)) {
            ObjectReferenceType targetObjectRef = outlierModel.getObject().getObjectRef();
            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(targetObjectRef.getOid(), task, result);
            if (userTypeObject != null) {
                UserType user = userTypeObject.asObjectable();
                user.getAssignment().forEach(assignment -> {
                    if (assignment.getTargetRef().getType().equals(RoleType.COMPLEX_TYPE)) {
                        PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(assignment.getTargetRef().getOid(), task, result);
                        if (rolePrismObject != null) {
                            roles.add(rolePrismObject.asObjectable());
                        }
                    }
                });
                user.getRoleMembershipRef().forEach(roleMembershipRef -> {
                    if (roleMembershipRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                        PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(roleMembershipRef.getOid(), task, result);
                        if (rolePrismObject != null) {
                            roles.add(rolePrismObject.asObjectable());
                        }
                    }
                });
            }
        } else {
            for (String oid : anomalyOidSet) {
                PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
                if (rolePrismObject != null) {
                    roles.add(rolePrismObject.asObjectable());
                }
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

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleType> getTable() {
        return (MainObjectListPanel<RoleType>) get(ID_DATATABLE);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected void onRefresh(AjaxRequestTarget target) {

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

    @Contract(" -> new")
    private @NotNull InlineMenuItem createViewDetailsMenu() {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.view.details.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (category.equals(AnomalyTableCategory.OUTLIER_OVERVIEW)) {
                            RoleAnalysisMultiplePartitionAnomalyResultTabPopup detailsPanel =
                                    new RoleAnalysisMultiplePartitionAnomalyResultTabPopup(
                                            ((PageBase) getPage()).getMainPopupBodyId(),
                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected List<RoleAnalysisOutlierPartitionType> load() {
                                            return getPartitionModelObject();
                                        }
                                    }
                                    ,

                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected DetectedAnomalyResult load() {
                                            //TODO
                                            return getAnomalyResultMapModelObject()
                                                    .get(getRowModel().getObject().getValue().getOid()).get(0);
                                        }
                                    },
                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected RoleAnalysisOutlierType load() {
                                            return getOutlierModelObject();
                                        }
                                    }

                            );
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        } else {
                            RoleAnalysisSinglePartitionAnomalyResultTabPopup detailsPanel =
                                    new RoleAnalysisSinglePartitionAnomalyResultTabPopup(
                                            ((PageBase) getPage()).getMainPopupBodyId(),
                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected RoleAnalysisOutlierPartitionType load() {
                                            return getPartitionSingleModelObject();
                                        }
                                    }
                                    ,

                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected DetectedAnomalyResult load() {
                                            //TODO
                                            return getAnomalyResultMapModelObject()
                                                    .get(getRowModel().getObject().getValue().getOid()).get(0);
                                        }
                                    },
                                    new LoadableDetachableModel<>() {
                                        @Override
                                        protected RoleAnalysisOutlierType load() {
                                            return getOutlierModelObject();
                                        }
                                    }

                            );
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }
                    }
                };
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };
    }

    private @NotNull InlineMenuItem createRecertifyInlineMenu() {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.recertify.title")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_RECYCLE);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        //TODO
                    }
                };
            }
        };
    }

    @Contract(" -> new")
    private @NotNull InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-minus-circle");
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisOutlierType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        //TODO
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return getTable().getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
    }

}
