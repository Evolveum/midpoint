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

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizedMessageTemplateContentType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TemplateContentDetailsPanel extends MultivalueContainerDetailsPanel<LocalizedMessageTemplateContentType> {

    public TemplateContentDetailsPanel(String id, IModel<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public TemplateContentDetailsPanel(String id, IModel<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<LocalizedMessageTemplateContentType> createDisplayNamePanel(String displayNamePanelId) {
        return new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue()));
    }
}
