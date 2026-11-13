/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create.CreateScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.input.EnumCardChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Base step for choosing the tested object: a recorded test object, an object of the external system found by its
 * primary identifier, or a newly created object.
 */
public abstract class AbstractSelectTestObjectConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String CLASS_DOT = AbstractSelectTestObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_SELECT_OBJECT = CLASS_DOT + "selectObject";
    private static final String OP_FIND_OBJECT = CLASS_DOT + "findObject";
    private static final String OP_CREATE_OBJECT = CLASS_DOT + "createObject";

    private static final String ID_OBJECT_SOURCE = "objectSource";
    private static final String ID_PREVIOUSLY_CREATED = "previouslyCreated";
    private static final String ID_EXISTING_OBJECT = "existingObject";
    private static final String ID_NEW_OBJECT = "newObject";
    private static final String ID_NEW_OBJECT_ATTRIBUTES = "newObjectAttributes";

    /**
     * Wrapper of the object to create.
     */
    private final LoadableModel<ShadowWrapper> newObjectModel = LoadableModel.create(this::createNewObjectWrapper, false);

    /**
     * State of the recorded test objects of the object class and the user's choice among them.
     */
    private final TestObjectSelection selection;

    /**
     * Form of the object to create, shown for the new object source.
     */
    private WebMarkupContainer newObjectPanel;

    public AbstractSelectTestObjectConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
        this.selection = new TestObjectSelection(helper.getDetailsModel(), valueModel);

    }

    @Override
    public boolean isCompleted() {
        return getFlowState().getTestedObjectModel().getObject() != null || super.isCompleted();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        TestObjectsTablePanel previouslyCreated = createPreviouslyCreatedPanel();
        add(previouslyCreated);
        ExistingObjectLookupPanel existingObject = createExistingObjectPanel();
        add(existingObject);
        newObjectPanel = createNewObjectPanel();
        add(newObjectPanel);
        add(createObjectSourcePanel(previouslyCreated, existingObject));
    }

    @Override
    protected void onBeforeRender() {
        selection.getTestObjectsModel().reset();
        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        selection.detach();
        super.onDetach();
    }

    @Override
    protected boolean isSubmitEnable() {
        return switch (selection.getObjectSource()) {
            case PREVIOUSLY_CREATED -> selection.getSelectedShadowOidModel().getObject() != null;
            case EXISTING_OBJECT -> selection.getFoundObjectModel().getObject() != null;
            case NEW_OBJECT -> true;
        };
    }

    @Override
    protected String getSubmitIcon() {
        return isCreatingNewObject() ? "fa fa-plus" : super.getSubmitIcon();
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> getString(isCreatingNewObject()
                ? "AbstractSelectTestObjectConnectorStepPanel.createTestObject"
                : "AbstractSelectTestObjectConnectorStepPanel.continue");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        selectObjectPerformed(target);
    }

    /**
     * Called after the tested object was chosen and stored in the flow state.
     *
     * @param target the request target
     */
    protected void onObjectSelected(AjaxRequestTarget target) {
        nextWithoutConfirmationPerformed(target);
    }

    /**
     * Tells whether the user chose to create a new object for the test.
     *
     * @return true if a new object is created
     */
    protected final boolean isCreatingNewObject() {
        return selection.getObjectSource() == TestObjectSource.NEW_OBJECT;
    }

    private EnumCardChoicePanel<TestObjectSource> createObjectSourcePanel(TestObjectsTablePanel previouslyCreated,
            ExistingObjectLookupPanel existingObject) {
        List<EnumCardChoicePanel.CardOption<TestObjectSource>> options = createObjectSourceOptions();
        EnumCardChoicePanel<TestObjectSource> panel = new EnumCardChoicePanel<>(ID_OBJECT_SOURCE,
                LambdaModel.of(selection::getObjectSource, selection::setObjectSource), options, true, false) {

            @Override
            protected void onSelectionChanged(AjaxRequestTarget target) {
                target.add(previouslyCreated, existingObject, newObjectPanel, getSubmit());
            }

            @Override
            protected String getSelectedCardCssClass() {
                return "border-primary text-primary";
            }

            @Override
            protected String getCardSizeCssClass() {
                return "py-3";
            }
        };
        panel.add(new VisibleBehaviour(() -> options.size() > 1));
        return panel;
    }

    private List<EnumCardChoicePanel.CardOption<TestObjectSource>> createObjectSourceOptions() {
        return Arrays.stream(TestObjectSource.values())
                .filter(source -> source != TestObjectSource.PREVIOUSLY_CREATED
                        || !selection.getTestObjectsModel().getObject().isEmpty())
                .filter(source -> source != TestObjectSource.NEW_OBJECT || isCreateScriptConfirmed())
                .map(source -> EnumCardChoicePanel.createLocalizedOption(source, this, ""))
                .toList();
    }

    private boolean isCreateScriptConfirmed() {
        return ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                getDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE, getObjectClassName());
    }

    private TestObjectsTablePanel createPreviouslyCreatedPanel() {
        TestObjectsTablePanel panel = new TestObjectsTablePanel(ID_PREVIOUSLY_CREATED,
                selection.getSelectedShadowOidModel(), selection.getTestObjectsModel(), this::getObjectClassDefinition) {

            @Override
            protected void onSelectionChanged(AjaxRequestTarget target) {
                target.add(getSubmit());
            }
        };
        panel.setOutputMarkupId(true);
        panel.setOutputMarkupPlaceholderTag(true);
        panel.add(new VisibleBehaviour(() -> selection.getObjectSource() == TestObjectSource.PREVIOUSLY_CREATED));
        return panel;
    }

    private ExistingObjectLookupPanel createExistingObjectPanel() {
        ExistingObjectLookupPanel panel = new ExistingObjectLookupPanel(ID_EXISTING_OBJECT,
                selection.getPrimaryIdentifierModel(), selection.getFoundObjectModel(), this::getObjectClassDefinition,
                getTestedOperationName()) {

            @Override
            protected void onFindObject(AjaxRequestTarget target) {
                findObjectPerformed(target);
            }

            @Override
            protected void onObjectChanged(AjaxRequestTarget target) {
                target.add(getSubmit());
            }
        };
        panel.setOutputMarkupPlaceholderTag(true);
        panel.add(new VisibleBehaviour(() -> selection.getObjectSource() == TestObjectSource.EXISTING_OBJECT));
        return panel;
    }

    private ResourceObjectDefinition getObjectClassDefinition() {
        return getTestingResourceModel().findResourceObjectClassDefinition(new QName(getObjectClassName()));
    }

    private void findObjectPerformed(AjaxRequestTarget target) {
        ShadowType object = readFromExternalSystem(OP_FIND_OBJECT, SearchByIdScriptConnectorStepPanel.PANEL_TYPE,
                "AbstractSelectTestObjectConnectorStepPanel.findObjectFailed", target,
                task -> getDetailsModel().getConnectorDevelopmentOperation().findTestingResourceObject(
                        getObjectClassName(), selection.getPrimaryIdentifierModel().getObject(), task, task.getResult()));
        if (object == null && !isOperationFailed()) {
            getPageBase().error(getString("AbstractSelectTestObjectConnectorStepPanel.objectNotFound"));
        }
        selection.getFoundObjectModel().setObject(object);
        target.add(getSubmit(), getFeedback());
    }

    private WebMarkupContainer createNewObjectPanel() {
        WebMarkupContainer container = new WebMarkupContainer(ID_NEW_OBJECT);
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        container.add(new VisibleBehaviour(() -> selection.getObjectSource() == TestObjectSource.NEW_OBJECT));
        container.add(createNewObjectAttributes());
        return container;
    }

    private TestObjectAttributesPanel createNewObjectAttributes() {
        return new TestObjectAttributesPanel(ID_NEW_OBJECT_ATTRIBUTES, newObjectModel);
    }

    private void selectObjectPerformed(AjaxRequestTarget target) {
        ShadowWrapper testObject = isCreatingNewObject()
                ? changeExternalSystem(OP_CREATE_OBJECT, CreateScriptConnectorStepPanel.PANEL_TYPE,
                        "AbstractSelectTestObjectConnectorStepPanel.createObjectFailed", target, this::createTestObject)
                : readFromExternalSystem(OP_SELECT_OBJECT, SearchByIdScriptConnectorStepPanel.PANEL_TYPE,
                        "AbstractSelectTestObjectConnectorStepPanel.selectObjectFailed", target, this::loadSelectedObject);
        if (isOperationFailed()) {
            target.add(getFeedback());
            return;
        }
        if (testObject == null) {
            getPageBase().error(getString(isCreatingNewObject()
                    ? "AbstractSelectTestObjectConnectorStepPanel.objectNotRetrieved"
                    : "AbstractSelectTestObjectConnectorStepPanel.objectNotFound"));
            target.add(getFeedback(), newObjectPanel, getSubmit());
            return;
        }

        getFlowState().objectSelected(testObject, selection.getObjectSource());
        onObjectSelected(target);
    }

    private ShadowWrapper createTestObject(Task task) throws CommonException {
        OperationResult result = task.getResult();
        String oid = getDetailsModel().getConnectorDevelopmentOperation().addTestObject(
                newObjectModel.getObject().getObjectDelta().getObjectToAdd().asObjectable(), task, result);
        getDetailsModel().reloadPrismObjectByOid();
        newObjectModel.reset();
        return createWrapper(loadObject(oid, task), task);
    }

    private ShadowWrapper loadSelectedObject(Task task) throws CommonException {
        ShadowType object = selection.getObjectSource() == TestObjectSource.PREVIOUSLY_CREATED
                ? loadObject(selection.getSelectedShadowOidModel().getObject(), task)
                : selection.getFoundObjectModel().getObject();
        return createWrapper(object, task);
    }

    private ShadowType loadObject(String oid, Task task) {
        PrismObject<ShadowType> object = WebModelServiceUtils.loadObject(ShadowType.class, oid, getPageBase(), task, task.getResult());
        return object != null ? object.asObjectable() : null;
    }

    private ShadowWrapper createWrapper(ShadowType object, Task task) throws CommonException {
        return object != null
                ? ConnectorDevelopmentWizardUtil.createShadowWrapperForModification(
                        getPageBase(), object.asPrismObject(), task, task.getResult())
                : null;
    }
}
