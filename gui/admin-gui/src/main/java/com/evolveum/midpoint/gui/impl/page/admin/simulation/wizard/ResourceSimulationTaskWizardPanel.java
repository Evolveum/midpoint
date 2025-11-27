/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationItemRefsTable;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResult;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.dialog.AdditionalOperationConfirmationPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.server.TaskTablePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.cases.api.util.QueryUtils.createQueryForObjectTypeSimulationTasks;

@PanelType(name = "rw-simulation-task")
@PanelInstance(identifier = "rw-simulation-task",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSimulationTaskWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class ResourceSimulationTaskWizardPanel<C extends Containerable> extends AbstractResourceWizardBasicPanel<C> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSimulationTaskWizardPanel.class);

    private static final String PANEL_TYPE = "rw-simulation-task";
    private static final String ID_TABLE = "table";

    public ResourceSimulationTaskWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        TaskTablePanel tablePanel = new TaskTablePanel(ID_TABLE) {

            @Override
            protected @NotNull ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> getTaskQuery(), null, null);
            }

            @Override
            public void setAdditionalBoxCssClasses(String boxCssClasses) {
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }

            @Override
            protected boolean isReportObjectButtonVisible() {
                return false;
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createDefaultColumns();
                addCustomColumns(columns);
                return columns;
            }

            @Override
            protected void onNameColumnPerform(@NotNull IModel<SelectableBean<TaskType>> rowModel, AjaxRequestTarget target) {
                SelectableBean<TaskType> object = rowModel.getObject();
                TaskType task = object.getValue();
                if (!hasUnsavedChanges()) {
                    objectDetailsPerformed(task);
                }

                AdditionalOperationConfirmationPanel confirmationPanel = new AdditionalOperationConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("SimulationTaskWizardPanel.redirect.unsavedChanges.message")) {

                    @Override
                    protected void performOnProcess(AjaxRequestTarget target) {
                        getHelper().onSaveObjectPerformed(target);
                        objectDetailsPerformed(task);
                    }

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        objectDetailsPerformed(task);
                    }
                };

                getPageBase().showMainPopup(confirmationPanel, target);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_TASKS;
            }
        };
        tablePanel.setOutputMarkupId(true);
        add(tablePanel);
    }

    protected boolean hasUnsavedChanges() {
        OperationResult result = new OperationResult(ResourceSimulationTaskWizardPanel.class.getName() + ".hasUnsavedChanges");
        Collection<ObjectDelta<? extends ObjectType>> deltas = List.of();
        try {
            deltas = getHelper().getDetailsModel().collectDeltaWithoutSavedDeltas(result);
        } catch (CommonException e) {
            LOGGER.error("Couldn't check for unsaved changes: {}", e.getMessage(), e);
        }

        return !deltas.isEmpty();
    }

    protected ObjectQuery getTaskQuery() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceDef = getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        ResourceDetailsModel holderDetailsModel = getAssignmentHolderDetailsModel();
        String resourceOid = holderDetailsModel.getObjectType().getOid();
        if (resourceDef != null && resourceDef.getRealValue() != null && resourceOid != null) {
            return createQueryForObjectTypeSimulationTasks(resourceDef.getRealValue(), resourceOid);
        }

        return getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_PARENT)
                .isNull()
                .build();
    }

    protected boolean isRefColumnVisible() {
        return true;
    }

    private void addCustomColumns(@NotNull List<IColumn<SelectableBean<TaskType>, String>> columns) {

        if (isRefColumnVisible()) {
            columns.add(0, new ObjectReferenceColumn<>(createStringResource("pageTasks.task.objectRef"),
                    SelectableBeanImpl.F_VALUE + "." + TaskType.F_OBJECT_REF.getLocalPart()) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                    SelectableBean<TaskType> bean = rowModel.getObject();
                    ObjectReferenceType objectRef = bean.getValue().getObjectRef();
                    if (objectRef != null) {
                        objectRef.asReferenceValue().clearParent();
                    }
                    return Model.ofList(Collections.singletonList(objectRef));
                }

                @Override
                protected Collection<SelectorOptions<GetOperationOptions>> getOptions(ObjectReferenceType ref) {
                    if (ref != null && QNameUtil.match(ResourceType.COMPLEX_TYPE, ref.getType())) {
                        return GetOperationOptions.createNoFetchReadOnlyCollection();
                    }
                    return null;
                }
            });
        }

        columns.add(new AbstractColumn<>(createStringResource("SimulationTaskWizardPanel.simulation.result")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> item, String id, IModel<SelectableBean<TaskType>> iModel) {

                ObjectReferenceType resultRef = getSimulationResultReference(iModel.getObject().getValue());

                if (resultRef == null) {
                    Label noSimulationLabel = new Label(id, createStringResource("SimulationTaskWizardPanel.noSimulationResult"));
                    noSimulationLabel.setOutputMarkupId(true);
                    item.add(noSimulationLabel);
                    return;
                }

                AjaxIconButton button = new AjaxIconButton(id, Model.of("fa fa-search"),
                        createStringResource("SimulationTaskWizardPanel.button.showResult")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onShowSimulationResult(target, resultRef.getOid());
                    }
                };
                button.showTitleAsLabel(true);
                button.setOutputMarkupId(true);
                button.add(AttributeModifier.replace("class", "btn btn-sm btn-primary"));
                item.add(button);
            }
        });
    }

    protected void onShowSimulationResult(AjaxRequestTarget target, String resultOid) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, resultOid);
        getPageBase().navigateToNext(PageSimulationResult.class, params);
    }

    private @Nullable ObjectReferenceType getSimulationResultReference(@NotNull TaskType task) {
        TaskActivityStateType activityState = task.getActivityState();
        if (activityState == null || activityState.getActivity() == null) {
            return null;
        }

        ActivitySimulationStateType simulation = activityState.getActivity().getSimulation();
        if (simulation == null || simulation.getResultRef() == null) {
            return null;
        }

        ObjectReferenceType ref = simulation.getResultRef();
        return ref.getOid() != null ? ref : null;
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return false;
    }

    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("SimulationTaskWizardPanel.breadcrumb");
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("SimulationTaskWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("SimulationTaskWizardPanel.subText");
    }

    protected CorrelationItemRefsTable getTable() {
        return (CorrelationItemRefsTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
