/*
 * Copyright (c) 2010-2018 Evolveum

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
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * Created by honchar.
 */
public class FocusApplicablePoliciesTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final long serialVersionUID = 1L;

    private static final String ID_APPLICABLE_POLICIES_CONTAINER = "applicablePoliciesContainer";
    private static final String ID_APPLICABLE_POLICIES_PANEL = "applicablePolicyPanel";

    public FocusApplicablePoliciesTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusWrapperModel) {
        super(id, mainForm, focusWrapperModel);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer applicablePoliciesContainer = new WebMarkupContainer(ID_APPLICABLE_POLICIES_CONTAINER);
        applicablePoliciesContainer.setOutputMarkupId(true);
        add(applicablePoliciesContainer);

        ApplicablePolicyConfigPanel applicablePolicyPanel = new ApplicablePolicyConfigPanel(ID_APPLICABLE_POLICIES_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ASSIGNMENT));

        applicablePoliciesContainer.add(applicablePolicyPanel);
    }
}
