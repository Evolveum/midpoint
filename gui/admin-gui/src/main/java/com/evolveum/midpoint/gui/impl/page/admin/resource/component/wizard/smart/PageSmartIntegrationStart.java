/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/smartIntegrationStart", matchUrlForSecurity = "/admin/config/smartIntegrationStart")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION) })
public class PageSmartIntegrationStart extends PageAdminConfiguration {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSmartIntegrationStart.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_NEXT = "next";
    private static final String ID_ACE_EDITOR = "aceEditor";

    private static final String CLASS_DOT = PageSmartIntegrationStart.class.getName() + ".";
    private static final String OP_SAVE_RESOURCE = CLASS_DOT + "saveResource";

    private final IModel<String> initialResourceModel = Model.of("""
            <resource
                xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                xmlns:icfc="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-3">

                <name>TODO</name>
                <connectorRef>
                    <!-- TODO -->
                </connectorRef>
                <connectorConfiguration>
                    <!-- TODO -->
                </connectorConfiguration>
            </resource>""");

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        setOutputMarkupId(true);
        mainForm.add(new AjaxSubmitButton(ID_NEXT) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        });

        AceEditor editor = new AceEditor(ID_ACE_EDITOR, initialResourceModel);
        editor.setModeForDataLanguage(PrismContext.LANG_XML);
        mainForm.add(editor);
    }

    private void onNextPerformed(AjaxRequestTarget target) {
        var xml = initialResourceModel.getObject();

        LOGGER.info("Creating resource from XML:\n{}", xml);

        var resourceOid = taskAwareExecutor(target, OP_SAVE_RESOURCE)
                .run((task, result) -> {
                    ResourceType resourceObject = (ResourceType) PrismContext.get().parserFor(xml).parse().asObjectable();
                    return getSmartIntegrationService().createNewResource(
                            resourceObject.getName(),
                            resourceObject.getConnectorRef(),
                            resourceObject.getConnectorConfiguration(),
                            task, result);
                });

        if (resourceOid != null) {
            // TODO what if the test resource operation failed?
            PageSmartIntegrationDefiningTypes.navigateTo(this, resourceOid);
        }
    }
}
