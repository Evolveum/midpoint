/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.table;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.ItemDefinitionPanel;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionDto;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.page.admin.resources.dto.AttributeDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class DefinitionTablePanel<T extends ItemDefinitionDto> extends BasePanel<List<T>> {

    private static final String ID_DEFINITIONS = "definitions";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";

    public DefinitionTablePanel(String id, IModel<List<T>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListDataProvider<T> attributeProvider = new ListDataProvider<>(this, getModel(), true) {

            @Override
            public IModel<T> model(T object) {
                return new IModel<>() {
                    @Override
                    public T getObject() {
                        return object;
                    }
                };
            }
        };
        attributeProvider.setSort(AttributeDto.F_DISPLAY_ORDER, SortOrder.ASCENDING);
        BoxedTablePanel<T> attributeTable = new BoxedTablePanel<>(ID_DEFINITIONS, attributeProvider, initColumns()) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return createTableButtons(id);
            }
        };
        attributeTable.setOutputMarkupId(true);
        attributeTable.setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
        attributeTable.setShowPaging(true);
        add(attributeTable);
    }

    private List<IColumn<T, String>> initColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new AjaxLinkColumn<>(createStringResource("SchemaListPanel.name"), ItemDefinitionDto.F_NAME, ItemDefinitionDto.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<T> rowModel) {
                ItemDefinitionPanel itemDefPanel = new ItemDefinitionPanel(getPageBase().getMainPopupBodyId(), (IModel<ItemDefinitionDto>) rowModel) {

                        @Override
                        protected void refresh(AjaxRequestTarget target) {
                            target.add(DefinitionTablePanel.this);
                        }
                };
                getPageBase().showMainPopup(itemDefPanel, target);
            }

            @Override
            public boolean isEnabled(IModel<T> rowModel) {
                return rowModel.getObject() instanceof ItemDefinitionDto;
            }
        });
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.displayName"), ItemDefinitionDto.F_DISPLAY_NAME));
//        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.nativeAttributeName"), AttributeDto.F_NATIVE_ATTRIBUTE_NAME, AttributeDto.F_NATIVE_ATTRIBUTE_NAME));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.minMax"), ItemDefinitionDto.F_MIN_MAX_OCCURS));
//        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.minMax"), ItemDefinitionDto.F_MAX_OCCURS));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.displayOrder"), ItemDefinitionDto.F_DISPLAY_ORDER, ItemDefinitionDto.F_DISPLAY_ORDER));
        columns.add(new PropertyColumn<>(createStringResource("PrismItemDefinitionType.type"), ItemDefinitionDto.F_TYPE, ItemDefinitionDto.F_TYPE));

//        CheckBoxColumn<AttributeDto> check = new CheckBoxColumn<>(createStringResource("SchemaListPanel.returnedByDefault"), AttributeDto.F_RETURNED_BY_DEFAULT);
//        check.setEnabled(false);
//        columns.add(check);

        return columns;
    }

    private WebMarkupContainer createTableButtons(String id) {
        return new ButtonBar<>(id, ID_BUTTON_BAR, DefinitionTablePanel.this, createToolbarButtonsList(ID_BUTTON));
    }


    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
//        buttonsList.add(createNewDefinitionBUtton(idButton));
        return buttonsList;
    }

    private AjaxIconButton createNewDefinitionBUtton(String buttonId) {
        return new AjaxIconButton(buttonId, new Model<>("fa fa-plus"), createStringResource("SchemaListPanel.newDefinition")) {

            @Override
            public void onClick(AjaxRequestTarget target) {

                //TODO ugly hack
                PrismPropertyDefinition ppd = createNewDefinition();
                ItemDefinitionPanel itemDefPanel = new ItemDefinitionPanel(getPageBase().getMainPopupBodyId(), new Model<>(new ItemDefinitionDto(ppd)), true) {

                    @Override
                    protected void refresh(AjaxRequestTarget target) {
                        DefinitionTablePanel.this.getModelObject().add((T) getModelObject());
                        target.add(DefinitionTablePanel.this);
                        newDefinitionAdded(target, getModelObject());

                    }
                };
                getPageBase().showMainPopup(itemDefPanel, target);
            }
        };
    }

    protected void newDefinitionAdded(AjaxRequestTarget target, ItemDefinitionDto newDefinition) {

    }

    protected PrismPropertyDefinition createNewDefinition() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
