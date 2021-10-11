/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ProfilingLevel;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerWrapperImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 *
 */
@Component
public class ProfilingLoggerLevelPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LoggingLevelType>>, Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE-1;
    }

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return wrapper.getParent() instanceof ProfilingClassLoggerContainerValueWrapperImpl && wrapper.getItemName().equals(ClassLoggerConfigurationType.F_LEVEL);
    }

    @Override
    public Panel createPanel(PrismPropertyPanelContext<LoggingLevelType> panelCtx) {
        DropDownChoicePanel<ProfilingLevel> dropDownProfilingLevel = new DropDownChoicePanel<>(panelCtx.getComponentId(), new Model<ProfilingLevel>() {

            private static final long serialVersionUID = 1L;


            @Override
            public ProfilingLevel getObject() {
            return ProfilingLevel.fromLoggerLevelType(panelCtx.getRealValueModel().getObject());
            }

            @Override
            public void setObject(ProfilingLevel object) {
                super.setObject(object);
                panelCtx.getRealValueModel().setObject(ProfilingLevel.toLoggerLevelType(object));
            }

        }, WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer<>(), true);


        return dropDownProfilingLevel;
    }
}
