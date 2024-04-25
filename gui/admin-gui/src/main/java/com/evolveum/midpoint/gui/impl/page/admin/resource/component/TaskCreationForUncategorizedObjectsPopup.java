/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.gui.impl.page.admin.TemplateChoicePanel;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Popup panel for creating task for unrecognized resource object (work with object class).
 */
public class TaskCreationForUncategorizedObjectsPopup extends TaskCreationPopup<CompiledObjectCollectionView> {

    public TaskCreationForUncategorizedObjectsPopup(String id) {
        super(id);
    }

    @Override
    protected TemplateChoicePanel createChoicePanel(String id) {
        return new TemplateChoicePanel(id) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                getFlavorModel().setObject(SynchronizationTaskFlavor.SHADOW_RECLASSIFICATION);
            }

            @Override
            protected QName getType() {
                return TaskType.COMPLEX_TYPE;
            }

            @Override
            protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
                CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
                return profile.getObjectCollectionViews()
                        .stream()
                        .filter(view -> view.getArchetypeOid() != null
                                && view.getArchetypeOid().equals(SystemObjectsType.ARCHETYPE_SHADOW_RECLASSIFICATION_TASK.value()))
                        .collect(Collectors.toList());
            }

            @Override
            protected Component createTilePanel(String id, IModel<Tile<CompiledObjectCollectionView>> tileModel) {
                Component panel = super.createTilePanel(id, tileModel);
                panel.add(AttributeAppender.append("class", "disabled active"));
                return panel;
            }

            @Override
            protected boolean isSelectable() {
                return true;
            }

            @Override
            protected void onTemplateChosePerformed(CompiledObjectCollectionView view, AjaxRequestTarget target) {
                getFlavorModel().setObject(determineTaskFlavour(view.getArchetypeOid()));
            }
        };
    }

    protected boolean getDefaultSimulationTag() {
        return true;
    }
}
