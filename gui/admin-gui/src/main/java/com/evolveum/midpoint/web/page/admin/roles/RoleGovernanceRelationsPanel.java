package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class RoleGovernanceRelationsPanel extends RoleMemberPanel<RoleType> {

    public RoleGovernanceRelationsPanel(String id, IModel<RoleType> model, List<RelationTypes> relations, PageBase pageBase) {
        super(id, model, relations, pageBase);
    }

    @Override
    protected List<InlineMenuItem> createNewMemberInlineMenuItems() {
        List<InlineMenuItem> newMemberMenuItems = new ArrayList<>();
        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createApprover"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.APPROVER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignApprovers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.APPROVER.getRelation(), target);
                    }
                }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createOwner"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.OWNER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignOwners"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.OWNER.getRelation(), target);
                    }
                }));
        newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createManager"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.MANAGER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addManagers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.MANAGER.getRelation(), target);
                    }
                }));
        return newMemberMenuItems;
    }

    @Override
    protected List<InlineMenuItem> createRemoveMemberInlineMenuItems() {
        return super.createRemoveMemberInlineMenuItems();
    }

    @Override
    protected List<InlineMenuItem> createMemberRecomputeInlineMenuItems() {
        return new ArrayList<>();
    }



}
