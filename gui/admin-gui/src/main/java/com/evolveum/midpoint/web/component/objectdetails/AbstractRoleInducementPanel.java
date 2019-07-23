/*
 * Copyright (c) 2010-2017 Evolveum

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
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.assignment.InducementsPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

/**
 * Created by honchar
 */
public class AbstractRoleInducementPanel<R extends AbstractRoleType> extends AbstractObjectTabPanel<R> {
   
    private static final String ID_INDUCEMENT_PANEL = "inducementPanel";
   
    public AbstractRoleInducementPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<R>> focusWrapperModel,
                                    PageBase page) {
        super(id, mainForm, focusWrapperModel);
        initLayout();
    }

    private void initLayout() {
        InducementsPanel inducementsPanel = new InducementsPanel(ID_INDUCEMENT_PANEL,
        		PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), AbstractRoleType.F_INDUCEMENT));
//                new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), AbstractRoleType.F_INDUCEMENT));

        add(inducementsPanel);
    }
}
