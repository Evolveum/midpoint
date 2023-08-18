/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.util.List;

import com.evolveum.midpoint.web.component.model.delta.DeltaDto;

import com.evolveum.midpoint.web.component.prism.show.ChangesPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class ModificationTargetPreviewPanel extends BasePanel<String> implements Popupable {

    private static final String ID_PANELS = "changes_panel";

    List<DeltaDto> deltaDtos;
    IModel<List<VisualizationDto>> visualizationDto;

    public ModificationTargetPreviewPanel(String id, IModel<String> messageModel, List<DeltaDto> deltaDtos, IModel<List<VisualizationDto>> model) {
        super(id, messageModel);
        this.deltaDtos = deltaDtos;
        this.visualizationDto = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        intiLayout();

    }

    protected void intiLayout() {
        ChangesPanel changesPanel = new ChangesPanel(ID_PANELS, visualizationDto) {

            @Override
            protected IModel<String> createTitle() {
                return () -> {
                    int size = visualizationDto.getObject().size();
                    String key = size != 1 ? "PagePreviewChanges.primaryChangesMore" : "PagePreviewChanges.primaryChangesOne";

                    return getString(key, size);
                };
            }
        };

        changesPanel.setOutputMarkupId(true);
        add(changesPanel);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.modification.details.panel.title");
    }
}
