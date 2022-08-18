/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.wizard.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ConnectorHostTypeComparator;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman
 */
public class NameStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(NameStep.class);

    private static final String DOT_CLASS = NameStep.class.getName() + ".";
    private static final String OPERATION_DISCOVER_CONNECTORS = DOT_CLASS + "discoverConnectors";
    private static final String OPERATION_SAVE_RESOURCE = DOT_CLASS + "saveResource";

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_CONNECTOR_HOST = "connectorHost";
    private static final String ID_CONNECTOR = "connector";

    private final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModelRaw;

    private final LoadableModel<String> resourceNameModel;
    private final LoadableModel<String> resourceDescriptionModel;
    private final LoadableModel<PrismObject<ConnectorHostType>> selectedHostModel;
    private final LoadableModel<List<PrismObject<ConnectorType>>> allConnectorsModel;
    private final LoadableModel<List<PrismObject<ConnectorType>>> relevantConnectorsModel; // filtered, based on selected host
    private final LoadableModel<PrismObject<ConnectorType>> selectedConnectorModel;
    private final IModel<String> schemaChangeWarningModel;
    private final LoadableModel<List<PrismObject<ConnectorHostType>>> allHostsModel; // this one is not dependent on resource content

    private final PageResourceWizard parentPage;

    public NameStep(@NotNull NonEmptyLoadableModel<PrismObject<ResourceType>> modelRaw, @NotNull final PageResourceWizard parentPage) {
        super(parentPage);
        this.parentPage = parentPage;
        this.resourceModelRaw = modelRaw;

        resourceNameModel = new LoadableModel<>() {
            @Override
            protected String load() {
                return PolyString.getOrig(resourceModelRaw.getObject().getName());
            }
        };
        parentPage.registerDependentModel(resourceNameModel);

        resourceDescriptionModel = new LoadableModel<>() {
            @Override
            protected String load() {
                return resourceModelRaw.getObject().asObjectable().getDescription();
            }
        };
        parentPage.registerDependentModel(resourceDescriptionModel);

        allHostsModel = new LoadableModel<>(false) {
            @Override
            protected List<PrismObject<ConnectorHostType>> load() {
                return WebModelServiceUtils.searchObjects(ConnectorHostType.class, null, null, NameStep.this.parentPage);
            }
        };

        selectedHostModel = new LoadableModel<>(false) {
            @Override
            protected PrismObject<ConnectorHostType> load() {
                return getExistingConnectorHost();
            }
        };
        parentPage.registerDependentModel(selectedHostModel);

        allConnectorsModel = new LoadableModel<>(false) {
            @Override
            protected List<PrismObject<ConnectorType>> load() {
                return WebModelServiceUtils.searchObjects(ConnectorType.class, null, null, NameStep.this.parentPage);
            }
        };
        parentPage.registerDependentModel(allConnectorsModel);

        relevantConnectorsModel = new LoadableModel<>(false) {
            @Override
            protected List<PrismObject<ConnectorType>> load() {
                return loadConnectors(selectedHostModel.getObject());
            }
        };
        parentPage.registerDependentModel(relevantConnectorsModel);

        selectedConnectorModel = new LoadableModel<>(false) {
            @Override
            protected PrismObject<ConnectorType> load() {
                return getExistingConnector();
            }
        };
        parentPage.registerDependentModel(selectedConnectorModel);

        schemaChangeWarningModel = () -> {
            PrismObject<ConnectorType> selectedConnector = getConnectorDropDown().getInput().getModel().getObject();
            return isConfigurationSchemaCompatible(selectedConnector) ? "" : getString("NameStep.configurationWillBeLost");
        };
        initLayout();
    }

    private void initLayout() {
        parentPage.addEditingEnabledBehavior(this);

        TextFormGroup name = new TextFormGroup(ID_NAME, resourceNameModel, createStringResource("NameStep.name"), "col-md-3", "col-md-6", true);
        add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, resourceDescriptionModel,
                createStringResource("NameStep.description"), "col-md-3", "col-md-6",
                false, 3);
        //parentPage.addEditingEnabledBehavior(description);
        add(description);

        DropDownFormGroup<PrismObject<ConnectorHostType>> hostDropDown = createHostDropDown();
        //parentPage.addEditingEnabledBehavior(hostDropDown);
        add(hostDropDown);

        DropDownFormGroup<PrismObject<ConnectorType>> connectorDropDown = createConnectorDropDown();
        //parentPage.addEditingEnabledBehavior(connectorDropDown);
        add(connectorDropDown);
    }

    @SuppressWarnings("unchecked")
    private DropDownFormGroup<PrismObject<ConnectorType>> getConnectorDropDown() {
        return (DropDownFormGroup<PrismObject<ConnectorType>>) get(ID_CONNECTOR);
    }

    private DropDownFormGroup<PrismObject<ConnectorType>> createConnectorDropDown() {

        // Suppress because if removed, compiler reports: IChoiceRenderer is abstract; cannot be instantiated
        //noinspection Convert2Diamond
        return new DropDownFormGroup<>(
                ID_CONNECTOR, selectedConnectorModel, relevantConnectorsModel,
                new IChoiceRenderer<PrismObject<ConnectorType>>() {

                    @Override
                    public PrismObject<ConnectorType> getObject(String id,
                            IModel<? extends List<? extends PrismObject<ConnectorType>>> choices) {
                        return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
                    }

                    @Override
                    public Object getDisplayValue(PrismObject<ConnectorType> object) {
                        return WebComponentUtil.getName(object);
                    }

                    @Override
                    public String getIdValue(PrismObject<ConnectorType> object, int index) {
                        if (index < 0) {
                            //noinspection unchecked
                            List<PrismObject<ConnectorType>> connectors = (List<PrismObject<ConnectorType>>) getConnectorDropDown().getInput().getChoices();
                            for (PrismObject<ConnectorType> connector : connectors) {
                                if (connector.getOid().equals(selectedConnectorModel.getObject().getOid())) {
                                    return Integer.toString(connectors.indexOf(connector));
                                }
                            }
                        }
                        return Integer.toString(index);
                    }
                }, createStringResource("NameStep.connectorType"), "col-md-3", "col-md-6", true) {

            @Override
            protected DropDownChoice<PrismObject<ConnectorType>> createDropDown(String id, IModel<List<PrismObject<ConnectorType>>> choices,
                    IChoiceRenderer<PrismObject<ConnectorType>> renderer, boolean required) {
                DropDownChoice<PrismObject<ConnectorType>> choice = super.createDropDown(id, choices, renderer, required);
                choice.add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(getConnectorDropDown().getAdditionalInfoComponent());
                    }
                });
                choice.setOutputMarkupId(true);
                return choice;
            }

            @Override
            protected Component createAdditionalInfoComponent(String id) {
                Label l = new Label(id, schemaChangeWarningModel);
                l.add(new AttributeAppender("class", "text-danger"));
                l.setOutputMarkupId(true);
                return l;
            }
        };
    }

    private boolean isConfigurationSchemaCompatible(PrismObject<ConnectorType> newConnectorObject) {
        if (newConnectorObject == null) {
            return true;        // shouldn't occur
        }

        PrismContainer<?> configuration = ResourceTypeUtil.getConfigurationContainer(resourceModelRaw.getObject());
        if (configuration == null || configuration.isEmpty() || configuration.getValue().hasNoItems()) {
            return true;            // no config -> no loss
        }

        // for the time being let us simply compare namespaces of the current and old connector
        PrismObject<ConnectorType> existingConnectorObject = getExistingConnector();
        if (existingConnectorObject == null) {
            return true;
        }

        ConnectorType existingConnector = existingConnectorObject.asObjectable();
        ConnectorType newConnector = newConnectorObject.asObjectable();
        return StringUtils.equals(existingConnector.getNamespace(), newConnector.getNamespace());
    }

    @Nullable
    private PrismObject<ConnectorType> getSelectedConnector() {
        PrismObject<ConnectorType> connector = null;
        DropDownFormGroup<PrismObject<ConnectorType>> connectorTypeDropDown = getConnectorDropDown();
        if (connectorTypeDropDown != null && connectorTypeDropDown.getInput() != null && connectorTypeDropDown.getInput().getModelObject() != null) {
            connector = connectorTypeDropDown.getInput().getModel().getObject();
        }
        return connector;
    }

    @Nullable
    private PrismObject<ConnectorType> getExistingConnector() {
        return ResourceTypeUtil.getConnectorIfPresent(resourceModelRaw.getObject());
    }

    @Nullable
    private PrismObject<ConnectorHostType> getExistingConnectorHost() {
        PrismObject<ConnectorType> connector = getExistingConnector();
        if (connector == null || connector.asObjectable().getConnectorHostRef() == null) {
            return null;
        }
        for (PrismObject<ConnectorHostType> host : allHostsModel.getObject()) {
            if (connector.asObjectable().getConnectorHostRef().getOid().equals(host.getOid())) {
                return host;
            }
        }
        return null;
    }

    @NotNull
    private DropDownFormGroup<PrismObject<ConnectorHostType>> createHostDropDown() {
        // Suppress because if removed, compiler reports: IChoiceRenderer is abstract; cannot be instantiated
        //noinspection Convert2Diamond
        return new DropDownFormGroup<>(ID_CONNECTOR_HOST, selectedHostModel,
                allHostsModel, new IChoiceRenderer<PrismObject<ConnectorHostType>>() {

            @Override
            public PrismObject<ConnectorHostType> getObject(String id,
                    IModel<? extends List<? extends PrismObject<ConnectorHostType>>> choices) {
                if (StringUtils.isBlank(id)) {
                    return null;
                }
                return choices.getObject().get(Integer.parseInt(id));
            }

            @Override
            public Object getDisplayValue(PrismObject<ConnectorHostType> object) {
                if (object == null) {
                    return NameStep.this.getString("NameStep.hostNotUsed");
                }
                return ConnectorHostTypeComparator.getUserFriendlyName(object);
            }

            @Override
            public String getIdValue(PrismObject<ConnectorHostType> object, int index) {
                return Integer.toString(index);
            }
        },
                createStringResource("NameStep.connectorHost"), "col-md-3", "col-md-6", false) {

            @Override
            protected DropDownChoice<PrismObject<ConnectorHostType>> createDropDown(String id, IModel<List<PrismObject<ConnectorHostType>>> choices,
                    IChoiceRenderer<PrismObject<ConnectorHostType>> renderer, boolean required) {
                DropDownChoice<PrismObject<ConnectorHostType>> choice = super.createDropDown(id, choices, renderer, required);
                choice.add(new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        discoverConnectorsPerformed(target);
                    }
                });
                return choice;
            }
        };
    }

    private List<PrismObject<ConnectorType>> loadConnectors(PrismObject<ConnectorHostType> host) {
        List<PrismObject<ConnectorType>> filtered = filterConnectors(host);

        filtered.sort((c1, c2) -> {
            String name1 = c1.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);
            String name2 = c2.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);

            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        });

        return filtered;
    }

    private List<PrismObject<ConnectorType>> filterConnectors(PrismObject<ConnectorHostType> host) {
        List<PrismObject<ConnectorType>> filtered = new ArrayList<>();
        for (PrismObject<ConnectorType> connector : allConnectorsModel.getObject()) {
            if (isConnectorOnHost(connector, host)) {
                filtered.add(connector);
            }
        }
        return filtered;
    }

    private boolean isConnectorOnHost(PrismObject<ConnectorType> connector, @Nullable PrismObject<ConnectorHostType> host) {
        PrismReference connHostRef = connector.findReference(ConnectorType.F_CONNECTOR_HOST_REF);
        String connHostOid = connHostRef != null ? connHostRef.getOid() : null;
        String hostOid = host != null ? host.getOid() : null;
        return Objects.equals(connHostOid, hostOid);
    }

    @SuppressWarnings("unchecked")
    private void discoverConnectorsPerformed(AjaxRequestTarget target) {
        DropDownChoice<PrismObject<ConnectorHostType>> connectorHostChoice =
                ((DropDownFormGroup<PrismObject<ConnectorHostType>>) get(ID_CONNECTOR_HOST)).getInput();
        PrismObject<ConnectorHostType> connectorHostObject = connectorHostChoice.getModelObject();
        ConnectorHostType host = connectorHostObject != null ? connectorHostObject.asObjectable() : null;

        if (host != null) {
            discoverConnectors(host);
            allConnectorsModel.reset();
        }
        relevantConnectorsModel.reset();

        DropDownFormGroup<PrismObject<ConnectorType>> connectorDropDown = getConnectorDropDown();
        PrismObject<ConnectorType> selectedConnector = connectorDropDown.getInput().getModelObject();
        if (selectedConnector != null) {
            if (!isConnectorOnHost(selectedConnector, connectorHostObject)) {
                PrismObject<ConnectorType> compatibleConnector = null;
                for (PrismObject<ConnectorType> relevantConnector : relevantConnectorsModel.getObject()) {
                    if (isConfigurationSchemaCompatible(relevantConnector)) {
                        compatibleConnector = relevantConnector;
                        break;
                    }
                }
                selectedConnectorModel.setObject(compatibleConnector);
            }
        }
        target.add(connectorDropDown.getInput(), connectorDropDown.getAdditionalInfoComponent(), ((PageBase) getPage()).getFeedbackPanel());
    }

    private void discoverConnectors(ConnectorHostType host) {
        PageBase page = (PageBase) getPage();
        Task task = page.createSimpleTask(OPERATION_DISCOVER_CONNECTORS);
        OperationResult result = task.getResult();
        try {
            ModelService model = page.getModelService();
            model.discoverConnectors(host, task, result);
        } catch (CommonException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't discover connectors", ex);
        } finally {
            result.recomputeStatus();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            page.showResult(result);
        }
    }

    @Override
    public void applyState() {
        parentPage.refreshIssues(null);
        if (parentPage.isReadOnly() || !isComplete()) {
            return;
        }

        PrismContext prismContext = parentPage.getPrismContext();
        Task task = parentPage.createSimpleTask(OPERATION_SAVE_RESOURCE);
        OperationResult result = task.getResult();
        boolean saved = false;

        try {
            PrismObject<ResourceType> resource = resourceModelRaw.getObject();
            PrismObject<ConnectorType> connector = getSelectedConnector();
            if (connector == null) {
                throw new IllegalStateException("No connector selected");        // should be treated by form validation
            }

            ObjectDelta<?> delta;
            final String oid = resource.getOid();
            boolean isNew = oid == null;
            if (isNew) {
                resource = prismContext.createObject(ResourceType.class);
                ResourceType resourceType = resource.asObjectable();
                resourceType.setName(PolyStringType.fromOrig(resourceNameModel.getObject()));
                resourceType.setDescription(resourceDescriptionModel.getObject());
                resourceType.setConnectorRef(ObjectTypeUtil.createObjectRef(connector, prismContext));
                delta = DeltaFactory.Object.createAddDelta(resource);
            } else {
                PrismObject<ResourceType> oldResourceObject =
                        WebModelServiceUtils.loadObject(ResourceType.class, oid,
                                GetOperationOptions.createRawCollection(),
                                parentPage, parentPage.createSimpleTask("loadResource"), result);
                if (oldResourceObject == null) {
                    throw new SystemException("Resource being edited (" + oid + ") couldn't be retrieved");
                }
                ResourceType oldResource = oldResourceObject.asObjectable();
                S_ItemEntry i = prismContext.deltaFor(ResourceType.class);
                if (!StringUtils.equals(PolyString.getOrig(oldResource.getName()), resourceNameModel.getObject())) {
                    i = i.item(ResourceType.F_NAME).replace(PolyString.fromOrig(resourceNameModel.getObject()));
                }
                if (!StringUtils.equals(oldResource.getDescription(), resourceDescriptionModel.getObject())) {
                    i = i.item(ResourceType.F_DESCRIPTION).replace(resourceDescriptionModel.getObject());
                }
                String oldConnectorOid = oldResource.getConnectorRef() != null ? oldResource.getConnectorRef().getOid() : null;
                String newConnectorOid = connector.getOid();
                if (!StringUtils.equals(oldConnectorOid, newConnectorOid)) {
                    i = i.item(ResourceType.F_CONNECTOR_REF).replace(ObjectTypeUtil.createObjectRef(connector, prismContext).asReferenceValue());
                }
                if (!isConfigurationSchemaCompatible(connector)) {
                    i = i.item(ResourceType.F_CONNECTOR_CONFIGURATION).replace();
                }
                delta = i.asObjectDelta(oid);
            }
            if (!delta.isEmpty()) {
                parentPage.logDelta(delta);
                WebModelServiceUtils.save(delta, ModelExecuteOptions.create().raw(), result, null, parentPage);
                parentPage.resetModels();
                saved = true;
            }

            if (isNew) {
                parentPage.setEditedResourceOid(delta.getOid());
            }

        } catch (RuntimeException | SchemaException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save resource", ex);
            result.recordFatalError(createStringResource("NameStep.message.saveResource.fatalError", ex.getMessage()).getString(), ex);
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if (parentPage.showSaveResultInPage(saved, result)) {
            parentPage.showResult(result);
        }

        parentPage.getConfigurationStep().resetConfiguration();
        parentPage.getConfigurationStep().updateConfigurationTabs();
    }
}
