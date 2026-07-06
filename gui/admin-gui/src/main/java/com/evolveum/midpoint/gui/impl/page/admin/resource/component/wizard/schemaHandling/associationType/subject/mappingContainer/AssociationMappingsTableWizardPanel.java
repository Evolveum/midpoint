/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceNavigationWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabSeparatedTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "rw-association-mappings")
@PanelInstance(identifier = "rw-association-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AssociationMappingsTableWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-association-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AssociationMappingsTableWizardPanel.outboundTable", icon = "fa fa-arrow-right-from-bracket"))
public abstract class AssociationMappingsTableWizardPanel<C extends Containerable> extends AbstractResourceNavigationWizardBasicPanel<C> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationMappingsTableWizardPanel.class);

    private static final String ID_TAB_TABLE = "tabTable";

    private final MappingDirection initialTab;
    boolean isInboundTabSelected = true;

    public AssociationMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper,
            MappingDirection initialTab) {
        super(id, superHelper);
        this.initialTab = initialTab;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createInboundTableTab());
        tabs.add(createOutboundTableTab());

        TabSeparatedTabbedPanel<ITab> tabPanel = new TabSeparatedTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onAjaxUpdate(@NotNull Optional<AjaxRequestTarget> optional) {
                optional.ifPresent(target -> target.add(getButtonsContainer()));
            }

            @Override
            protected void onClickTabPerformed(int index, @NotNull Optional<AjaxRequestTarget> target) {
                isInboundTabSelected = index == 0;
                if (getTable().isValidFormComponents(target.orElse(null))) {
                    super.onClickTabPerformed(index, target);
                }
            }
        };

        switchTabs(tabPanel);

        tabPanel.setOutputMarkupId(true);
        add(tabPanel);
    }

    private void switchTabs(TabSeparatedTabbedPanel<ITab> tabPanel) {
        switch (initialTab) {
            case INBOUND:
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                tabPanel.setSelectedTab(1);
                break;
        }
    }

    private @NotNull IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> inboundEvalModel() {
        return evalModel(ShadowAssociationDefinitionType.F_INBOUND);
    }

    private @NotNull IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> outboundEvalModel() {
        return evalModel(ShadowAssociationDefinitionType.F_OUTBOUND);
    }

    // TODO currently broken for empty inbound/outbound containers:
    //  when no mapping value exists yet and we create the first one here,
    //  midpoint persists only the evaluator expression, but not the mapping rows edited later.
    //  Most likely cause: newly created value in this multivalue container is not fully tracked
    //  in wrapper/delta processing (possibly value identity/ID handling).
    //  Fix this here, then remove ensureMappingExists(...) from ResourceAssociationTypeWizardPanelNew.
    private @NotNull IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> evalModel(
            @NotNull ItemPath containerPath) {

        boolean isInbound = containerPath.equals(ShadowAssociationDefinitionType.F_INBOUND);
        IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> assocSubjectModel =
                PrismContainerValueWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ItemPath.create(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));

        PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subject = assocSubjectModel.getObject();

        try {
            PrismContainerWrapper<MappingType> container = subject.findContainer(containerPath);

            PrismContainerValueWrapper<MappingType> valueWrapper;
            if (container.getValues().isEmpty()) {
                PrismContainerValue<MappingType> newValue = container.getItem().createNewValue();

                ExpressionType expression = newValue.asContainerable().beginExpression();
                if (isInbound) {
                    ExpressionUtil.updateAssociationSynchronizationExpressionValue(
                            expression,
                            new AssociationSynchronizationExpressionEvaluatorType());
                } else {
                    ExpressionUtil.updateAssociationConstructionExpressionValue(
                            expression,
                            new AssociationConstructionExpressionEvaluatorType());
                }

                valueWrapper = WebPrismUtil.createNewValueWrapper(
                        container,
                        newValue,
                        getPageBase(),
                        getAssignmentHolderDetailsModel().createWrapperContext());

                valueWrapper.setStatus(ValueStatus.ADDED);
                container.getValues().add(valueWrapper);
            } else {
                valueWrapper = container.getValues().get(0);
            }

            ItemPath evaluatorPath = ItemPath.create(
                    isInbound
                            ? SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION
                            : SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION);

            PrismContainerValueWrapper<MappingType> finalValueWrapper = valueWrapper;
            return PrismContainerValueWrapperModel.fromContainerValueWrapper(
                    () -> finalValueWrapper,
                    evaluatorPath);

        } catch (SchemaException e) {
            throw new RuntimeException(
                    "Cannot load " + (isInbound ? "inbound" : "outbound") + " association evaluator",
                    e);
        }
    }

    private boolean isAttributeVisible() {
        try {
            ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(
                    getAssignmentHolderDetailsModel().getRefinedSchema(), getValueModel().getObject());
            if (assocDef == null) {
                return false;
            }
            return assocDef.isComplex();
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot load resource schema", e);
            return false;
        }
    }

    public TabbedPanel<ITab> getTabPanel() {
        //noinspection unchecked
        return ((TabbedPanel<ITab>) get(ID_TAB_TABLE));
    }

    @SuppressWarnings("rawtypes")
    protected AssociationAttributeMappingsTable getTable() {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        return (AssociationAttributeMappingsTable) tabPanel.get(TabbedPanel.TAB_PANEL_ID);
    }

    @Override
    protected String getSaveLabelKey() {
        return "InboundMappingsTableWizardPanel.saveButton";
    }

    protected abstract void inEditInboundAttributeValue(
            IModel<PrismContainerValueWrapper<MappingType>> value,
            AjaxRequestTarget target,
            MappingDirection initialTab);

    protected abstract void inEditOutboundAttributeValue(
            IModel<PrismContainerValueWrapper<MappingType>> value,
            AjaxRequestTarget target,
            MappingDirection initialTab);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("AssociationMappingsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AssociationMappingsTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType) {
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }

    private ResourceType getResourceTypeObject() {
        return getAssignmentHolderDetailsModel().getObjectType();
    }

    private ITab createInboundTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.inbound")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationAttributeMappingsTable<>(panelId, Model.of(MappingDirection.INBOUND),
                        Model.of(false), inboundEvalModel(), getResourceTypeObject().getOid()) {

                    @Override
                    protected ResourceType getResourceType() {
                        return getResourceWithAppliedDeltaType();
                    }

                    @Override
                    protected boolean isAttributeVisible() {
                        return AssociationMappingsTableWizardPanel.this.isAttributeVisible();
                    }

                    @Override
                    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
                        creatEditMainConfigurationButton(toolbar);
                        super.initPanelToolbarButtons(toolbar);
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return true;
                    }

                    @Override
                    protected void performOnEditMapping(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditInboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    protected void creatEditMainConfigurationButton(@NotNull RepeatingView repeatingView) {
        AjaxIconButton newObjectButton = new AjaxIconButton(repeatingView.newChildId(),
                Model.of("fa fa-cog"),
                createStringResource("AssociationMappingsTableWizardPanel.editMainConfigurationButton")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showMainMappingConfigurationWizardFragment(target, isInboundTabSelected);
            }
        };

        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(AttributeAppender.replace("class", "btn btn-link ms-auto"));
        repeatingView.add(newObjectButton);
    }

    protected abstract void showMainMappingConfigurationWizardFragment(
            AjaxRequestTarget target, boolean isInboundTabSelected);

    private @NotNull ITab createOutboundTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.outbound")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationAttributeMappingsTable<>(panelId, Model.of(MappingDirection.OUTBOUND),
                        Model.of(false), outboundEvalModel(), getResourceTypeObject().getOid()) {

                    @Override
                    protected ResourceType getResourceType() {
                        return getResourceWithAppliedDeltaType();
                    }

                    @Override
                    protected boolean isAttributeVisible() {
                        return AssociationMappingsTableWizardPanel.this.isAttributeVisible();
                    }

                    @Override
                    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
                        creatEditMainConfigurationButton(toolbar);
                        super.initPanelToolbarButtons(toolbar);
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return false;
                    }

                    @Override
                    protected void performOnEditMapping(@NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditOutboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    protected ResourceType getResourceWithAppliedDeltaType() {
        ResourceDetailsModel resourceDetailsModel = getAssignmentHolderDetailsModel();
        PrismObjectWrapper<ResourceType> objectWrapper = resourceDetailsModel.getObjectWrapper();
        PrismObject<ResourceType> objectApplyDelta;
        try {
            objectApplyDelta = objectWrapper.getObjectApplyDelta();
        } catch (CommonException e) {
            LOGGER.error("Couldn't get resource object with applied delta, returning the original object. Details: {}",
                    e.getMessage(), e);
            return null;
        }

        return objectApplyDelta.asObjectable();
    }

    @Override
    protected String getSubmitButtonCssClass() {
        return "ms-auto btn-primary";
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        //TBD
    }

    @Override
    protected String getButtonContainerAdditionalCssClass() {
        return "col-12 p-0";
    }
}
