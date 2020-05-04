/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/claimableWorkItems", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CLAIMABLE_WORK_ITEMS_URL,
                label = "PageWorkItemsClaimable.auth.claimableWorkItems.label",
                description = "PageWorkItemsClaimable.auth.claimableWorkItems.description")})
public class PageWorkItemsClaimable extends PageCaseWorkItems {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageWorkItemsClaimable.class.getName() + ".";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String OPERATION_LOAD_CLAIMABLE_WORK_ITEMS = DOT_CLASS + "loadClaimableWorkItems";
    private static final String OPERATION_CLAIM_ITEMS = DOT_CLASS + "claimItem";

    public PageWorkItemsClaimable() {
        super();
    }

    @Override
    protected ObjectFilter getCaseWorkItemsFilter(){
        OperationResult result = new OperationResult(OPERATION_LOAD_CLAIMABLE_WORK_ITEMS);
        try {
            return QueryUtils.filterForClaimableItems(getPrismContext().queryFor(CaseWorkItemType.class),
                    SecurityUtils.getPrincipalUser().getOid(),
                    getRepositoryService(), getRelationRegistry(), result)
                    .buildFilter();
        } catch (SchemaException ex){
            return null;
        }
    }

    @Override
    protected List<InlineMenuItem> createRowActions(){
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new ButtonInlineMenuItem(createStringResource("pageWorkItem.button.claim")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        claimWorkItemPerformed(getRowModel(), target);
                    }
                };
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>)getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null){
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(CaseWorkItemUtil.isCaseWorkItemNotClosed(workItem));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return createStringResource("CaseWorkItemsPanel.confirmWorkItemsClaimAction");
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_CLAIM);
            }
        });

        return menu;
    }

    private void claimWorkItemPerformed(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel, AjaxRequestTarget target){
        List<PrismContainerValueWrapper<CaseWorkItemType>> selectedWorkItems = new ArrayList<>();
        if (rowModel == null) {
            ContainerableListPanel<CaseWorkItemType> tablePanel = getCaseWorkItemsTable().getContainerableListPanel();
            selectedWorkItems.addAll(tablePanel.getProvider().getSelectedData());
        } else {
            selectedWorkItems.addAll(Arrays.asList(rowModel.getObject()));
        }

        if (selectedWorkItems.size() == 0){
            warn(getString("CaseWorkItemsPanel.noWorkItemIsSelected"));
            target.add(getFeedbackPanel());
            return;
        }
        for (PrismContainerValueWrapper<CaseWorkItemType> workItem : selectedWorkItems) {
            WebComponentUtil.claimWorkItemActionPerformed(workItem.getRealValue(), OPERATION_CLAIM_ITEMS, target,
                    PageWorkItemsClaimable.this);
        }
    }

    @Override
    public Breadcrumb redirectBack() {
        return redirectBack(1);
    }
}
