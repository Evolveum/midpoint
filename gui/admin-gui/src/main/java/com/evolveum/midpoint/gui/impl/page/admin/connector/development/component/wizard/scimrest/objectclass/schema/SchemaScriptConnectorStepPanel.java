/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-schema-script")
@PanelInstance(identifier = "cdw-schema-script",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.schemaScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SchemaScriptConnectorStepPanel extends ScriptsConnectorStepPanel {

    private static final String DOT_CLASS = SchemaScriptConnectorStepPanel.class.getName() + ".";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";

    private static final String PANEL_TYPE = "cdw-schema-script";
    static final String TASK_NATIVE_SCRIPTS_KEY = "taskNativeScriptKey";
    static final String TASK_CONNID_SCRIPTS_KEY = "taskConnIdScriptKey";

    public SchemaScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected List<String> getTokensKeys() {
        return List.of(TASK_NATIVE_SCRIPTS_KEY, TASK_CONNID_SCRIPTS_KEY);
    }

    @Override
    protected void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException {
        if (object.getIntent() == ConnDevScriptIntentType.NATIVE) {
            getDetailsModel().getConnectorDevelopmentOperation().saveNativeSchemaScript(object, task, result);
        } else if (object.getIntent() == ConnDevScriptIntentType.CONNID) {
            getDetailsModel().getConnectorDevelopmentOperation().saveConnIdSchemaScript(object, task, result);
        }

    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.schemaScript");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.schemaScript.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.schemaScript.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected void onAfterSave(AjaxRequestTarget target) {
        try {
            PrismReferenceWrapper<Referencable> resourceRef = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

            @Nullable PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef.getValue().getRealValue(), getPageBase());
            getPageBase().getCacheDispatcher().dispatchInvalidation(ResourceType.class, resource.getOid(), false, null);
            ProvisioningObjectsUtil.refreshResourceSchema(resource, OPERATION_REFRESH_SCHEMA, getPageBase(), false);

            getDetailsModel().reloadPrismObjectByOid();

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
