/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedRadioPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Table of the objects of an object class found in the external system using the developed connector, with their identifiers.
 */
public class ResourceObjectsTablePanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_TABLE = "table";

    /**
     * OID of the testing resource whose objects are searched.
     */
    private final IModel<String> resourceOidModel;

    /**
     * Definition of the object class, used for the search and for the identifier columns.
     */
    private final IModel<ResourceObjectDefinition> objectClassDefinitionModel;

    public ResourceObjectsTablePanel(String id, IModel<String> selectedObjectOidModel, IModel<String> resourceOidModel,
            IModel<ResourceObjectDefinition> objectClassDefinitionModel) {
        super(id, selectedObjectOidModel);
        this.resourceOidModel = resourceOidModel;
        this.objectClassDefinitionModel = objectClassDefinitionModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createRadioGroup());
    }

    /**
     * Called after the user picked another object.
     *
     * @param target the request target
     */
    protected void onSelectionChanged(AjaxRequestTarget target) {
    }

    private RadioGroup<String> createRadioGroup() {
        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, getModel());
        radioGroup.setOutputMarkupId(true);

        BoxedTablePanel<SelectableBean<ShadowType>> table = createTable();
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

    private BoxedTablePanel<SelectableBean<ShadowType>> createTable() {
        BoxedTablePanel<SelectableBean<ShadowType>> table = new BoxedTablePanel<>(ID_TABLE, createProvider(), createColumns()) {

            @Override
            protected Item<SelectableBean<ShadowType>> customizeNewRowItem(Item<SelectableBean<ShadowType>> item,
                    IModel<SelectableBean<ShadowType>> model) {
                Item<SelectableBean<ShadowType>> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class", () -> isSelected(model) ? "table-primary" : null));
                return row;
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private SelectableBeanObjectDataProvider<ShadowType> createProvider() {
        boolean logCaptureEnabled = WebModelServiceUtils.isEnableExperimentalFeature(getPageBase());

        SelectableBeanObjectDataProvider<ShadowType> provider = new SelectableBeanObjectDataProvider<>(this, null) {

            @Override
            public Class<ShadowType> getType() {
                return ShadowType.class;
            }

            @Override
            public ObjectQuery getQuery() {
                ResourceObjectDefinition objectClassDefinition = objectClassDefinitionModel.getObject();
                return objectClassDefinition == null ? null : ObjectQueryUtil.createResourceAndObjectClassQuery(
                        resourceOidModel.getObject(), objectClassDefinition.getObjectClassName());
            }

            @Override
            public boolean isUseObjectCounting() {
                return false;
            }
        };
        provider.setOptions(getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATIONS).dontRetrieve()
                .build());
        provider.setEmptyListOnNullQuery(true);
        provider.setSort(null);
        provider.setTaskConsumer((Consumer<Task> & Serializable) task ->
                ConnectorDevelopmentWizardUtil.configureConnectorDevelopmentTask(task, logCaptureEnabled));
        return provider;
    }

    private List<IColumn<SelectableBean<ShadowType>, String>> createColumns() {
        List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();
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

    private IColumn<SelectableBean<ShadowType>, String> createRadioColumn() {
        return new AbstractColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem, String componentId,
                    IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(createRadioPanel(componentId, rowModel));
            }

            @Override
            public String getCssClass() {
                return "icon align-middle";
            }
        };
    }

    private IsolatedRadioPanel<String> createRadioPanel(String id, IModel<SelectableBean<ShadowType>> rowModel) {
        return new IsolatedRadioPanel<>(id, () -> getObjectOid(rowModel), Model.of(true));
    }

    private boolean isPrimaryIdentifier(QName identifierName) {
        ResourceObjectDefinition objectClassDefinition = objectClassDefinitionModel.getObject();
        return objectClassDefinition != null && objectClassDefinition.getPrimaryIdentifiers().stream()
                .anyMatch(definition -> definition.getItemName().equals(identifierName));
    }

    private IColumn<SelectableBean<ShadowType>, String> createIdentifierColumn(ShadowSimpleAttributeDefinition<?> identifier) {
        QName identifierName = identifier.getItemName();
        String title = ConnectorDevelopmentWizardUtil.getAttributeTitle(identifier);
        IModel<String> header = isPrimaryIdentifier(identifierName)
                ? createStringResource("TestObjectsTablePanel.primaryIdentifierColumn", title)
                : Model.of(title);

        return new AbstractColumn<>(header) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem, String componentId,
                    IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(createAttributeValueLabel(componentId, rowModel, identifierName));
            }
        };
    }

    private Label createAttributeValueLabel(String id, IModel<SelectableBean<ShadowType>> rowModel, QName attributeName) {
        return new Label(id, () -> {
            ShadowType object = getObject(rowModel);
            return object == null ? null
                    : ConnectorDevelopmentWizardUtil.formatValues(ShadowUtil.getAttributeValues(object, attributeName));
        });
    }

    private boolean isSelected(IModel<SelectableBean<ShadowType>> rowModel) {
        return StringUtils.equals(getObjectOid(rowModel), getModelObject());
    }

    private String getObjectOid(IModel<SelectableBean<ShadowType>> rowModel) {
        ShadowType object = getObject(rowModel);
        return object != null ? object.getOid() : null;
    }

    private ShadowType getObject(IModel<SelectableBean<ShadowType>> rowModel) {
        SelectableBean<ShadowType> row = rowModel.getObject();
        return row != null ? row.getValue() : null;
    }
}
