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

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

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
    private static final String ID_FULL_PROCESSING = "fullProcessing";
    private static final String ID_ERRORS = "errors";

    private IModel<ImportOptionsType> model;
    private IModel<Boolean> fullProcessingModel;

    public ImportOptionsPanel(String id, @NotNull IModel<ImportOptionsType> model, @NotNull IModel<Boolean> fullProcessingModel) {
        super(id);
        this.model = model;
        this.fullProcessingModel = fullProcessingModel;

        setRenderBodyOnly(true);

        initLayout();
    }

    private void initLayout() {
        add(new CheckBox(ID_PROTECTED_BY_ENCRYPTION, new PropertyModel<>(model, "encryptProtectedValues")));
        add(new CheckBox(ID_FETCH_RESOURCE_SCHEMA, new PropertyModel<>(model, "fetchResourceSchema")));
        add(new CheckBox(ID_KEEP_OID, new PropertyModel<>(model, "keepOid")));
        add(new CheckBox(ID_OVERWRITE_EXISTING_OBJECT, new PropertyModel<>(model, "overwrite")));
        add(new CheckBox(ID_REFERENTIAL_INTEGRITY, new PropertyModel<>(model, "referentialIntegrity")));
        add(new CheckBox(ID_SUMMARIZE_ERRORS, new PropertyModel<>(model, "summarizeErrors")));
        add(new CheckBox(ID_SUMMARIZE_SUCCESSES, new PropertyModel<>(model, "summarizeSucceses")));
        add(new CheckBox(ID_VALIDATE_DYNAMIC_SCHEMA, new PropertyModel<>(model, "validateDynamicSchema")));
        add(new CheckBox(ID_VALIDATE_STATIC_SCHEMA, new PropertyModel<>(model, "validateStaticSchema")));
        add(new CheckBox(ID_FULL_PROCESSING, fullProcessingModel));
        add(new TextField<Integer>(ID_ERRORS, new PropertyModel<>(model, "stopAfterErrors")));
    }
}
