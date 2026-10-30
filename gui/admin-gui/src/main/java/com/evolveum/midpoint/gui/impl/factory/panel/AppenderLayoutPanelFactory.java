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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderLayoutType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

@Component
public class AppenderLayoutPanelFactory  extends AbstractInputGuiComponentFactory<AppenderLayoutType> {

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 1;      // TODO: over, či je to správna priorita (pozri nižšie)
    }

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }
    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<AppenderLayoutType> panelCtx) {
        return new DropDownChoicePanel<>(panelCtx.getComponentId(), new AppenderLayoutModel(panelCtx),
                WebComponentUtil.createReadonlyModelFromEnum(AppenderLayoutType.class),
                new EnumChoiceRenderer<>(), false);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!AppenderConfigurationType.F_LAYOUT.equals( wrapper.getItemName())){
        return false;
    }
        PrismContainerValueWrapper<?> parent = wrapper.getParent();
        return parent != null && parent.getRealValue() instanceof AppenderConfigurationType;
    }

    private static class AppenderLayoutModel extends Model<AppenderLayoutType> {

        private final PrismPropertyPanelContext<AppenderLayoutType> panelCtx;

        AppenderLayoutModel(PrismPropertyPanelContext<AppenderLayoutType> panelCtx) {
            this.panelCtx = panelCtx;
        }

        @Override
        public AppenderLayoutType getObject() {
            AppenderLayoutType layout = panelCtx.getRealValueModel().getObject();
            return layout != null ? layout : AppenderLayoutType.PATTERN;
        }

        @Override
        public void setObject(AppenderLayoutType object) {
            super.setObject(object);
            panelCtx.getRealValueModel().setObject(object == AppenderLayoutType.PATTERN ? null : object);
        }
    }
}
