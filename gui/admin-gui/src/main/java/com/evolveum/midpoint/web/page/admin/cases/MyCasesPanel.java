/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.page.admin.server.CasesTablePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;


@PanelType(name = "myRequests")
public class MyCasesPanel extends CasesTablePanel {

    private static final long serialVersionUID = 1L;

    public MyCasesPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, configurationType);
    }

    protected ObjectFilter getCasesFilter(){
        return QueryUtils.filterForMyRequests(getPrismContext().queryFor(CaseType.class),
                        AuthUtil.getPrincipalUser().getOid())
                .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                .buildFilter();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }



}
