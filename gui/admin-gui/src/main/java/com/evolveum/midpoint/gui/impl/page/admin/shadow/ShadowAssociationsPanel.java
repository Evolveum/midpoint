/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

public class ShadowAssociationsPanel extends AbstractShadowPanel {

    private static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<ShadowAssociationsType>> parentModel;

    public ShadowAssociationsPanel(
            String id,
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationsType>> parentModel,
            IModel<ShadowWrapper> shadowModel) {
        super(id, shadowModel);
        this.parentModel = parentModel;
    }

    @Override
    protected void initLayout() {
        add(new ShadowAssociationsTable(ID_PANEL, parentModel, getResourceModel()));
    }
}
