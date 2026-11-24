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
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedRadioPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Table of the recorded test objects of an object class with their identifiers.
 */
public class TestObjectsTablePanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_TABLE = "table";

    /**
     * Recorded test objects shown in the table.
     */
    private final IModel<List<ShadowType>> testObjectsModel;

    /**
     * Definition of the object class, used to find the identifier columns.
     */
    private final IModel<ResourceObjectDefinition> objectClassDefinitionModel;

    /**
     * Creates the table of recorded test objects.
     *
     * @param id component id
     * @param selectedObjectOidModel OID of the picked test object
     * @param testObjectsModel recorded test objects shown in the table
     * @param objectClassDefinitionModel definition of the object class
     */
    public TestObjectsTablePanel(String id, IModel<String> selectedObjectOidModel,
            IModel<List<ShadowType>> testObjectsModel, IModel<ResourceObjectDefinition> objectClassDefinitionModel) {
        super(id, selectedObjectOidModel);
        this.testObjectsModel = testObjectsModel;
        this.objectClassDefinitionModel = objectClassDefinitionModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createRadioGroup());
    }

    /**
     * Called after the user picked another test object.
     *
     * @param target the request target
     */
    protected void onSelectionChanged(AjaxRequestTarget target) {
    }

    private RadioGroup<String> createRadioGroup() {
        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, getModel());
        radioGroup.setOutputMarkupId(true);

        BoxedTablePanel<ShadowType> table = createTable();
        radioGroup.add(table);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                TableUtil.updateRows(table.getDataTable(), target);
                onSelectionChanged(target);
            }
        });
        return radioGroup;
    }

    private BoxedTablePanel<ShadowType> createTable() {
        ListDataProvider<ShadowType> provider = new ListDataProvider<>(this, testObjectsModel, true) {

            @Override
            protected Comparator<ShadowType> getComparator(String property) {
                QName identifier = QName.valueOf(property);
                return Comparator.comparing(shadow ->
                        ConnectorDevelopmentWizardUtil.formatValues(ShadowUtil.getAttributeValues(shadow, identifier)));
            }
        };
        provider.setSort(null);

        BoxedTablePanel<ShadowType> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<ShadowType, String> provider, int pageSize) {
                return false;
            }

            @Override
            protected Item<ShadowType> customizeNewRowItem(Item<ShadowType> item, IModel<ShadowType> model) {
                Item<ShadowType> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class",
                        () -> StringUtils.equals(model.getObject().getOid(), TestObjectsTablePanel.this.getModelObject()) ? "table-primary" : null));
                return row;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<ShadowType, String>> createColumns() {
        List<IColumn<ShadowType, String>> columns = new ArrayList<>();
        columns.add(createRadioColumn());

        getIdentifierDefinitions().forEach(identifier -> columns.add(createIdentifierColumn(identifier)));
        return columns;
    }

    private List<ShadowSimpleAttributeDefinition<?>> getIdentifierDefinitions() {
        ResourceObjectDefinition objectClassDefinition = objectClassDefinitionModel.getObject();
        if (objectClassDefinition == null) {
            return List.of();
        }

        List<ShadowSimpleAttributeDefinition<?>> identifiers = new ArrayList<>(objectClassDefinition.getPrimaryIdentifiers());
        identifiers.addAll(objectClassDefinition.getSecondaryIdentifiers());
        return identifiers;
    }

    private boolean isPrimaryIdentifier(QName identifierName) {
        ResourceObjectDefinition objectClassDefinition = objectClassDefinitionModel.getObject();
        return objectClassDefinition != null && objectClassDefinition.getPrimaryIdentifiers().stream()
                .anyMatch(definition -> definition.getItemName().equals(identifierName));
    }

    private IColumn<ShadowType, String> createRadioColumn() {
        return new AbstractColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ShadowType>> cellItem, String componentId, IModel<ShadowType> rowModel) {
                cellItem.add(createRadioPanel(componentId, rowModel));
            }

            @Override
            public String getCssClass() {
                return "icon align-middle";
            }
        };
    }

    private IsolatedRadioPanel<String> createRadioPanel(String id, IModel<ShadowType> rowModel) {
        return new IsolatedRadioPanel<>(id, () -> rowModel.getObject().getOid(), Model.of(true));
    }

    private IColumn<ShadowType, String> createIdentifierColumn(ShadowSimpleAttributeDefinition<?> identifier) {
        QName identifierName = identifier.getItemName();
        String title = ConnectorDevelopmentWizardUtil.getAttributeTitle(identifier);
        IModel<String> header = isPrimaryIdentifier(identifierName)
                ? createStringResource("TestObjectsTablePanel.primaryIdentifierColumn", title)
                : Model.of(title);

        return new AbstractColumn<>(header, identifierName.toString()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ShadowType>> cellItem, String componentId, IModel<ShadowType> rowModel) {
                cellItem.add(createIdentifierValueLabel(componentId, rowModel, identifierName));
            }
        };
    }

    private Label createIdentifierValueLabel(String id, IModel<ShadowType> rowModel, QName identifierName) {
        return new Label(id, () -> ConnectorDevelopmentWizardUtil.formatValues(
                ShadowUtil.getAttributeValues(rowModel.getObject(), identifierName)));
    }
}
