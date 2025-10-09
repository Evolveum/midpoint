/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
