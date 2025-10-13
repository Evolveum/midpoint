/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.factory;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;

public interface GuiComponentFactory<T extends ItemPanelContext<?, ?>> {

    <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper);

    Component createPanel(T panelCtx);

    Integer getOrder();

    default void configure(T panelCtx, Component component) {
        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(component));
    }
}
