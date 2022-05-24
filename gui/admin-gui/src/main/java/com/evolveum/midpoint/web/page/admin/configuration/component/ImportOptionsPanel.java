/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportOptionsType;

/**
 * @author lazyman
 */
public class ImportOptionsPanel extends BasePanel {

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
    private static final String ID_COMPAT_MODE = "compatMode";
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
        add(new CheckBoxPanel(ID_PROTECTED_BY_ENCRYPTION, new PropertyModel<>(model, "encryptProtectedValues"),
                createStringResource("importOptionsPanel.protectedByEncryption")));
        add(new CheckBoxPanel(ID_FETCH_RESOURCE_SCHEMA, new PropertyModel<>(model, "fetchResourceSchema"),
                createStringResource("importOptionsPanel.fetchResourceSchema")));
        add(new CheckBoxPanel(ID_KEEP_OID, new PropertyModel<>(model, "keepOid"),
                createStringResource("importOptionsPanel.keepOid")));
        add(new CheckBoxPanel(ID_OVERWRITE_EXISTING_OBJECT, new PropertyModel<>(model, "overwrite"),
                createStringResource("importOptionsPanel.overwriteExistingObject")));
        add(new CheckBoxPanel(ID_REFERENTIAL_INTEGRITY, new PropertyModel<>(model, "referentialIntegrity"),
                createStringResource("importOptionsPanel.referentialIntegrity")));
        add(new CheckBoxPanel(ID_SUMMARIZE_ERRORS, new PropertyModel<>(model, "summarizeErrors"),
                createStringResource("importOptionsPanel.summarizeErrors")));
        add(new CheckBoxPanel(ID_SUMMARIZE_SUCCESSES, new PropertyModel<>(model, "summarizeSucceses"),
                createStringResource("importOptionsPanel.summarizeSuccesses")));
        add(new CheckBoxPanel(ID_VALIDATE_DYNAMIC_SCHEMA, new PropertyModel<>(model, "validateDynamicSchema"),
                createStringResource("importOptionsPanel.validateDynamicSchema")));
        add(new CheckBoxPanel(ID_VALIDATE_STATIC_SCHEMA, new PropertyModel<>(model, "validateStaticSchema"),
                createStringResource("importOptionsPanel.validateStaticSchema")));
        add(new CheckBoxPanel(ID_FULL_PROCESSING, fullProcessingModel,
                createStringResource("importOptionsPanel.fullProcessing")));
        add(new CheckBoxPanel(ID_COMPAT_MODE, new PropertyModel<>(model, "compatMode"),
                createStringResource("importOptionsPanel.compatMode")));

        add(new TextField<Integer>(ID_ERRORS, new PropertyModel<>(model, "stopAfterErrors")));
    }
}
