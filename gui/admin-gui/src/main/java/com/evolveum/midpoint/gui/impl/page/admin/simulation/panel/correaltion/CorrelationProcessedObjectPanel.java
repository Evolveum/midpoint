/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsProvider;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.CorrelationStatus.fromSimulationResultProcessedObject;

public abstract class CorrelationProcessedObjectPanel
        extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationProcessedObjectPanel.class);
    private static final String DOT_CLASS = ProcessedObjectsPanel.class.getName() + ".";
    private static final String OPERATION_MARK_OBJECT = DOT_CLASS + "markObject";

    private final IModel<List<MarkType>> availableMarksModel;

    public CorrelationProcessedObjectPanel(String id, IModel<List<MarkType>> availableMarksModel) {
        super(id, SimulationResultProcessedObjectType.class);
        this.availableMarksModel = availableMarksModel;
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
                () -> processedObject, CorrelationProcessedObjectPanel.this.getSimulationResultModel());
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
                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());

                IModel<String> title = () -> SimulationsGuiUtil.getShadowNameFromAttribute(processedObject);

                AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, title) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SimulationResultProcessedObjectType object = rowModel.getObject().getValue();
                        if (object == null) {
                            return;
                        }

                        String simulationResultOid = CorrelationProcessedObjectPanel.this
                                .getSimulationResultModel().getObject().getOid();
                        navigateToSimulationResultObject(simulationResultOid, null, object, target);
                    }
                };
                linkPanel.setOutputMarkupId(true);
                item.add(linkPanel);
            }
        };
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<SimulationResultProcessedObjectType>> createProvider() {
        return new ProcessedObjectsProvider(this, getSearchModel()) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return CorrelationProcessedObjectPanel.this.getSimulationResultModel().getObject().getOid();
            }

            @Override
            protected boolean additionalMatching(SimulationResultProcessedObjectType object) {
                CorrelationStatus correlationStatus = fromSimulationResultProcessedObject(getPageBase(), object);
                CorrelationStatus filterStatus = filterByStatus();
                if (filterStatus != null) {
                    return correlationStatus == filterStatus;
                }

                return super.additionalMatching(object);
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

                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());

                assert processedObject != null;

                List<ResourceObjectOwnerOptionType> candidates =
                        getCorrelationCandidateModel(processedObject).getObject();

                Badge badge = createStatusBadge(candidates.size(), getPageBase());
                BadgeListPanel statusPanel =
                        new BadgeListPanel(componentId, () -> Collections.singletonList(badge));

                cellItem.add(statusPanel);
            }

            @Override
            public String getCssClass() {
                return "col-1 align-middle";
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

                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());

                assert processedObject != null;

                List<ResourceObjectOwnerOptionType> candidates =
                        getCorrelationCandidateModel(processedObject).getObject();

                CorrelationUtil.CandidateDisplayData displayData = createCandidateDisplay(getPageBase(), candidates);

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

    protected @Nullable CorrelationStatus filterByStatus() {
        return null;
    }

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
    }

}
