package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Honchar.
 */
public class DeleteAllPanel extends Panel  implements Popupable{

    private static final Trace LOGGER = TraceManager.getTrace(DeleteAllPanel.class);

    private static final String DOT_CLASS = DeleteAllPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_ITERATIVE_TASK = DOT_CLASS + "searchIterativeTask";
    private static final String OPERATION_COUNT_TASK = DOT_CLASS + "countObjectsTask";

    private static final String ID_CONTENT = "content";
    private static final String ID_CHB_USERS = "checkboxUsers";
    private static final String ID_CHB_ORG = "checkboxOrg";
    private static final String ID_CHB_ACCOUNT_SHADOW = "checkboxAccountShadow";
    private static final String ID_CHB_NON_ACCOUNT_SHADOW = "checkboxNonAccountShadow";
    private static final String ID_TEXT_USERS = "confirmTextUsers";
    private static final String ID_TEXT_ORGS = "confirmTextOrgUnits";
    private static final String ID_TEXT_ACC_SHADOWS = "confirmTextAccountShadow";
    private static final String ID_TEXT_NON_ACC_SHADOW = "confirmTextNonAccountShadows";
    private static final String ID_YES = "yes";
    private static final String ID_NO = "no";
    private static final String ID_TOTAL = "totalCountLabel";

    private IModel<DeleteAllDto> model = new Model(new DeleteAllDto());

    public DeleteAllPanel(String id){
        super(id);
        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        add(content);
        initLayout(content);
    }

    public IModel<DeleteAllDto> getModel(){
        return model;
    }

    private void updateLabelModel(AjaxRequestTarget target, String labelID){
        LoadableModel<String> model = (LoadableModel<String>)getLabel(labelID).getDefaultModel();
        model.reset();

        model = (LoadableModel<String>)getLabel(ID_TOTAL).getDefaultModel();
        model.reset();

        target.add(getLabel(labelID));
        target.add(getLabel(ID_TOTAL));
    }

    private void initLayout(WebMarkupContainer content){

        CheckBox deleteUsersCheckbox = new CheckBox(ID_CHB_USERS, new PropertyModel<Boolean>(model, DeleteAllDto.F_USERS));
        deleteUsersCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_USERS);
            }
        });
        content.add(deleteUsersCheckbox);

        CheckBox deleteOrgsCheckbox = new CheckBox(ID_CHB_ORG, new PropertyModel<Boolean>(model, DeleteAllDto.F_ORGS));
        deleteOrgsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ORGS);
            }
        });
        content.add(deleteOrgsCheckbox);

        CheckBox deleteAccountShadowsCheckbox = new CheckBox(ID_CHB_ACCOUNT_SHADOW,
                new PropertyModel<Boolean>(model, DeleteAllDto.F_ACC_SHADOW));
        deleteAccountShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ACC_SHADOWS);
            }
        });
        content.add(deleteAccountShadowsCheckbox);

        CheckBox deleteNonAccountShadowsCheckbox = new CheckBox(ID_CHB_NON_ACCOUNT_SHADOW,
                new PropertyModel<Boolean>(model, DeleteAllDto.F_NON_ACC_SHADOW));
        deleteNonAccountShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_NON_ACC_SHADOW);
            }
        });
        content.add(deleteNonAccountShadowsCheckbox);

        Label usersLabel = new Label(ID_TEXT_USERS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteUsersMessage();
            }
        });
        usersLabel.setOutputMarkupId(true);
        content.add(usersLabel);

        Label orgsLabel = new Label(ID_TEXT_ORGS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteOrgUnitsMessage();
            }
        });
        orgsLabel.setOutputMarkupId(true);
        content.add(orgsLabel);

        Label accShadowsLabel = new Label(ID_TEXT_ACC_SHADOWS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteAccountShadowsMessage();
            }
        });
        accShadowsLabel.setOutputMarkupId(true);
        content.add(accShadowsLabel);

        Label nonAccShadowsLabel = new Label(ID_TEXT_NON_ACC_SHADOW, new LoadableModel<String>() {

            @Override
            protected String load() {
                return createDeleteNonAccountShadowsMessage();
            }
        });
        nonAccShadowsLabel.setOutputMarkupId(true);
        content.add(nonAccShadowsLabel);

        Label countLabel = new Label(ID_TOTAL, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createTotalMessage();
            }
        });
        countLabel.setOutputMarkupId(true);
        content.add(countLabel);

        AjaxButton yesButton = new AjaxButton(ID_YES, new StringResourceModel("deleteAllDialog.yes",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                yesPerformed(target);
            }
        };
        content.add(yesButton);

        AjaxButton noButton = new AjaxButton(ID_NO, new StringResourceModel("deleteAllDialog.no",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        content.add(noButton);
    }

    private Label getLabel(String ID){
        return (Label)get(ID_CONTENT +":"+ID);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
    }

    private String createTotalMessage(){
        DeleteAllDto dto = model.getObject();
        dto.setObjectsToDelete(0);

        if(dto.getDeleteUsers()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getUserCount());
        }
        if(dto.getDeleteOrgs()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getOrgUnitCount());
        }
        if(dto.getDeleteAccountShadow()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getAccountShadowCount());
        }
        if(dto.getDeleteNonAccountShadow()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getNonAccountShadowCount());
        }

        return createStringResource("deleteAllDialog.label.totalToDelete", dto.getObjectsToDelete()).getString();
    }

    private String createDeleteUsersMessage(){
        if(!model.getObject().getDeleteUsers()){
            return createStringResource("deleteAllDialog.label.usersDelete", 0).getString();
        }
        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            dto.setUserCount(getPagebase().getModelService().countObjects(UserType.class, null, options, task, result));

            //We need to substract 1, because we are not deleting user 'Administrator'
            dto.setUserCount(dto.getUserCount()-1);
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getUserCount());
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logUnexpectedException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        return createStringResource("deleteAllDialog.label.usersDelete", dto.getUserCount()).getString();
    }

    private String createDeleteOrgUnitsMessage(){
        if(!model.getObject().getDeleteOrgs()){
            return createStringResource("deleteAllDialog.label.orgUnitsDelete", 0).getString();
        }

        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            dto.setOrgUnitCount(getPagebase().getModelService().countObjects(OrgType.class, null, options, task, result));

            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getOrgUnitCount());
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logUnexpectedException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        return createStringResource("deleteAllDialog.label.orgUnitsDelete", dto.getOrgUnitCount()).getString();
    }

    private void countShadows(boolean isAccountShadow){
        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_SEARCH_ITERATIVE_TASK);
        OperationResult result = new OperationResult(OPERATION_SEARCH_ITERATIVE_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, getPagebase().getPrismContext())
                    .item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
                    .buildFilter();
            if (isAccountShadow) {
                ObjectQuery query = ObjectQuery.createObjectQuery(filter);
                dto.setAccountShadowCount(getPagebase().getModelService().countObjects(ShadowType.class, query, options, task, result));
                dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getAccountShadowCount());
            } else {
                ObjectQuery query = ObjectQuery.createObjectQuery(NotFilter.createNot(filter));
                dto.setNonAccountShadowCount(getPagebase().getModelService().countObjects(ShadowType.class, query, options, task, result));
                dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getNonAccountShadowCount());
            }

        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logUnexpectedException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }
    }

    private String createDeleteNonAccountShadowsMessage(){
        if(!model.getObject().getDeleteNonAccountShadow()){
            return createStringResource("deleteAllDialog.label.nonAccountShadowsDelete", 0).getString();
        }
        DeleteAllDto dto = model.getObject();

        countShadows(false);

        return createStringResource("deleteAllDialog.label.nonAccountShadowsDelete", dto.getNonAccountShadowCount()).getString();
    }

    private String createDeleteAccountShadowsMessage(){
        if(!model.getObject().getDeleteAccountShadow()){
            return createStringResource("deleteAllDialog.label.accountShadowsDelete", 0).getString();
        }

        DeleteAllDto dto = model.getObject();
        countShadows(true);

        return createStringResource("deleteAllDialog.label.accountShadowsDelete", dto.getAccountShadowCount()).getString();
    }

    public int getObjectsToDelete(){
        return model.getObject().getObjectsToDelete();
    }

    private PageBase getPagebase(){
        return (PageBase) getPage();
    }

    public void yesPerformed(AjaxRequestTarget target) {

    }

    public void noPerformed(AjaxRequestTarget target) {
        getPagebase().hideMainPopup(target);
    }

    @Override
    public int getWidth() {
        return 650;
    }

    @Override
    public int getHeight() {
        return 350;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("pageDebugList.dialog.title.deleteAll");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}

