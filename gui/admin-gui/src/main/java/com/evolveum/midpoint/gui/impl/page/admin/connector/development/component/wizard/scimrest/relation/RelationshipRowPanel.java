/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Selectable row of one relationship.
 */
public class RelationshipRowPanel extends BasePanel<RelationshipDto> {

    private static final String ID_SELECTION_RADIO = "selectionRadio";
    private static final String ID_NAME = "name";
    private static final String ID_SUBJECT_SIDE = "subjectSide";
    private static final String ID_OBJECT_SIDE = "objectSide";

    public RelationshipRowPanel(String id, IModel<RelationshipDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createSelectionRadio());
        add(createNameLabel());
        add(createRelationshipSidePanel(ID_SUBJECT_SIDE, getModel().map(RelationshipDto::getSubject)));
        add(createRelationshipSidePanel(ID_OBJECT_SIDE, getModel().map(RelationshipDto::getObject)));
    }

    private Radio<RelationshipDto> createSelectionRadio() {
        Radio<RelationshipDto> radio = new Radio<>(ID_SELECTION_RADIO, getModel());
        radio.setOutputMarkupId(true);
        return radio;
    }

    private Label createNameLabel() {
        return new Label(ID_NAME, getModel().map(RelationshipDto::getDisplayName));
    }

    private RelationshipSidePanel createRelationshipSidePanel(String id, IModel<RelationshipSideDto> model) {
        return new RelationshipSidePanel(id, model);
    }
}
