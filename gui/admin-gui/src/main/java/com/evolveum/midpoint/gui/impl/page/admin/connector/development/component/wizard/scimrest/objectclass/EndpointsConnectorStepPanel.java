/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public abstract class EndpointsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String MANUAL_OPTION = "__manual__";

    private static final int PAGE_SIZE = 5;

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_ROWS = "rows";
    private static final String ID_RADIO = "radio";
    private static final String ID_METHOD = "method";
    private static final String ID_URI = "uri";
    private static final String ID_SOURCE = "source";
    private static final String ID_PAGING_NAVIGATOR = "pagingNavigator";
    private static final String ID_MANUAL_ITEM = "manualItem";
    private static final String ID_MANUAL_RADIO = "manualRadio";
    private static final String ID_MANUAL_OPERATION = "manualOperation";
    private static final String ID_MANUAL_URI = "manualUri";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel;

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevHttpEndpointType>>> valuesModel;
    private IModel<String> selectedModel;
    private IModel<ConnDevHttpOperationType> manualOperationModel;
    private IModel<String> manualUriModel;

    public EndpointsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper);
        this.objectClassModel = objectClassModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initSelectionModels();
        initLayout();
    }

    /**
     * This step panel is reused across visits (it's only constructed once, on the first visit to
     * its parent part), so {@link #onInitialize()} - and the selection it set up from whatever was
     * confirmed back then - only ever runs once. Without re-syncing here, coming back to this step
     * later would keep showing a stale selection instead of the actually confirmed endpoint.
     */
    @Override
    public void applyState() {
        initSelectionModels();
    }

    private void createValuesModel() {
        valuesModel = new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> load() {
                try {
                    PrismContainerWrapper<ConnDevObjectClassInfoType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_DETECTED_SCHEMA, ConnDevSchemaType.F_OBJECT_CLASS));
                    Optional<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassContainer = container.getValues().stream().filter(value ->
                                    StringUtils.equals(value.getRealValue().getName(), objectClassModel.getObject().getRealValue().getName()))
                            .findFirst();

                    if (objectClassContainer.isPresent()) {
                        try {
                            PrismContainerWrapper<ConnDevHttpEndpointType> endpointsContainer = objectClassContainer
                                    .get().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);
                            // Filter endpoints which contain any of the supported intents for the script.
                            return endpointsContainer.getValues().stream()
                                    .filter(value ->
                                            getEndpointIntents().stream().anyMatch(
                                                i -> value.getRealValue().getSuggestedUse().contains(i))
                                    )
                                    .toList();
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return List.of();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Selection state lives here rather than on the candidates' own wrapper (unlike most
     * radio-group steps in this wizard) since the manual option has no backing candidate to flag
     * as selected.
     */
    private void initSelectionModels() {
        // Reuse the existing Model instances (just change their value) rather than replacing them -
        // initLayout() hands the ORIGINAL instances to the RadioGroup/form components once, at
        // construction time, and they keep that exact reference forever. Assigning a brand new
        // Model.of(...) to the field here (as applyState()'s repeat calls would) would silently
        // disconnect the already-built UI from this panel's state - it'd keep showing/writing to
        // the stale original instance while this method's own checks work against the new one.
        if (manualOperationModel == null) {
            manualOperationModel = Model.of(ConnDevHttpOperationType.GET);
        } else {
            manualOperationModel.setObject(ConnDevHttpOperationType.GET);
        }
        if (manualUriModel == null) {
            manualUriModel = Model.of("");
        } else {
            manualUriModel.setObject("");
        }

        List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> candidates = valuesModel.getObject();
        Optional<ConnDevHttpEndpointType> confirmed = getConfirmedEndpoint();
        String initial = null;
        if (confirmed.isPresent()) {
            boolean matchesCandidate = candidates.stream()
                    .anyMatch(candidate -> StringUtils.equals(candidate.getRealValue().getName(), confirmed.get().getName()));
            if (matchesCandidate) {
                initial = confirmed.get().getName();
            } else {
                initial = MANUAL_OPTION;
                manualOperationModel.setObject(confirmed.get().getOperation());
                manualUriModel.setObject(confirmed.get().getUri());
            }
        }
        if (selectedModel == null) {
            selectedModel = Model.of(initial);
        } else {
            selectedModel.setObject(initial);
        }
    }

    private Optional<ConnDevHttpEndpointType> getConfirmedEndpoint() {
        try {
            PrismContainerWrapper<ConnDevHttpEndpointType> container =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);
            return container.getValues().stream()
                    .map(PrismContainerValueWrapper::getRealValue)
                    .filter(value -> getEndpointIntents().stream().anyMatch(i -> value.getSuggestedUse().contains(i)))
                    .findFirst();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Collection<ConnDevHttpEndpointIntentType> getEndpointIntents();

    /**
     * Which script this step's endpoint feeds - matched against {@link WaitingScriptConnectorStepPanel#getScriptType()}
     * in {@link #markScriptForRegeneration} to find *this* operation's waiting step, not just the
     * nearest one of any kind (a single object class's wizard flow can have several, one per
     * operation).
     */
    protected abstract ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType();

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, selectedModel);
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        PageableListView<PrismContainerValueWrapper<ConnDevHttpEndpointType>> rows =
                new PageableListView<>(ID_ROWS, valuesModel, PAGE_SIZE) {
                    @Override
                    protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevHttpEndpointType>> listItem) {
                        Radio<String> radio = new Radio<>(ID_RADIO, Model.of(listItem.getModelObject().getRealValue().getName()), radioGroup);
                        radio.setOutputMarkupId(true);
                        listItem.add(radio);

                        Label method = new Label(ID_METHOD, createStringResource(listItem.getModelObject().getRealValue().getOperation()));
                        method.setOutputMarkupId(true);
                        listItem.add(method);

                        Label uri = new Label(ID_URI, () -> listItem.getModelObject().getRealValue().getUri());
                        uri.setOutputMarkupId(true);
                        listItem.add(uri);

                        Label source = new Label(ID_SOURCE, () -> listItem.getModelObject().getRealValue().getRelevantDocumentations().isEmpty()
                                ? createStringResource("EndpointsConnectorStepPanel.source.manual").getString()
                                : createStringResource("EndpointsConnectorStepPanel.source.documentation").getString());
                        source.setOutputMarkupId(true);
                        listItem.add(source);

                        listItem.add(AttributeAppender.append("style", "cursor: pointer;"));
                        listItem.add(new AjaxEventBehavior("click") {
                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                String name = listItem.getModelObject().getRealValue().getName();
                                if (!StringUtils.equals(name, selectedModel.getObject())) {
                                    selectedModel.setObject(name);
                                    target.add(radio);
                                }
                            }
                        });
                    }
                };
        rows.setOutputMarkupId(true);
        radioGroup.add(rows);

        PagingNavigator pagingNavigator = new PagingNavigator(ID_PAGING_NAVIGATOR, rows);
        pagingNavigator.add(new VisibleBehaviour(() -> rows.getPageCount() > 1));
        radioGroup.add(pagingNavigator);

        WebMarkupContainer manualItem = new WebMarkupContainer(ID_MANUAL_ITEM);
        manualItem.setOutputMarkupId(true);
        manualItem.add(AttributeAppender.append("style", "cursor: pointer;"));
        Radio<String> manualRadio = new Radio<>(ID_MANUAL_RADIO, Model.of(MANUAL_OPTION), radioGroup);
        manualRadio.setOutputMarkupId(true);

        manualItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                if (!MANUAL_OPTION.equals(selectedModel.getObject())) {
                    selectedModel.setObject(MANUAL_OPTION);
                    target.add(manualRadio);
                }
            }
        });
        radioGroup.add(manualItem);
        manualItem.add(manualRadio);

        DropDownChoicePanel<ConnDevHttpOperationType> manualOperation = new DropDownChoicePanel<>(
                ID_MANUAL_OPERATION, manualOperationModel, Model.ofList(List.of(ConnDevHttpOperationType.values())),
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ConnDevHttpOperationType object) {
                        return createStringResource(object).getObject();
                    }
                }, false);
        manualOperation.setOutputMarkupId(true);
        // Purely client-side (no AJAX round trip): check the manual radio the instant this field
        // gets focus, so the user sees it selected immediately. This is cosmetic only - the
        // server-side selectedModel is updated below, in the "change" handler, atomically with
        // the actual value commit, so there is no race with clicking Next (a plain AjaxLink that
        // does not submit the form / re-read the radio group on its own).
        manualOperation.getBaseFormComponent().add(AttributeAppender.append(
                "onfocus", "document.getElementById('" + manualRadio.getMarkupId() + "').checked = true;"));
        manualOperation.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedModel.setObject(MANUAL_OPTION);
                target.add(manualRadio);
            }
        });
        manualItem.add(manualOperation);

        TextField<String> manualUri = new TextField<>(ID_MANUAL_URI, manualUriModel);
        manualUri.setOutputMarkupId(true);
        manualUri.add(AttributeAppender.append(
                "onfocus", "document.getElementById('" + manualRadio.getMarkupId() + "').checked = true;"));
        manualUri.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedModel.setObject(MANUAL_OPTION);
                target.add(manualRadio);
            }
        });
        manualItem.add(manualUri);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // model updated automatically; the browser already unchecks sibling radios
                // natively (they share one HTML "name"), so no re-render is needed here
            }
        });
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (!saveEndpoint(target)) {
            return false;
        }
        markScriptForRegeneration();
        super.onNextPerformed(target);
        return false;
    }

    private boolean saveEndpoint(AjaxRequestTarget target) {
        String selected = selectedModel.getObject();
        if (selected == null) {
            getPageBase().error(getString("EndpointsConnectorStepPanel.noEndpointSelected"));
            target.add(getFeedback());
            return false;
        }
        boolean manual = MANUAL_OPTION.equals(selected);
        if (manual && StringUtils.isBlank(manualUriModel.getObject())) {
            getPageBase().error(getString("EndpointsConnectorStepPanel.manualUriEmpty"));
            target.add(getFeedback());
            return false;
        }

        try {
            PrismContainerWrapper<ConnDevHttpEndpointType> container =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);

            List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> valuesToRemove = container.getValues().stream()
                    .filter(value -> getEndpointIntents().stream().anyMatch(i -> value.getRealValue().getSuggestedUse().contains(i)))
                    .toList();

            if (!valuesToRemove.isEmpty()) {
                valuesToRemove.forEach(
                        value -> {
                            try {
                                container.remove(value, getDetailsModel().getPageAssignmentHolder());
                            } catch (SchemaException e) {
                                throw new RuntimeException(e);
                            }
                        });

                // Persist the removal on its own: adding the new value in the same delta as removing
                // the old one risks the new value being assigned the CID that only becomes free once
                // the removal is applied, colliding with it ("Attempt to add a container value with
                // an id that already exists").
                OperationResult removeResult = getHelper().onSaveObjectPerformed(target);
                if (removeResult != null && removeResult.isError()) {
                    target.add(getFeedback());
                    return false;
                }
            }

            PrismContainerWrapper<ConnDevHttpEndpointType> finalContainer =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);

            if (manual) {
                addManualEndpoint(finalContainer);
            } else {
                valuesModel.getObject().stream()
                        .filter(value -> StringUtils.equals(value.getRealValue().getName(), selected))
                        .findFirst()
                        .ifPresent(value -> addClonedEndpoint(finalContainer, value));
            }

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        if (result == null || result.isError()) {
            target.add(getFeedback());
            return false;
        }
        return true;
    }

    private Optional<WaitingScriptConnectorStepPanel> findWaitingPanel() {
        if (!(getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            return Optional.empty();
        }
        return parentWizardModel.getActiveChildrenSteps().stream()
                .filter(step -> step instanceof WaitingScriptConnectorStepPanel waitingPanel
                        && waitingPanel.getScriptType() == getScriptType())
                .map(step -> (WaitingScriptConnectorStepPanel) step)
                .findFirst();
    }

    private void markScriptForRegeneration() {
        findWaitingPanel().ifPresent(waitingPanel -> waitingPanel.resetScript(getPageBase()));
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleEnableBehaviour(
                () -> true,
                () -> findWaitingPanel().map(waitingPanel -> !waitingPanel.isGenerationRunning()).orElse(true));
    }

    private boolean existSavedScript() {
        return ConnectorDevelopmentWizardUtil.existScript(
                getDetailsModel(), getScriptType(), objectClassModel.getObject().getRealValue().getName());
    }

    @Override
    protected Component createCustomButtonsAnchor(String id) {
        InlineMenuItem skipItem = new InlineMenuItem(createStringResource("EndpointsConnectorStepPanel.skipGeneration")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onSkipGenerationPerformed(target);
                    }
                };
            }
        };
        DropdownButtonPanel menu = new DropdownButtonPanel(id, new DropdownButtonDto(null, null, null, List.of(skipItem))) {
            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-light border";
            }
        };
        menu.add(new VisibleBehaviour(this::existSavedScript));
        return menu;
    }

    private void onSkipGenerationPerformed(AjaxRequestTarget target) {
        findWaitingPanel().flatMap(WaitingScriptConnectorStepPanel::followingScriptPanel).ifPresent(scriptPanel -> {
            scriptPanel.dropGeneratedScript();
            getWizard().setActiveStepById(scriptPanel.getStepId());
            getWizard().fireActiveStepChanged(getWizard().getActiveStep());
            target.add(getWizard().getPanel());
        });
    }

    private void addClonedEndpoint(PrismContainerWrapper<ConnDevHttpEndpointType> finalContainer,
            PrismContainerValueWrapper<ConnDevHttpEndpointType> selected) {
        try {
            PrismContainerValue<ConnDevHttpEndpointType> clone =
                    selected.getRealValue().asPrismContainerValue().cloneComplex(CloneStrategy.REUSE);
            clone.removeItem(ConnDevHttpEndpointType.F_SUGGESTED_USE);
            clone.asContainerable().getSuggestedUse().addAll(getEndpointIntents());
            attachNewEndpointValue(finalContainer, clone);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void addManualEndpoint(PrismContainerWrapper<ConnDevHttpEndpointType> finalContainer) {
        try {
            ConnDevHttpEndpointType manual = new ConnDevHttpEndpointType()
                    .name(manualOperationModel.getObject().name() + " " + manualUriModel.getObject())
                    .operation(manualOperationModel.getObject())
                    .uri(manualUriModel.getObject());
            manual.getSuggestedUse().addAll(getEndpointIntents());
            attachNewEndpointValue(finalContainer, manual.asPrismContainerValue());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachNewEndpointValue(PrismContainerWrapper<ConnDevHttpEndpointType> finalContainer,
            PrismContainerValue<ConnDevHttpEndpointType> value) throws SchemaException {
        // Attach the value to the real container (not just its GUI wrapper list) so it is part of
        // the delta computed by onSaveObjectPerformed() below - otherwise the selection only
        // exists in the wrapper and is lost on reload.
        value.setId(null);
        value.setParent(finalContainer.getItem());
        finalContainer.getItem().add(value);

        PrismContainerValueWrapper<ConnDevHttpEndpointType> newValueWrapper = WebPrismUtil.createNewValueWrapper(
                finalContainer,
                value,
                getPageBase(),
                getDetailsModel().createWrapperContext());
        finalContainer.getValues().add(newValueWrapper);
    }

    @Override
    public boolean isCompleted() {
        if (ConnectorDevelopmentWizardUtil.existContainerValue(objectClassModel.getObject(), getScriptItemName())) {
            return true;
        }

        try {
            PrismContainerWrapper<ConnDevHttpEndpointType> container =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);

            return container.getValues().stream()
                    .anyMatch(value -> value.getRealValue() != null
                            && getEndpointIntents().stream().anyMatch(i -> value.getRealValue().getSuggestedUse().contains(i)));

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract ItemPath getScriptItemName();

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
