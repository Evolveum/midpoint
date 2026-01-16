/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.RepositoryShadowBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectsPanel.createReloadButton;

public abstract class SimulationDataSamplingPanel extends BasePanel<ResourceDetailsModel> implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationDataSamplingPanel.class);

    private static final String ID_BUTTONS = "buttons";
    protected static final String ID_RUN_SAMPLING = "runSampling";
    private static final String ID_NO = "no";

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_INFO_MESSAGE = "infoMessage";

    private static final String ID_SAMPLE_SIZE_CONTAINER = "sampleSizeContainer";
    private static final String ID_SAMPLE_SIZE_TITLE = "sampleSizeTitle";
    private static final String ID_SAMPLE_SIZE_DESCRIPTION = "sampleSizeDescription";
    private static final String ID_SAMPLE_SIZE_LABEL = "sampleSizeLabel";
    private static final String ID_SAMPLE_SIZE = "sampleSize";
    private static final String ID_SAMPLE_SIZE_PRESETS = "sampleSizePresets";
    private static final String ID_SAMPLE_SIZE_PRESENTS_ICON = "presetIcon";
    private static final String ID_PRESET_BTN = "presetBtn";
    private static final String ID_PRESET_LABEL = "presetLabel";

    Fragment footer;

    IModel<Integer> sampleSizeModel = Model.of();
    List<Integer> presets;
    ObjectQuery appliedQuery = null;

    {
        presets = new ArrayList<>();
        presets.add(50);
        presets.add(100);
        presets.add(300);
        presets.add(500);
        presets.add(1000);
        presets.add(null);
    }

    public SimulationDataSamplingPanel(String id, IModel<ResourceDetailsModel> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initFooter();
        Label subtitleLabel = createLabelComponent(ID_SUBTITLE, getSubtitleModel());
        subtitleLabel.setOutputMarkupId(true);
        add(subtitleLabel);

        initInfoMessages();
        initSampleSizeSection();

        initSearchTable();
    }

    private void initSampleSizeSection() {
        WebMarkupContainer container = new WebMarkupContainer(ID_SAMPLE_SIZE_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        container.add(createLabelComponent(ID_SAMPLE_SIZE_TITLE,
                createStringResource("SimulationDataSamplingPanel.sampleSize.title")));
        container.add(createLabelComponent(ID_SAMPLE_SIZE_DESCRIPTION,
                createStringResource("SimulationDataSamplingPanel.sampleSize.description")));

        container.add(new LabelWithHelpPanel(ID_SAMPLE_SIZE_LABEL,
                createStringResource("SimulationDataSamplingPanel.sampleSize.label")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("SimulationDataSamplingPanel.sampleSize.label.help");
            }
        });

        NumberTextField<Integer> sampleSize =
                new NumberTextField<>(ID_SAMPLE_SIZE, sampleSizeModel, Integer.class);
        sampleSize.setOutputMarkupId(true);
        sampleSize.setMinimum(0);

        sampleSize.add(AttributeModifier.replace(
                "placeholder",
                createStringResource("SimulationDataSamplingPanel.sampleSize.placeholder")
        ));

        container.add(sampleSize);

        sampleSize.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(container);
                target.add(getFooter());
            }
        });

        ListView<Integer> presetsView = buildChips(sampleSize, container);
        container.add(presetsView);
    }

    private @NotNull ListView<Integer> buildChips(NumberTextField<Integer> sampleSize, WebMarkupContainer container) {
        ListView<Integer> presetsView = new ListView<>(ID_SAMPLE_SIZE_PRESETS, presets) {
            @Override
            protected void populateItem(@NotNull ListItem<Integer> item) {
                Integer preset = item.getModelObject();

                AjaxLink<Void> btn = new AjaxLink<>(ID_PRESET_BTN) {

                    @Override
                    public void onClick(@NotNull AjaxRequestTarget target) {
                        sampleSize.clearInput();
                        sampleSizeModel.setObject(preset);
                        target.add(container);
                        target.add(getFooter());
                    }
                };

                btn.setOutputMarkupId(true);
                btn.add(AttributeModifier.replace("class", (IModel<String>) () -> {
                    boolean selected = compareSampleSizePreset(preset);
                    String base = "btn rounded-pill px-3 d-inline-flex align-items-center gap-2";
                    return selected
                            ? base + " btn-outline-primary border-2 selected-bg-primary"
                            : base + " btn-outline-secondary";
                }));

                WebMarkupContainer icon = new WebMarkupContainer(ID_SAMPLE_SIZE_PRESENTS_ICON);
                icon.setOutputMarkupPlaceholderTag(true);
                icon.add(new VisibleBehaviour(() -> compareSampleSizePreset(preset)));
                btn.add(icon);

                String label = preset != null ? preset.toString() :
                        createStringResource("SimulationDataSamplingPanel.sampleSize.preset.all").getObject();
                btn.add(new Label(ID_PRESET_LABEL, Model.of(label)));
                item.add(btn);
            }
        };

        presetsView.setOutputMarkupId(true);
        return presetsView;
    }

    public boolean compareSampleSizePreset(Integer preset) {
        Integer sampleSize = sampleSizeModel.getObject();
        if (preset == null && sampleSize == null) {
            return true;
        }
        return preset != null && preset.equals(sampleSize);
    }

    private void initInfoMessages() {
        RepeatingView repeatingView = new RepeatingView(ID_INFO_MESSAGE);
        List<IModel<String>> infoMessagesModels = getInfoMessagesModels();
        for (IModel<String> infoMessageModel : infoMessagesModels) {
            MessagePanel<?> infoMessagePanel = buildInfoMessagePanel(repeatingView.newChildId(), infoMessageModel);
            repeatingView.add(infoMessagePanel);
        }
        add(repeatingView);
    }

    protected List<IModel<String>> getInfoMessagesModels() {
        return List.of();
    }

    private @NotNull MessagePanel<?> buildInfoMessagePanel(String id, IModel<String> infoMessageModel) {
        MessagePanel<?> infoMessage = new MessagePanel<>(id, MessagePanel.MessagePanelType.INFO,
                infoMessageModel, false) {

            @Contract(pure = true)
            @Override
            protected @NotNull Object getIconTypeCss() {
                return "fa fa-info-circle";
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> createHeaderCss() {
                return Model.of("alert-info");
            }
        };
        infoMessage.setOutputMarkupId(true);
        infoMessage.add(new VisibleBehaviour(() -> infoMessageModel != null));
        return infoMessage;
    }

    protected StringResourceModel getSubtitleModel() {
        return createStringResource("SimulationDataSamplingPanel.subtitle");
    }

    /**
     * Creates a label component with common output markup settings.
     *
     * @param title The label model (typically a localized string resource)
     * @return Configured {@link Label} instance
     */
    private @NotNull Label createLabelComponent(String id, StringResourceModel title) {
        Label label = new Label(id, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);
        createNoButton(footer);
        createRunButton(footer);
        add(footer);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private void createNoButton(@NotNull Fragment footer) {
        AjaxButton noButton = new AjaxButton(ID_NO,
                createStringResource("SimulationDataSamplingPanel.cancelButton.label")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        footer.add(noButton);
    }

    protected abstract ResourceObjectTypeDefinition getObjectTypeDefinition();

    protected void createRunButton(@NotNull Fragment footer) {
        AjaxIconButton runButton = new AjaxIconButton(ID_RUN_SAMPLING, Model.of("fa fa-play"),
                createStringResource("SimulationDataSamplingPanel.runSimulationButton.label")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectQuery usedQueryIfChangesApplied = getUsedQueryIfChangesApplied();
                getPageBase().hideMainPopup(target);
                yesPerformed(target, getSampleSize(), usedQueryIfChangesApplied);
            }
        };
        runButton.showTitleAsLabel(true);
        footer.add(runButton);
    }

    private @Nullable ObjectQuery getResourceContentQuery() {
        ResourceObjectTypeDefinition objectType = getObjectTypeDefinition();

        try {
            return ObjectQueryUtil.createResourceAndKindIntent(
                    getModelObject().getObjectType().getOid(),
                    objectType.getKind(),
                    objectType.getIntent());
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot create query for resource content", e);
        }
        return null;
    }

    private void initSearchTable() {
        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        ShadowTablePanel shadowTablePanel = new ShadowTablePanel(ID_TABLE, null) {

            @Override
            protected UserProfileStorage.@Nullable TableId getTableId() {
                return null;
            }

            @Override
            public @Nullable PageStorage getPageStorage() {
                return null;
            }

            @Override
            public boolean displayIsolatedNoValuePanel() {
                return getDataProvider().size() == 0;
            }

            @Override
            protected StringResourceModel getNoValuePanelCustomSubTitleModel() {
                return createStringResource("SimulationDataSamplingPanel.noShadowsFound.reloadHint");
            }

            @Override
            protected boolean isFooterVisible(boolean defaultCondition) {
                return super.isFooterVisible(defaultCondition) && !displayIsolatedNoValuePanel();
            }

            @Override
            protected @NotNull ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                return SimulationDataSamplingPanel.this.createProvider(
                        getSearchModel(), (CompiledShadowCollectionView) getObjectCollectionView());

//                Load right from resource
//                var provider = createSelectableBeanObjectDataProvider(() -> getResourceContentQuery(), null,
//                        createSearchOptions());
//                provider.setEmptyListOnNullQuery(true);
//                provider.setSort(null);
//                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
//                return provider;
            }

            @Override
            protected CompiledShadowCollectionView findContainerPanelConfig() {
                ResourceType resource = SimulationDataSamplingPanel.this.getModelObject().getObjectType();
                return getPageBase().getCompiledGuiProfile()
                        .findShadowCollectionView(
                                resource.getOid(), getObjectTypeDefinition().getKind(), getObjectTypeDefinition().getIntent());
            }

            @Override
            protected boolean isSearchResultInfoVisible() {
                return !displayIsolatedNoValuePanel();
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected @NotNull Search createSearch() {
                Search search = super.createSearch();
                search.setSearchMode(SearchBoxModeType.AXIOM_QUERY);
                return search;
            }

            @Override
            public @NotNull String getTableContainerAdditionalCssClasses() {
                return "shadow-sample-table-overflow";
            }

            @Override
            protected @NotNull Component createHeader(String headerId) {
                Component header = super.createHeader(headerId);
                header.add(AttributeModifier.replace("class", ""));
                return header;
            }

            @Override
            protected boolean showTableAsCard() {
                return false;
            }

            @Override
            protected boolean isDeleteOnlyRepoShadowAllow() {
                return false;
            }

            @Override
            protected @NotNull SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setPanelType(CollectionPanelType.REPO_SHADOW);
                var objTypeDef = getObjectTypeDefinition();
                if (objTypeDef != null) {
                    searchContext.setDefinitionOverride(objTypeDef.getPrismObjectDefinition());
                }
                return searchContext;
            }

            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                if (compiledView != null) {
                    return compiledView;
                }
                return super.getObjectCollectionView();
            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return false;
            }

            @Override
            protected @NotNull @Unmodifiable List<InlineMenuItem> createInlineMenu() {
                return List.of();
            }

            @Override
            protected boolean isFulltextEnabled() {
                return false;
            }

            @Override
            protected @NotNull List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = new ArrayList<>();
                buttonsList.add(createReloadButton(
                        buttonId,
                        getPageBase(),
                        getShadowTable(),
                        SimulationDataSamplingPanel.this.getModelObject().getObjectType(),
                        () -> getObjectTypeDefinition(),
                        () -> getObjectTypeDefinition().getKind(),
                        false,
                        "btn btn-default btn-sm mr-2"));
                return buttonsList;
            }

            @Override
            protected boolean isPendingOperationsVisible() {
                return false;
            }

        };
        shadowTablePanel.setOutputMarkupId(true);
        shadowTablePanel.setOutputMarkupPlaceholderTag(true);
        form.add(shadowTablePanel);
    }

    public Integer getSampleSize() {
        return sampleSizeModel.getObject();
    }

    private ShadowTablePanel getShadowTable() {
        return (ShadowTablePanel) get(createComponentPath(ID_FORM, ID_TABLE));
    }

    protected final @NotNull RepositoryShadowBeanObjectDataProvider createProvider(
            IModel<Search<ShadowType>> searchModel, CompiledShadowCollectionView collection) {
        RepositoryShadowBeanObjectDataProvider provider = new RepositoryShadowBeanObjectDataProvider(
                getPageBase(), searchModel, null) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getResourceContentQuery();
            }

            @Override
            public ObjectQuery getQuery() {
                appliedQuery = super.getQuery();
                return appliedQuery;
            }
        };
        provider.setCompiledObjectCollectionView(collection);
        return provider;
    }

    private @Nullable ObjectQuery getUsedQueryIfChangesApplied() {
        boolean isAdditionalQuerySpecified = !appliedQuery.equivalent(getResourceContentQuery());
        return isAdditionalQuerySpecified ? appliedQuery : null;
    }

    private @NotNull Collection<SelectorOptions<GetOperationOptions>> createSearchOptions() {
        GetOperationOptionsBuilder builder = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATIONS).dontRetrieve();
        return builder.build();
    }

    public void yesPerformed(AjaxRequestTarget target, @Nullable Integer sampleSize, @Nullable ObjectQuery query) {
        //do nothing by default
    }

    @Override
    public int getWidth() {
        return 40;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("SimulationDataSamplingPanel.title");
    }

    @Override
    public Component getContent() {
        return SimulationDataSamplingPanel.this;
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa fa-flask");
    }
}
