/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.Wizard;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/resources/wizard", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
            label = "PageAdminResources.auth.resourcesAll.label",
            description = "PageAdminResources.auth.resourcesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL,
            label = "PageResourceWizard.auth.resource.label",
            description = "PageResourceWizard.auth.resource.description")})
public class PageResourceWizard extends PageAdmin {

    private static final String ID_WIZARD = "wizard";
    public static final String PARAM_OID = "oid";
    public static final String PARAM_CONFIG_ONLY = "configOnly";
    public static final String PARAM_READ_ONLY = "readOnly";
    private static final Trace LOGGER = TraceManager.getTrace(PageResourceWizard.class);

    // these models should be reset after each 'save' operation, in order to fetch current data (on demand)
    // each step should use corresponding model
    // these models have always non-null and mutable content
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelRaw;                // contains resolved connector as well
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelNoFetch;            // contains resolved connector as well
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelFull;
    @NotNull private final ResourceWizardIssuesModel issuesModel;

    // additional models that have to be reset after each 'save' operation
    @NotNull private final Collection<LoadableModel<?>> dependentModels = new HashSet<>();

    // for new resources: should be set after first save; for others: should be set on page creation
    private String editedResourceOid;

    private final boolean configurationOnly;
    private boolean readOnly;
    private ConfigurationStep configurationStep;

    public PageResourceWizard(@NotNull PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);                        // to be available in the methods called within this constructor as well

        configurationOnly = parameters.get(PARAM_CONFIG_ONLY).toBoolean();
        readOnly = parameters.get(PARAM_READ_ONLY).toBoolean();

        editedResourceOid = getResourceOid();                                // might be null at this moment

        LOGGER.debug("Resource wizard called with oid={}, configOnly={}, readOnly={}", editedResourceOid, configurationOnly, readOnly);

        modelRaw = createResourceModel(getOperationOptionsBuilder()
                        .raw()
                        .item(ResourceType.F_CONNECTOR_REF).resolve()
                        .build());
        modelNoFetch = createResourceModel(getOperationOptionsBuilder()
                        .noFetch()
                        .item(ResourceType.F_CONNECTOR_REF).resolve()
                        .build());
        modelFull = createResourceModel(null);

        issuesModel = new ResourceWizardIssuesModel(modelFull, this);

        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        createInstanceBreadcrumb();
    }

    @NotNull
    private NonEmptyLoadableModel<PrismObject<ResourceType>> createResourceModel(final Collection<SelectorOptions<GetOperationOptions>> options) {
        return new NonEmptyLoadableModel<PrismObject<ResourceType>>(false) {
            @NotNull
            @Override
            protected PrismObject<ResourceType> load() {
                try {
                    if (editedResourceOid == null) {
                        return getPrismContext().createObject(ResourceType.class);
                    }
                    PrismObject<ResourceType> resource = loadResourceModelObject(options);
                    if (resource == null) {
                        throw new RestartResponseException(PageError.class);
                    }
                    return resource;
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load resource", ex);
                    throw new RestartResponseException(PageError.class);
                }
            }
        };
    }

    // named differently from "loadResource", as this is used in the superclass
    private PrismObject<ResourceType> loadResourceModelObject(Collection<SelectorOptions<GetOperationOptions>> options) {
        Task task = createSimpleTask("loadResource");
        return WebModelServiceUtils.loadObject(ResourceType.class, editedResourceOid, options, this, task, task.getResult());
    }


    public String getEditedResourceOid() {
        return editedResourceOid;
    }

    public void setEditedResourceOid(String editedResourceOid) {
        this.editedResourceOid = editedResourceOid;
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (editedResourceOid == null) {
                    return createStringResource("PageResourceWizard.title").getString();
                }
                String name = WebComponentUtil.getName(modelRaw.getObject());
                return createStringResource("PageResourceWizard.title.edit", name).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel wizardModel = new ResourceWizardModel(this);
        if (!configurationOnly) {
            wizardModel.add(new NameStep(modelRaw, this));
        }
        configurationStep = new ConfigurationStep(modelNoFetch, this);
        wizardModel.add(configurationStep);
        if (!configurationOnly) {
            wizardModel.add(new SchemaStep(modelFull, this));
            wizardModel.add(new SchemaHandlingStep(modelFull, this));
            wizardModel.add(new SynchronizationStep(modelFull, this));
            wizardModel.add(new CapabilityStep(modelFull, this));
        }

        Wizard wizard = new Wizard(ID_WIZARD, new Model<>(wizardModel), issuesModel);
        wizard.setOutputMarkupId(true);
        add(wizard);
    }

    public void refreshIssues(@Nullable AjaxRequestTarget target) {
        issuesModel.reset();
        if (target != null) {
            Wizard wizard = (Wizard) get(ID_WIZARD);
            target.add(wizard.getIssuesPanel());
            target.add(wizard.getSteps());
            target.add(wizard.getButtons());
        }
    }

    public ConfigurationStep getConfigurationStep() {
        return configurationStep;
    }

    @NotNull public ResourceWizardIssuesModel getIssuesModel() {
        return issuesModel;
    }

    public void resetModels() {
        LOGGER.info("Resetting models");
        modelRaw.reset();
        modelNoFetch.reset();
        modelFull.reset();
        for (LoadableModel<?> model : dependentModels) {
            model.reset();
        }
    }

    public void registerDependentModel(@NotNull LoadableModel<?> model) {
        dependentModels.add(model);
    }

    // questionable
    public boolean isNewResource() {
        return editedResourceOid == null;
    }

    public ObjectDelta<ResourceType> computeDiff(PrismObject<ResourceType> oldResource, PrismObject<ResourceType> newResource) {
        ObjectDelta<ResourceType> delta = oldResource.diff(newResource);
        if (!delta.isModify()) {
            return delta;
        }
        delta.getModifications().removeIf(itemDelta -> ResourceType.F_FETCH_RESULT.equivalent(itemDelta.getPath()));
        return delta;
    }

    // TODO change to debug
    public void logDelta(ObjectDelta delta) {
        LOGGER.info("Applying delta:\n{}", delta.debugDump());
    }

    public boolean isCurrentStepComplete() {
        Wizard wizard = (Wizard) get(ID_WIZARD);
        WizardStep activeStep = (WizardStep) wizard.getModelObject().getActiveStep();
        return activeStep == null || activeStep.isComplete();
    }

    public Wizard getWizard() {
        return (Wizard) get(ID_WIZARD);
    }

    public boolean isConfigurationOnly() {
        return configurationOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void addEditingEnabledBehavior(Component... components) {
        for (Component component : components) {
            component.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isEnabled() {
                    return !readOnly;
                }
            });
        }
    }

    public void addEditingVisibleBehavior(Component... components) {
        for (Component component : components) {
            component.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return !readOnly;
                }
            });
        }
    }

    public NonEmptyModel<Boolean> getReadOnlyModel() {
        return new NonEmptyModel<Boolean>() {
            @NotNull
            @Override
            public Boolean getObject() {
                return readOnly;
            }

            @Override
            public void setObject(@NotNull Boolean object) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void detach() {
            }
        };
    }

    public void visualize(AjaxRequestTarget target) {
        setResponsePage(new PageResourceVisualization(modelFull.getObject()));
    }

    public boolean showSaveResultInPage(boolean saved, OperationResult result) {
        return saved || WebComponentUtil.showResultInPage(result);
    }

    protected String getResourceOid() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return resourceOid != null ? resourceOid.toString() : null;
    }
}
