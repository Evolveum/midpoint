/**
 * Copyright (c) 2016 Evolveum
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.component.tabs;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;


/**
 * Tab that contains a singleton panel.
 *
 * @author semancik
 */
public abstract class PanelTab<T extends ObjectType> extends AbstractTab {

    private static final long serialVersionUID = 1L;

    private VisibleEnableBehaviour visible;
    private WebMarkupContainer panel;

    public PanelTab(IModel<String> title) {
        super(title);
    }

    public PanelTab(IModel<String> title, VisibleEnableBehaviour visible) {
        super(title);
        this.visible = visible;
    }

    @Override
    public WebMarkupContainer getPanel(String panelId) {
        if (panel == null) {
            panel = createPanel(panelId);
        }

        return panel;
    }

    public abstract WebMarkupContainer createPanel(String panelId);

    @Override
    public boolean isVisible() {
        if (visible == null) {
            return true;
        }

        return visible.isVisible();
    }
}
