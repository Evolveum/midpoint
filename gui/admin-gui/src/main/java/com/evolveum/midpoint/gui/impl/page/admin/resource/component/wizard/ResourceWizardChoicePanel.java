/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardChoicePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

public abstract class ResourceWizardChoicePanel<T extends TileEnum> extends WizardChoicePanel<T> {

    public ResourceWizardChoicePanel(String id, ResourceDetailsModel resourceModel, Class<T> tileTypeClass) {
        super(id, resourceModel, tileTypeClass);
    }

    @Override
    protected void addDefaultTile(List<Tile<T>> list) {
        Tile<T> tile = new Tile<>(
                "fa fa-server",
                getPageBase().createStringResource("ResourceWizardChoicePanel.toResource").getString());
        list.add(tile);
    }

    @Override
    protected void onTileClick(T value, AjaxRequestTarget target) {
        if (value == null) {
            goToResourcePerformed();
            return;
        }
        onResourceTileClick(value, target);
    }

    protected abstract void onResourceTileClick(T value, AjaxRequestTarget target);

    private void goToResourcePerformed() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getResourceModel().getObjectType().getOid());
        getPageBase().navigateToNext(PageResource.class, parameters);
    }
}
