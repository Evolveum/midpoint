/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObjects;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.createResultDurationText;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.createTaskStateLabel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationResultsPanel extends MainObjectListPanel<SimulationResultType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimulationResultsPanel.class);

    private static final String DOT_CLASS = SimulationResultsPanel.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";

    public SimulationResultsPanel(String id, ContainerPanelConfigurationType config) {
        super(id, SimulationResultType.class, config);
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<SimulationResultType>> createProvider() {
        ISelectableDataProvider<SelectableBean<SimulationResultType>> provider = super.createProvider();
        if (provider instanceof SortableDataProvider) {
            ((SortableDataProvider) provider).setSort(SimulationResultType.F_START_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
        }
        return provider;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULTS;
    }

    @Override
    protected void objectDetailsPerformed(SimulationResultType object) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

        getPageBase().navigateToNext(PageSimulationResult.class, params);
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<SimulationResultType>> rowModel) {
        return WebComponentUtil.isAuthorizedForPage(PageSimulationResult.class);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String buttonId) {
        return new ArrayList<>();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new ButtonInlineMenuItem(createStringResource("SimulationResultsPanel.listObjects")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-solid fa-eye");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<SelectableBean<SimulationResultType>> selected = isAnythingSelected(getRowModel());
                        if (selected.isEmpty()) {
                            getPageBase().warn(getString("FocusListInlineMenuHelper.message.nothingSelected"));
                            target.add(getFeedbackPanel());
                            return;
                        }

                        SelectableBean<SimulationResultType> bean = selected.get(0);
                        listProcessedObjectsPerformed(bean.getValue());
                    }
                };
            }
        });
        items.add(new ButtonInlineMenuItem(createStringResource("SimulationResultsPanel.delete")) {

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-solid fa-trash-can");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // todo nasty fix for showing warning on page when nothing was selected, needs to be fixed in MenuLinkPanel
                        List<SelectableBean<SimulationResultType>> selected = isAnythingSelected(getRowModel());
                        if (selected.isEmpty()) {
                            getPageBase().warn(getString("FocusListInlineMenuHelper.message.nothingSelected"));
                            target.add(getFeedbackPanel());
                            return;
                        }

                        deleteConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                ColumnMenuAction action = (ColumnMenuAction) getAction();

                // todo nasty fix for showing warning on page when nothing was selected, needs to be fixed in MenuLinkPanel, fix after release!
                List<SelectableBean<SimulationResultType>> selected = isAnythingSelected(action.getRowModel());
                if (selected.isEmpty()) {
                    return null;
                }

                return createConfirmationMessage(action,
                        "SimulationResultsPanel.message.deleteResultActionSingle", "SimulationResultsPanel.message.deleteResultActionMulti");
            }
        });
        items.add(new InlineMenuItem(createStringResource("SimulationResultsPanel.deleteObjects")) {

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // todo nasty fix for showing warning on page when nothing was selected, needs to be fixed in MenuLinkPanel, fix after release!
                        List<SelectableBean<SimulationResultType>> selected = isAnythingSelected(getRowModel());
                        if (selected.isEmpty()) {
                            getPageBase().warn(getString("FocusListInlineMenuHelper.message.nothingSelected"));
                            target.add(getFeedbackPanel());
                            return;
                        }

                        deleteResultObjectsConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                ColumnMenuAction action = (ColumnMenuAction) getAction();

                // todo nasty fix for showing warning on page when nothing was selected, needs to be fixed in MenuLinkPanel, fix after release!
                List<SelectableBean<SimulationResultType>> selected = isAnythingSelected(action.getRowModel());
                if (selected.isEmpty()) {
                    return null;
                }

                return createConfirmationMessage(action,
                        "SimulationResultsPanel.message.deleteResultObjectsActionSingle",
                        "SimulationResultsPanel.message.deleteResultObjectsActionMulti");
            }
        });

        return items;
    }

    private void deleteResultObjectsConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultType>> rowModel) {
        List<SelectableBean<SimulationResultType>> objects = isAnythingSelected(rowModel);

        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(objects.size() == 1 ? OPERATION_DELETE_OBJECT : OPERATION_DELETE_OBJECTS);
        for (SelectableBean<SimulationResultType> object : objects) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            SimulationResultType simResult = object.getValue();

            try {
                Task task = getPageBase().createSimpleTask(OPERATION_DELETE_OBJECT);

                ObjectDelta<SimulationResultType> delta = getPrismContext().deltaFactory().object()
                        .createModificationReplaceContainer(SimulationResultType.class, simResult.getOid(), SimulationResultType.F_PROCESSED_OBJECT, new Containerable[0]);

                ExecuteChangeOptionsDto executeOptions = getExecuteOptions();
                ModelExecuteOptions options = executeOptions.createOptions(getPrismContext());

                LOGGER.debug("Using options {}.", executeOptions);
                getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), options, task, subResult);
                subResult.computeStatus();
            } catch (Exception ex) {
                String name = WebComponentUtil.getName(simResult);

                subResult.recomputeStatus();
                subResult.recordFatalError(getString("SimulationResultsPanel.message.deleteResultObjectsFailed", name), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete processed objects for simulation result {}", ex, name);
            }
        }
        result.computeStatusComposite();

        getPageBase().showResult(result);
        target.add(getFeedbackPanel());
        refreshTable(target);
        clearCache();
    }

    private IModel<String> createConfirmationMessage(ColumnMenuAction<SelectableBean<SimulationResultType>> action, String singleKey, String multiKey) {
        return () -> {
            IModel<SelectableBean<SimulationResultType>> result = action.getRowModel();
            if (result != null) {
                return getString(singleKey, WebComponentUtil.getName(result.getObject().getValue()));
            }

            if (getSelectedObjectsCount() == 1) {
                SimulationResultType object = getSelectedRealObjects().get(0);
                return getString(singleKey, WebComponentUtil.getName(object));
            }

            return getString(multiKey, getSelectedObjectsCount());
        };
    }

    @Override
    protected List<IColumn<SelectableBean<SimulationResultType>, String>> createDefaultColumns() {
        List<IColumn<SelectableBean<SimulationResultType>, String>> columns = super.createDefaultColumns();
        columns.add(new LambdaColumn<>(createStringResource("ProcessedObjectsPanel.duration"), row -> createResultDurationText(row.getValue(), this)));
        columns.add(new AbstractColumn<>(createStringResource("ProcessedObjectsPanel.executionState")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultType>>> item, String id, IModel<SelectableBean<SimulationResultType>> model) {
                Component label = createTaskStateLabel(id, () -> model.getObject().getValue(), null, getPageBase());
                item.add(label);
            }
        });
        columns.add(createProcessedObjectsColumn());

        return columns;
    }

    private void listProcessedObjectsPerformed(SimulationResultType object) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

        getPageBase().navigateToNext(PageSimulationResultObjects.class, params);
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultType>, String> createCustomExportableColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (SimulationResultType.F_START_TIMESTAMP.equivalent(path)) {
            return createStartTimestampColumn(displayModel);
        }

        return super.createCustomExportableColumn(displayModel, customColumn, expression);
    }

    private IColumn<SelectableBean<SimulationResultType>, String> createProcessedObjectsColumn() {
        return new ProcessedObjectsCountColumn<>(createStringResource("SimulationResultsPanel.processedObjects")) {

            @Override
            protected @NotNull IModel<ObjectCounts> createCountModel(IModel<SelectableBean<SimulationResultType>> rowModel) {
                return new LoadableDetachableModel<>() {

                    @Override
                    protected ObjectCounts load() {
                        ObjectCounts counts = new ObjectCounts();

                        SimulationResultType result = rowModel.getObject().getValue();
                        int total = SimulationResultTypeUtil.getObjectsProcessed(result);
                        counts.setTotal(total);

                        Map<BuiltInSimulationMetricType, Integer> builtIn = SimulationsGuiUtil.getBuiltInMetrics(result);
                        builtIn.forEach((k, v) -> {
                            switch (k) {
                                case ADDED:
                                    counts.setAdded(v);
                                    break;
                                case MODIFIED:
                                    counts.setModified(v);
                                    break;
                                case DELETED:
                                    counts.setDeleted(v);
                                    break;
                            }
                        });

                        int unmodified = SimulationsGuiUtil.getUnmodifiedProcessedObjectCount(result, builtIn);
                        counts.setUnmodified(unmodified);

                        return counts;
                    }
                };
            }
        };
    }

    private IColumn<SelectableBean<SimulationResultType>, String> createStartTimestampColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, SimulationResultType.F_START_TIMESTAMP.getLocalPart(), row -> {
            XMLGregorianCalendar start = row.getValue().getStartTimestamp();
            return start != null ? WebComponentUtil.getLongDateTimeFormattedValue(start, getPageBase()) : null;
        });
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }
}
