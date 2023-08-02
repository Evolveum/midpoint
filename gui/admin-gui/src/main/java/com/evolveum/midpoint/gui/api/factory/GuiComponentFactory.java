/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
