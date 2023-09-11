/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getSessionOptionType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession.PARAM_IS_WIZARD;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.deleteSingleRoleAnalysisSession;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionStatisticType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysis", matchUrlForSecurity = "/admin/roleAnalysis")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                        label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                        description = "PageRoleAnalysis.auth.roleAnalysisAll.description")
        })

public class PageRoleAnalysis extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageRoleAnalysis.class.getName() + ".";
    private static final String OP_DELETE_SESSION = DOT_CLASS + "deleteSession";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageRoleAnalysis(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisSessionType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisSessionType>> selectedObjects = getTable().getSelectedObjects();
                        OperationResult result = new OperationResult(OP_DELETE_SESSION);
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisSessionType> selectableSession = selectedObjects.get(0);
                                deleteSingleRoleAnalysisSession((PageBase) getPage(), selectableSession.getValue().getOid(), result
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisSessionType>> rowModel = getRowModel();
                                deleteSingleRoleAnalysisSession((PageBase) getPage(), rowModel.getObject().getValue().getOid(), result
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisSessionType> selectedObject : selectedObjects) {
                                try {
                                    String parentOid = selectedObject.getValue().asPrismObject().getOid();
                                    deleteSingleRoleAnalysisSession((PageBase) getPage(), parentOid, result);

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        getTable().refreshTable(target);
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

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE,
                    () -> getString("PageRoleAnalysis.menu.nonNativeRepositoryWarning")));
            return;
        }
        MainObjectListPanel<RoleAnalysisSessionType> table = new MainObjectListPanel<>(ID_TABLE, RoleAnalysisSessionType.class) {

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(PageRoleAnalysis.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected IColumn<SelectableBean<RoleAnalysisSessionType>, String> createIconColumn() {
                return super.createIconColumn();
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> columns = new ArrayList<>();

                LoadableModel<PrismContainerDefinition<RoleAnalysisSessionType>> containerDefinitionModel
                        = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisSessionType.class);

                LoadableModel<PrismContainerDefinition<AbstractAnalysisSessionOptionType>> abstractContainerDefinitionModel
                        = WebComponentUtil.getContainerDefinitionModel(AbstractAnalysisSessionOptionType.class);

                IColumn<SelectableBean<RoleAnalysisSessionType>, String> column = new AbstractExportableColumn<>(createStringResource("RoleAnalysisProcessModeType.processMode")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, RoleAnalysisSessionType.F_PROCESS_MODE);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractProcessMode(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractProcessMode(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("AbstractAnalysisSessionOptionType.similarityThreshold")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, abstractContainerDefinitionModel,
                                AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractSimilarity(model)));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractSimilarity(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("AbstractAnalysisSessionOptionType.minPropertiesOverlap")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, abstractContainerDefinitionModel,
                                AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {

                        cellItem.add(new Label(componentId, extractMinPropertiesOverlap(model)));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractMinPropertiesOverlap(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("AbstractAnalysisSessionOptionType.propertiesRange")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, abstractContainerDefinitionModel,
                                AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractPropertiesRange(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractPropertiesRange(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("AbstractAnalysisSessionOptionType.minMembersCount")) {
                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, abstractContainerDefinitionModel,
                                AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractMinGroupOption(model)));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractMinGroupOption(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("RoleAnalysisSessionStatisticType.processedObjectCount")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                                RoleAnalysisSessionStatisticType.F_PROCESSED_OBJECT_COUNT));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractProcessedObjectCount(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractProcessedObjectCount(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("RoleAnalysisSessionStatisticType.meanDensity")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                                RoleAnalysisSessionStatisticType.F_MEAN_DENSITY));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {

                        RoleAnalysisSessionType value = model.getObject().getValue();
                        if (value != null
                                && value.getSessionStatistic() != null
                                && value.getSessionStatistic().getMeanDensity() != null) {

                            String meanDensity = new DecimalFormat("#.###")
                                    .format(Math.round(value.getSessionStatistic().getMeanDensity() * 1000.0) / 1000.0);

                            String colorClass = densityBasedColor(meanDensity);

                            Label label = new Label(componentId, meanDensity + " (%)");
                            label.setOutputMarkupId(true);
                            label.add(new AttributeModifier("class", colorClass));
                            label.add(AttributeModifier.append("style", "width: 100px;"));

                            cellItem.add(label);
                        } else {
                            cellItem.add(new EmptyPanel(componentId));
                        }

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractMeanDensity(rowModel);
                    }

                };
                columns.add(column);

                return columns;
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation,
                    CompiledObjectCollectionView collectionView) {

                PageParameters parameters = new PageParameters();
                parameters.add(PARAM_IS_WIZARD, true);
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);

            }

            @Override
            protected TableId getTableId() {
                return TableId.TABLE_USERS;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("PageMining.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "PageMining.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "PageMining.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

    }

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
            LoadableModel<PrismContainerDefinition<C>> containerDefinitionModel,
            ItemPath itemPath) {
        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                containerDefinitionModel,
                itemPath,
                (PageBase) getPage())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };
    }

    private static IModel<String> extractProcessMode(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        if (model.getObject() != null) {
            RoleAnalysisSessionType value = model.getObject().getValue();
            if (value != null
                    && value.getProcessMode() != null) {
                return Model.of(value.getProcessMode().value());
            }

        }
        return Model.of("");
    }

    private static IModel<String> extractSimilarity(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        AbstractAnalysisSessionOptionType sessionOptionType = null;
        if (model.getObject().getValue() != null) {
            sessionOptionType = getSessionOptionType(model.getObject().getValue());
        }

        if (sessionOptionType != null && sessionOptionType.getSimilarityThreshold() != null) {
            return Model.of(sessionOptionType.getSimilarityThreshold() + " (%)");
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractMinPropertiesOverlap(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        AbstractAnalysisSessionOptionType sessionOptionType = null;
        if (model.getObject().getValue() != null) {
            sessionOptionType = getSessionOptionType(model.getObject().getValue());
        }

        if (sessionOptionType != null && sessionOptionType.getMinPropertiesOverlap() != null) {
            return Model.of(sessionOptionType.getMinPropertiesOverlap().toString());
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractPropertiesRange(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        AbstractAnalysisSessionOptionType sessionOptionType = null;
        if (model.getObject().getValue() != null) {
            sessionOptionType = getSessionOptionType(model.getObject().getValue());
        }

        if (sessionOptionType != null && sessionOptionType.getPropertiesRange() != null) {
            RangeType propertiesRange = sessionOptionType.getPropertiesRange();
            return Model.of("from " + propertiesRange.getMin()
                    + " to "
                    + propertiesRange.getMax());
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractMinGroupOption(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        AbstractAnalysisSessionOptionType sessionOptionType = null;
        if (model.getObject().getValue() != null) {
            sessionOptionType = getSessionOptionType(model.getObject().getValue());
        }

        if (sessionOptionType != null && sessionOptionType.getMinMembersCount() != null) {
            return Model.of(sessionOptionType.getMinMembersCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractProcessedObjectCount(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getProcessedObjectCount() != null) {
            return Model.of(value.getSessionStatistic().getProcessedObjectCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractMeanDensity(IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getMeanDensity() != null) {

            String meanDensity = new DecimalFormat("#.###")
                    .format(Math.round(value.getSessionStatistic().getMeanDensity() * 1000.0) / 1000.0);

            return Model.of(meanDensity + " (%)");
        } else {
            return Model.of("");
        }
    }

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }

    private MainObjectListPanel<RoleAnalysisSessionType> getTable() {
        return (MainObjectListPanel<RoleAnalysisSessionType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }
}
