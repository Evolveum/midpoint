/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.prism.AccountFooterPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";
    private static final String OPERATION_LOAD_USER = "pageUser.loadUser";

    private IModel<ObjectWrapper> userModel;
    private IModel<List<ObjectWrapper>> accountsModel;

    public PageUser() {
        userModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadUserWrapper();
            }
        };
        accountsModel = new LoadableModel<List<ObjectWrapper>>(false) {

            @Override
            protected List<ObjectWrapper> load() {
                return loadAcccountWrappers();
            }
        };

        initLayout();
    }

    private ObjectWrapper loadUserWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);

        PrismObject<UserType> user = null;
        StringValue userOid = getPageParameters().get(PARAM_USER_ID);
        if (userOid != null && StringUtils.isNotEmpty(userOid.toString())) {
            try {
                PropertyReferenceListType resolve = new PropertyReferenceListType();
                resolve.getProperty().add(Utils.fillPropertyReference("account"));
                resolve.getProperty().add(Utils.fillPropertyReference("resource"));

                user = getModelService().getObject(UserType.class, userOid.toString(), resolve, result);
            } catch (Exception ex) {
                //todo handle exception
                ex.printStackTrace();
            } finally {
                result.recomputeStatus();
                showResult(result);
            }
        } else {
            try {
                UserType userType = new UserType();
                getMidpointApplication().getPrismContext().adopt(userType);
                user = userType.asPrismObject();
            } catch (Exception ex) {
                //todo error handling
                ex.printStackTrace();
            }
        }

        if (user == null) {
            showResult(result);
            throw new RestartResponseException(PageUsers.class);
        }

        ObjectWrapper wrapper = new ObjectWrapper(null, null, user,
                com.evolveum.midpoint.web.component.prism.ContainerStatus.MODIFYING);
        wrapper.setShowEmpty(!isEditingUser());

        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel("userForm", userModel,
                new PackageResourceReference(PageUser.class, "User.png")) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageUser.description");
            }
        };
        mainForm.add(userForm);

        Accordion accordion = new Accordion("accordion");
        accordion.setMultipleSelect(true);
        accordion.setOpenedPanel(0);
        mainForm.add(accordion);

        AccordionItem accounts = new AccordionItem("accounts", createStringResource("pageUser.accounts"));
        accordion.getBodyContainer().add(accounts);
        initAccounts(accounts);

        AccordionItem assignments = new AccordionItem("assignments", createStringResource("pageUser.assignments"));
        accordion.getBodyContainer().add(assignments);
        initAssignments(assignments);

        initButtons(mainForm);
    }

    private void initAccounts(AccordionItem accounts) {
        ListView<ObjectWrapper> accountList = new ListView<ObjectWrapper>("accountList",
                accountsModel) {

            @Override
            protected void populateItem(ListItem<ObjectWrapper> item) {
                PrismObjectPanel account = new PrismObjectPanel("account", item.getModel(),
                        new PackageResourceReference(PageUser.class, "Hdd.png")) {

                    @Override
                    public WebMarkupContainer createFooterPanel(String footerId, IModel<ObjectWrapper> model) {
                        //todo
                        return new AccountFooterPanel(footerId, new Model("some id"),
                                new Model<String>("probably active"));
                    }
                };
                item.add(account);
            }
        };

        accounts.getBodyContainer().add(accountList);
    }

    private List<ObjectWrapper> loadAcccountWrappers() {
        List<ObjectWrapper> list = new ArrayList<ObjectWrapper>();

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<AccountShadowType> accounts = prismUser.asObjectable().getAccount();
        for (AccountShadowType account : accounts) {
            String resourceName = null;
            ResourceType resource = account.getResource();
            if (resource != null && StringUtils.isNotEmpty(resource.getName())) {
                resourceName = resource.getName();
            }

            ObjectWrapper wrapper = new ObjectWrapper(account.getName(), resourceName,
                    account.asPrismObject(), ContainerStatus.MODIFYING);
            wrapper.setMinimalized(true);
            list.add(wrapper);
        }

        return list;
    }

    private IModel<List> createAssignmentsList() {
        return new LoadableModel<List>(false) {

            @Override
            protected List load() {
                return new ArrayList();
            }
        };
    }

    private void initAssignments(AccordionItem assignments) {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new CheckBoxHeaderColumn());
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.type"), "type", "type"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.name"), "name", "name"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.active"), "active", "active"));

        ISortableDataProvider provider = new ListDataProvider(createAssignmentsList());
        TablePanel assignmentTable = new TablePanel("assignmentTable", provider, columns);
        assignmentTable.setShowPaging(false);

        assignments.getBodyContainer().add(assignmentTable);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton("save",
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, form);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSaveError(target, form);
            }
        };
        mainForm.add(save);

        AjaxLinkButton recalculate = new AjaxLinkButton("recalculate",
                createStringResource("pageUser.button.recalculate")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                recalculatePerformed(target);
            }
        };
        mainForm.add(recalculate);

        AjaxLinkButton refresh = new AjaxLinkButton("refresh",
                createStringResource("pageUser.button.refresh")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshPerformed(target);
            }
        };
        mainForm.add(refresh);

        AjaxLinkButton cancel = new AjaxLinkButton("cancel",
                createStringResource("pageUser.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageUsers.class);
    }

    private void savePerformed(AjaxRequestTarget target, Form form) {
        //todo implement
    }

    private void recalculatePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void refreshPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void onSaveError(AjaxRequestTarget target, Form form) {
        //todo implement
    }
}
