/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.mark.component.MarksOfObjectListPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsProvider;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.TitleWithMarks;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND;

//TODO remove duplication with ProcessedObjectsPanel
public abstract class CorrelationProcessedObjectPanel
        extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    private static final String DOT_CLASS = ProcessedObjectsPanel.class.getName() + ".";
    private static final String OPERATION_MARK_OBJECT = DOT_CLASS + "markObject";

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationProcessedObjectPanel.class);

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<List<MarkType>> availableMarksModel;
    IModel<CorrelationDefinitionType> correlationDefinitionModel = Model.of();
    Map<ItemPath, ItemPath> shadowCorrelationPathMap;
    String markOidForSearch = MARK_SHADOW_CORRELATION_OWNER_FOUND.value();

    public CorrelationProcessedObjectPanel(String id, IModel<List<MarkType>> availableMarksModel) {
        super(id, SimulationResultProcessedObjectType.class);
        this.availableMarksModel = availableMarksModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        loadCorrelationDefinition();
        loadCorrelationPathMap();
    }

    @Override
    public String getAdditionalBoxCssClasses() {
        return super.getAdditionalBoxCssClasses() + " table-td-middle";
    }

    private void loadCorrelationPathMap() {
        var mappings = findCandidateMappings(getPageBase(), getSimulationResultModel().getObject());
        shadowCorrelationPathMap = getShadowCorrelationPathMap(correlationDefinitionModel.getObject(), mappings);
    }

    private void loadCorrelationDefinition() {
        var correlationDefinition = findUsedCorrelationDefinition(getPageBase(), getSimulationResultModel().getObject());
        correlationDefinitionModel.setObject(correlationDefinition);
    }

    protected String getMarkOidForSearch() {
        return null;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return createRowMenuItems();
    }

    private @NotNull List<InlineMenuItem> createRowMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.markProtected"), true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-fw " + GuiStyleConstants.CLASS_SHADOW_ICON_PROTECTED);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return getMarkMenuAction(SystemObjectsType.MARK_PROTECTED);
            }
        });

        items.add(markInlineMenuAction("pageContentAccounts.menu.markDoNotTouch", SystemObjectsType.MARK_DO_NOT_TOUCH));
        items.add(markInlineMenuAction("pageContentAccounts.menu.markCorrelateLater", SystemObjectsType.MARK_CORRELATE_LATER));
        items.add(markInlineMenuAction("pageContentAccounts.menu.markInvalidData", SystemObjectsType.MARK_INVALID_DATA));
        items.add(modifyMarkInlineMenuAction());
        return items;
    }

    private @NotNull InlineMenuItem markInlineMenuAction(final String key, final SystemObjectsType mark) {
        return new InlineMenuItem(createStringResource(key), true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return getMarkMenuAction(mark);
            }
        };
    }

    private ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>> getMarkMenuAction(SystemObjectsType mark) {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                markObjects(getRowModel(), Collections.singletonList(mark.value()), target);
            }
        };
    }

    public InlineMenuItem modifyMarkInlineMenuAction() {
        return new InlineMenuItem(createStringResource("MainObjectListPanel.menu.modifyMark"), true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {

                        IModel<SelectableBean<SimulationResultProcessedObjectType>> selected = getRowModel();
                        if (selected == null) {
                            warn(getString("MainObjectListPanel.message.noFocusSelected"));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        LoadableDetachableModel<PrismObjectWrapper<? extends ObjectType>> focusModel = loadWrapper(selected.getObject().getValue());

                        if (focusModel.getObject() == null) {
                            warn(getString("ProcessedObjectsPanel.message.noObjectFound", selected.getObject().getValue().getOid()));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        MarksOfObjectListPopupPanel<?> popup = new MarksOfObjectListPopupPanel(
                                getPageBase().getMainPopupBodyId(), focusModel) {

                            @Override
                            protected void onSave(AjaxRequestTarget target) {
                                refreshTable(target);
                            }
                        };

                        getPageBase().showMainPopup(popup, target);
                    }
                };
            }
        };
    }

    private @NotNull LoadableDetachableModel<PrismObjectWrapper<? extends ObjectType>> loadWrapper(
            SimulationResultProcessedObjectType resultProcessedObjectType) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismObjectWrapper<? extends ObjectType> load() {
                if (resultProcessedObjectType == null) {
                    return null;
                }

                Task task = getPageBase().createSimpleTask("createWrapper");

                Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                        .noFetch()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_MARK_REF)).resolve()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_LIFECYCLE_STATE)).resolve()
                        .build();

                try {
                    PrismObject prismObject = WebModelServiceUtils.loadObject(
                            ObjectTypes.getObjectTypeClass(resultProcessedObjectType.getType()), resultProcessedObjectType.getOid(), options, getPageBase(), task, task.getResult());

                    if (prismObject == null) {
                        return null;
                    }

                    PrismObjectWrapperFactory<? extends ObjectType> factory = getPageBase().findObjectWrapperFactory(prismObject.getDefinition());
                    OperationResult result = task.getResult();
                    WrapperContext ctx = new WrapperContext(task, result);
                    ctx.setCreateIfEmpty(true);

                    return factory.createObjectWrapper(prismObject, ItemStatus.NOT_CHANGED, ctx);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create object wrapper for " + resultProcessedObjectType, e);
                }
                return null;
            }
        };
    }

    private List<SimulationResultProcessedObjectType> getSelectedObjects(
            IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        if (rowModel != null) {
            return Collections.singletonList(rowModel.getObject().getValue());
        }

        return getSelectedRealObjects();
    }

    private void markObjects(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel, List<String> markOids,
            AjaxRequestTarget target) {

        List<SimulationResultProcessedObjectType> selected = getSelectedObjects(rowModel);
        PageBase page = getPageBase();

        if (selected == null || selected.isEmpty()) {
            page.warn(getString("ResourceContentPanel.message.markShadowPerformed.warning"));
            target.add(page.getFeedbackPanel());
            return;
        }

        Task task = page.createSimpleTask(OPERATION_MARK_OBJECT);
        OperationResult result = task.getResult();

        for (var object : selected) {
            if (ObjectProcessingStateType.ADDED.equals(object.getState())) {
                // skip object, since it is added
                continue;
            }

            // We recreate statements (can not reuse them between multiple objects - we can create new or clone
            // but for each delta we need separate statement
            List<PolicyStatementType> statements = new ArrayList<>();
            for (String oid : markOids) {
                statements.add(new PolicyStatementType().markRef(oid, MarkType.COMPLEX_TYPE)
                        .type(PolicyStatementTypeType.APPLY));
            }

            try {
                @SuppressWarnings("unchecked")
                var type = (Class<? extends ObjectType>) page.getPrismContext().getSchemaRegistry()
                        .getCompileTimeClassForObjectType(object.getType());
                var delta = page.getPrismContext().deltaFactory().object()
                        .createModificationAddContainer(type,
                                object.getOid(), ObjectType.F_POLICY_STATEMENT,
                                statements.toArray(new PolicyStatementType[0]));
                page.getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
            } catch (Exception e) {
                result.recordPartialError(
                        createStringResource(
                                "ProcessedObjectsPanel.message.markObjectError", object)
                                .getString(),
                        e);
                LOGGER.error("Could not mark object {} with marks {}", object, markOids, e);
            }
        }

        result.computeStatusIfUnknown();
        page.showResult(result);

        refreshTable(target);
        target.add(page.getFeedbackPanel());
    }

    private void markObjects(IModel<SelectableBean<SimulationResultProcessedObjectType>> model, AjaxRequestTarget target) {
        markObjects(model, Collections.singletonList(SystemObjectsType.MARK_PROTECTED.value()), target);
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        this.markOidForSearch = getMarkOidForSearch();
        SearchContext ctx = new SearchContext();
        List<DisplayableValue<String>> values = createSearchValuesForAvailableMarks();
        ctx.setAvailableEventMarks(values);
        ctx.setSelectedEventMark(markOidForSearch);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Serializable> Search<T> loadSearch(PageStorage storage) {
        Search<T> search = null;

        if (markOidForSearch != null && storage != null && markOidForSearch.equals(getMarkOidForSearch())) {
            search = storage.getSearch();
        }

        if (!isUseStorageSearch(search)) {
            search = createSearch();
        }
        return search;
    }

    private List<DisplayableValue<String>> createSearchValuesForAvailableMarks() {
        return availableMarksModel.getObject().stream()
                .map(o -> new DisplayableValueImpl<>(
                        o.getOid(),
                        WebComponentUtil.getDisplayNameOrName(o.asPrismObject()),
                        o.getDescription()))
                .sorted(Comparator.comparing(DisplayableValueImpl::getLabel, Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_CORRELATION_PROCESSED_OBJECTS;
    }

    @Override
    protected boolean isCollapsableTable() {
        return true;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createCheckboxColumn() {
        return null; // Disable selection for now. Do we want to support actions (e.g. marking)?
    }

    @Override
    protected @NotNull Component createCollapsibleContent(
            String id,
            @NotNull IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        ProcessedObject<?> processedObject = SimulationsGuiUtil
                .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());
        CorrelationCandidatePanel components = new CorrelationCandidatePanel(id,
                () -> processedObject,
                CorrelationProcessedObjectPanel.this.getSimulationResultModel(),
                correlationDefinitionModel,
                shadowCorrelationPathMap);
        components.setOutputMarkupId(true);
        return components;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createNameColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        displayModel = displayModel == null ? createStringResource("ProcessedObjectsPanel.nameColumn") : displayModel;

        return new ContainerableNameColumn<>(
                displayModel,
                ProcessedObjectsProvider.SORT_BY_NAME,
                customColumn,
                expression, getPageBase()) {
            @Override
            protected IModel<String> getContainerName(SelectableBean<SimulationResultProcessedObjectType> rowModel) {
                return () -> null;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> item,
                    String id,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {

                ProcessedObject<?> processedObject = SimulationsGuiUtil.parseProcessedObject(rowModel.getObject().getValue(), getPageBase());
                IModel<String> title = () -> SimulationsGuiUtil.getShadowNameFromAttribute(processedObject);
                IModel<String> realMarksModel = () -> createRealMarksList(getPageBase(), processedObject);
                item.add(new TitleWithMarks(id, title, realMarksModel) {

                    @Override
                    protected boolean isTitleLinkEnabled() {
                        return rowModel.getObject().getValue() != null;
                    }

                    @Override
                    protected void onTitleClicked(AjaxRequestTarget target) {
                        SimulationResultProcessedObjectType object = rowModel.getObject().getValue();
                        if (object == null) {
                            return;
                        }

                        String simulationResultOid = CorrelationProcessedObjectPanel.this
                                .getSimulationResultModel().getObject().getOid();
                        navigateToSimulationResultObject(simulationResultOid, null, object, target);
                    }

                });
            }
        };
    }

    public static String createRealMarksList(PageBase page, ProcessedObject<?> obj) {
        return ProcessedObjectsPanel.createRealMarksList(page, obj);
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<SimulationResultProcessedObjectType>> createProvider() {
        return new ProcessedObjectsProvider(this, getSearchModel()) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return CorrelationProcessedObjectPanel.this.getSimulationResultModel().getObject().getOid();
            }
        };
    }

    @Override
    public List<SimulationResultProcessedObjectType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(SelectableBean::getValue).collect(Collectors.toList());
    }

    @NotNull
    protected abstract IModel<SimulationResultType> getSimulationResultModel();

    @Override
    protected List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> createDefaultColumns() {
        List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> columns = new ArrayList<>();

        columns.add(0, new AbstractColumn<>(
                createStringResource("SimulationResultObjectsPanel.column.status")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> cellItem,
                    String componentId,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                SimulationResultProcessedObjectType simulationResultProcessedObject = rowModel.getObject().getValue();
                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(simulationResultProcessedObject, getPageBase());

                assert processedObject != null;
                List<ObjectReferenceType> eventMarkRef = simulationResultProcessedObject.getEventMarkRef();
                Badge badge = createStatusBadge(eventMarkRef, getPageBase());
                BadgeListPanel statusPanel =
                        new BadgeListPanel(componentId, () -> Collections.singletonList(badge));
                statusPanel.add(AttributeModifier.append("class", "font-weight-semibold"));
                statusPanel.add(AttributeModifier.append("style", "font-size:12px"));
                cellItem.add(statusPanel);
            }

            @Override
            public String getCssClass() {
                return "col-1 align-middle py-4";
            }
        });

        columns.add(1, createNameColumn(null, null, null));

        columns.add(new AbstractColumn<>(
                createStringResource("SimulationResultObjectsPanel.column.correlatedCandidates")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> cellItem,
                    String componentId,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {

                final SimulationResultProcessedObjectType processedObjectType = rowModel.getObject().getValue();
                ProcessedObject<ShadowType> processedObject = (ProcessedObject<ShadowType>) SimulationsGuiUtil
                        .parseProcessedObject(processedObjectType, getPageBase());

                if (processedObject == null) {
                    throw new SystemException("Processed object " + processedObjectType + " was not parsed correctly");
                }

                final ShadowType shadowAfterChanges = getShadowAfterChanges(processedObject);
                final Optional<String> correlatedOwnerOid = getCorrelatedOwner(shadowAfterChanges);

                List<ResourceObjectOwnerOptionType> candidates =
                        getCorrelationCandidateModel(shadowAfterChanges).getObject();

                CandidateDisplayData displayData = createCandidateDisplay(getPageBase(), candidates,
                        correlatedOwnerOid.orElse(null));

                AjaxIconButton panel = new AjaxIconButton(componentId,
                        () -> displayData.icon,
                        () -> displayData.text) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ConfirmationPanel confirmationPanel = new ConfirmationPanel(
                                getPageBase().getMainPopupBodyId(),
                                createStringResource("CorrelationProcessedObjectPanel.confirmNavigateToCandidate")) {

                            @Override
                            public void yesPerformed(AjaxRequestTarget target) {
                                ResourceObjectOwnerOptionType resourceObjectOwnerOptionType = candidates.get(0);
                                ObjectReferenceType candidateOwnerRef = resourceObjectOwnerOptionType.getCandidateOwnerRef();
                                DetailsPageUtil.dispatchToObjectDetailsPage(UserType.class,
                                        candidateOwnerRef.getOid(),
                                        getPageBase(), false);
                            }
                        };
                        getPageBase().showMainPopup(confirmationPanel, target);
                    }
                };

                panel.setOutputMarkupId(true);
                panel.showTitleAsLabel(true);
                panel.setEnabled(candidates.size() == 1);
                if (panel.isEnabled()) {
                    panel.add(AttributeAppender.append("class", "btn btn-link p-0"));
                }

                cellItem.add(panel);
            }

            @Override
            public String getCssClass() {
                return "col-5 align-middle";
            }
        });

        return columns;
    }

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
    }

    @Override
    public void refreshTable(AjaxRequestTarget target) {

        getDataProvider().detach();
        super.refreshTable(target);
    }
}
