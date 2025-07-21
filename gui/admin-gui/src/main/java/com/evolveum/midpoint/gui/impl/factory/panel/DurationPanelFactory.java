/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DurationPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.validator.DurationValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.markup.html.form.Form;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

@Component
public class DurationPanelFactory extends TextPanelFactory<Duration> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        QName type = wrapper.getTypeName();
        return DOMUtil.XSD_DURATION.equals(type);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Duration> panelCtx) {
        InputPanel panel = super.getPanel(panelCtx);
        if (panel instanceof TextPanel) {
            panel = new DurationPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        }
        Form<?> form = Form.findForm(panelCtx.getParentContainer());
        form.add(new DurationValidator(panel, panelCtx.getPageBase().getConverter(Duration.class)));
        return panel;
    }

    @Override
    public Integer getOrder() {
        return 100000;
    }

}
