/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
//@PanelType(name = "selectObjectClassesWizard")
//@PanelInstance(identifier = "selectObjectClassesWizard",
//        applicableForType = ResourceType.class,
//        applicableForOperation = OperationTypeType.ADD,
//        display = @PanelDisplay(label = "PageResource.wizard.step.selectObjectClasses", icon = "fa fa-table-cells"))
public class SelectObjectClassesStepPanel extends BasicWizardPanel {

    private static final String ID_SEARCH_FIELD = "searchFiled";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_TABLE = "table";

    private final ResourceDetailsModel resourceModel;
//    private final LoadableModel<List<QName>> selected;

    public SelectObjectClassesStepPanel(ResourceDetailsModel model) {
        super();
        this.resourceModel = model;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.selectObjectClasses");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.selectObjectClasses.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.selectObjectClasses.subText");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        TextField<String> objectClass = new TextField<>(ID_SEARCH_FIELD, new Model<>());
        objectClass.setOutputMarkupId(true);
        add(objectClass);

        AjaxButton searchButton = new AjaxButton(ID_SEARCH_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                updateSearchPerformed(target);
            }
        };
        searchButton.setOutputMarkupId(true);
        searchButton.setOutputMarkupPlaceholderTag(true);
        add(searchButton);

        BoxedTablePanel<SelectableBean<ObjectClassDto>> table = new BoxedTablePanel(ID_TABLE, createProvider(), createColumns());
        add(table);
    }

    private void updateSearchPerformed(AjaxRequestTarget target) {
        BoxedTablePanel table = getTable();
        ((ObjectClassDataProvider)table.getDataTable().getDataProvider()).setFilter(getObjectClassText().getModelObject());
        target.add(getTable());
    }

    private TextField<String> getObjectClassText() {
        return (TextField) get(ID_SEARCH_FIELD);
    }

    private BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(ID_TABLE);
    }

    private List<IColumn<SelectableBean<ObjectClassWrapper>, String>> createColumns() {
        List<IColumn<SelectableBean<ObjectClassWrapper>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>() {
            @Override
            protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<SelectableBean<ObjectClassWrapper>> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(target, table, rowModel, selected);
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.name")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, new PropertyModel<>(iModel, "value." + ObjectClassWrapper.F_OBJECT_CLASS_NAME)));
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.nativeName")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, new PropertyModel<>(iModel, "value." + ObjectClassWrapper.F_NATIVE_OBJECT_CLASS)));
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.type")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, getPageBase().createStringResource(
                        "SelectObjectClassesStepPanel.table.column.type.${value.auxiliary}",
                        iModel)));
            }
        });
        return columns;
    }

    private ObjectClassDataProvider createProvider() {
        return new ObjectClassDataProvider(SelectObjectClassesStepPanel.this, resourceModel.getObjectClassesModel());
    }
}
