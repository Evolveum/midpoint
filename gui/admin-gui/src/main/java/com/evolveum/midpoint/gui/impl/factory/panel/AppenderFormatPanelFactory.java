/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderFormatType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

@Component
public class AppenderFormatPanelFactory extends AbstractInputGuiComponentFactory<AppenderFormatType> {

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 1;
    }

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }
    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<AppenderFormatType> panelCtx) {
        return new DropDownChoicePanel<>(panelCtx.getComponentId(), new AppenderFormatModel(panelCtx),
                WebComponentUtil.createReadonlyModelFromEnum(AppenderFormatType.class),
                new EnumChoiceRenderer<>(), false);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!AppenderConfigurationType.F_FORMAT.equals( wrapper.getItemName())){
        return false;
    }
        PrismContainerValueWrapper<?> parent = wrapper.getParent();
        return parent != null && parent.getRealValue() instanceof AppenderConfigurationType;
    }

    private static class AppenderFormatModel extends Model<AppenderFormatType> {

        private final PrismPropertyPanelContext<AppenderFormatType> panelCtx;

        AppenderFormatModel(PrismPropertyPanelContext<AppenderFormatType> panelCtx) {
            this.panelCtx = panelCtx;
        }

        @Override
        public AppenderFormatType getObject() {
            AppenderFormatType format = panelCtx.getRealValueModel().getObject();
            return format != null ? format : AppenderFormatType.PATTERN;
        }

        @Override
        public void setObject(AppenderFormatType object) {
            super.setObject(object);
            panelCtx.getRealValueModel().setObject(object == AppenderFormatType.PATTERN ? null : object);
        }
    }
}
