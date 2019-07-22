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
import com.evolveum.midpoint.gui.impl.prism.component.PolyStringEditorPanel;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class WorkItemDetailsPanelFactory extends AbstractGuiComponentFactory<CaseWorkItemType> {

    private static final long serialVersionUID = 1L;

    @Autowired
    GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return CaseWorkItemType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<CaseWorkItemType> panelCtx) {
        WorkItemDetailsPanel panel = new WorkItemDetailsPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);
        return panel;
    }

}
