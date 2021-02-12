/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.services;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;


/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(url = "/admin/services", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                label = "PageServices.auth.services.label",
                description = "PageServices.auth.services.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL,
                label = "PageServices.auth.services.view.label",
                description = "PageServices.auth.services.view.description")})
public class PageServices extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageServices.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageServices.class);
    private static final String OPERATION_DELETE_SERVICES = DOT_CLASS + "deleteServices";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private IModel<Search> searchModel;

    public PageServices() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ServiceType> table = new MainObjectListPanel<ServiceType>(ID_TABLE, ServiceType.class) {
            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
                serviceDetailsPerformed(target, service);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_SERVICES;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<ServiceType> listInlineMenuHelper = new FocusListInlineMenuHelper<ServiceType>(ServiceType.class, PageServices.this, this) {
                    private static final long serialVersionUID = 1L;

                    protected boolean isShowConfirmationDialog(ColumnMenuAction action) {
                        return PageServices.this.isShowConfirmationDialog(action);
                    }

                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
                        return PageServices.this.getConfirmationMessageModel(action, actionName);
                    }
                };
                return listInlineMenuHelper.createRowActions(getType());
            }

            @Override
            protected List<IColumn<SelectableBean<ServiceType>, String>> createDefaultColumns() {
                return ColumnUtils.getDefaultServiceColumns();
            }

            @Override
            protected List<ItemPath> getFixedSearchItems() {
                List<ItemPath> fixedSearchItems = new ArrayList<>();
                fixedSearchItems.add(ObjectType.F_NAME);
                fixedSearchItems.add(AbstractRoleType.F_DISPLAY_NAME);
                fixedSearchItems.add(AbstractRoleType.F_IDENTIFIER);
                return fixedSearchItems;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    protected void serviceDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
        navigateToNext(PageService.class, parameters);
    }

     private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
        return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);
    }

    private MainObjectListPanel<ServiceType> getObjectListPanel() {
        return (MainObjectListPanel<ServiceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }
}
