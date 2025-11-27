/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.prism.Containerable;

public class VerticalFormMappingPanel<C extends MappingType> extends VerticalFormDefaultContainerablePanel<C> {

    public VerticalFormMappingPanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return true;
    }

    @Override
    protected boolean isVisibleSubContainerHeader(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    @Override
    protected boolean isShowEmptyButtonVisible() {
        return false;
    }

    protected String getCssClassForFormSubContainer() {
        return "m-0";
    }

    protected String getCssClassForFormSubContainerOfValuePanel() {
        return "card-body mb-0 px-3 pt-0 pb-3";
    }

    @Override
    protected String getCssClassForFormContainer() {
        return "p-0";
    }

    @Override
    protected boolean isRemoveValueButtonVisible() {
        return false;
    }
}
