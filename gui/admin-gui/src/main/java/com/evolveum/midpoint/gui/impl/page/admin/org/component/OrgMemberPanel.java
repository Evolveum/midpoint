/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsQueryUtil;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "orgMembers")
@PanelInstance(identifier = "orgMembers", applicableForType = OrgType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 60))
@PanelInstance(identifier = "orgGovernance", applicableForType = OrgType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 70))
public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);

    public OrgMemberPanel(String id, FocusDetailsModels<OrgType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ObjectQuery getMemberQuery(@NotNull QueryScope scope, @NotNull Collection<QName> relations) {
        if (isSubtreeScope() && scope == QueryScope.ALL) {
            // Special case - searching for org descendants
            OrgType org = getModelObject();
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(org, getRelationValue());
            ObjectQuery query = getPageBase().getPrismContext().queryFor(getSearchTypeClass())
                    .type(getSearchTypeClass())
                    .isChildOf(ref.asReferenceValue()).build();

            LOGGER.trace("Searching all members of {} with query:\n{}", org, query.debugDumpLazily());
            return query;
        } else {
            return super.getMemberQuery(scope, relations);
        }
    }

    // FIXME: the code seems to be exactly the same as the code of the super method ... reconsider
    @Override
    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .ref(MemberOperationsQueryUtil.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    @Override
    protected List<QName> getNewMemberObjectTypes() {
        List<QName> objectTypes = ObjectTypeListUtil.createFocusTypeList();
        objectTypes.add(ResourceType.COMPLEX_TYPE);
        return objectTypes;
    }

    private Class<? extends AssignmentHolderType> getSearchTypeClass() {
        return getMemberPanelStorage().getSearch().getTypeClass();
    }

    @Override
    protected boolean reloadPageOnRefresh() {
        return "orgTreeMembers".equals(getStorageKeyTabSuffix());
    }

    @Override
    protected String getStorageKeyTabSuffix() {
        if (getPanelConfiguration() == null) {
            return "orgTreeMembers";
        }
        if ("orgMembers".equals(getPanelConfiguration().getIdentifier())) {
            return "orgMembers";
        }
        if ("orgGovernance".equals(getPanelConfiguration().getIdentifier())) {
            return "orgGovernance";
        }
        return "orgTreeMembers";
    }

    @Override
    protected @NotNull List<QName> getRelationsForRecomputeTask() {
        if (CollectionUtils.isEmpty(getSupportedRelations())) {
            return Collections.singletonList(PrismConstants.Q_ANY);
        }
        return super.getRelationsForRecomputeTask();
    }

    @Override
    protected Popupable createAssignPopup(QName stableRelation) {
        ChooseOrgMemberPopup browser = new ChooseOrgMemberPopup(OrgMemberPanel.this.getPageBase().getMainPopupBodyId(),
                getMemberPanelStorage().getSearch(), loadMultiFunctionalButtonModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected OrgType getAssignmentTargetRefObject() {
                return OrgMemberPanel.this.getModelObject();
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList() {
                return new ArrayList<>();
            }

            @Override
            protected QName getRelationIfIsStable() {
                return stableRelation;
            }

            @Override
            protected boolean shouldHideTaskLink() {
                return OrgMemberPanel.this.shouldHideTaskLink();
            }

            @Override
            public Component getFeedbackPanel() {
                return OrgMemberPanel.this.getFeedback();
            }
        };
        browser.setOutputMarkupId(true);
        return browser;
    }


}
