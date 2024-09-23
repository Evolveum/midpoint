/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schema;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.SelectObjectClassesStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.CreateComplexOrEnumerationWizardPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismSchemaWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceSchemaWizardPanel extends AbstractWizardPanel<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaWizardPanel.class);

    private static final String DOT_CLASS = ResourceOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";

    public ResourceSchemaWizardPanel(String id, WizardPanelHelper<ResourceType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createSchemaSteps()))));
    }

    private List<WizardStep> createSchemaSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new SelectObjectClassesStepPanel(getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceSchemaWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = ResourceSchemaWizardPanel.this.onSavePerformed(target);

                if (result != null && !result.isError()) {
                    try {
                        PrismObject<ResourceType> prismObject = getDetailsModel().getObjectWrapper().getObjectApplyDelta();
                        WebPrismUtil.cleanupEmptyContainers(prismObject);
                        getDetailsModel().getObjectClassesModel().detach();
                        getPageBase().getCacheDispatcher().dispatchInvalidation(ResourceType.class, getDetailsModel().getObjectWrapper().getOid(), false, null);
                        ProvisioningObjectsUtil.refreshResourceSchema(prismObject, OPERATION_REFRESH_SCHEMA, target, getPageBase(), result);
                        getDetailsModel().reset();
                        getDetailsModel().reloadPrismObjectModel(loadPrismObject());
                    } catch (CommonException e) {
                        LOGGER.error("Couldn't refresh resource schema.", e);
                    }
                    if (!result.isError()) {
                        onExitPerformed(target);
                    }
                }
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                return getPageBase().createStringResource("WizardPanel.submit");
            }

            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected boolean isExitButtonVisible() {
                return true;
            }
        });
        return steps;
    }

    private PrismObject<ResourceType> loadPrismObject() {
        Task task = getPageBase().createSimpleTask(WebModelServiceUtils.OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<ResourceType> prismObject = null;
        try {
            String focusOid = OnePageParameterEncoder.getParameter(getPageBase());
            prismObject = WebModelServiceUtils.loadObject(
                    ResourceType.class,
                    focusOid,
                    getAssignmentHolderModel().getPageResource().getOperationOptionsBuilder().build(),
                    false,
                    getPageBase(),
                    task,
                    result);
            LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, prismObject);
        } catch (RestartResponseException e) {
            //ignore restart exception
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            throw getPageBase().redirectBackViaRestartResponseException();
        }
        result.computeStatusIfUnknown();
        if (prismObject == null && result.isFatalError()) {
            getSession().getFeedbackMessages().clear();
            getSession().error(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"));
            throw new RestartResponseException(PageError404.class);
        }
        getPageBase().showResult(result, false);
        return prismObject;
    }
}
