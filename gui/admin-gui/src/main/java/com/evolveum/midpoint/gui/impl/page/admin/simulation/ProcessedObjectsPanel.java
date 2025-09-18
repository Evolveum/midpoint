/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.mark.component.MarksOfObjectListPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;
import com.evolveum.midpoint.schema.SelectorOptions;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AvailableMarkSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.ChoicesSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.data.column.DeltaProgressBarColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.Nullable;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ProcessedObjectsPanel extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ProcessedObjectsPanel.class);
    private final IModel<List<MarkType>> availableMarksModel;

    private static final String DOT_CLASS = ProcessedObjectsPanel.class.getName() + ".";
    private static final String OPERATION_MARK_OBJECT = DOT_CLASS + "markObject";
//    private static final String OPERATION_UNMARK_OBJECT = DOT_CLASS + "unmarkObject";
//    private static final String OPERATION_LOAD_OBJECTS = DOT_CLASS + "loadObjects";

    public ProcessedObjectsPanel(String id, IModel<List<MarkType>> availableMarksModel) {
        super(id, SimulationResultProcessedObjectType.class);

        this.availableMarksModel = availableMarksModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Search<SimulationResultProcessedObjectType> search = getSearchModel().getObject();
        PropertySearchItemWrapper<?> wrapper = search.findPropertySearchItem(SimulationResultProcessedObjectType.F_STATE);
        if (wrapper instanceof ChoicesSearchItemWrapper) {
            updateSearchForObjectProcessingState((ChoicesSearchItemWrapper) wrapper);
        }
    }

    private void updateSearchForObjectProcessingState(ChoicesSearchItemWrapper<ObjectProcessingStateType> wrapper) {
        ObjectProcessingStateType state = getPredefinedProcessingState();
        if (state == null) {
            wrapper.setValue(null);
            return;
        }

        List<DisplayableValue<ObjectProcessingStateType>> availableValues = wrapper.getAvailableValues();
        DisplayableValue<ObjectProcessingStateType> newValue = availableValues.stream()
                .filter(d -> Objects.equals(d.getValue(), state))
                .findFirst().orElse(null);

        wrapper.setValue(newValue);
    }

    private void updateSearchForAvailableMarks(AvailableMarkSearchItemWrapper wrapper) {
        List<DisplayableValue<String>> marks = wrapper.getAvailableValues();
        marks.clear();
        marks.addAll(createSearchValuesForAvailableMarks());

        String markOid = getPredefinedMarkOid();
        wrapper.setValue(markOid);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        Search<SimulationResultProcessedObjectType> search = getSearchModel().getObject();
        PropertySearchItemWrapper<?> wrapper = search.findPropertySearchItem(SimulationResultProcessedObjectType.F_EVENT_MARK_REF);
        if (wrapper instanceof AvailableMarkSearchItemWrapper) {
            updateSearchForAvailableMarks((AvailableMarkSearchItemWrapper) wrapper);
        }
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_PROCESSED_OBJECTS;
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
    protected SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();

        List<DisplayableValue<String>> values = createSearchValuesForAvailableMarks();
        ctx.setAvailableEventMarks(values);
        ctx.setSelectedEventMark(getPredefinedMarkOid());

        return ctx;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return createRowMenuItems();
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createIconColumn() {
        return SimulationsGuiUtil.createProcessedObjectIconColumn(getParentPage());
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createNameColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        displayModel = displayModel == null ? createStringResource("ProcessedObjectsPanel.nameColumn") : displayModel;

        return new ContainerableNameColumn<>(displayModel, ProcessedObjectsProvider.SORT_BY_NAME, customColumn, expression, getPageBase()) {
            @Override
            protected IModel<String> getContainerName(SelectableBean<SimulationResultProcessedObjectType> rowModel) {
                return () -> null;
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> item, String id, IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                IModel<ProcessedObject<?>> model = new LoadableDetachableModel<>() {

                    @Override
                    protected ProcessedObject<?> load() {
                        return SimulationsGuiUtil.parseProcessedObject(rowModel.getObject().getValue(), getPageBase());
                    }
                };

                IModel<String> title = () -> SimulationsGuiUtil.getProcessedObjectName(model.getObject(), getPageBase());
                IModel<String> realMarksModel = () -> createRealMarksList(model.getObject());
                item.add(new TitleWithMarks(id, title, realMarksModel) {

                    @Override
                    protected boolean isTitleLinkEnabled() {
                        return rowModel.getObject().getValue() != null;
                    }

                    @Override
                    protected void onTitleClicked(AjaxRequestTarget target) {
                        onObjectNameClicked(rowModel.getObject(), target);
                    }

                    @Override
                    protected void onIconClicked(AjaxRequestTarget target) {
                        showOperationResult(target, model);
                    }

                    @Override
                    protected IModel<String> createIconCssModel() {
                        return () -> {
                            OperationResultStatus status = model.getObject().getResultStatus();
                            if (status == null) {
                                return null;
                            }

                            return status.isConsideredSuccess() ? null : GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_WARNING_COLORED;
                        };
                    }

                    @Override
                    protected IModel<String> createIconTitleModel() {
                        return () -> {
                            OperationResultStatus status = model.getObject().getResultStatus();
                            if (status == null) {
                                return null;
                            }

                            return getString(LocalizationUtil.createKeyForEnum(status));
                        };
                    }

                    @Override
                    protected IModel<String> createSecondaryMarksList() {
                        return () -> createProcessedObjectDescription(model.getObject());
                    }
                });
            }
        };
    }

    private void showOperationResult(AjaxRequestTarget target, IModel<ProcessedObject<?>> model) {
        PageBase page = getPageBase();

        IModel<OperationResult> result = () -> model.getObject().getResult();

        page.showMainPopup(new OperationResultPopupPanel(page.getMainPopupBodyId(), result), target);
    }

    private String createRealMarksList(ProcessedObject<?> obj) {
        if (obj == null) {
            return null;
        }

        PageBase page = getPageBase();
        Task task = page.getPageTask();

        PrismObject<ShadowType> shadow = WebModelServiceUtils.loadObject(ShadowType.class, obj.getOid(),
                GetOperationOptions.createRawCollection(), page, task, task.getResult());
        if (shadow == null) {
            return null;
        }

        return WebComponentUtil.createMarkList(shadow.asObjectable(), getPageBase());
    }

    private String createProcessedObjectDescription(ProcessedObject<?> obj) {
        if (obj == null) {
            return null;
        }

        Collection<String> eventMarkOids = obj.getMatchingEventMarksOids();
        // resolve names from markRefs
        Object[] names = eventMarkOids.stream()
                .map(oid -> {
                    List<MarkType> marks = availableMarksModel.getObject();
                    MarkType mark = marks.stream()
                            .filter(t -> Objects.equals(t.getOid(), oid))
                            .findFirst().orElse(null);
                    if (mark == null) {
                        return null;
                    }
                    return WebComponentUtil.getDisplayNameOrName(mark.asPrismObject());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toArray();

        return StringUtils.joinWith(", ", names);
    }

    private List<InlineMenuItem> createRowMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.markProtected"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-fw " + GuiStyleConstants.CLASS_SHADOW_ICON_PROTECTED);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        markObjects(getRowModel(), target);
                    }
                };
            }
        });

        items.add(modifyMarkInlineMenuAction());

//        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.mark"), true) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isHeaderMenuItem() {
//                return false;
//            }
//
//            @Override
//            public CompositedIconBuilder getIconCompositedBuilder() {
//                return getDefaultCompositedIconBuilder("fa-fw " + GuiStyleConstants.CLASS_MARK);
//            }
//
//            @Override
//            public InlineMenuItemAction initAction() {
//                return new ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>>() {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onSubmit(AjaxRequestTarget target) {
//
//                        ObjectFilter marksFilter = PrismContext.get().queryFor(MarkType.class)
//                                .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
//                                .ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value())
//                                .buildFilter();
//
//                        ObjectBrowserPanel<MarkType> browser = new ObjectBrowserPanel<>(
//                                getPageBase().getMainPopupBodyId(), MarkType.class,
//                                Collections.singletonList(MarkType.COMPLEX_TYPE), true, getPageBase(), marksFilter) {
//
//                            protected void addPerformed(AjaxRequestTarget target, QName type, List<MarkType> selected) {
//                                LOGGER.debug("Selected marks: {}", selected);
//
//                                List<String> markOids = Lists.transform(selected, MarkType::getOid);
//                                markObjects(getRowModel(), markOids, target);
//                                super.addPerformed(target, type, selected);
//                            }
//
//                            public org.apache.wicket.model.StringResourceModel getTitle() {
//                                return createStringResource("pageContentAccounts.menu.mark.select");
//                            }
//                        };
//
//                        getPageBase().showMainPopup(browser, target);
//                    }
//                };
//            }
//        });
//
//        items.add(new InlineMenuItem(createStringResource("ProcessedObjectsPanel.menu.mark.remove"), true) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public InlineMenuItemAction initAction() {
//                return new ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>>() {
//
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onSubmit(AjaxRequestTarget target) {
//                        removeMarkPerformed(target, getRowModel());
//                    }
//                };
//            }
//        });

        return items;
    }

    public InlineMenuItem modifyMarkInlineMenuAction() {
        return new InlineMenuItem(createStringResource("MainObjectListPanel.menu.modifyMark"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultProcessedObjectType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {

                        IModel<SelectableBean<SimulationResultProcessedObjectType>> selected = getRowModel();
                        if (selected == null) {
                            warn(getString("MainObjectListPanel.message.noFocusSelected"));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        LoadableDetachableModel<PrismObjectWrapper<? extends ObjectType>> focusModel = loadWrapper(selected.getObject().getValue());

                        if (focusModel == null || focusModel.getObject() == null) {
                            warn(getString("ProcessedObjectsPanel.message.noObjectFound", selected.getObject().getValue().getOid()));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        MarksOfObjectListPopupPanel popup = new MarksOfObjectListPopupPanel(
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

    private LoadableDetachableModel<PrismObjectWrapper<? extends ObjectType>> loadWrapper(
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

//    private String[] collectExistingMarks(List<SimulationResultProcessedObjectType> selected) {
//        var markOids = new HashSet<>();
//
//        var options = getPageBase().getOperationOptionsBuilder()
//                .raw()
//                .build();
//
//        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_OBJECTS);
//        OperationResult result = task.getResult();
//
//        for (var object : selected) {
//            PrismObject<?> loaded = WebModelServiceUtils.loadObject(
//                    ObjectTypes.getObjectTypeClass(object.getType()), object.getOid(), options, true, getPageBase(), task, result);
//            if (loaded == null) {
//                continue;
//            }
//
//            ObjectType objectType = (ObjectType) loaded.asObjectable();
//            objectType.getPolicyStatement().stream()
//                    .map(PolicyStatementType::getMarkRef)
//                    .filter(Objects::nonNull)
//                    .map(ObjectReferenceType::getOid)
//                    .forEach(markOids::add);
//        }
//
//        return markOids.toArray(String[]::new);
//    }

    private List<SimulationResultProcessedObjectType> getSelectedObjects(
            IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        if (rowModel != null) {
            return Collections.singletonList(rowModel.getObject().getValue());
        }

        return getSelectedRealObjects();
    }

//    private void removeMarkPerformed(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
//        List<SimulationResultProcessedObjectType> selected = getSelectedObjects(rowModel);
//
//        if (selected.isEmpty()) {
//            warn(getString("ProcessedObjectsPanel.message.noObjectsSelected"));
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//        var selectedMarks = collectExistingMarks(selected);
//
//        if (selectedMarks.length == 0) {
//            warn(getString("ProcessedObjectsPanel.message.noMarksSelectedForRemoval"));
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//        ObjectFilter marksFilter = PrismContext.get().queryFor(MarkType.class)
//                .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
//                .ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value())
//                .and()
//                .id(selectedMarks)
//                .buildFilter();
//
//        ObjectBrowserPanel<MarkType> browser = new ObjectBrowserPanel<>(
//                getPageBase().getMainPopupBodyId(), MarkType.class,
//                Collections.singletonList(MarkType.COMPLEX_TYPE), true, getPageBase(), marksFilter) {
//
//            @Override
//            protected void addPerformed(AjaxRequestTarget target, QName type, List<MarkType> selectedMarks) {
//                List<String> markOids = Lists.transform(selectedMarks, MarkType::getOid);
//                removeObjectMarks(target, selected, markOids);
//
//                super.addPerformed(target, type, selectedMarks);
//            }
//
//            @Override
//            public org.apache.wicket.model.StringResourceModel getTitle() {
//                return createStringResource("ProcessedObjectsPanel.menu.mark.select.remove");
//            }
//
//            @Override
//            protected org.apache.wicket.model.StringResourceModel getAddButtonTitle() {
//                return createStringResource("ProcessedObjectsPanel.menu.mark.remove");
//            }
//        };
//
//        getPageBase().showMainPopup(browser, target);
//    }

//    private <O extends ObjectType> void removeObjectMarks(
//            AjaxRequestTarget target, List<SimulationResultProcessedObjectType> selected, List<String> markOids) {
//
//        if (selected.isEmpty()) {
//            warn(getString("ProcessedObjectsPanel.message.noObjectsSelected"));
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//        if (markOids.isEmpty()) {
//            warn(getString("ProcessedObjectsPanel.message.noMarksSelectedForRemoval"));
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//        var options = getPageBase().getOperationOptionsBuilder()
//                .raw()
//                .build();
//
//        Task task = getPageBase().createSimpleTask(OPERATION_UNMARK_OBJECT);
//        OperationResult result = task.getResult();
//
//        for (var object : selected) {
//            if (ObjectProcessingStateType.ADDED.equals(object.getState())) {
//                // skip object, since it is added
//                continue;
//            }
//
//            PrismObject<O> loaded = WebModelServiceUtils.loadObject(
//                    ObjectTypes.getObjectTypeClass(object.getType()), object.getOid(), options, true, getPageBase(), task, result);
//            if (loaded == null) {
//                continue;
//            }
//
//            List<PolicyStatementType> statements = findMatchingPolicyStatements(loaded, markOids);
//            if (statements.isEmpty()) {
//                continue;
//            }
//
//            try {
//                var delta = loaded.createDelta(ChangeType.MODIFY);
//                delta.addModificationDeleteContainer(
//                        ObjectType.F_POLICY_STATEMENT, statements.toArray(new PolicyStatementType[0]));
//
//                getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
//            } catch (Exception e) {
//                result.recordPartialError(
//                        getString("ProcessedObjectsPanel.message.unmarkObjectError", object),
//                        e);
//                LOGGER.error("Could not unmark object {} with marks {}", object, markOids, e);
//            }
//        }
//
//        result.computeStatusIfUnknown();
//        getPageBase().showResult(result);
//
//        refreshTable(target);
//        target.add(getPageBase().getFeedbackPanel());
//    }

//    private <O extends ObjectType> List<PolicyStatementType> findMatchingPolicyStatements(
//            PrismObject<O> object, List<String> markOids) {
//
//        List<PolicyStatementType> statements = new ArrayList<>();
//        for (var statement : object.asObjectable().getPolicyStatement()) {
//            if (markOids.contains(statement.getMarkRef().getOid())) {
//                statements.add(statement.clone());
//            }
//        }
//
//        return statements;
//    }

    private void onObjectNameClicked(@NotNull SelectableBean<SimulationResultProcessedObjectType> bean, AjaxRequestTarget target) {
        SimulationResultProcessedObjectType object = bean.getValue();
        if (object == null) {
            return;
        }
        String markOid = getPredefinedMarkOid();
        String simulationResultOid = getSimulationResultOid();
        navigateToSimulationResultObject(simulationResultOid, markOid, object, target);
    }

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, simulationResultOid);
        if (markOid != null) {
            params.set(PageSimulationResultObject.PAGE_PARAMETER_MARK_OID, markOid);
        }
        params.set(PageSimulationResultObject.PAGE_PARAMETER_CONTAINER_ID, object.getId());

        getPageBase().navigateToNext(PageSimulationResultObject.class, params);
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<SimulationResultProcessedObjectType>> createProvider() {
        return new ProcessedObjectsProvider(this, getSearchModel()) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return ProcessedObjectsPanel.this.getSimulationResultOid();
            }
        };
    }

    @NotNull
    protected abstract String getSimulationResultOid();

    /**
     * Mark OID that should be used in search filter during first initialization (can be changed by user later)
     */
    protected String getPredefinedMarkOid() {
        return null;
    }

    /**
     * Object processing state that should be used in search filter during first initialization (can be changed by user later)
     */
    protected ObjectProcessingStateType getPredefinedProcessingState() {
        return null;
    }

    @Override
    public List<SimulationResultProcessedObjectType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(SelectableBean::getValue).collect(Collectors.toList());
    }

    @Override
    protected PrismContainerDefinition<SimulationResultProcessedObjectType> getContainerDefinitionForColumns() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(SimulationResultType.class)
                .findContainerDefinition(SimulationResultType.F_PROCESSED_OBJECT);
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createCustomExportableColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (SimulationResultProcessedObjectType.F_STATE.equivalent(path)) {
            return createStateColumn(displayModel);
        } else if (SimulationResultProcessedObjectType.F_TYPE.equivalent(path)) {
            return createTypeColumn(displayModel);
        } else if (SimulationResultProcessedObjectType.F_DELTA.equivalent(path)) {
            return createDeltaColumn();
        }

        return super.createCustomExportableColumn(displayModel, customColumn, expression);
    }

    private IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createDeltaColumn() {
        return new DeltaProgressBarColumn<>(createStringResource("ProcessedObjectsPanel.deltaColumn")) {

            @Override
            protected @NotNull IModel<ObjectDelta<?>> createObjectDeltaModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                return () -> {
                    SimulationResultProcessedObjectType object = rowModel.getObject().getValue();
                    ObjectDeltaType delta = object != null ? object.getDelta() : null;
                    if (delta == null) {
                        return null;
                    }

                    try {
                        return DeltaConvertor.createObjectDelta(delta);
                    } catch (SchemaException ex) {
                        LOGGER.debug("Couldn't parse object delta", ex);

                        getPageBase().error(ex.getMessage());

                        return null;
                    }
                };
            }
        };
    }

    private IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createStateColumn(IModel<String> displayModel) {
        return new AbstractColumn<>(displayModel, SimulationResultProcessedObjectType.F_STATE.getLocalPart()) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> item, String id,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> row) {

                item.add(SimulationsGuiUtil.createProcessedObjectStateLabel(id, () -> row.getObject().getValue()));
            }
        };
    }

    private IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createTypeColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, SimulationResultProcessedObjectType.F_TYPE.getLocalPart(),
                row -> SimulationsGuiUtil.getProcessedObjectType(row::getValue));
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        return new ArrayList<>();
    }

    @Override
    public void refreshTable(AjaxRequestTarget target) {
        super.refreshTable(target);

        // Here we want to align what is search item for "event mark" (which oid) with page url.
        // Url should be updated if selected mark oid has changed so that navigation works correctly
        // between processed object list and details, same for bookmarking urls.

        PropertySearchItemWrapper<?> wrapper = getSearchModel().getObject().findPropertySearchItem(SimulationResultProcessedObjectType.F_EVENT_MARK_REF);
        if (!(wrapper instanceof AvailableMarkSearchItemWrapper)) {
            return;
        }

        AvailableMarkSearchItemWrapper markWrapper = (AvailableMarkSearchItemWrapper) wrapper;
        DisplayableValue<String> value = markWrapper.getValue();
        String newMarkOid = value != null ? value.getValue() : null;
        if (Objects.equals(getPredefinedMarkOid(), newMarkOid)) {
            return;
        }

        PageParameters params = getPage().getPageParameters();
        params.remove(SimulationPage.PAGE_PARAMETER_MARK_OID);
        if (newMarkOid != null) {
            params.add(SimulationPage.PAGE_PARAMETER_MARK_OID, newMarkOid);
        }

        throw new RestartResponseException(getPage());
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
}
