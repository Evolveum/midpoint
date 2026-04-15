/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.MappingUtil.createSituationMappingBadge;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.SimulationChangesPanel;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsProvider;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Panel displaying processed objects from mapping simulation results.
 *
 * <p>Shows situation badges, object names, detected value changes,
 * and operation result details for each processed object.</p>
 */
public abstract class MappingProcessedObjectPanel
        extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<List<MarkType>> availableMarksModel;
    String defaultMarkOidForSearch = MARK_SHADOW_CORRELATION_OWNER_FOUND.value();

    public MappingProcessedObjectPanel(String id, IModel<List<MarkType>> availableMarksModel) {
        super(id, SimulationResultProcessedObjectType.class);
        this.availableMarksModel = availableMarksModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public String getAdditionalBoxCssClasses() {
        return super.getAdditionalBoxCssClasses() + " table-td-middle";
    }

    protected String getDefaultMarkOidForSearch() {
        return null;
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        this.defaultMarkOidForSearch = getDefaultMarkOidForSearch();
        SearchContext ctx = new SearchContext();
        List<DisplayableValue<String>> values = createSearchValuesForAvailableMarks();
        ctx.setAvailableEventMarks(values);
        ctx.setSelectedEventMark(defaultMarkOidForSearch);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Serializable> Search<T> loadSearch(PageStorage storage) {
        Search<T> search = null;
        if (storage != null && defaultMarkOidForSearch != null && defaultMarkOidForSearch.equals(getDefaultMarkOidForSearch())) {
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
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_MAPPING_PROCESSED_OBJECTS;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createCheckboxColumn() {
        return null; // Disable selection for now. Do we want to support actions (e.g. marking)?
    }

    @Override
    protected boolean isCollapsableTable() {
        return false; //TODO: complex mapping
    }

    @Override
    protected Component createCollapsibleContent(
            String id,
            @NotNull IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        //TODO: complex mapping
        return null;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createNameColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        displayModel = displayModel == null ? createStringResource("MappingProcessedObjectPanel.object") : displayModel;

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

                IModel<String> title = () -> SimulationsGuiUtil.getProcessedObjectName(processedObject, getPageBase());

                AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, title) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SimulationResultProcessedObjectType object = rowModel.getObject().getValue();
                        if (object == null) {
                            return;
                        }

                        String simulationResultOid = MappingProcessedObjectPanel.this
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
                return MappingProcessedObjectPanel.this.getSimulationResultModel().getObject().getOid();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                String resultOid = getSimulationResultOid();
                //TODO  Inbound mapping simulation has no focus record id, in case of outbound mapping simulation it is always set.
                return getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                        .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                        .id(resultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_FOCUS_RECORD_ID).isNull()
                        .build();
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
                createStringResource("MappingProcessedObjectPanel.column.situation")) {

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
                Badge badge = createSituationMappingBadge(eventMarkRef, getPageBase());
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
                createStringResource("MappingProcessedObjectPanel.column.changes")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> cellItem,
                    String componentId,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {

                IModel<List<VisualizationDto>> visualizationsModel = getVisualizationDtoModel(rowModel);

                IModel<List<SimulationChangeSummaryDto>> summaryModel = new LoadableDetachableModel<>() {
                    @Override
                    protected @NotNull List<SimulationChangeSummaryDto> load() {
                        List<SimulationChangeSummaryDto> rv = new ArrayList<>();

                        List<VisualizationDto> visualizations = visualizationsModel.getObject();
                        if (visualizations == null) {
                            return rv;
                        }

                        for (VisualizationDto visualization : visualizations) {
                            rv.add(new SimulationChangeSummaryDto(visualization));
                        }

                        return rv;
                    }
                };

                cellItem.add(new SimulationChangesPanel(componentId, summaryModel));
            }

            @Override
            public String getCssClass() {
                return "col-5 align-middle";
            }
        });

        columns.add(new AbstractColumn<>(
                createStringResource("MappingProcessedObjectPanel.column.details")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> cellItem,
                    String componentId,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {

                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());

                OperationResult result = processedObject != null ? processedObject.getResult() : null;

                cellItem.add(new SimulationDetailsPanel(componentId, Model.of(result)) {
                    @Override
                    protected void onShowDetails(OperationResult result, AjaxRequestTarget ajaxRequestTarget) {
                        OperationResultPopupPanel popup = new OperationResultPopupPanel(
                                getPageBase().getMainPopupBodyId(),
                                Model.of(result));
                        getPageBase().showMainPopup(popup, ajaxRequestTarget);
                    }
                });
            }

            @Override
            public String getCssClass() {
                return "col-3 align-middle";
            }
        });

        return columns;
    }

    private @NotNull IModel<List<VisualizationDto>> getVisualizationDtoModel(
            IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<VisualizationDto> load() {
                try {

                    SimulationResultProcessedObjectType simulationResultProcessedObject = rowModel.getObject().getValue();
                    ProcessedObject<?> object = SimulationsGuiUtil
                            .parseProcessedObject(simulationResultProcessedObject, getPageBase());

                    if (object == null || object.getDelta() == null) {
                        return Collections.emptyList();
                    }

                    object.fixEstimatedOldValuesInDelta();

                    Visualization visualization = SimulationsGuiUtil.createVisualization(object.getDelta(), getPageBase());
                    if (visualization == null) {
                        return Collections.emptyList();
                    }

                    return List.of(new VisualizationDto(visualization));
                } catch (Exception ex) {
                    // intentionally empty
                }

                return Collections.emptyList();
            }
        };
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
