/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.component.ExpressionPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.ExpressionWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar.
 */
@Component

public class ExpressionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> {

    private static final long serialVersionUID = 1L;

    @Autowired
    GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }
    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return ExpressionType.COMPLEX_TYPE.equals(wrapper.getTypeName()) && wrapper instanceof ExpressionWrapper
                && ((ExpressionWrapper)wrapper).isConstructionExpression();
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext panelCtx) {
        ExpressionPropertyPanel panel = new ExpressionPropertyPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());

        return panel;
    }


}