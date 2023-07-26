/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DurationPanel;
import com.evolveum.midpoint.web.component.input.DurationWithOneElementPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.validator.DurationValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DelayedDeleteActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PreProvisionActivationMappingType;
import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.form.Form;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import java.util.List;

@Component
public class DurationWithOneElementPanelFactory extends TextPanelFactory<Duration> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    private List<ItemName> positiveItems = List.of(DelayedDeleteActivationMappingType.F_DELETE_AFTER);

    private List<ItemName> negativeItems = List.of(PreProvisionActivationMappingType.F_CREATE_BEFORE);

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        QName type = wrapper.getTypeName();
        if (!DOMUtil.XSD_DURATION.equals(type)) {
            return false;
        }
        if (notMatchForItemName(wrapper.getItemName())) {
            return false;
        }
        if (valueWrapper.getRealValue() == null) {
            return true;
        }
        return containsOnlyOneElement(valueWrapper);
    }

    private boolean notMatchForItemName(ItemName itemName) {
        for (ItemName positiveItem : positiveItems) {
            if(positiveItem.equivalent(itemName)) {
                return false;
            }
        }

        for (ItemName negativeItem : negativeItems) {
            if(negativeItem.equivalent(itemName)) {
                return false;
            }
        }
        return true;
    }

    private <VW extends PrismValueWrapper<?>> boolean containsOnlyOneElement(VW valueWrapper) {
        Duration duration = (Duration) valueWrapper.getRealValue();
        boolean foundValue = false;

        if (duration.getYears() > 0) {
            foundValue = true;
        }

        if (duration.getMonths() > 0) {
            if (foundValue) {
                return false;
            }
            foundValue = true;
        }

        if (duration.getDays() > 0) {
            if (foundValue) {
                return false;
            }
            foundValue = true;
        }

        if (duration.getHours() > 0) {
            if (foundValue) {
                return false;
            }
            foundValue = true;
        }

        if (duration.getMinutes() > 0) {
            if (foundValue) {
                return false;
            }
            foundValue = true;
        }

        if (duration.getSeconds() > 0) {
            if (foundValue) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Duration> panelCtx) {
        InputPanel panel = super.getPanel(panelCtx);
        if (panel instanceof TextPanel) {
            boolean isPositive =
                    positiveItems.stream().anyMatch(positiveItem -> positiveItem.equivalent(panelCtx.getDefinitionName()));
            panel = new DurationWithOneElementPanel(
                    panelCtx.getComponentId(), panelCtx.getRealValueModel(), isPositive);
        }
        return panel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
