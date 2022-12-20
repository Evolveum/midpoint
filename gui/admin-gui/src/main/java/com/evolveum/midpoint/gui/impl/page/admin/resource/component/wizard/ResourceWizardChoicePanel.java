/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import javax.xml.namespace.QName;

public abstract class ResourceWizardChoicePanel<T extends TileEnum> extends EnumWizardChoicePanel<T, ResourceDetailsModel> {

    public ResourceWizardChoicePanel(String id, ResourceDetailsModel resourceModel, Class<T> tileTypeClass) {
        super(id, resourceModel, tileTypeClass);
    }

    protected abstract void onTileClickPerformed(T value, AjaxRequestTarget target);

    @Override
    protected QName getObjectType() {
        return ResourceType.COMPLEX_TYPE;
    }
}
