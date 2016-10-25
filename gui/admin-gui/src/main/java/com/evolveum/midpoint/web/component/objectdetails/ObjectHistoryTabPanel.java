/*
 * Copyright (c) 2016 Evolveum
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
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
public class ObjectHistoryTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final String ID_MAIN_PANEL = "mainPanel";

    public ObjectHistoryTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                     PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        initLayout(focusWrapperModel, page);
    }

    private void initLayout(LoadableModel<ObjectWrapper<F>> focusWrapperModel, PageBase page) {
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, page, focusWrapperModel.getObject().getOid());
        panel.setOutputMarkupId(true);
        add(panel);
    }



}
