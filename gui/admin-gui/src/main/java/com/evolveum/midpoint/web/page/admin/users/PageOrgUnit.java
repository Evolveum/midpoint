/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class PageOrgUnit extends PageAdminUsers {

    public static final String PARAM_ORG_ID = "orgId";

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
    private static final String DOT_CLASS = PageOrgUnit.class.getName() + ".";

    private static final String ID_FORM = "form";
    private static final String ID_NAME = "name";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_COST_CENTER = "costCenter";
    private static final String ID_LOCALITY = "locality";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_PARENT_ORG_UNITS = "parentOrgUnits";
    private static final String ID_ORG_TYPE = "orgType";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";

    private IModel<OrgType> orgModel = new LoadableModel<OrgType>(false) {

        @Override
        protected OrgType load() {
            return loadOrgUnit();
        }
    };

    public PageOrgUnit() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new Model<String>(), createStringResource("ObjectType.name"),
                "col-md-4", "col-md-6", true);
        form.add(name);
        TextFormGroup displayName = new TextFormGroup(ID_DISPLAY_NAME, new Model<String>(),
                createStringResource("OrgType.displayName"), "col-md-4", "col-md-6", true);
        form.add(displayName);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new Model<String>(),
                createStringResource("ObjectType.description"), "col-md-4", "col-md-6");
        form.add(description);

        CheckFormGroup requestable = new CheckFormGroup(ID_REQUESTABLE, new Model<Boolean>(),
                createStringResource("OrgType.requestable"), "col-md-4", "col-md-6");
        form.add(requestable);

        TextFormGroup identifier = new TextFormGroup(ID_IDENTIFIER, new Model<String>(),
                createStringResource("OrgType.identifier"), "col-md-4", "col-md-6", false);
        form.add(identifier);
        TextFormGroup costCenter = new TextFormGroup(ID_COST_CENTER, new Model<String>(),
                createStringResource("OrgType.costCenter"), "col-md-4", "col-md-6", false);
        form.add(costCenter);
        TextFormGroup locality = new TextFormGroup(ID_LOCALITY, new Model<String>(),
                createStringResource("OrgType.locality"), "col-md-4", "col-md-6", false);
        form.add(locality);

        initButtons(form);
    }

    private void initButtons(Form form) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(save);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        form.add(back);
    }

    private OrgType loadOrgUnit() {
        //todo implement
        return null;
    }
}
