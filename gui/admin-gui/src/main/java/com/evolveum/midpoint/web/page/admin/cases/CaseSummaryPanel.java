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
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class CaseSummaryPanel extends ObjectSummaryPanel<CaseType> {

    private static final long serialVersionUID = 1L;

    public CaseSummaryPanel(String id, Class type, IModel<CaseType> model, ModelServiceLocator serviceLocator) {
        super(id, type, model, serviceLocator);
    }

    @Override
    protected String getIconCssClass() {
        return GuiStyleConstants.EVO_CASE_OBJECT_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
