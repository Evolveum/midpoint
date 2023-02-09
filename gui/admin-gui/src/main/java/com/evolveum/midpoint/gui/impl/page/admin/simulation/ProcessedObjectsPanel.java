/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectsPanel extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    private static final long serialVersionUID = 1L;

    private IModel<List<MarkType>> availableMarksModel;

    public ProcessedObjectsPanel(String id, IModel<List<MarkType>> availableMarksModel) {
        super(id, SimulationResultProcessedObjectType.class);

        this.availableMarksModel = availableMarksModel;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        String markOid = getMarkOid();
        PropertySearchItemWrapper wrapper = getSearchModel().getObject().findPropertySearchItem(SimulationResultProcessedObjectType.F_EVENT_MARK_REF);
        if (wrapper instanceof AvailableMarkSearchItemWrapper) {
            AvailableMarkSearchItemWrapper markWrapper = (AvailableMarkSearchItemWrapper) wrapper;
            markWrapper.setValue(markOid);
        }
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_PROCESSED_OBJECTS;
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();

        List<DisplayableValue<String>> values = availableMarksModel.getObject().stream()
                .map(o -> new DisplayableValueImpl<>(
                        o.getOid(),
                        WebComponentUtil.getDisplayNameOrName(o.asPrismObject()),
                        o.getDescription()))
                .sorted(Comparator.comparing(d -> d.getLabel(), Comparator.naturalOrder()))
                .collect(Collectors.toList());
        ctx.setAvailableEventMarks(values);
        ctx.setSelectedEventMark(getMarkOid());

        return ctx;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createIconColumn() {
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                SimulationResultProcessedObjectType object = model.getObject().getValue();
                ObjectType obj = object.getBefore() != null ? object.getBefore() : object.getAfter();
                if (obj == null || obj.asPrismObject() == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(WebComponentUtil.createDefaultColoredIcon(object.getType())));
                }

                return new DisplayType()
                        .icon(new IconType().cssClass(WebComponentUtil.createDefaultIcon(obj.asPrismObject())));
            }
        };
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
                IModel<String> title = () -> {
                    SimulationResultProcessedObjectType obj = rowModel.getObject().getValue();
                    if (obj == null || obj.getName() == null) {
                        return getString("ProcessedObjectsPanel.unnamed");
                    }

                    return WebComponentUtil.getTranslatedPolyString(obj.getName());
                };
                IModel<String> description = () -> {
                    SimulationResultProcessedObjectType obj = rowModel.getObject().getValue();
                    if (obj == null) {
                        return null;
                    }

                    List<ObjectReferenceType> eventMarkRefs = obj.getEventMarkRef();
                    // resolve names from markRefs
                    List<String> names = eventMarkRefs.stream()
                            .map(ref -> {
                                List<MarkType> marks = availableMarksModel.getObject();
                                MarkType mark = marks.stream()
                                        .filter(t -> Objects.equals(t.getOid(), ref.getOid()))
                                        .findFirst().orElse(null);
                                if (mark == null) {
                                    return null;
                                }
                                return WebComponentUtil.getDisplayNameOrName(mark.asPrismObject());
                            })
                            .filter(name -> name != null)
                            .collect(Collectors.toList());
                    names.sort(Comparator.naturalOrder());

                    return StringUtils.joinWith(", ", names.toArray(new String[names.size()]));
                };
                item.add(new TitleWithDescriptionPanel(id, title, description) {

                    @Override
                    protected boolean isTitleLinkEnabled() {
                        return rowModel.getObject().getValue() != null;
                    }

                    @Override
                    protected void onTitleClicked(AjaxRequestTarget target) {
                        onObjectNameClicked(target, rowModel.getObject());
                    }
                });
            }
        };
    }

    private void onObjectNameClicked(AjaxRequestTarget target, SelectableBean<SimulationResultProcessedObjectType> bean) {
        SimulationResultProcessedObjectType object = bean.getValue();
        if (object == null) {
            return;
        }

        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, getSimulationResultOid());
        String markOid = getMarkOid();
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
    protected String getSimulationResultOid() {
        return null;
    }

    protected String getMarkOid() {
        return null;
    }

    @Override
    public List<SimulationResultProcessedObjectType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(o -> o.getValue()).collect(Collectors.toList());
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
        }

        return super.createCustomExportableColumn(displayModel, customColumn, expression);
    }

    private IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createStateColumn(IModel<String> displayModel) {
        return new AbstractColumn<>(displayModel) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultProcessedObjectType>>> item, String id,
                    IModel<SelectableBean<SimulationResultProcessedObjectType>> row) {

                item.add(GuiSimulationsUtil.createProcessedObjectStateLabel(id, () -> row.getObject().getValue()));
            }
        };
    }

    private IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createTypeColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, row -> GuiSimulationsUtil.getProcessedObjectType(row::getValue));
    }
}
