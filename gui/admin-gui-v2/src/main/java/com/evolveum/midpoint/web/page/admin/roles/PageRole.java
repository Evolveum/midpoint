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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

/**
 * @author lazyman
 */
public class PageRole extends PageAdminRoles {

    public static final String PARAM_ROLE_ID = "roleId";
    private static final String OPERATION_LOAD_ROLE = "pageRole.loadRole";
    private static final String OPERATION_SAVE_ROLE = "pageRole.saveRole";
    private IModel<String> model;

    public PageRole() {
        model = new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return loadRole();
            }
        };
        initLayout();
    }

    private String loadRole() {
        StringValue roleOid = getPageParameters().get(PARAM_ROLE_ID);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return null;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE);
        try {
            PrismObject<RoleType> role = getModelService().getObject(RoleType.class, roleOid.toString(), null, result);

            PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
            return domProcessor.serializeObjectToString(role);
        } catch (Exception ex) {
            result.recordFatalError(ex.getMessage(), ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        return null;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        final IModel<Boolean> editable = new Model<Boolean>(false);
        mainForm.add(new AjaxCheckBox("edit", editable) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                editPerformed(target, editable.getObject());
            }
        });
        AceEditor<String> editor = new AceEditor<String>("aceEditor", model);
        editor.setReadonly(!editable.getObject());
        mainForm.add(editor);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
                createStringResource("pageRole.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSaveError(target, form);
            }
        };
        mainForm.add(saveButton);

        AjaxLinkButton backButton = new AjaxLinkButton("backButton",
                createStringResource("pageRole.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageRoles.class);
            }
        };
        mainForm.add(backButton);
    }

    public void editPerformed(AjaxRequestTarget target, boolean editable) {
        AceEditor editor = (AceEditor) get("mainForm:aceEditor");

        target.appendJavaScript(editor.setReadonly(!editable));
    }

    public void onSaveError(AjaxRequestTarget target, Form form) {
        //todo implement
    }

    public void savePerformed(AjaxRequestTarget target) {

    }
}
