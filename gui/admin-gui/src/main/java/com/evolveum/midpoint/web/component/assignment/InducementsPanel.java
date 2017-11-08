/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class InducementsPanel extends AbstractRoleAssignmentPanel {

    private static final long serialVersionUID = 1L;


    public InducementsPanel(String id, IModel<ContainerWrapper<AssignmentType>> inducementContainerWrapperModel){
        super(id, inducementContainerWrapperModel);

    }
    
    @Override
    protected void initPaging() {
        getInducementsTabStorage().setPaging(ObjectPaging.createPaging(0, getItemsPerPage()));

    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE;
    }

    @Override
    protected int getItemsPerPage() {
        return (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE);
    }

    private AssignmentsTabStorage getInducementsTabStorage(){
        return getParentPage().getSessionStorage().getInducementsTabStorage();
    }

    @Override
    protected boolean showAllAssignmentsVisible(){
        return false;
    }
}
