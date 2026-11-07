/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Table of simple attributes for entering test object values.
 */
public class TestObjectAttributesPanel extends BasePanel<ShadowWrapper> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_FILTER = "filter";
    private static final String ID_TABLE = "table";

    private final IModel<String> searchTextModel = Model.of();
    private final IModel<Boolean> showRequiredOnlyModel = Model.of(false);

    public TestObjectAttributesPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        BoxedTablePanel<PrismPropertyWrapper<Object>> table = createTable();
        add(table);
        add(createFilterPanel(table));
    }

    private BoxedTablePanel<PrismPropertyWrapper<Object>> createTable() {
        ListDataProvider<PrismPropertyWrapper<Object>> provider = new ListDataProvider<>(this, this::getVisibleAttributes);
        provider.setSort(null);

        BoxedTablePanel<PrismPropertyWrapper<Object>> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<PrismPropertyWrapper<Object>, String> provider, int pageSize) {
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private AttributesFilterPanel createFilterPanel(BoxedTablePanel<PrismPropertyWrapper<Object>> table) {
        return new AttributesFilterPanel(ID_FILTER, searchTextModel, showRequiredOnlyModel,
                createStringResource("TestObjectAttributesPanel.showRequiredOnly"), target -> target.add(table));
    }
    private List<IColumn<PrismPropertyWrapper<Object>, String>> createColumns() {
        List<IColumn<PrismPropertyWrapper<Object>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<>(createStringResource("SearchByIdResultConnectorStepPanel.attribute")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(new Label(componentId, () -> getAttributeLabel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("TestObjectAttributesPanel.requirement")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                Label requirement = new Label(componentId, () -> getString(rowModel.getObject().isMandatory()
                        ? "prismPropertyPanel.required"
                        : "TestObjectAttributesPanel.optional"));
                requirement.add(AttributeAppender.append("class",
                        () -> rowModel.getObject().isMandatory() ? "badge text-bg-danger" : "badge text-bg-secondary"));
                cellItem.add(requirement);
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("TestObjectAttributesPanel.testValue")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(new PrismPropertyWrapperColumnPanel<>(
                        componentId, (IModel<PrismPropertyWrapper<Object>>) (IModel) rowModel, ColumnType.VALUE));
            }
        });
        return columns;
    }

    private List<PrismPropertyWrapper<Object>> getVisibleAttributes() {
        String searchText = StringUtils.trim(searchTextModel.getObject());
        boolean requiredOnly = Boolean.TRUE.equals(showRequiredOnlyModel.getObject());

        return getAttributes().stream()
                .filter(attribute -> attribute.canAdd())
                .filter(attribute -> StringUtils.isEmpty(searchText)
                        || StringUtils.containsIgnoreCase(getAttributeLabel(attribute), searchText))
                .filter(attribute -> !requiredOnly || attribute.isMandatory())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("unchecked")
    private List<PrismPropertyWrapper<Object>> getAttributes() {
        try {
            return getModelObject().findContainer(ShadowType.F_ATTRIBUTES).getValue().getItems().stream()
                    .filter(PrismPropertyWrapper.class::isInstance)
                    .map(attribute -> (PrismPropertyWrapper<Object>) attribute)
                    .toList();
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't find attributes of the new object", e);
        }
    }

    private String getAttributeLabel(PrismPropertyWrapper<Object> attribute) {
        return StringUtils.defaultIfBlank(attribute.getDisplayName(), attribute.getItemName().getLocalPart());
    }
}
