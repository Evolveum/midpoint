/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ProfilingLevel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ProfilingClassLoggerContainerValueWrapperImpl;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;

/**
 * @author skublik
 */
@Component
public class ProfilingLoggerLevelPanelFactory extends AbstractInputGuiComponentFactory<LoggingLevelType> {

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 1;
    }

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper.getParent() instanceof ProfilingClassLoggerContainerValueWrapperImpl && wrapper.getItemName().equals(ClassLoggerConfigurationType.F_LEVEL);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<LoggingLevelType> panelCtx) {
        DropDownChoicePanel<ProfilingLevel> dropDownProfilingLevel = new DropDownChoicePanel<>(panelCtx.getComponentId(), new ProfilingLevelModel(panelCtx),
                WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer<>(), true);

        return dropDownProfilingLevel;
    }

    private static class ProfilingLevelModel extends Model<ProfilingLevel> {

        private static final long serialVersionUID = 1L;

        private PrismPropertyPanelContext<LoggingLevelType> panelCtx;

        public ProfilingLevelModel(PrismPropertyPanelContext<LoggingLevelType> panelCtx) {
            this.panelCtx = panelCtx;
        }

        @Override
        public ProfilingLevel getObject() {
            return ProfilingLevel.fromLoggerLevelType(panelCtx.getRealValueModel().getObject());
        }

        @Override
        public void setObject(ProfilingLevel object) {
            super.setObject(object);
            panelCtx.getRealValueModel().setObject(ProfilingLevel.toLoggerLevelType(object));
        }
    }
}
