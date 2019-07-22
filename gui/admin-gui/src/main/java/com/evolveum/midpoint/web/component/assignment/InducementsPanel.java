/*
 * Copyright (c) 2018 Evolveum
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

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
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
