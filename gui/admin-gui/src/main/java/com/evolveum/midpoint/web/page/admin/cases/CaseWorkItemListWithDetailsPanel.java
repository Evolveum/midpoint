package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class CaseWorkItemListWithDetailsPanel extends MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, String> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(CaseWorkItemListWithDetailsPanel.class);

    private static final String DOT_CLASS = CaseWorkItemListWithDetailsPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_CLASS + "delegateWorkItem";

    private static final String ID_WORK_ITEM_APPROVE_BUTTON = "workItemApproveButton";
    private static final String ID_WORK_ITEM_REJECT_BUTTON = "workItemRejectButton";
    private static final String ID_WORK_ITEM_DELEGATE_BUTTON = "workItemDelegateButton";
    private static final String ID_ACTION_BUTTONS = "actionButtons";

    public CaseWorkItemListWithDetailsPanel(String id, IModel<PrismContainerWrapper<CaseWorkItemType>> model, UserProfileStorage.TableId tableId, PageStorage pageStorage){
        super(id, model, tableId, pageStorage);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        WebMarkupContainer actionButtonsContainer = new WebMarkupContainer(ID_ACTION_BUTTONS);
        actionButtonsContainer.setOutputMarkupId(true);
        actionButtonsContainer.add(new VisibleBehaviour(() -> !isParentCaseClosed()));
        getDetailsPanelContainer().add(actionButtonsContainer);

        AjaxButton workItemApproveButton = new AjaxButton(ID_WORK_ITEM_APPROVE_BUTTON,
                createStringResource("pageWorkItem.button.approve")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                savePerformed(ajaxRequestTarget, unwrapPanelModel(), true);
            }
        };
        workItemApproveButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemApproveButton);

        AjaxButton workItemRejectButton = new AjaxButton(ID_WORK_ITEM_REJECT_BUTTON,
                createStringResource("pageWorkItem.button.reject")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                savePerformed(ajaxRequestTarget, unwrapPanelModel(), false);
            }
        };
        workItemRejectButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemRejectButton);

        AjaxButton workItemDelegateButton = new AjaxButton(ID_WORK_ITEM_DELEGATE_BUTTON,
                createStringResource("pageWorkItem.button.delegate")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                delegatePerformed(ajaxRequestTarget);
            }
        };
        workItemDelegateButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemDelegateButton);
    }

    @Override
    protected void initPaging() {
        getWorkitemsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, ((int) CaseWorkItemListWithDetailsPanel.this.getPageBase().getItemsPerPage(getTableId()))));
    }

    protected abstract UserProfileStorage.TableId getTableId();

    @Override
    protected abstract ObjectQuery createQuery();

    @Override
    protected boolean enableActionNewObject() {
        return false;
    }

    @Override
    protected boolean isSearchEnabled(){
        return false;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> createColumns() {
        return getWorkItemColumns();
    }

    @Override
    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
        return new WebMarkupContainer(contentAreaId);
    }

    @Override
    protected List<PrismContainerValueWrapper<CaseWorkItemType>> postSearch(
            List<PrismContainerValueWrapper<CaseWorkItemType>> workItems) {
        return workItems;
    }

    @Override
    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<CaseWorkItemType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();
        return defs;
    }

    @Override
    protected MultivalueContainerDetailsPanel<CaseWorkItemType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<CaseWorkItemType>> item) {
        return createWorkItemDetailsPanel(item);
    }

//    @Override
//    public void itemDetailsPerformed(AjaxRequestTarget target,  IModel<PrismContainerValueWrapper<CaseWorkItemType>> model){}

    private ObjectTabStorage getWorkitemsTabStorage(){
        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
    }

    private MultivalueContainerDetailsPanel<CaseWorkItemType> createWorkItemDetailsPanel(
            ListItem<PrismContainerValueWrapper<CaseWorkItemType>> item) {
        MultivalueContainerDetailsPanel<CaseWorkItemType> detailsPanel = new  MultivalueContainerDetailsPanel<CaseWorkItemType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<CaseWorkItemType> createDisplayNamePanel(String displayNamePanelId) {
                ItemRealValueModel<CaseWorkItemType> displayNameModel = new ItemRealValueModel<CaseWorkItemType>(item.getModel());
                return new DisplayNamePanel<CaseWorkItemType>(displayNamePanelId, displayNameModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IModel<String> getDescriptionLabelModel() {
                        CaseType caseType = CaseWorkItemUtil.getCase(displayNameModel.getObject());
                        return Model.of(caseType != null && caseType.getDescription() != null ? caseType.getDescription() : "");
                    }
                };
            }

            @Override
            protected void addBasicContainerValuePanel(String idPanel) {
                //todo fix  implement with WorkItemDetailsPanelFactory
                WorkItemDetailsPanel workItemDetails = new WorkItemDetailsPanel(idPanel, Model.of(item.getModel().getObject().getRealValue()));
                workItemDetails.setOutputMarkupId(true);
                add(workItemDetails);
            }

        };
        return detailsPanel;
    }


    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> getWorkItemColumns(){
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

//                        columns.add(new IconColumn<ContainerValueWrapper<CaseWorkItemType>>(Model.of("")) {
//
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            protected IModel<String> createIconModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
//                                return new IModel<String>() {
//
//                                    private static final long serialVersionUID = 1L;
//
//                                    @Override
//                                    public String getObject() {
//                                        return WebComponentUtil.createDefaultBlackIcon(AssignmentsUtil.getTargetType(rowModel.getObject().getContainerValue().asContainerable()));
//                                    }
//                                };
//                            }
//
//                        });
        columns.add(new LinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(unwrapRowModel(rowModel).getName());
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemListWithDetailsPanel.this.itemDetailsPerformed(target, rowModel);

            }
        });

        columns.addAll(ColumnUtils.getDefaultWorkItemColumns(getPageBase()));
        return columns;
    }

    private boolean isParentCaseClosed(){
        return CaseTypeUtil.isClosed(CaseWorkItemUtil.getCase(unwrapPanelModel()));
    }

    private CaseWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel){
        return rowModel.getObject().getRealValue();
    }

    private CaseWorkItemType unwrapPanelModel(){
        return getModelObject().getItem().getRealValue();
    }


    private void savePerformed(AjaxRequestTarget target, CaseWorkItemType workItem, boolean approved) {
        Task task = getPageBase().createSimpleTask(OPERATION_SAVE_WORK_ITEM);
        OperationResult result = task.getResult();
        try {
            //todo implement custom panels
//            WorkItemDto dto = workItemDtoModel.getObject();
//            if (approved) {
//                boolean requiredFieldsPresent = getWorkItemPanel().checkRequiredFields();
//                if (!requiredFieldsPresent) {
//                    target.add(getFeedbackPanel());
//                    return;
//                }
//            }
//            ObjectDelta delta = getWorkItemPanel().getDeltaFromForm();
//            if (delta != null) {
//                //noinspection unchecked
//                getPrismContext().adopt(delta);
//            }
            try {
                assumePowerOfAttorneyIfRequested(result);
                //todo fix comment and delta
                getPageBase().getWorkflowService().completeWorkItem(WorkItemId.of(workItem), approved, "",
                        null, task, result);
            } finally {
                dropPowerOfAttorneyIfRequested(result);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
        }
        getPageBase().processResult(target, result, false);
    }

    private void delegatePerformed(AjaxRequestTarget target) {
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                getPageBase().getMainPopupBodyId(), UserType.class,
                Collections.singletonList(UserType.COMPLEX_TYPE), false, getPageBase(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                CaseWorkItemListWithDetailsPanel.this.getPageBase().hideMainPopup(target);
                delegateConfirmedPerformed(target, user);
            }

        };
        panel.setOutputMarkupId(true);
        getPageBase().showMainPopup(panel, target);
    }

    private void delegateConfirmedPerformed(AjaxRequestTarget target, UserType delegate) {
        Task task = getPageBase().createSimpleTask(OPERATION_DELEGATE_WORK_ITEM);
        OperationResult result = task.getResult();
        try {
            List<ObjectReferenceType> delegates = Collections.singletonList(ObjectTypeUtil.createObjectRef(delegate, getPrismContext()));
            try {
                assumePowerOfAttorneyIfRequested(result);
                getPageBase().getWorkflowService().delegateWorkItem(WorkItemId.of(unwrapPanelModel()), delegates, WorkItemDelegationMethodType.ADD_ASSIGNEES,
                        task, result);
            } finally {
                dropPowerOfAttorneyIfRequested(result);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't delegate work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delegate work item", ex);
        }
        getPageBase().processResult(target, result, false);
    }

    private void assumePowerOfAttorneyIfRequested(OperationResult result) {
//        if (powerDonor != null) {
//            WebModelServiceUtils.assumePowerOfAttorney(powerDonor, getModelInteractionService(), getTaskManager(), result);
//        }
    }

    private void dropPowerOfAttorneyIfRequested(OperationResult result) {
//        if (powerDonor != null) {
//            WebModelServiceUtils.dropPowerOfAttorney(getModelInteractionService(), getTaskManager(), result);
//        }
    }

}
