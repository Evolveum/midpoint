/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.roles.component;

import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class UserOrgReferenceChoosePanel extends SimplePanel<Boolean>{

    private static final String ID_TYPE = "type";

    private Boolean isOwnerOrg = Boolean.FALSE;

    public UserOrgReferenceChoosePanel(String id, Boolean model) {
        super(id, new Model<>(model));
        this.isOwnerOrg = model;
    }

    public Boolean getOwnerOrg() {
        return isOwnerOrg;
    }

    @Override
    protected void initLayout() {
        TwoStateBooleanPanel type = new TwoStateBooleanPanel(ID_TYPE, new PropertyModel<Boolean>(this, "isOwnerOrg"),
                "UserOrgReferenceChoosePanel.type.user", "UserOrgReferenceChoosePanel.type.org", null){

            @Override
            protected void onStateChanged(AjaxRequestTarget target, Boolean newValue) {
                onReferenceTypeChangePerformed(target, newValue);
            }
        };
        add(type);
    }

    /**
     *  Override to provide custom action on change state event
     * */
    protected void onReferenceTypeChangePerformed(AjaxRequestTarget target, Boolean newValue){}
}
