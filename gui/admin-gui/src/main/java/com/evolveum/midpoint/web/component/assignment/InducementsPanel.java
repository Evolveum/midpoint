/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class InducementsPanel extends AbstractRoleAssignmentPanel {

    private static final long serialVersionUID = 1L;


    public InducementsPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> inducementContainerWrapperModel){
        super(id, inducementContainerWrapperModel);

    }

    @Override
    protected void initCustomPaging() {
        getInducementsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, ((int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE))));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE;
    }

    private ObjectTabStorage getInducementsTabStorage(){
        return getParentPage().getSessionStorage().getInducementsTabStorage();
    }
}
