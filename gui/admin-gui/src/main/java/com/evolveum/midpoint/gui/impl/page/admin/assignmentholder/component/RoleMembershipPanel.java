/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AbstractObjectListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "roleMemberships")
public class RoleMembershipPanel<AH extends AssignmentHolderType> extends AbstractObjectListPanel<AH> {

    public RoleMembershipPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected Class<AH> getDefaultType() {
        return (Class<AH>) AssignmentHolderType.class;
    }

    protected ObjectQuery getCustomizeContentQuery() {

        if (getPageBase().isNativeRepo()) {
            return getPrismContext().queryFor(AbstractRoleType.class)
                    .referencedBy(getObjectDetailsModel().getObjectType().getClass(), AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                    .id(getObjectDetailsModel().getObjectWrapper().getOid())
                    .and().not().type(ArchetypeType.class)
                    .build();
        }

        String[] oids = getObjectDetailsModel().getObjectType().getRoleMembershipRef().stream()
                .filter(r -> r.getOid() != null)
                .map(r -> r.getOid())
                .toArray(String[]::new);

        return getPrismContext().queryFor(AbstractRoleType.class)
                .id(oids)
                .and().not().type(ArchetypeType.class)
                .build();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_ROLE_MEMBERSHIP;
    }
}
