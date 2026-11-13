/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractCapabilityTestStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ResourceObjectsTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectSelection;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.input.EnumCardChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for testing the GET (search by ID) operation.
 */
@PanelType(name = "cdw-show-search-by-id-result")
@PanelInstance(identifier = "cdw-show-search-by-id-result",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.showSearchByIdResult", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchByIdObjectConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String CLASS_DOT = SearchByIdObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_RETRIEVE_OBJECT = CLASS_DOT + "retrieveObject";

    private static final String PANEL_TYPE = "cdw-show-search-by-id-result";

    private static final String ID_OBJECT_SOURCE = "objectSource";
    private static final String ID_SEARCH_RESULTS = "searchResults";
    private static final String ID_MANUAL_IDENTIFIER = "manualIdentifier";
    private static final String ID_IDENTIFIER = "identifier";

    /**
     * Selected way of choosing the object: from the search results or by a typed identifier.
     */
    private final IModel<SearchByIdTestObjectSource> objectSourceModel = Model.of(SearchByIdTestObjectSource.SEARCH_RESULTS);

    /**
     * State of the object lookup and of its failure.
     */
    private final TestObjectSelection selection;

    /**
     * @param flowState shared with {@link SearchByIdResultConnectorStepPanel}, which shows the object retrieved here
     */
    public SearchByIdObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
        this.selection = new TestObjectSelection(helper.getDetailsModel(), valueModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ResourceObjectsTablePanel searchResults = createSearchResultsPanel();
        add(searchResults);
        WebMarkupContainer manualIdentifier = createManualIdentifierPanel();
        add(manualIdentifier);
        add(createObjectSourcePanel(searchResults, manualIdentifier));
    }

    private EnumCardChoicePanel<SearchByIdTestObjectSource> createObjectSourcePanel(ResourceObjectsTablePanel searchResults,
            WebMarkupContainer manualIdentifier) {
        return new EnumCardChoicePanel<>(ID_OBJECT_SOURCE, objectSourceModel, createObjectSourceOptions(), true, false) {

            @Override
            protected void onSelectionChanged(AjaxRequestTarget target) {
                target.add(searchResults, manualIdentifier, getSubmit());
            }

            @Override
            protected String getSelectedCardCssClass() {
                return "border-primary text-primary";
            }
        };
    }

    private List<EnumCardChoicePanel.CardOption<SearchByIdTestObjectSource>> createObjectSourceOptions() {
        return Arrays.stream(SearchByIdTestObjectSource.values())
                .map(source -> EnumCardChoicePanel.createLocalizedOption(source, this, ""))
                .toList();
    }

    private ResourceObjectsTablePanel createSearchResultsPanel() {
        ResourceObjectsTablePanel panel = new ResourceObjectsTablePanel(ID_SEARCH_RESULTS, selection.getSelectedShadowOidModel(),
                () -> getTestingResourceModel().getObjectWrapper().getOid(), this::getObjectClassDefinition) {

            @Override
            protected void onSelectionChanged(AjaxRequestTarget target) {
                target.add(getSubmit());
            }
        };
        panel.setOutputMarkupPlaceholderTag(true);
        panel.add(new VisibleBehaviour(() -> objectSourceModel.getObject() == SearchByIdTestObjectSource.SEARCH_RESULTS));
        return panel;
    }

    private WebMarkupContainer createManualIdentifierPanel() {
        WebMarkupContainer container = new WebMarkupContainer(ID_MANUAL_IDENTIFIER);
        container.setOutputMarkupPlaceholderTag(true);
        container.add(new VisibleBehaviour(() -> objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER));
        container.add(createIdentifierField());
        return container;
    }

    private TextField<String> createIdentifierField() {
        TextField<String> identifier = new TextField<>(ID_IDENTIFIER, selection.getPrimaryIdentifierModel());
        identifier.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getSubmit());
            }
        });
        return identifier;
    }

    private ResourceObjectDefinition getObjectClassDefinition() {
        return getTestingResourceModel().findResourceObjectClassDefinition(new QName(getObjectClassName()));
    }

    private void retrieveObjectPerformed(AjaxRequestTarget target) {
        getFlowState().getRetrievedObjectModel().setObject(null);
        ShadowType retrievedObject = readFromExternalSystem(OP_RETRIEVE_OBJECT, SearchByIdScriptConnectorStepPanel.PANEL_TYPE,
                "SearchByIdObjectConnectorStepPanel.retrieveFailed", target, this::retrieveObject);
        if (isOperationFailed()) {
            return;
        }
        if (retrievedObject == null) {
            getPageBase().error(getString("SearchByIdObjectConnectorStepPanel.objectNotFound"));
            target.add(getFeedback());
            return;
        }

        getFlowState().getRetrievedObjectModel().setObject(retrievedObject);
        nextWithoutConfirmationPerformed(target);
    }

    private ShadowType retrieveObject(Task task) throws CommonException {
        return objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER
                ? getDetailsModel().getConnectorDevelopmentOperation().findTestingResourceObject(
                        getObjectClassName(), selection.getPrimaryIdentifierModel().getObject(), task, task.getResult())
                : retrieveSelectedObject(task, task.getResult());
    }

    private ShadowType retrieveSelectedObject(Task task, OperationResult result) {
        PrismObject<ShadowType> shadow = WebModelServiceUtils.loadObject(
                ShadowType.class, selection.getSelectedShadowOidModel().getObject(), getPageBase(), task, result);
        return shadow != null ? shadow.asObjectable() : null;
    }

    @Override
    public boolean isCompleted() {
        return getFlowState().getRetrievedObjectModel().getObject() != null || super.isCompleted();
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION);
    }

    @Override
    protected boolean isSubmitEnable() {
        if (objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER) {
            return StringUtils.isNotBlank(selection.getPrimaryIdentifierModel().getObject());
        }
        return selection.getSelectedShadowOidModel().getObject() != null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("SearchByIdObjectConnectorStepPanel.retrieveObject");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        retrieveObjectPerformed(target);
    }
}
