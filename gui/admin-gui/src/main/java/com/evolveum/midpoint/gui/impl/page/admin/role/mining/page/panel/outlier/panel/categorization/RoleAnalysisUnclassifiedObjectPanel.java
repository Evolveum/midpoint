/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierDashboardPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.Select2MultiChoice;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

@PanelType(name = "unclassified-objects")
@PanelInstance(
        identifier = "unclassified-objects",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.unclassified.objects",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 40
        )
)
public class RoleAnalysisUnclassifiedObjectPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";
    private static final String ID_SELECTION_PANEL = "selector";
    private static final String ID_FILTER_LABEL = "filter";
    private static final String ID_DISTRIBUTION_ROLE_PANEL = "distributionRole";
    private static final String ID_DISTRIBUTION_USER_PANEL = "distributionUser";

    boolean isRoleSelected = true;

    LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel;

    public RoleAnalysisUnclassifiedObjectPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        selectionModel = new LoadableModel<>(false) {
            @Override
            protected List<RoleAnalysisObjectCategorizationType> load() {
                return List.of();
            }
        };

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        WebMarkupContainer roleDistributionPanel = initDistributionRolePanel(
                getObjectDetailsModels().getObjectType(), ID_DISTRIBUTION_ROLE_PANEL, RoleType.class);
        roleDistributionPanel.setOutputMarkupId(true);
        roleDistributionPanel.add(new VisibleBehaviour(() -> isRoleSelected));
        container.add(roleDistributionPanel);

        WebMarkupContainer userDistributionPanel = initDistributionRolePanel(
                getObjectDetailsModels().getObjectType(), ID_DISTRIBUTION_USER_PANEL, UserType.class);
        userDistributionPanel.setOutputMarkupId(true);
        userDistributionPanel.add(new VisibleBehaviour(() -> !isRoleSelected));
        container.add(userDistributionPanel);

        Label filterLabel = new Label(ID_FILTER_LABEL, createStringResource("RoleAnalysisObjectCategoryPanel.filter.label.text"));
        filterLabel.setOutputMarkupId(true);
        container.add(filterLabel);

        Select2MultiChoice<RoleAnalysisObjectCategorizationType> categorySelectionButton = createCategorySelectionButton(ID_SELECTION_PANEL);
        categorySelectionButton.setOutputMarkupId(true);
        container.add(categorySelectionButton);

        ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();

        LoadableModel<List<ITab>> tabsModel = new LoadableModel<>(false) {
            @Override
            protected List<ITab> load() {
                return createTabs(session, selectionModel);
            }
        };
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_PANEL, tabsModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Contract("_, _ -> new")
            @Override
            protected @NotNull WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        isRoleSelected = index == 0;
                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        assert target != null;
                        target.add(getPageBase().getFeedbackPanel());
                        target.add(container);
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeModifier.append(CLASS_CSS, "p-0 m-0"));
        container.add(tabPanel);

    }

    protected List<ITab> createTabs(RoleAnalysisSessionType session, LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel) {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(getPageBase().createStringResource("Roles categorization"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<FocusType> components = (MainObjectListPanel<FocusType>) buildRoleCategoryTable(panelId, session, selectionModel, RoleType.class);
                components.setOutputMarkupId(true);
                return components;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Users categorization"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<FocusType> components = (MainObjectListPanel<FocusType>) buildRoleCategoryTable(panelId, session, selectionModel, UserType.class);
                components.setOutputMarkupId(true);
                return components;
            }
        });
        return tabs;
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public WebMarkupContainer buildRoleCategoryTable(String panelId, @NotNull RoleAnalysisSessionType session, LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel, Class<?> typeClass) {

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = session.getIdentifiedCharacteristics();
        if (identifiedCharacteristics == null || identifiedCharacteristics.getRoles() == null) {
            return new WebMarkupContainer(panelId);
        }
        List<RoleAnalysisIdentifiedCharacteristicsItemType> items;
        if (typeClass.equals(RoleType.class)) {
            RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
            items = roles.getItem();
            if (items == null) {
                return new WebMarkupContainer(panelId);
            }
        } else {
            RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
            items = users.getItem();
            if (items == null) {
                return new WebMarkupContainer(panelId);
            }
        }

        Map<String, List<RoleAnalysisObjectCategorizationType>> params = new HashMap<>();

        //TODO ugly hack remove later
        SelectableBeanObjectDataProvider<FocusType> selectableBeanObjectDataProvider = new SelectableBeanObjectDataProvider<>(
                this, Set.of()) {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected List<?> searchObjects(Class type, ObjectQuery query, Collection collection, Task task, OperationResult result) {

                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                Integer end = offset + maxSize;

                List<FocusType> objects = new ArrayList<>();
                int counter = 0;
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                for (RoleAnalysisIdentifiedCharacteristicsItemType item : items) {
                    List<RoleAnalysisObjectCategorizationType> category = item.getCategory();

                    if (selectionModel.getObject() != null
                            && !selectionModel.getObject().isEmpty()
                            && (category == null || !new HashSet<>(category).containsAll(selectionModel.getObject()))) {
                        continue;
                    }
                    counter++;

                    params.put(item.getObjectRef().getOid(), item.getCategory());

                    if (counter >= offset) {
                        PrismObject<FocusType> focusTypeObject = roleAnalysisService.getFocusTypeObject(item.getObjectRef().getOid(), task, result);
                        if (focusTypeObject != null) {
                            objects.add(focusTypeObject.asObjectable());
                        } else {
                            counter--;
                        }
                    }

                    if (counter >= end) {
                        break;
                    }
                }

                return objects;
            }

            @Override
            protected Integer countObjects(Class<FocusType> type, ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) {
                int count = 0;
                for (RoleAnalysisIdentifiedCharacteristicsItemType item : items) {
                    List<RoleAnalysisObjectCategorizationType> category = item.getCategory();
                    if (selectionModel.getObject() != null
                            && !selectionModel.getObject().isEmpty()
                            && (category == null || !new HashSet<>(category).containsAll(selectionModel.getObject()))) {
                        continue;
                    }

                    count++;
                }

                return count;
            }
        };

        MainObjectListPanel<FocusType> table = new MainObjectListPanel<>(panelId, FocusType.class, null) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected boolean showTableAsCard() {
                return false;
            }

            @Override
            protected IColumn<SelectableBean<FocusType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<FocusType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<FocusType>, String>> columns = super.createDefaultColumns();
                IColumn<SelectableBean<FocusType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.status")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<FocusType>> iModel) {
                        return Model.of("");
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisCluster.table.header.cluster.state")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisCluster.table.header.cluster.state.help");
                            }
                        };

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<FocusType>>> cellItem,
                            String componentId, IModel<SelectableBean<FocusType>> model) {
                        if (model == null || model.getObject() == null || model.getObject().getValue() == null) {
                            cellItem.add(new EmptyPanel(componentId));
                            return;
                        }

                        List<RoleAnalysisObjectCategorizationType> roleAnalysisObjectCategorizationTypes = params
                                .get(model.getObject().getValue().getOid());
                        if (roleAnalysisObjectCategorizationTypes == null || roleAnalysisObjectCategorizationTypes.isEmpty()) {
                            cellItem.add(new EmptyPanel(componentId));
                            return;
                        }

                        RepeatingView repeatingView = new RepeatingView(componentId);

                        for (RoleAnalysisObjectCategorizationType categorization : roleAnalysisObjectCategorizationTypes) {
                            Label label = new Label(repeatingView.newChildId(), categorization.value());
                            label.add(AttributeModifier.append("class", "badge badge-info mr-1"));
                            repeatingView.add(label);
                        }

                        cellItem.add(repeatingView);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Class<FocusType> getDefaultType() {
                return (Class<FocusType>) typeClass;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<FocusType>> createProvider() {
                return selectableBeanObjectDataProvider;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }
        };

        table.setOutputMarkupId(true);
        return table;
    }

    private Select2MultiChoice<RoleAnalysisObjectCategorizationType> createCategorySelectionButton(String buttonId) {

        CategorySelectionProvider choiceProvider = new CategorySelectionProvider();
        Select2MultiChoice<RoleAnalysisObjectCategorizationType> multiselect = new Select2MultiChoice<>(buttonId,
                initSelectedModel(),
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<RoleAnalysisObjectCategorizationType> object = multiselect.getModel().getObject();
                selectionModel.setObject(new ArrayList<>(object));
                target.add(getTabPanel());
                target.add(getTabPanel().getParent());
            }
        });

        return multiselect;
    }

    private LoadableModel<Collection<RoleAnalysisObjectCategorizationType>> initSelectedModel() {
        return new LoadableModel<>(false) {

            @Override
            protected Collection<RoleAnalysisObjectCategorizationType> load() {
                return selectionModel.getObject();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private RoleAnalysisTabbedPanel<ITab> getTabPanel() {
        return (RoleAnalysisTabbedPanel<ITab>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_PANEL));
    }

    private @NotNull WebMarkupContainer initDistributionRolePanel(@NotNull RoleAnalysisSessionType session, String panelId, Class<?> typeClass) {

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = session.getIdentifiedCharacteristics();
        if (identifiedCharacteristics == null || identifiedCharacteristics.getRoles() == null) {
            return new WebMarkupContainer(panelId);
        }

        String title;
        RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer;
        if (typeClass.equals(RoleType.class)) {
            title = "Role classification";
            itemContainer = identifiedCharacteristics.getRoles();
        } else {
            title = "User classification";
            itemContainer = identifiedCharacteristics.getUsers();
        }

        List<RoleAnalysisIdentifiedCharacteristicsItemType> items = itemContainer.getItem();

        if (items == null) {
            return new WebMarkupContainer(panelId);
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        List<ProgressValueModel> progressValueModels = prepareDistributionCatalog(itemContainer, isRoleSelected);

        RoleAnalysisOutlierDashboardPanel<?> distributionHeader = new RoleAnalysisOutlierDashboardPanel<>(panelId,
                createStringResource(title)) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected boolean isFooterVisible() {
                return false;
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {

                    @Override
                    protected @NotNull Component getPanelComponent(String id) {
                        ProgressBarPanel components = new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                List<ProgressBar> progressBars = new ArrayList<>();
                                for (ProgressValueModel progressValueModel : progressValueModels) {
                                    if (progressValueModel.isVisible()) {
                                        progressBars.add(progressValueModel.buildProgressBar());
                                    }
                                }
                                return progressBars;
                            }
                        });
                        components.setOutputMarkupId(true);
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-0 px-0";
                    }

                    @Override
                    protected @NotNull Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);

                        for (ProgressValueModel valueModel : progressValueModels) {
                            if (!valueModel.isVisible()) {
                                continue;
                            }
                            MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                                @Contract("_ -> new")
                                @Override
                                protected @NotNull Component getTitleComponent(String id) {
                                    return new IconWithLabel(id, Model.of(valueModel.getTitle())) {
                                        @Override
                                        protected String getIconCssClass() {
                                            return "fa fa-circle fa-2xs align-middle " + valueModel.getTextClass();
                                        }

                                        @Override
                                        protected String getIconComponentCssStyle() {
                                            return "font-size:8px;margin-bottom:2px;";
                                        }

                                        @Override
                                        protected String getLabelComponentCssClass() {
                                            return "txt-toned";
                                        }

                                        @Override
                                        protected String getComponentCssClass() {
                                            return super.getComponentCssClass() + "mb-1 gap-2";
                                        }
                                    };
                                }

                                @Contract("_ -> new")
                                @Override
                                protected @NotNull Component getValueComponent(String id) {
                                    Label label = new Label(id, valueModel.getIntegerValue());
                                    label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                    label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
                                    return label;
                                }
                            };
                            resolved.setOutputMarkupId(true);
                            view.add(resolved);
                        }

                        return view;

                    }
                };
                panel.setOutputMarkupId(true);
                panel.add(AttributeModifier.append(CLASS_CSS, "col-12"));
                return panel;
            }
        };

        distributionHeader.setOutputMarkupId(true);
        return distributionHeader;

    }

    private double calculatePercentage(int value, int total) {
        if (total == 0) {
            return 0;
        }
        if (value == 0) {
            return 0;
        }
        return value * 100 / (double) total;
    }

    static class ProgressValueModel implements Serializable {
        int integerValue;
        double doubleValue;
        String title;
        String textClass;
        boolean visible;
        ProgressBar.State progressBarColor;

        public ProgressValueModel(
                boolean visible,
                int integerValue,
                double doubleValue,
                ProgressBar.State progressBarColor,
                String title,
                String textClass) {
            this.integerValue = integerValue;
            this.doubleValue = doubleValue;
            this.title = title;
            this.textClass = textClass;
            this.progressBarColor = progressBarColor;
            this.visible = visible;
        }

        public int getIntegerValue() {
            return integerValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public String getTitle() {
            return title;
        }

        public String getTextClass() {
            return textClass;
        }

        public ProgressBar.State getProgressBarColor() {
            return progressBarColor;
        }

        public boolean isVisible() {
            return visible;
        }

        public ProgressBar buildProgressBar() {
            return new ProgressBar(getDoubleValue(), getProgressBarColor());
        }
    }

    private @NotNull List<ProgressValueModel> prepareDistributionCatalog(
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            boolean isRoleSelected) {
        int unPopular = itemContainer.getUnPopularCount();
        int noiseExclusive = itemContainer.getNoiseExclusiveCount();
        int anomalyExclusive = itemContainer.getAnomalyExclusiveCount();

        int total = unPopular + noiseExclusive;
        if (isRoleSelected) {
            total += anomalyExclusive;
        }

        List<ProgressValueModel> progressValues = new ArrayList<>();
        progressValues.add(new ProgressValueModel(
                true, noiseExclusive, calculatePercentage(noiseExclusive, total),
                ProgressBar.State.WARNING, "Noise exclusive", "text-warning"));
        progressValues.add(new ProgressValueModel(true, unPopular, calculatePercentage(unPopular, total),
                ProgressBar.State.DANGER, "Unpopular", "text-danger"));
        if (isRoleSelected) {
            progressValues.add(new ProgressValueModel(true, anomalyExclusive, calculatePercentage(anomalyExclusive, total),
                    ProgressBar.State.INFO, "Anomaly exclusive", "text-info"));
        }

        progressValues.sort(Comparator.comparing(ProgressValueModel::getDoubleValue).reversed());
        return progressValues;
    }

}
