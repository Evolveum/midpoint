/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import java.io.IOException;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-auth-scripts")
@PanelInstance(identifier = "cdw-auth-scripts",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.authScripts", icon = "fa fa-wrench"),
        containerPath = "empty")
public class AuthScriptsConnectorStepPanel extends ScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-auth-script";

    static final String TASK_AUTH_SCRIPTS_KEY = "taskAuthScriptKey";

    public AuthScriptsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ConnDevArtifactType getOriginalContainerValue() {
        try {
            PrismContainerWrapper<ConnDevArtifactType> container = getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTHENTICATION_SCRIPT));
            if (container != null) {
                return container.getValue().getRealValue();
            }
        } catch (SchemaException e) {
            //todo
            return null;
        }
        return null;
    }

    @Override
    protected String getTokenForTaskForObtainResult() {
        return TASK_AUTH_SCRIPTS_KEY;
    }

    @Override
    protected void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException {
        getDetailsModel().getConnectorDevelopmentOperation().saveAuthenticationScript(object, task, result);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.authScripts");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.authScripts.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.authScripts.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> {
            if (getValueModel() == null || !getValueModel().isLoaded()) {
                return false;
            }

            if (getValueModel().getObject() == null) {
                getValueModel().detach();
                return false;
            }

            ConnDevArtifactType result = getValueModel().getObject();
            return StringUtils.isNotBlank(result.getContent());
        };
    }

    //    @Override
//    public boolean onNextPerformed(AjaxRequestTarget target) {
//        WizardModel model = getWizard();
//        if (model.hasNext()) {
//            model.next();
//            target.add(model.getPanel());
//        }
//
//        return false;
//    }
}
