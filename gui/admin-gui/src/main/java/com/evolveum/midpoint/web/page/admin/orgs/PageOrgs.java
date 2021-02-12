/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.orgs;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/orgs", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORGS_URL,
                label = "PageRoles.auth.orgs.label",
                description = "PageRoles.auth.orgs.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORGS_VIEW_URL,
                label = "PageRoles.auth.orgs.view.label",
                description = "PageRoles.auth.orgs.view.description")})
public class PageOrgs extends PageAdmin {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgs.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageOrgs() {
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

        MainObjectListPanel<OrgType> table = new MainObjectListPanel<OrgType>(ID_TABLE, OrgType.class, getQueryOptions()) {
            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, OrgType org) {
                PageOrgs.this.orgDetailsPerformed(target, org.getOid());
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_ORGS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<OrgType> listInlineMenuHelper = new FocusListInlineMenuHelper<OrgType>(OrgType.class, PageOrgs.this, this){
                    private static final long serialVersionUID = 1L;

                    protected boolean isShowConfirmationDialog(ColumnMenuAction action){
                        return PageOrgs.this.isShowConfirmationDialog(action);
                    }

                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
                        return PageOrgs.this.getConfirmationMessageModel(action, actionName);
                    }

                };
                return listInlineMenuHelper.createRowActions(getType());
            }

            @Override
            protected List<IColumn<SelectableBean<OrgType>, String>> createDefaultColumns() {
                return ColumnUtils.getDefaultOrgColumns(getPageBase());
            }

            @Override
            protected List<ItemPath> getFixedSearchItems() {
                List<ItemPath> fixedSearchItems = new ArrayList<>();
                fixedSearchItems.add(ObjectType.F_NAME);
                fixedSearchItems.add(AbstractRoleType.F_DISPLAY_NAME);
                fixedSearchItems.add(OrgType.F_PARENT_ORG_REF);
                return fixedSearchItems;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return getOperationOptionsBuilder()
                .item(ObjectType.F_PARENT_ORG_REF).resolve()
                .build();
    }

    private void orgDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageOrgUnit.class, parameters);
    }

    private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
        return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);

    }

    private MainObjectListPanel<OrgType> getObjectListPanel() {
        return (MainObjectListPanel<OrgType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }
}
