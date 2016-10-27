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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * @author Kate Honchar
 */
public class RequestAssignmentTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RequestAssignmentTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(RequestAssignmentTabPanel.class);
    private static final String ID_MAIN_PANEL = "mainPanel";

    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

    public RequestAssignmentTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                    LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        this.assignmentsModel = assignmentsModel;
        initLayout(focusWrapperModel, page);
    }

    private void initLayout(LoadableModel<ObjectWrapper<F>> focusWrapperModel, PageBase page) {
    	Class targetFocusClass = getObjectWrapper().getObject().getCompileTimeClass();
        MultipleAssignmentSelectorPanel<F, UserType, RoleType> panel = new MultipleAssignmentSelectorPanel<F, UserType, RoleType>(ID_MAIN_PANEL,
                assignmentsModel, focusWrapperModel.getObject().getObject(), targetFocusClass, RoleType.class, page);
        add(panel);
    }

}
