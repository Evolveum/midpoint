/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsProvider;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
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
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND;

public abstract class CorrelationProcessedObjectPanel
        extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

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
        if (storage != null && markOidForSearch.equals(getMarkOidForSearch())) {
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

                ProcessedObject<?> processedObject = SimulationsGuiUtil
                        .parseProcessedObject(rowModel.getObject().getValue(), getPageBase());

                assert processedObject != null;
                @Nullable ObjectDelta<?> delta = processedObject.getDelta();
                List<String> correlatedOwnersOid = findCorrelatedOwners(delta);

                List<ResourceObjectOwnerOptionType> candidates =
                        getCorrelationCandidateModel(processedObject).getObject();

                CandidateDisplayData displayData = createCandidateDisplay(getPageBase(), candidates, correlatedOwnersOid);

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
