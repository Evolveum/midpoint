/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationResultsPanel extends MainObjectListPanel<SimulationResultType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimulationResultsPanel.class);

    private static final String DOT_CLASS = SimulationResultsPanel.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";

    public SimulationResultsPanel(String id, ContainerPanelConfigurationType config) {
        super(id, SimulationResultType.class, null, config);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULTS;
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, SimulationResultType object) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

        getPageBase().navigateToNext(PageSimulationResult.class, params);
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
                        SelectableBean<SimulationResultType> bean = getRowModel().getObject();
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
                        deleteConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return createConfirmationMessage((ColumnMenuAction) getAction(),
                        "SimulationResultsPanel.message.deleteResultActionSingle", "SimulationResultsPanel.message.deleteResultActionMulti");
            }
        });
        items.add(new InlineMenuItem(createStringResource("SimulationResultsPanel.deleteObjects")) {

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteResultObjectsConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return createConfirmationMessage((ColumnMenuAction<SelectableBean<SimulationResultType>>) getAction(),
                        "SimulationResultsPanel.message.deleteResultObjectsActionSingle",
                        "SimulationResultsPanel.message.deleteResultObjectsActionMulti");
            }
        });

        return items;
    }

    private void deleteResultObjectsConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultType>> rowModel) {
        List<SelectableBean<SimulationResultType>> objects = isAnythingSelected(target, rowModel);

        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(objects.size() == 1 ? OPERATION_DELETE_OBJECT : OPERATION_DELETE_OBJECTS);
        for (SelectableBean<SimulationResultType> object : objects) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            SimulationResultType simResult = object.getValue();

            try {
                getPageBase().getRepositoryService().deleteSimulatedProcessedObjects(simResult.getOid(), null, subResult);
            } catch (Exception ex) {
                String name = WebComponentUtil.getName(simResult);

                subResult.recordFatalError(getString("SimulationResultsPanel.message.deleteResultObjectsFailed", name), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete processed objects for simulation result {}", ex, name);
            } finally {
                subResult.computeStatusIfUnknown();
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
        columns.add(new LambdaColumn<>(createStringResource("ProcessedObjectsPanel.duration"), row -> PageSimulationResult.createResultDurationText(row.getValue(), this)));
        columns.add(new AbstractColumn<>(createStringResource("ProcessedObjectsPanel.executionState")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultType>>> item, String id, IModel<SelectableBean<SimulationResultType>> model) {
                Component label = PageSimulationResult.createTaskStateLabel(id, () -> model.getObject().getValue(), null, getPageBase());
                item.add(label);
            }
        });

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

    private IColumn<SelectableBean<SimulationResultType>, String> createStartTimestampColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, row -> {
            XMLGregorianCalendar start = row.getValue().getStartTimestamp();
            return start != null ? WebComponentUtil.getLongDateTimeFormattedValue(start, getPageBase()) : null;
        });
    }
}
