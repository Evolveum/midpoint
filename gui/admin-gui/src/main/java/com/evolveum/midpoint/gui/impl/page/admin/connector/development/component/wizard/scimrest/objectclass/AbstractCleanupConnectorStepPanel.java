/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for deleting the recorded test objects from the external system and removing their records.
 */
public abstract class AbstractCleanupConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String CLASS_DOT = AbstractCleanupConnectorStepPanel.class.getName() + ".";
    private static final String OP_CLEAN_UP = CLASS_DOT + "cleanUp";

    private static final String ID_DELETE_AVAILABLE_ALERT = "deleteAvailableAlert";
    private static final String ID_DELETE_NOT_CONFIGURED_ALERT = "deleteNotConfiguredAlert";
    private static final String ID_TABLE = "table";

    /**
     * State of the recorded test objects of the object class and the user's choice of those to keep.
     */
    private final TestObjectSelection selection;

    /**
     * Table of the recorded test objects, refreshed when the objects are reloaded or marked to keep.
     */
    private BoxedTablePanel<CleanupObjectDto> table;

    public AbstractCleanupConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
        this.selection = new TestObjectSelection(helper.getDetailsModel(), valueModel);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !ConnectorDevelopmentWizardUtil.getTestObjectOids(getDetailsModel()).isEmpty();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createAlert(ID_DELETE_AVAILABLE_ALERT, this::isDeleteAvailable));
        add(createAlert(ID_DELETE_NOT_CONFIGURED_ALERT, () -> !isDeleteAvailable()));
        table = createTable();
        add(table);
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
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createCheckExistenceButton(customButtons.newChildId()));
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> {
            if (!isDeleteAvailable()) {
                return getString("AbstractCleanupConnectorStepPanel.continue");
            }
            return selection.getKeptObjectOids().isEmpty()
                    ? getString("AbstractCleanupConnectorStepPanel.continueWithNone")
                    : createStringResource("AbstractCleanupConnectorStepPanel.continueWithKept", selection.getKeptObjectOids().size()).getString();
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        cleanUpPerformed(target);
    }

    private WebMarkupContainer createAlert(String id, SerializableSupplier<Boolean> visibility) {
        WebMarkupContainer alert = new WebMarkupContainer(id);
        alert.add(new VisibleBehaviour(visibility));
        return alert;
    }

    private BoxedTablePanel<CleanupObjectDto> createTable() {
        ListDataProvider<CleanupObjectDto> provider = new ListDataProvider<>(this, createRowsModel(), true);
        provider.setSort(null);

        BoxedTablePanel<CleanupObjectDto> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected Item<CleanupObjectDto> customizeNewRowItem(Item<CleanupObjectDto> item, IModel<CleanupObjectDto> model) {
                Item<CleanupObjectDto> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class", () -> selection.isKept(model.getObject().getOid()) ? "table-active" : null));
                return row;
            }

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<CleanupObjectDto, String> provider, int pageSize) {
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<CleanupObjectDto, String>> createColumns() {
        List<IColumn<CleanupObjectDto, String>> columns = new ArrayList<>();
        columns.add(createKeepColumn());
        columns.add(new LambdaColumn<>(createStringResource("AbstractCleanupConnectorStepPanel.object"),
                CleanupObjectDto.F_OBJECT_CLASS, CleanupObjectDto::getObjectClass));
        columns.add(new LambdaColumn<>(createStringResource("AbstractCleanupConnectorStepPanel.primaryIdentifier"),
                CleanupObjectDto.F_PRIMARY_IDENTIFIER, CleanupObjectDto::getPrimaryIdentifier));
        columns.add(new LambdaColumn<>(createStringResource("AbstractCleanupConnectorStepPanel.name"),
                CleanupObjectDto.F_NAME, CleanupObjectDto::getName));
        return columns;
    }

    private IColumn<CleanupObjectDto, String> createKeepColumn() {
        return new CheckBoxColumn<>(() -> "") {

            @Override
            public Component getHeader(String componentId) {
                IsolatedCheckBoxPanel header = new IsolatedCheckBoxPanel(componentId, createKeepAllModel()) {

                    @Override
                    public void onUpdate(AjaxRequestTarget target) {
                        target.add(table, getSubmit());
                    }
                };
                header.add(new VisibleBehaviour(AbstractCleanupConnectorStepPanel.this::isDeleteAvailable));
                return header;
            }

            @Override
            protected IModel<Boolean> getCheckBoxValueModel(IModel<CleanupObjectDto> rowModel) {
                String oid = rowModel.getObject().getOid();
                return LambdaModel.of(() -> selection.isKept(oid), kept -> selection.setKept(oid, Boolean.TRUE.equals(kept)));
            }

            @Override
            protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<CleanupObjectDto> rowModel) {
                check.add(new VisibleBehaviour(AbstractCleanupConnectorStepPanel.this::isDeleteAvailable));
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<CleanupObjectDto>> cellItem, AjaxRequestTarget target,
                    DataTable table, IModel<CleanupObjectDto> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                target.add(getSubmit(), table);
            }
        };
    }

    private IModel<Boolean> createKeepAllModel() {
        return LambdaModel.of(selection::isAllKept, keepAll -> selection.setAllKept(Boolean.TRUE.equals(keepAll)));
    }

    private IModel<List<CleanupObjectDto>> createRowsModel() {
        return () -> selection.getTestObjectsModel().getObject().stream()
                .map(this::createRow)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private CleanupObjectDto createRow(ShadowType shadow) {
        return new CleanupObjectDto(shadow,
                shadow.getObjectClass() != null ? shadow.getObjectClass().getLocalPart() : null,
                ConnectorDevelopmentWizardUtil.formatPrimaryIdentifiers(shadow),
                PolyString.getOrig(shadow.getName()));
    }

    private boolean isDeleteAvailable() {
        return ConnectorDevelopmentWizardUtil.isOperationCompleted(
                getDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.DELETE, getObjectClassName());
    }

    private AjaxIconButton createCheckExistenceButton(String id) {
        AjaxIconButton button = new AjaxIconButton(id, Model.of("fa fa-sync"),
                createStringResource("AbstractCleanupConnectorStepPanel.checkExistence")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                selection.getTestObjectsModel().reset();
                target.add(table);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.replace("class", "btn btn-link"));
        button.add(new VisibleBehaviour(() -> !isDeleteAvailable()));
        return button;
    }

    private void cleanUpPerformed(AjaxRequestTarget target) {
        selection.getTestObjectsModel().reset();
        changeExternalSystem(OP_CLEAN_UP, null, "AbstractCleanupConnectorStepPanel.cleanUpFailed", target,
                task -> {
                    cleanUp(task);
                    return null;
                });
        if (isOperationFailed()) {
            return;
        }
        selection.setAllKept(false);
        nextWithoutConfirmationPerformed(target);
    }

    private void cleanUp(Task task) throws CommonException {
        List<String> deletedOids = !isDeleteAvailable() ? List.of() : selection.getTestObjectsModel().getObject().stream()
                .map(ShadowType::getOid)
                .filter(oid -> !selection.isKept(oid))
                .toList();
        ConnectorDevelopmentOperation operation = getDetailsModel().getConnectorDevelopmentOperation();
        operation.forgetMissingTestObjects(task, task.getResult());
        operation.deleteTestObjects(deletedOids, task, task.getResult());
        getDetailsModel().reloadPrismObjectByOid();
    }
}
