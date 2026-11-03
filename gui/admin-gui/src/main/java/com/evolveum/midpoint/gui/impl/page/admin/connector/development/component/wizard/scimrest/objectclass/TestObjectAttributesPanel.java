/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Table for editing the attribute values of a test object, either of the new object to create or of an existing object to modify.
 */
public class TestObjectAttributesPanel extends BasePanel<ShadowWrapper> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_EXISTING_OBJECT = "existingObject";
    private static final String ID_FILTER = "filter";
    private static final String ID_TABLE = "table";

    private static final String SORT_ATTRIBUTE = "attribute";
    private static final String SORT_REQUIREMENT = "requirement";
    private static final String SORT_VALUE = "value";

    /**
     * Search text and the filters of the required attributes and of the changed attributes of an existing object.
     */
    private final AttributesFilterState<PrismPropertyWrapper<Object>> filterState = new AttributesFilterState<>(
            new AttributesFilter<>(createStringResource("TestObjectAttributesPanel.showRequiredOnly"), PrismPropertyWrapper::isMandatory),
            new AttributesFilter<>(() -> getString("TestObjectAttributesPanel.showModifiedOnly", countChangedAttributes()),
                    this::isExistingObject, this::isChanged));

    /**
     * Table of the attributes, refreshed when a filter is applied.
     */
    private BoxedTablePanel<PrismPropertyWrapper<Object>> table;

    /**
     * Filter panel, refreshed when the count of changed attributes changes.
     */
    private AttributesFilterPanel<PrismPropertyWrapper<Object>> filterPanel;

    /**
     * Creates the table of test object attributes.
     *
     * @param id component id
     * @param model test object being edited
     */
    public TestObjectAttributesPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        table = createTable();
        add(table);
        add(createExistingObjectCard());
        filterPanel = createFilterPanel();
        add(filterPanel);
    }

    private TestObjectCardPanel createExistingObjectCard() {
        TestObjectCardPanel card = new TestObjectCardPanel(ID_EXISTING_OBJECT, this::getExistingObject);
        card.add(new VisibleBehaviour(this::isExistingObject));
        return card;
    }

    private BoxedTablePanel<PrismPropertyWrapper<Object>> createTable() {
        ListDataProvider<PrismPropertyWrapper<Object>> provider = new ListDataProvider<>(this, this::getVisibleAttributes, true) {

            @Override
            protected Comparator<PrismPropertyWrapper<Object>> getComparator(String property) {
                return switch (property) {
                    case SORT_REQUIREMENT -> Comparator.comparing(attribute -> !attribute.isMandatory());
                    case SORT_VALUE -> Comparator.comparing(
                            attribute -> getAttributeValue(attribute), String.CASE_INSENSITIVE_ORDER);
                    default -> Comparator.comparing(
                            attribute -> getAttributeLabel(attribute), String.CASE_INSENSITIVE_ORDER);
                };
            }
        };
        provider.setSort(null);

        BoxedTablePanel<PrismPropertyWrapper<Object>> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected Item<PrismPropertyWrapper<Object>> customizeNewRowItem(Item<PrismPropertyWrapper<Object>> item,
                    IModel<PrismPropertyWrapper<Object>> model) {
                Item<PrismPropertyWrapper<Object>> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class",
                        () -> isExistingObject() && isChanged(model.getObject()) ? "table-warning" : null));
                return row;
            }

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<PrismPropertyWrapper<Object>, String> provider, int pageSize) {
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private AttributesFilterPanel<PrismPropertyWrapper<Object>> createFilterPanel() {
        AttributesFilterPanel<PrismPropertyWrapper<Object>> filter =
                new AttributesFilterPanel<>(ID_FILTER, () -> filterState, target -> target.add(table));
        filter.setOutputMarkupId(true);
        return filter;
    }

    private List<IColumn<PrismPropertyWrapper<Object>, String>> createColumns() {
        List<IColumn<PrismPropertyWrapper<Object>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<>(createStringResource("TestObjectAttributesPanel.attribute"), SORT_ATTRIBUTE) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(createAttributeLabel(componentId, rowModel));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("TestObjectAttributesPanel.requirement"), SORT_REQUIREMENT) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(createRequirementLabel(componentId, rowModel));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("TestObjectAttributesPanel.testValue"), SORT_VALUE) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(createValuePanel(componentId, rowModel));
            }
        });
        columns.add(new AbstractColumn<>(null) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismPropertyWrapper<Object>>> cellItem, String componentId,
                    IModel<PrismPropertyWrapper<Object>> rowModel) {
                cellItem.add(createRevertButton(componentId, rowModel));
            }
        });
        return columns;
    }

    private Label createAttributeLabel(String id, IModel<PrismPropertyWrapper<Object>> rowModel) {
        return new Label(id, () -> getAttributeLabel(rowModel.getObject()));
    }

    private Label createRequirementLabel(String id, IModel<PrismPropertyWrapper<Object>> rowModel) {
        Label requirement = new Label(id, () -> getString(rowModel.getObject().isMandatory()
                ? "prismPropertyPanel.required"
                : "TestObjectAttributesPanel.optional"));
        requirement.add(AttributeAppender.append("class",
                () -> rowModel.getObject().isMandatory()
                        ? "badge bg-info-subtle border border-info-subtle text-info-emphasis"
                        : "badge bg-light border text-dark"));
        return requirement;
    }

    private PrismPropertyWrapperColumnPanel<Object> createValuePanel(String id,
            IModel<PrismPropertyWrapper<Object>> rowModel) {
        return new PrismPropertyWrapperColumnPanel<>(id, rowModel, ColumnType.VALUE) {

            @Override
            protected void onValueUpdated(AjaxRequestTarget target) {
                if (isExistingObject()) {
                    target.add(findParent(SelectableDataTable.SelectableRowItem.class),
                            filterPanel);
                    onAttributeChanged(target);
                }
            }
        };
    }

    private AjaxIconButton createRevertButton(String id, IModel<PrismPropertyWrapper<Object>> rowModel) {
        AjaxIconButton button = new AjaxIconButton(id, Model.of("fa fa-undo"),
                createStringResource("TestObjectAttributesPanel.revert")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                revertAttribute(rowModel.getObject());
                target.add(filterState.isAnyFilterApplied()
                        ? table
                        : findParent(SelectableDataTable.SelectableRowItem.class),
                        filterPanel);
                onAttributeChanged(target);
            }
        };
        button.add(AttributeAppender.append("class", "btn btn-link btn-sm"));
        button.add(AttributeAppender.append("class", () -> isRevertable(rowModel.getObject()) ? null : "invisible"));
        return button;
    }

    private boolean isRevertable(PrismPropertyWrapper<Object> attribute) {
        return isExistingObject() && attribute.isSingleValue() && isChanged(attribute);
    }

    /**
     * Called after a value of any attribute changed on the client.
     *
     * @param target the request target
     */
    protected void onAttributeChanged(AjaxRequestTarget target) {
    }

    private boolean isExistingObject() {
        return getModelObject() != null && getModelObject().getStatus() == ItemStatus.NOT_CHANGED;
    }

    private boolean isChanged(PrismPropertyWrapper<Object> attribute) {
        try {
            return attribute.getDelta() != null;
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't compute change of attribute " + attribute.getItemName(), e);
        }
    }

    private long countChangedAttributes() {
        return getAttributes().stream()
                .filter(attribute -> !attribute.isReadOnly())
                .filter(this::isChanged)
                .count();
    }

    private void revertAttribute(PrismPropertyWrapper<Object> attribute) {
        try {
            PrismPropertyValueWrapper<Object> value = attribute.getValue();
            value.setRealValue(value.getOldValue().getRealValue());
            if (value.getStatus() == ValueStatus.MODIFIED) {
                value.setStatus(ValueStatus.NOT_CHANGED);
            }
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't revert attribute " + attribute.getItemName(), e);
        }
    }

    private List<PrismPropertyWrapper<Object>> getVisibleAttributes() {
        return getAttributes().stream()
                .filter(attribute -> isExistingObject() ? !attribute.isReadOnly() : attribute.canAdd())
                .filter(attribute -> filterState.matches(getAttributeLabel(attribute), attribute))
                .sorted(Comparator.comparing((PrismPropertyWrapper<Object> attribute) -> !attribute.isMandatory()))
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

    private String getAttributeValue(PrismPropertyWrapper<Object> attribute) {
        return ConnectorDevelopmentWizardUtil.formatValues(attribute.getValues().stream()
                .map(PrismPropertyValueWrapper::getRealValue)
                .filter(Objects::nonNull)
                .toList());
    }

    private ShadowType getExistingObject() {
        return getModelObject().getObjectOld().asObjectable();
    }
}
