/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/attorneyWorkItems",
                        matchUrlForSecurity = "/admin/attorneyWorkItems")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                        label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                        description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ATTORNEY_WORK_ITEMS_URL,
                        label = "PageAttorneySelection.auth.workItems.attorney.label",
                        description = "PageAttorneySelection.auth.workItems.attorney.description")
        })
public class PageAttorneySelection extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);

    private static final String DOT_CLASS = PageUsers.class.getName() + ".";

    private static final String OPERATION_GET_DONOR_FILTER = DOT_CLASS + "getDonorFilter";
    public static final String PARAMETER_DONOR_OID = "donorOid";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageAttorneySelection() {
        this(null);
    }

    public PageAttorneySelection(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);


        ObjectListPanel<UserType> table = new ObjectListPanel<UserType>(ID_TABLE, UserType.class,
                UserProfileStorage.TableId.PAGE_USER_SELECTION, Collections.emptyList()) {

//            @Override
//            protected boolean isRefreshEnabled() {
//                return false;
//            }
//
//            @Override
//            protected int getAutoRefreshInterval() {
//                return 0;
//            }

            @Override
            protected IColumn<SelectableBean<UserType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected IColumn<SelectableBean<UserType>, String> createNameColumn(IModel<String> columnNameModel, String itemPath) {
                return new ObjectNameColumn<UserType>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                        UserType object = rowModel.getObject().getValue();
                        selectUserPerformed(target, object.getOid());
                    }
                };
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createColumns() {
                return PageAttorneySelection.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return null;
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                if (query == null) {
                    query = PageAttorneySelection.this.getPrismContext().queryFactory().createQuery();
                }

                ModelInteractionService service = getModelInteractionService();

                Task task = createSimpleTask(OPERATION_GET_DONOR_FILTER);
                try {
                    ObjectFilter filter = query.getFilter();
                    // todo target authorization action
                    filter = service.getDonorFilter(UserType.class, filter, null,
                            task, task.getResult());

                    query.setFilter(filter);

                    return query;
                } catch (CommonException ex) {
                    LOGGER.error("Couldn't get donor filter, reason: {}", ex.getMessage());
                    LOGGER.debug("Couldn't get donor filter", ex);

                    PageError error = new PageError(ex);
                    throw new RestartResponseException(error);
                }
            }
        };
        table.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
        List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<UserType>, String> column = new PropertyColumn(
                createStringResource("UserType.givenName"), UserType.F_GIVEN_NAME.getLocalPart(),
                SelectableBeanImpl.F_VALUE + ".givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.familyName"),
                UserType.F_FAMILY_NAME.getLocalPart(), SelectableBeanImpl.F_VALUE + ".familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.fullName"),
                UserType.F_FULL_NAME.getLocalPart(), SelectableBeanImpl.F_VALUE + ".fullName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.emailAddress"), null,
                SelectableBeanImpl.F_VALUE + ".emailAddress");
        columns.add(column);

        return columns;
    }

    private void selectUserPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PARAMETER_DONOR_OID, oid);
        PageWorkItemsAttorney workItemsPage = new PageWorkItemsAttorney(parameters);
        navigateToNext(workItemsPage);
    }
}
