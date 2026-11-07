/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for cleaning up test objects created on the testing resource.
 */
@PanelType(name = "cdw-create-cleanup")
@PanelInstance(identifier = "cdw-create-cleanup",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createCleanup", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateCleanupConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateCleanupConnectorStepPanel.class);

    private static final String CLASS_DOT = CreateCleanupConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_TEST_OBJECTS = CLASS_DOT + "loadTestObjects";
    private static final String OP_CLEAN_UP = CLASS_DOT + "cleanUp";

    private static final String PANEL_TYPE = "cdw-create-cleanup";

    private static final String ID_DELETE_AVAILABLE_ALERT = "deleteAvailableAlert";
    private static final String ID_DELETE_NOT_CONFIGURED_ALERT = "deleteNotConfiguredAlert";
    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    private final Set<String> keptObjectOids = new HashSet<>();

    private final Set<String> missingObjectOids = new HashSet<>();

    private final LoadableModel<List<ShadowType>> testObjectsModel = new LoadableModel<>() {

        @Override
        protected List<ShadowType> load() {
            return loadExistingTestObjects();
        }
    };

    public CreateCleanupConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        add(createAlert(ID_DELETE_AVAILABLE_ALERT, this::isDeleteAvailable));
        add(createAlert(ID_DELETE_NOT_CONFIGURED_ALERT, () -> !isDeleteAvailable()));
        add(createTable());
    }

    @Override
    protected void onDetach() {
        testObjectsModel.detach();
        super.onDetach();
    }

    private WebMarkupContainer createAlert(String id, SerializableSupplier<Boolean> visibility) {
        WebMarkupContainer alert = new WebMarkupContainer(id);
        alert.add(new VisibleBehaviour(visibility));
        return alert;
    }

    private BoxedTablePanel<ShadowType> createTable() {
        ListDataProvider<ShadowType> provider = new ListDataProvider<>(this, testObjectsModel);
        provider.setSort(null);

        BoxedTablePanel<ShadowType> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<ShadowType, String> provider, int pageSize) {
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<ShadowType, String>> createColumns() {
        List<IColumn<ShadowType, String>> columns = new ArrayList<>();
        columns.add(createKeepColumn());
        columns.add(createLabelColumn("CreateCleanupConnectorStepPanel.object",
                shadow -> shadow.getObjectClass() != null ? shadow.getObjectClass().getLocalPart() : null));
        columns.add(createLabelColumn("CreateCleanupConnectorStepPanel.primaryIdentifier",
                shadow -> ConnectorDevelopmentWizardUtil.formatValues(getPrimaryIdentifierValues(shadow))));
        columns.add(createLabelColumn("CreateCleanupConnectorStepPanel.fullName",
                shadow -> PolyString.getOrig(shadow.getName())));
        return columns;
    }

    private IColumn<ShadowType, String> createKeepColumn() {
        return new CheckBoxColumn<>(() -> isDeleteAvailable() ? getString("CreateCleanupConnectorStepPanel.keep") : "") {

            @Override
            protected IModel<Boolean> getCheckBoxValueModel(IModel<ShadowType> rowModel) {
                String oid = rowModel.getObject().getOid();
                return LambdaModel.of(() -> keptObjectOids.contains(oid), kept -> {
                    if (Boolean.TRUE.equals(kept)) {
                        keptObjectOids.add(oid);
                    } else {
                        keptObjectOids.remove(oid);
                    }
                });
            }

            @Override
            protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<ShadowType> rowModel) {
                check.add(new VisibleBehaviour(CreateCleanupConnectorStepPanel.this::isDeleteAvailable));
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<ShadowType>> cellItem, AjaxRequestTarget target,
                    DataTable table, IModel<ShadowType> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                target.add(getSubmit());
            }
        };
    }

    private IColumn<ShadowType, String> createLabelColumn(String headerKey, SerializableFunction<ShadowType, String> textProvider) {
        return new AbstractColumn<>(createStringResource(headerKey)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ShadowType>> cellItem, String componentId, IModel<ShadowType> rowModel) {
                cellItem.add(new Label(componentId, () -> textProvider.apply(rowModel.getObject())));
            }
        };
    }

    private static Collection<?> getPrimaryIdentifierValues(ShadowType shadow) {
        Collection<ShadowSimpleAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        if (identifiers == null) {
            return List.of();
        }
        return identifiers.stream()
                .flatMap(identifier -> identifier.getRealValues().stream())
                .toList();
    }

    private List<ShadowType> loadExistingTestObjects() {
        Task task = getPageBase().createSimpleTask(OP_LOAD_TEST_OBJECTS);
        List<ShadowType> objects = new ArrayList<>();
        missingObjectOids.clear();
        for (String oid : ConnectorDevelopmentWizardUtil.getTestObjectOids(getDetailsModel())) {
            try {
                ShadowType shadow = getPageBase().getModelService()
                        .getObject(ShadowType.class, oid, null, task, task.getResult().createMinorSubresult(OP_LOAD_TEST_OBJECTS))
                        .asObjectable();
                if (Boolean.TRUE.equals(shadow.isDead())) {
                    missingObjectOids.add(oid);
                } else if (isObjectOfThisObjectClass(shadow)) {
                    objects.add(shadow);
                }
            } catch (ObjectNotFoundException e) {
                missingObjectOids.add(oid);
            } catch (CommonException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check whether test object " + oid + " exists", e);
            }
        }
        return objects;
    }

    private boolean isObjectOfThisObjectClass(ShadowType shadow) {
        QName objectClass = shadow.getObjectClass();
        return objectClass != null && StringUtils.equals(objectClass.getLocalPart(), getObjectClassName());
    }

    private String getObjectClassName() {
        return valueModel.getObject().getRealValue().getName();
    }

    private boolean isDeleteAvailable() {
        return ConnectorDevelopmentWizardUtil.existScript(
                getDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.DELETE, getObjectClassName());
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createCheckExistenceButton(customButtons.newChildId()));
    }

    private AjaxButton createCheckExistenceButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("CreateCleanupConnectorStepPanel.checkExistence")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testObjectsModel.reset();
                target.add(CreateCleanupConnectorStepPanel.this.get(ID_TABLE));
            }
        };
        button.add(new VisibleBehaviour(() -> !isDeleteAvailable()));
        return button;
    }

    private void cleanUpPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_CLEAN_UP);
        OperationResult result = task.getResult();
        testObjectsModel.reset();
        try {
            Set<String> forgottenOids = new HashSet<>();
            for (ShadowType object : testObjectsModel.getObject()) {
                if (isDeleteAvailable() && !keptObjectOids.contains(object.getOid())) {
                    getPageBase().getModelService().executeChanges(
                            List.of(PrismContext.get().deltaFactory().object().createDeleteDelta(ShadowType.class, object.getOid())),
                            null, task, result);
                    forgottenOids.add(object.getOid());
                }
            }
            forgottenOids.addAll(missingObjectOids);
            ConnectorDevelopmentWizardUtil.removeTestObjects(getDetailsModel(), forgottenOids, task, result);
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't clean up test objects", e);
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            getPageBase().showResult(result);
            target.add(getFeedback());
            return;
        }
        keptObjectOids.clear();
        onNextPerformed(target);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !ConnectorDevelopmentWizardUtil.getTestObjectOids(getDetailsModel()).isEmpty();
    }

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                getDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE, getObjectClassName());
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> {
            if (!isDeleteAvailable()) {
                return getString("CreateCleanupConnectorStepPanel.continue");
            }
            return keptObjectOids.isEmpty()
                    ? getString("CreateCleanupConnectorStepPanel.continueWithNone")
                    : createStringResource("CreateCleanupConnectorStepPanel.continueWithKept", keptObjectOids.size()).getString();
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        cleanUpPerformed(target);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createCleanup");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createCleanup.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createCleanup.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
