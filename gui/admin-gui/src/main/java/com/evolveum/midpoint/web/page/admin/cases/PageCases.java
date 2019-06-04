package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * @author acope on 9/14/17.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/casesAll", matchUrlForSecurity = "/admin/casesAll")
        }, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALL_LABEL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageCases.auth.casesAll.label",
                description = "PageCases.auth.casesAll.description")
})
public class PageCases extends PageAdminObjectList<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageCases.class);

    private static final String DOT_CLASS = PageCases.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCE_DISPLAY_NAME = DOT_CLASS + "loadReferenceDisplayName";
    private static final String OPERATION_DELETE_CASE_OBJECT = DOT_CLASS + "deleteCaseObject";
    private static final String OPERATION_STOP_CASE_PROCESS = DOT_CLASS + "stopCaseProcess";

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_CASES_TABLE = "table";

    public PageCases() {
        super();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        LOGGER.trace("caseDetailsPerformed()");

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        navigateToNext(PageCase.class, pageParameters);
    }

    @Override
    protected List<IColumn<SelectableBean<CaseType>, String>> initColumns() {
        LOGGER.trace("initColumns()");

        List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

        IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.objectRef"), "objectRef"){
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return getObjectRef(rowModel);
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.actors")){
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        String actors = null;
                        SelectableBean<CaseType> caseModel = rowModel.getObject();
                        if (caseModel != null) {
                            CaseType caseIntance = caseModel.getValue();
                            if (caseIntance != null) {
                                List<CaseWorkItemType> caseWorkItemTypes = caseIntance.getWorkItem();
                                List<String> actorsList = new ArrayList<String>();
                                for (CaseWorkItemType caseWorkItem : caseWorkItemTypes) {
                                    List<ObjectReferenceType> assignees = caseWorkItem.getAssigneeRef();
                                    for (ObjectReferenceType actor : assignees) {
                                        actorsList.add(WebComponentUtil.getEffectiveName(actor, AbstractRoleType.F_DISPLAY_NAME, PageCases.this,
                                                OPERATION_LOAD_REFERENCE_DISPLAY_NAME));
                                    }
                                }
                                actors = String.join(", ", actorsList);
                            }
                        }
                        return actors;
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(
                createStringResource("pageCases.table.openTimestamp"),
                MetadataType.F_CREATE_TIMESTAMP.getLocalPart()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<CaseType>> rowModel) {
                CaseType object = rowModel.getObject().getValue();
                MetadataType metadata = object != null ? object.getMetadata() : null;
                XMLGregorianCalendar createdCal = metadata != null ? metadata.getCreateTimestamp() : null;
                final Date created;
                if (createdCal != null) {
                    created = createdCal.toGregorianCalendar().getTime();
//                    cellItem.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(created, DateLabelComponent.LONG_MEDIUM_STYLE)));
//                    cellItem.add(new TooltipBehavior());
                } else {
                    created = null;
                }
                cellItem.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getShortDateTimeFormattedValue(created, PageCases.this);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.closeTimestamp"), CaseType.F_CLOSE_TIMESTAMP.getLocalPart(), "value.closeTimestamp") {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<CaseType>> rowModel) {
                CaseType object = rowModel.getObject().getValue();
                XMLGregorianCalendar closedCal = object != null ? object.getCloseTimestamp() : null;
                final Date closed;
                if (closedCal != null) {
                    closed = closedCal.toGregorianCalendar().getTime();
//                    cellItem.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE)));
//                    cellItem.add(new TooltipBehavior());
                } else {
                    closed = null;
                }
                cellItem.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getShortDateTimeFormattedValue(closed, PageCases.this);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state");
        columns.add(column);

        column = new AbstractExportableColumn<SelectableBean<CaseType>, String>(
                createStringResource("pageCases.table.workitems")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, IModel<SelectableBean<CaseType>> model) {
                cellItem.add(new Label(componentId,
                        model.getObject().getValue() != null && model.getObject().getValue().getWorkItem() != null ?
                                model.getObject().getValue().getWorkItem().size() : null));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getWorkItem() != null ?
                        Integer.toString(rowModel.getObject().getValue().getWorkItem().size()) : "");
            }


        };
        columns.add(column);
        return columns;
    }

    @Override
    protected Class getType(){
        return CaseType.class;
    }

    @Override
    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.TABLE_CASES;
    }


    @Override
    protected boolean isCreateNewObjectEnabled(){
        return false;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return getOperationOptionsBuilder()
                .item(CaseType.F_OBJECT_REF).resolve()
                .item(CaseType.F_TARGET_REF).resolve()
                .build();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();

        menu.add(new ButtonInlineMenuItem(createStringResource("pageCases.button.stopProcess")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<CaseType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            stopCaseProcessConfirmed(target);
                        } else {
                            stopCaseProcessConfirmed(target, Arrays.asList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public String getButtonIconCssClass(){
                return GuiStyleConstants.CLASS_STOP_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return getObjectListPanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.stopProcess.multiple.confirmationMessage", getObjectListPanel().getSelectedObjectsCount()) :
                        createStringResource("pageCases.button.stopProcess.confirmationMessage");
            }

        });
        menu.add(new ButtonInlineMenuItem(createStringResource("pageCases.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<CaseType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteCaseObjectsConfirmed(target);
                        } else {
                            deleteCaseObjectsConfirmed(target, Arrays.asList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public String getButtonIconCssClass(){
                return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return getObjectListPanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.delete.multiple.confirmationMessage", getObjectListPanel().getSelectedObjectsCount()) :
                        createStringResource("pageCases.button.delete.confirmationMessage");
            }

        });

        return menu;
    }

    private <O extends ObjectType> String getObjectRef(IModel<SelectableBean<CaseType>> caseModel) {
        CaseType caseModelObject = caseModel.getObject().getValue();
        if (caseModelObject == null || caseModelObject.getObjectRef() == null) {
            return "";
        }
        return WebComponentUtil.getEffectiveName(caseModelObject.getObjectRef(), AbstractRoleType.F_DISPLAY_NAME, PageCases.this,
                OPERATION_LOAD_REFERENCE_DISPLAY_NAME);
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target){
        deleteCaseObjectsConfirmed(target, getObjectListPanel().getSelectedObjects());
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target, List<CaseType> casesToDelete){
        if (casesToDelete == null){
            return;
        }
        casesToDelete.forEach(caseObject -> {
            WebModelServiceUtils.deleteObject(CaseType.class, caseObject.getOid(),
                    ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions(),
                    new OperationResult(OPERATION_DELETE_CASE_OBJECT), PageCases.this);
        });
        target.add(PageCases.this);
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target){
        stopCaseProcessConfirmed(target, getObjectListPanel().getSelectedObjects());
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target, List<CaseType> casesToStop){
        if (casesToStop == null){
            return;
        }
        casesToStop.forEach(caseObject -> {
            Task task = createSimpleTask(OPERATION_STOP_CASE_PROCESS);
            OperationResult result = new OperationResult(OPERATION_STOP_CASE_PROCESS);
            try {
                getWorkflowService().stopProcessInstance(caseObject.getOid(), task, result);
            } catch (Exception ex){
                LOGGER.error("Couldn't stop case process, ", ex.getLocalizedMessage());
                result.recordFatalError("Couldn't stop case process, ", ex);
                showResult(result);
            }
        });
        target.add(PageCases.this);
    }
}
