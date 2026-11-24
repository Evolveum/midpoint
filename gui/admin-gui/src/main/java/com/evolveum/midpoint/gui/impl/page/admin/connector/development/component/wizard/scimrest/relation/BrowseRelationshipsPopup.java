/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.panel.SimpleCustomSearchPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

/**
 * Popup for selecting one of the other relationships, filterable by object class and name.
 */
public abstract class BrowseRelationshipsPopup extends BasePanel<RelationshipSelectionState> implements Popupable {

    private static final int PAGE_SIZE = 5;

    private static final String ID_OBJECT_CLASS_FILTER = "objectClassFilter";
    private static final String ID_NAME_SEARCH = "nameSearch";
    private static final String ID_SELECTION_FORM = "selectionForm";
    private static final String ID_RELATIONSHIP_RADIO_GROUP = "relationshipRadioGroup";
    private static final String ID_RELATIONSHIPS_TABLE = "relationshipsTable";
    private static final String ID_RELATIONSHIP_HEADER = "relationshipHeader";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_CONTINUE = "continue";

    private Fragment footer;

    public BrowseRelationshipsPopup(String id, IModel<RelationshipSelectionState> model) {
        super(id, model);
    }

    @Override
    public int getWidth() {
        return 1100;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("BrowseRelationshipsPopup.title");
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        IModel<RelationshipDto> pendingSelectionModel = Model.of();

        BoxedTablePanel<RelationshipDto> relationshipsTable = createRelationshipsTable();
        add(createObjectClassFilter(relationshipsTable));
        add(createNameSearchPanel(relationshipsTable));

        RadioGroup<RelationshipDto> relationshipRadioGroup = createRelationshipRadioGroup(pendingSelectionModel);
        relationshipRadioGroup.add(relationshipsTable);

        Form<Void> selectionForm = createSelectionForm();
        selectionForm.add(relationshipRadioGroup);
        add(selectionForm);

        footer = createFooter(pendingSelectionModel);
    }

    /**
     * Continues with the relationship selected in the selection state.
     *
     * @param target ajax request target
     */
    protected abstract void continuePerformed(AjaxRequestTarget target);

    private DropDownChoice<String> createObjectClassFilter(BoxedTablePanel<RelationshipDto> table) {
        DropDownChoice<String> filter = new DropDownChoice<>(ID_OBJECT_CLASS_FILTER,
                LambdaModel.of(getModel(), RelationshipSelectionState::getObjectClassFilter, RelationshipSelectionState::setObjectClassFilter),
                () -> getModelObject().getObjectClassFilterOptions()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return getString("BrowseRelationshipsPopup.allObjectClasses");
            }
        };
        filter.setNullValid(true);
        filter.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshRelationshipsTable(table, target);
            }
        });
        return filter;
    }

    private SimpleCustomSearchPanel createNameSearchPanel(BoxedTablePanel<RelationshipDto> table) {
        return new SimpleCustomSearchPanel(ID_NAME_SEARCH,
                LambdaModel.of(getModel(), RelationshipSelectionState::getNameFilter, RelationshipSelectionState::setNameFilter)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                refreshRelationshipsTable(table, target);
            }
        };
    }

    private Form<Void> createSelectionForm() {
        return new Form<>(ID_SELECTION_FORM);
    }

    private RadioGroup<RelationshipDto> createRelationshipRadioGroup(IModel<RelationshipDto> pendingSelectionModel) {
        RadioGroup<RelationshipDto> radioGroup = new RadioGroup<>(ID_RELATIONSHIP_RADIO_GROUP, pendingSelectionModel);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(footer);
            }
        });
        return radioGroup;
    }

    private BoxedTablePanel<RelationshipDto> createRelationshipsTable() {
        BoxedTablePanel<RelationshipDto> table = new BoxedTablePanel<>(
                ID_RELATIONSHIPS_TABLE, new ListDataProvider<>(this, () -> getModelObject().getFilteredOtherRelationships()), List.of(createRelationshipColumn())) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isPagingSizePanelVisible() {
                return false;
            }
        };
        table.setItemsPerPage(PAGE_SIZE);
        table.setOutputMarkupId(true);
        return table;
    }

    private IColumn<RelationshipDto, String> createRelationshipColumn() {
        return new AbstractColumn<>(Model.of("")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Component getHeader(String componentId) {
                return createRelationshipHeader(componentId);
            }

            @Override
            public void populateItem(Item<ICellPopulator<RelationshipDto>> cellItem, String componentId,
                    IModel<RelationshipDto> rowModel) {
                cellItem.add(createRelationshipRowPanel(componentId, rowModel));
            }
        };
    }

    private Fragment createRelationshipHeader(String id) {
        return new Fragment(id, ID_RELATIONSHIP_HEADER, this);
    }

    private RelationshipRowPanel createRelationshipRowPanel(String id, IModel<RelationshipDto> model) {
        return new RelationshipRowPanel(id, model);
    }

    private Fragment createFooter(IModel<RelationshipDto> pendingSelectionModel) {
        Fragment buttons = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        buttons.setOutputMarkupId(true);
        buttons.add(createCancelButton());
        buttons.add(createContinueButton(pendingSelectionModel));
        return buttons;
    }

    private AjaxButton createCancelButton() {
        return new AjaxButton(ID_CANCEL, createStringResource("Button.cancel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
    }

    private AjaxButton createContinueButton(IModel<RelationshipDto> pendingSelectionModel) {
        return new AjaxButton(ID_CONTINUE, createStringResource("BrowseRelationshipsPopup.continue")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                BrowseRelationshipsPopup.this.getModelObject().setSelectedRelationship(pendingSelectionModel.getObject());
                continuePerformed(target);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setEnabled(pendingSelectionModel.getObject() != null);
            }
        };
    }

    private void refreshRelationshipsTable(BoxedTablePanel<RelationshipDto> table, AjaxRequestTarget target) {
        table.setCurrentPage(0);
        target.add(table);
    }
}
