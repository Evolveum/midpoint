/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.page.admin.TemplateChoicePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Popup panel for creating task for recognized resource object (work with object type, so with kind and intent).
 */
public class TaskCreationFoCategorizedObjectsPopup extends TaskCreationPopup<CompiledObjectCollectionView> {

    public TaskCreationFoCategorizedObjectsPopup(String id) {
        super(id);
    }

    @Override
    protected TemplateChoicePanel createChoicePanel(String id) {
        return new TemplateChoicePanel(id) {

            @Override
            protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
                CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
                return profile.getObjectCollectionViews()
                        .stream()
                        .filter(view -> isSynchronizationTaskCollection(view.getArchetypeOid()))
                        .collect(Collectors.toList());
            }



            @Override
            protected void onTemplateChosePerformed(CompiledObjectCollectionView view, AjaxRequestTarget target) {
                getFlavorModel().setObject(determineTaskFlavour(view.getArchetypeOid()));
            }

            @Override
            protected QName getType() {
                return TaskType.COMPLEX_TYPE;
            }

            @Override
            protected boolean isSelectable() {
                return true;
            }
        };
    }

    private boolean isSynchronizationTaskCollection(String archetypeOid) {
        if (archetypeOid == null) {
            return false;
        }
        return archetypeOid.equals(SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())
                || archetypeOid.equals(SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())
                || archetypeOid.equals(SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
    }
}
