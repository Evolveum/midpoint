/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
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

    private IModel<ImportOptionsType> model;

    public ImportOptionsPanel(String id, IModel<ImportOptionsType> model) {
        super(id);
        Validate.notNull(model);
        this.model = model;
        
        initLayout();
    }
    
    private void initLayout() {
        CheckBox protectedByEncryption = new CheckBox("protectedByEncryption",
                new PropertyModel<Boolean>(model, "encryptProtectedValues"));
        add(protectedByEncryption);
        CheckBox fetchResourceSchema = new CheckBox("fetchResourceSchema",
                new PropertyModel<Boolean>(model, "fetchResourceSchema"));
        add(fetchResourceSchema);
        CheckBox keepOid = new CheckBox("keepOid",
                new PropertyModel<Boolean>(model, "keepOid"));
        add(keepOid);
        CheckBox overwriteExistingObject = new CheckBox("overwriteExistingObject",
                new PropertyModel<Boolean>(model, "overwrite"));
        add(overwriteExistingObject);
        CheckBox referentialIntegrity = new CheckBox("referentialIntegrity",
                new PropertyModel<Boolean>(model, "referentialIntegrity"));
        add(referentialIntegrity);
        CheckBox summarizeErrors = new CheckBox("summarizeErrors",
                new PropertyModel<Boolean>(model, "summarizeErrors"));
        add(summarizeErrors);
        CheckBox summarizeSuccesses = new CheckBox("summarizeSuccesses",
                new PropertyModel<Boolean>(model, "summarizeSucceses"));
        add(summarizeSuccesses);
        CheckBox validateDynamicSchema = new CheckBox("validateDynamicSchema",
                new PropertyModel<Boolean>(model, "validateDynamicSchema"));
        add(validateDynamicSchema);
        CheckBox validateStaticSchema = new CheckBox("validateStaticSchema",
                new PropertyModel<Boolean>(model, "validateStaticSchema"));
        add(validateStaticSchema);
        TextField<Integer> errors = new TextField<Integer>("errors",
                new PropertyModel<Integer>(model, "stopAfterErrors"));
        add(errors);
    }
}
