/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractTemplateChoicePanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Popup panel for creating task for unrecognized resource object (work with object class).
 */
public class TaskCreationForUnrecognizedObjectsPopup extends TaskCreationPopup<SynchronizationTaskFlavor> {

    public TaskCreationForUnrecognizedObjectsPopup(String id) {
        super(id);
    }

    @Override
    protected AbstractTemplateChoicePanel<SynchronizationTaskFlavor> createChoicePanel(String id) {
        return new AbstractTemplateChoicePanel<>(id) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                getFlavorModel().setObject(SynchronizationTaskFlavor.IMPORT);
            }

            @Override
            protected WebMarkupContainer createIconPanel(IModel<Tile<SynchronizationTaskFlavor>> tileModel, String idIcon) {
                WebMarkupContainer icon = new WebMarkupContainer(idIcon);
                icon.add(AttributeAppender.append("class", "fa fa-refresh"));
                return icon;
            }

            @Override
            protected void onTemplateChosePerformed(SynchronizationTaskFlavor view, AjaxRequestTarget target) {
                getFlavorModel().setObject(view);
            }

            @Override
            protected QName getType() {
                return TaskType.COMPLEX_TYPE;
            }

            @Override
            protected LoadableModel<List<Tile<SynchronizationTaskFlavor>>> loadTilesModel() {
                return new LoadableModel<>() {
                    @Override
                    protected List<Tile<SynchronizationTaskFlavor>> load() {
                        Tile<SynchronizationTaskFlavor> tile = new Tile<>(
                                null,
                                LocalizationUtil.translate("TaskCreationForUnrecognizedObjectsPopup.reclassification"));
                        tile.setValue(SynchronizationTaskFlavor.IMPORT);
                        tile.setSelected(true);
                        return List.of(tile);
                    }
                };
            }

            @Override
            protected Component createTilePanel(String id, IModel<Tile<SynchronizationTaskFlavor>> tileModel) {
                Component panel = super.createTilePanel(id, tileModel);
                panel.add(AttributeAppender.append("class", "disabled"));
                return panel;
            }

            @Override
            protected boolean isSelectable() {
                return true;
            }
        };
    }
}
