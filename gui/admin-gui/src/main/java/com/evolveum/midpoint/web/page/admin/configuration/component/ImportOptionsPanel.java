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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ImportOptionsPanel extends Panel {

    private static final String ID_PROTECTED_BY_ENCRYPTION = "protectedByEncryption";
    private static final String ID_FETCH_RESOURCE_SCHEMA = "fetchResourceSchema";
    private static final String ID_KEEP_OID = "keepOid";
    private static final String ID_OVERWRITE_EXISTING_OBJECT = "overwriteExistingObject";
    private static final String ID_REFERENTIAL_INTEGRITY = "referentialIntegrity";
    private static final String ID_SUMMARIZE_ERRORS = "summarizeErrors";
    private static final String ID_SUMMARIZE_SUCCESSES = "summarizeSuccesses";
    private static final String ID_VALIDATE_DYNAMIC_SCHEMA = "validateDynamicSchema";
    private static final String ID_VALIDATE_STATIC_SCHEMA = "validateStaticSchema";
    private static final String ID_ERRORS = "errors";

    private IModel<ImportOptionsType> model;

    public ImportOptionsPanel(String id, IModel<ImportOptionsType> model) {
        super(id);
        Validate.notNull(model);
        this.model = model;

        setRenderBodyOnly(true);

        initLayout();
    }

    private void initLayout() {
        CheckBox protectedByEncryption = new CheckBox(ID_PROTECTED_BY_ENCRYPTION,
                new PropertyModel<Boolean>(model, "encryptProtectedValues"));
        add(protectedByEncryption);
        CheckBox fetchResourceSchema = new CheckBox(ID_FETCH_RESOURCE_SCHEMA,
                new PropertyModel<Boolean>(model, "fetchResourceSchema"));
        add(fetchResourceSchema);
        CheckBox keepOid = new CheckBox(ID_KEEP_OID,
                new PropertyModel<Boolean>(model, "keepOid"));
        add(keepOid);
        CheckBox overwriteExistingObject = new CheckBox(ID_OVERWRITE_EXISTING_OBJECT,
                new PropertyModel<Boolean>(model, "overwrite"));
        add(overwriteExistingObject);
        CheckBox referentialIntegrity = new CheckBox(ID_REFERENTIAL_INTEGRITY,
                new PropertyModel<Boolean>(model, "referentialIntegrity"));
        add(referentialIntegrity);
        CheckBox summarizeErrors = new CheckBox(ID_SUMMARIZE_ERRORS,
                new PropertyModel<Boolean>(model, "summarizeErrors"));
        add(summarizeErrors);
        CheckBox summarizeSuccesses = new CheckBox(ID_SUMMARIZE_SUCCESSES,
                new PropertyModel<Boolean>(model, "summarizeSucceses"));
        add(summarizeSuccesses);
        CheckBox validateDynamicSchema = new CheckBox(ID_VALIDATE_DYNAMIC_SCHEMA,
                new PropertyModel<Boolean>(model, "validateDynamicSchema"));
        add(validateDynamicSchema);
        CheckBox validateStaticSchema = new CheckBox(ID_VALIDATE_STATIC_SCHEMA,
                new PropertyModel<Boolean>(model, "validateStaticSchema"));
        add(validateStaticSchema);
        TextField<Integer> errors = new TextField<Integer>(ID_ERRORS,
                new PropertyModel<Integer>(model, "stopAfterErrors"));
        add(errors);
    }
}
