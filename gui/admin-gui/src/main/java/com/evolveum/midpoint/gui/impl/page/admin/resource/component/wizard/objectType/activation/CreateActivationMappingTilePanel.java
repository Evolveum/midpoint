/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTilePanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.PrismContainerDefinition;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

public class CreateActivationMappingTilePanel extends ResourceTilePanel<PrismContainerDefinition, CreateActivationMappingTile> {
    public CreateActivationMappingTilePanel(String id, IModel<CreateActivationMappingTile> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        super.initLayout();
        add(AttributeAppender.replace(
                "class",
                "card col-12 catalog-tile-panel d-flex flex-column align-items-center p-2 h-100 mb-0 btn"));
        if (!getModelObject().canCreateNewValue()) {
            add(AttributeAppender.append(
                    "class",
                    "disabled"));
        } else {
            add(AttributeAppender.append(
                    "class",
                    "selectable"));
        }

        get(ID_DESCRIPTION).add(AttributeAppender.append("title", getModelObject().getDescription()));
    }
}
