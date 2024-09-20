/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.AssociationDefinitionWrapper;

import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;

import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.Iterator;
import java.util.List;

public abstract class AssociationTilePanel<T extends Tile<AssociationDefinitionWrapper>> extends BasePanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationTilePanel.class);

    private static final String ID_TITLE = "title";
    private static final String ID_SUBJECT = "subject";
    private static final String ID_OBJECT = "object";
    private static final String ID_ASSOCIATION_DATA = "associationData";

    private final ResourceDetailsModel resourceDetailsModel;


    public AssociationTilePanel(String id, IModel<T> model, ResourceDetailsModel resourceDetailsModel) {
        super(id, model);
        this.resourceDetailsModel = resourceDetailsModel;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append(
                "class",
                "d-flex flex-column vertical align-items-center rounded justify-content-left p-2 h-100"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
        setOutputMarkupId(true);

        Label title = new Label(ID_TITLE, createStringResource("AssociationTilePanel.title", getModelObject().getTitle()));
        title.setEscapeModelStrings(false);
        add(title);

        String subject = defineObject(getModelObject().getValue().getSubjects());

        Label subjectTitle = new Label(ID_SUBJECT, subject);
        subjectTitle.setEscapeModelStrings(false);
        add(subjectTitle);

        String object = defineObject(getModelObject().getValue().getObjects());

        Label objectTitle = new Label(ID_OBJECT, object);
        objectTitle.setEscapeModelStrings(false);
        add(objectTitle);

        String associationData = defineParticipant(getModelObject().getValue().getAssociationData());


        Label associationDataTitle = new Label(ID_ASSOCIATION_DATA, associationData);
        associationDataTitle.setEscapeModelStrings(false);
        add(associationDataTitle);
        associationDataTitle.add(new VisibleBehaviour(() -> getModelObject().getValue().getAssociationData() != null));

        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                AssociationTilePanel.this.onClick(getModelObject().getValue(), target);
            }
        });
    }

    private String defineObject(List<AssociationDefinitionWrapper.ParticipantWrapper> objects) {
        if (objects.isEmpty()) {
            return "";
        }

        StringBuilder ret = new StringBuilder();
        Iterator<AssociationDefinitionWrapper.ParticipantWrapper> iterator = objects.iterator();
        while(iterator.hasNext()) {
            AssociationDefinitionWrapper.ParticipantWrapper object = iterator.next();
            ret.append(defineParticipant(object));
            if (iterator.hasNext()) {
                ret.append(", ");
            }
        }
        return ret.toString();
    }

    private String defineParticipant(AssociationDefinitionWrapper.ParticipantWrapper participant) {
        if (participant == null) {
            return null;
        }

        try {
            if (participant.getKind() == null) {
                return LocalizationUtil.translate(
                        "AssociationTilePanel.objectClass",
                        new Object[]{participant.getObjectClass().getLocalPart()});
            }

            CompleteResourceSchema schemaResource = resourceDetailsModel.getRefinedSchema();
            ResourceObjectTypeDefinition def;
            if (StringUtils.isNotEmpty(participant.getIntent())) {
                def = schemaResource.getObjectTypeDefinition(participant.getKind(), participant.getIntent());
            } else {
                def = (ResourceObjectTypeDefinition) schemaResource.findDefaultDefinitionForKind(participant.getKind());
            }
            if (def != null) {
                return LocalizationUtil.translate(
                        "AssociationTilePanel.objectType",
                        new Object[]{GuiDisplayNameUtil.getDisplayName(def.getDefinitionBean())});
            }
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't load resource schema.", e);
        }
        return null;
    }

    protected abstract void onClick(AssociationDefinitionWrapper value, AjaxRequestTarget target);
}
