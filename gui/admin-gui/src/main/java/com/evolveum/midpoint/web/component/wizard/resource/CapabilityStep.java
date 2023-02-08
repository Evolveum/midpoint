/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.capability.*;
import com.evolveum.midpoint.web.component.wizard.resource.dto.Capability;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author lazyman
 * @author shood
 */
public class CapabilityStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(CapabilityStep.class);

    private static final String DOT_CLASS = CapabilityStep.class.getName() + ".";
    private static final String OPERATION_SAVE_CAPABILITIES = DOT_CLASS + "saveCapabilities";

    private static final String ID_CAPABILITY_TABLE = "tableRows";
    private static final String ID_CAPABILITY_ROW = "capabilityRow";
    private static final String ID_CAPABILITY_NAME = "capabilityName";
    private static final String ID_CAPABILITY_LINK = "capabilityLink";
    private static final String ID_CAPABILITY_DELETE = "capabilityDelete";
    private static final String ID_CAPABILITY_ADD = "capabilityAdd";
    private static final String ID_CAPABILITY_CONFIG = "capabilityConfig";
    private static final String ID_TOOLTIP = "tooltip";

    private static final String DIALOG_SELECT_CAPABILITY = "capabilitySelectPopup";

    @NotNull private final PageResourceWizard parentPage;
    @NotNull private final NonEmptyLoadableModel<CapabilityStepDto> dtoModel;
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel;

    public CapabilityStep(@NotNull NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel, @NotNull PageResourceWizard parentPage) {
        super(parentPage);
        this.parentPage = parentPage;
        this.resourceModel = resourceModel;
        this.dtoModel = new NonEmptyLoadableModel<CapabilityStepDto>(false) {
            @Override
            @NotNull
            protected CapabilityStepDto load() {
                return loadDtoModel();
            }
        };
        parentPage.registerDependentModel(dtoModel);

        initLayout();
    }

    @NotNull
    private CapabilityStepDto loadDtoModel() {
        ResourceType resource = resourceModel.getObject().asObjectable();
        return new CapabilityStepDto(getCapabilitiesFromResource(resource));
    }

    private List<CapabilityDto<CapabilityType>> getCapabilitiesFromResource(ResourceType resource) {
        List<CapabilityDto<CapabilityType>> capabilityList = new ArrayList<>();

        try {
            CapabilitiesType capabilitiesBean = resource.getCapabilities();
            Collection<Class<? extends CapabilityType>> nativeClasses =
                    CapabilityUtil.getNativeCapabilityClasses(capabilitiesBean);
            List<CapabilityType> capabilities = CapabilityUtil.getCapabilities(capabilitiesBean, true);

            for (CapabilityType capability : capabilities) {
                if (Capability.supports(capability.getClass())) {
                    capability = fillDefaults(capability);
                    capabilityList.add(new CapabilityDto<>(capability, nativeClasses.contains(capability.getClass())));
                } else {
                    LOGGER.warn("Capability unsupported by the Resource Wizard: {}", capability);
                }
            }

        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load capabilities", e);
            getPageBase().error(getString("CapabilityStep.message.cantLoadCaps") + e);
        }

        return capabilityList;
    }

    public static CapabilityType fillDefaults(CapabilityType capability) {
        CapabilityType normalized = CloneUtil.clone(capability);
        CapabilityUtil.fillDefaults(normalized);
        return normalized;
    }

    protected void initLayout() {
        final ListDataProvider<CapabilityDto<CapabilityType>> capabilityProvider = new ListDataProvider<>(this,
                new PropertyModel<List<CapabilityDto<CapabilityType>>>(dtoModel, CapabilityStepDto.F_CAPABILITIES));

        WebMarkupContainer tableBody = new WebMarkupContainer(ID_CAPABILITY_TABLE);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        WebMarkupContainer configBody = new WebMarkupContainer(ID_CAPABILITY_CONFIG);
        configBody.setOutputMarkupId(true);
        add(configBody);

        DataView<CapabilityDto<CapabilityType>> capabilityDataView = new DataView<CapabilityDto<CapabilityType>>(ID_CAPABILITY_ROW, capabilityProvider) {

            @Override
            protected void populateItem(final Item<CapabilityDto<CapabilityType>> capabilityRow) {
                final CapabilityDto<CapabilityType> dto = capabilityRow.getModelObject();

                AjaxLink<Void> name = new AjaxLink<Void>(ID_CAPABILITY_LINK) {
                    private static final long serialVersionUID = 1L;    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editCapabilityPerformed(target, dto);
                    }
                };
                Label label = new Label(ID_CAPABILITY_NAME, new IModel<String>() {
                    @Override
                    public String getObject() {
                        String rv = dto.getDisplayName();
                        if (Boolean.FALSE.equals(dto.getCapability().isEnabled())) {
                            rv += " " + getString("CapabilityStep.disabled");
                        }
                        return rv;
                    }
                });
                name.add(label);
                capabilityRow.add(name);

                Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
                if (dto.getTooltipKey() != null) {
                    tooltipLabel.add(new AttributeAppender("data-original-title", getString(dto.getTooltipKey())));
                    tooltipLabel.add(new InfoTooltipBehavior());
                } else {
                    tooltipLabel.setVisible(false);
                }
                tooltipLabel.setOutputMarkupId(true);
                tooltipLabel.setOutputMarkupPlaceholderTag(true);
                name.add(tooltipLabel);

                AjaxLink<Void> deleteLink = new AjaxLink<Void>(ID_CAPABILITY_DELETE) {
                    private static final long serialVersionUID = 1L;                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteCapabilityPerformed(target, dto);
                    }
                };
                deleteLink.add(new VisibleEnableBehaviour() {
                    @Override
                    public boolean isVisible() {
                        return !dto.isAmongNativeCapabilities() && !parentPage.isReadOnly();
                    }
                });
                name.add(deleteLink);

                capabilityRow.add(AttributeModifier.replace("class", new IModel<Object>() {

                    @Override
                    public Object getObject() {
                        return isSelected(capabilityRow.getModelObject()) ? "success" : null;
                    }
                }));
            }
        };
        tableBody.add(capabilityDataView);

        AjaxLink<Void> addLink = new AjaxLink<Void>(ID_CAPABILITY_ADD) {
            private static final long serialVersionUID = 1L;            @Override
            public void onClick(AjaxRequestTarget target) {
                addCapabilityPerformed(target);
            }
        };
        parentPage.addEditingVisibleBehavior(addLink);
        add(addLink);

        ModalWindow dialog = new AddCapabilityDialog(DIALOG_SELECT_CAPABILITY, dtoModel) {

            @Override
            protected void addPerformed(AjaxRequestTarget target){
                addCapabilitiesPerformed(target, getSelectedData());
            }
        };
        add(dialog);
    }

    private boolean isSelected(CapabilityDto capabilityDto) {
        return dtoModel.getObject().getSelectedDto() == capabilityDto;
    }

    private WebMarkupContainer getTable(){
        return (WebMarkupContainer)get(ID_CAPABILITY_TABLE);
    }

    private WebMarkupContainer getConfigContainer(){
        return (WebMarkupContainer)get(ID_CAPABILITY_CONFIG);
    }

    private void deleteCapabilityPerformed(AjaxRequestTarget target, CapabilityDto rowModel) {
        if (dtoModel.getObject().getSelectedDto() == rowModel) {
            dtoModel.getObject().setSelected(null);
            target.add(getConfigContainer().replaceWith(new WebMarkupContainer(ID_CAPABILITY_CONFIG)));
        }
        dtoModel.getObject().getCapabilities().remove(rowModel);

        target.add(getTable());
    }

    private void addCapabilitiesPerformed(AjaxRequestTarget target, List<CapabilityDto<CapabilityType>> selected) {
        for (CapabilityDto<CapabilityType> dto: selected) {
            dtoModel.getObject().getCapabilities().add(dto);
        }
        target.add(getTable());
        AddCapabilityDialog window = (AddCapabilityDialog) get(DIALOG_SELECT_CAPABILITY);
        window.close(target);
    }

    private void addCapabilityPerformed(AjaxRequestTarget target) {
        AddCapabilityDialog window = (AddCapabilityDialog)get(DIALOG_SELECT_CAPABILITY);
        window.updateTable(target, dtoModel);
        window.show(target);
    }

    @SuppressWarnings("unchecked")
    private void editCapabilityPerformed(final AjaxRequestTarget target, CapabilityDto<? extends CapabilityType> capability) {
        dtoModel.getObject().setSelected(capability);

        WebMarkupContainer config = getConfigContainer();
        WebMarkupContainer newConfig;
        CapabilityType capType = capability.getCapability();

        if (capType instanceof ActivationCapabilityType) {
            newConfig = new CapabilityActivationPanel(ID_CAPABILITY_CONFIG, new Model<>((CapabilityDto<ActivationCapabilityType>) capability), parentPage) {

                @Override
                public IModel<List<QName>> createAttributeChoiceModel(final IChoiceRenderer<QName> renderer) {
                    LoadableModel<List<QName>> attributeChoiceModel = new LoadableModel<List<QName>>(false) {

                        @Override
                        protected List<QName> load() {
                            List<QName> choices = new ArrayList<>();

                            PrismObject<ResourceType> resourcePrism = resourceModel.getObject();

                            try {
                                ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resourcePrism);
                                if (schema != null) {
                                    ResourceObjectDefinition def = schema.findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
                                    if (def != null) {
                                        for (ResourceAttributeDefinition<?> attribute : def.getAttributeDefinitions()) {
                                            choices.add(attribute.getItemName());
                                        }
                                    }
                                }
                            } catch (CommonException | RuntimeException e) {
                                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load resource schema attributes.", e);
                                getPageBase().error("Couldn't load resource schema attributes" + e);
                            }

                            choices.sort((o1, o2) -> {
                                String s1 = (String) renderer.getDisplayValue(o1);
                                String s2 = (String) renderer.getDisplayValue(o2);

                                return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
                            });

                            return choices;
                        }
                    };
                    parentPage.registerDependentModel(attributeChoiceModel);
                    return attributeChoiceModel;
                }
            };
        } else if (capType instanceof ScriptCapabilityType) {
            newConfig = new CapabilityScriptPanel(ID_CAPABILITY_CONFIG, new Model<>((CapabilityDto<ScriptCapabilityType>) capability), getTable(), parentPage);
        } else if (capType instanceof CredentialsCapabilityType) {
            newConfig = new CapabilityCredentialsPanel(ID_CAPABILITY_CONFIG, new Model<>((CapabilityDto<CredentialsCapabilityType>)capability), getTable(), parentPage);
        } else {
            newConfig = new CapabilityValuePanel(ID_CAPABILITY_CONFIG, new Model<>((CapabilityDto<CapabilityType>) capability), getTable(), parentPage);
        }
        // TODO other specific capabilities (paged, count, ...)

        newConfig.setOutputMarkupId(true);
        config.replaceWith(newConfig);

        target.add(newConfig);
        target.add(getTable());
    }

    @Override
    public void applyState() {
        parentPage.refreshIssues(null);
        if (parentPage.isReadOnly() || !isComplete()) {
            return;
        }
        savePerformed();
    }

    private void savePerformed() {
        Task task = getPageBase().createSimpleTask(OPERATION_SAVE_CAPABILITIES);
        OperationResult result = task.getResult();
        ModelService modelService = getPageBase().getModelService();

        boolean saved = false;

        try {
            PrismObject<ResourceType> oldResource;
            final PrismObject<ResourceType> resourceObject = resourceModel.getObject();
            ResourceType resource = resourceObject.asObjectable();

            List<CapabilityType> allConfiguredCapabilities = new ArrayList<>();
            if (resource.getCapabilities().getConfigured() != null) {
                for (CapabilityType cap : CapabilityUtil.getAllCapabilities(resource.getCapabilities().getConfigured())) {
                    if (!Capability.supports(cap.getClass())) {
                        allConfiguredCapabilities.add(cap);
                    }
                }
            }
            for (CapabilityDto<?> dto : dtoModel.getObject().getCapabilities()) {
                allConfiguredCapabilities.add(dto.getCapability());
            }
            resource.getCapabilities().setConfigured(
                    CapabilityUtil.createCapabilityCollection(allConfiguredCapabilities));

            oldResource = WebModelServiceUtils.loadObject(ResourceType.class, resource.getOid(), getPageBase(), task, result);
            if (oldResource != null) {
                ObjectDelta<ResourceType> delta = parentPage.computeDiff(oldResource, resourceObject);
                if (!delta.isEmpty()) {
                    parentPage.logDelta(delta);
                    @SuppressWarnings("unchecked") Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil
                            .createCollection(delta);
                    modelService.executeChanges(deltas, null, getPageBase().createSimpleTask(OPERATION_SAVE_CAPABILITIES), result);
                    parentPage.resetModels();
                    saved = true;
                }
            }
        } catch (CommonException|RuntimeException e){
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save capabilities", e);
            result.recordFatalError(getString("CapabilityStep.message.cantSaveCaps"), e);
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if (parentPage.showSaveResultInPage(saved, result)) {
            getPageBase().showResult(result);
        }
    }
}
