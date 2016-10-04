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

import com.evolveum.midpoint.gui.api.model.CountModelProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Tab that can display object count (small bubble with number) in the tab label.
 *
 * @author semancik
 */
public abstract class CountablePanelTab extends PanelTab implements CountModelProvider {

    private static final long serialVersionUID = 1L;

    public CountablePanelTab(IModel<String> title) {
        super(title);
    }

    public CountablePanelTab(IModel title, VisibleEnableBehaviour visible) {
        super(title, visible);
    }

    @Override
    public IModel<String> getCountModel() {
        // We cannot get the count information from the panel.
        // When we display the tab the panel does not exist yet.
        // The panel is created only when the tab is clicked.

        return new AbstractReadOnlyModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getCount();
            }
        };
    }

    public abstract String getCount();
}
