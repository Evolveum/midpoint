/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

public enum CollectionPanelType {
    ROLE_MEMBER_GOVERNANCE(true, "roleGovernance", true, FocusType.COMPLEX_TYPE),
    ROLE_MEMBER_MEMBER(true, "roleMembers", true, FocusType.COMPLEX_TYPE),
    SERVICE_MEMBER_GOVERNANCE(true, "serviceGovernance", true, FocusType.COMPLEX_TYPE),
    SERVICE_MEMBER_MEMBER(true, "serviceMembers", true, FocusType.COMPLEX_TYPE),
    ARCHETYPE_MEMBER_GOVERNANCE(true, "archetypeGovernance", true, FocusType.COMPLEX_TYPE),
    ARCHETYPE_MEMBER_MEMBER(true, "archetypeMembers", true, AssignmentHolderType.COMPLEX_TYPE),
    ORG_MEMBER_GOVERNANCE(true, "orgGovernance", true, FocusType.COMPLEX_TYPE),
    ORG_MEMBER_MEMBER(true, "orgMembers", true, AssignmentType.COMPLEX_TYPE),
    MEMBER_ORGANIZATION(true, null, true, AssignmentHolderType.COMPLEX_TYPE),
    CARDS_GOVERNANCE(true, null, true, FocusType.COMPLEX_TYPE),
    MEMBER_WIZARD(true, null, false, UserType.COMPLEX_TYPE),
    RESOURCE_SHADOW(false, null, false, null),
    REPO_SHADOW(false, null, false, null),
    ASSOCIABLE_SHADOW(false, null, false, null),
    PROJECTION_SHADOW(false, null, false, null),
    DEBUG(false, null, false, null),
    ASSIGNABLE(false, null, false, null);

    private boolean memberPanel;
    private String panelInstance;

    private boolean isAllowAllTypeSearch;
    private QName typeForNull;

    CollectionPanelType(boolean memberPanel, String panelInstance, boolean isAllowAllTypeSearch, QName typeForNull) {
        this.memberPanel = memberPanel;
        this.panelInstance = panelInstance;
        this.isAllowAllTypeSearch = isAllowAllTypeSearch;
        this.typeForNull = typeForNull;
    }

    public boolean isMemberPanel() {
        return memberPanel;
    }

    public static CollectionPanelType getPanelType(String panelInstance) {
        if (panelInstance == null) {
            return null;
        }
        for (CollectionPanelType collectionPanelType : CollectionPanelType.values()) {
            if (panelInstance.equals(collectionPanelType.panelInstance)) {
                return collectionPanelType;
            }
        }
        return null;
    }

    public QName getTypeForNull() {
        return typeForNull;
    }

    public boolean isAllowAllTypeSearch() {
        return isAllowAllTypeSearch;
    }
}
