/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
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
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization.CategorySelectionProvider.createTableProvider;

public abstract class RoleAnalysisAbstractClassificationObjectPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";
    private static final String ID_SELECTION_PANEL = "selector";
    private static final String ID_FILTER_LABEL = "filter";
    private static final String ID_DISTRIBUTION_ROLE_PANEL = "distributionRole";
    private static final String ID_DISTRIBUTION_USER_PANEL = "distributionUser";
    private static final int ROLE_TAB_INDEX = 1;

    public record PanelOptions(boolean advanced, String rolesTitleId, String usersTitleId) {};

    LoadableModel<Boolean> isRoleSelectedModel = new LoadableModel<>() {
        @Contract(pure = true)
        @Override
        protected @NotNull Boolean load() {
            return false;
        }
    };

    LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel;

    private final PanelOptions options;

    protected RoleAnalysisAbstractClassificationObjectPanel(
            String id,
            PanelOptions options,
            ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config

    ) {
        super(id, model, config);
        this.options = options;
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

        WebMarkupContainer roleDistributionPanel = CategorizationValueModel.buildDistributionRolePanel(
                getPageBase(), true, getObjectDetailsModels().getObjectType(), ID_DISTRIBUTION_ROLE_PANEL,
                options.advanced(), options.rolesTitleId(), RoleType.class);
        roleDistributionPanel.setOutputMarkupId(true);
        roleDistributionPanel.add(new VisibleBehaviour(() -> isRoleSelectedModel.getObject()));
        container.add(roleDistributionPanel);

        WebMarkupContainer userDistributionPanel = CategorizationValueModel.buildDistributionRolePanel(
                getPageBase(), false, getObjectDetailsModels().getObjectType(), ID_DISTRIBUTION_USER_PANEL,
                options.advanced(), options.usersTitleId(), UserType.class);
        userDistributionPanel.setOutputMarkupId(true);
        userDistributionPanel.add(new VisibleBehaviour(() -> !isRoleSelectedModel.getObject()));
        container.add(userDistributionPanel);

        Label filterLabel = new Label(ID_FILTER_LABEL, createStringResource("RoleAnalysisObjectCategoryPanel.filter.label.text"));
        filterLabel.setOutputMarkupId(true);
        container.add(filterLabel);

        Select2MultiChoice<RoleAnalysisObjectCategorizationType> categorySelectionButton = createCategorySelectionButton();
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

                        isRoleSelectedModel.setObject(index == ROLE_TAB_INDEX);
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
        tabs.add(new PanelTab(getPageBase().createStringResource(options.usersTitleId()), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<FocusType> components = (MainObjectListPanel<FocusType>) buildRoleCategoryTable(panelId, session, selectionModel, UserType.class);
                components.setOutputMarkupId(true);
                return components;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource(options.rolesTitleId()), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<FocusType> components = (MainObjectListPanel<FocusType>) buildRoleCategoryTable(panelId, session, selectionModel, RoleType.class);
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

        List<RoleAnalysisObjectCategorizationType> allowedValues = CategorySelectionProvider.allowedValues(
                options.advanced(), isRoleSelectedModel);

        //TODO ugly hack remove later
        SelectableBeanObjectDataProvider<FocusType> selectableBeanObjectDataProvider = createTableProvider(this,
                selectionModel, options.advanced(), items, params, isRoleSelectedModel);

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
                            if (!allowedValues.contains(categorization)) {
                                continue;
                            }
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

            @Contract(value = "_ -> new", pure = true)
            @Override
            protected @NotNull List<Component> createToolbarButtonsList(String buttonId) {
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

    private @NotNull Select2MultiChoice<RoleAnalysisObjectCategorizationType> createCategorySelectionButton() {

        CategorySelectionProvider choiceProvider = new CategorySelectionProvider(options.advanced(), isRoleSelectedModel);
        Select2MultiChoice<RoleAnalysisObjectCategorizationType> multiselect = new Select2MultiChoice<>(
                RoleAnalysisAbstractClassificationObjectPanel.ID_SELECTION_PANEL,
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

    @Contract(value = " -> new", pure = true)
    private @NotNull LoadableModel<Collection<RoleAnalysisObjectCategorizationType>> initSelectedModel() {
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

}
