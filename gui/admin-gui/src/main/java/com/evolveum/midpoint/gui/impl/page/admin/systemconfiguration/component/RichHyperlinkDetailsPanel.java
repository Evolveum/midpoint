/*
 * Copyright (c) 2010-2022 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RichHyperlinkDetailsPanel extends MultivalueContainerDetailsPanel<RichHyperlinkType> {

    public RichHyperlinkDetailsPanel(String id, IModel<PrismContainerValueWrapper<RichHyperlinkType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public RichHyperlinkDetailsPanel(String id, IModel<PrismContainerValueWrapper<RichHyperlinkType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<RichHyperlinkType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("RichHyperlinkType.label");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> getModelObject().getLabel();
            }

            @Override
            protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
                WebMarkupContainer c = new WebMarkupContainer(idTypeImage);
                c.setVisible(false);
                return c;
            }
        };

        return d;
    }
}
