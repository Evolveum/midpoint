/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.impl.component.AbstractObjectListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "inducedBy")
@PanelInstance(identifier = "inducedBy",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "InducedByPanel.label", order = 130))
public class InducedByPanel<AR extends AbstractRoleType> extends AbstractObjectListPanel<AR> {

    public InducedByPanel(String id, ObjectDetailsModels<AR> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected Class<AR> getDefaultType() {
        return (Class<AR>) AbstractRoleType.class;
    }

    protected ObjectQuery getCustomizeContentQuery(){
        return PrismContext.get().queryFor(AbstractRoleType.class)
                .item(ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TARGET_REF))
                .ref(getObjectDetailsModel().getObjectWrapper().getOid()).build();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_INDUCT_BY;
    }
}
