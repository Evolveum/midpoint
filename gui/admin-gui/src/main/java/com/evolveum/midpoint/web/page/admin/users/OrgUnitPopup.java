/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.UserRoleDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

public class OrgUnitPopup extends Panel {

    public OrgUnitPopup(String id, PageBase page) {
        super(id);

        initLayout(page);
    }

    private void initLayout(PageBase page) {
        Form orgUnitForm = new Form("orgUnitForm");
        add(orgUnitForm);
        initButtons(orgUnitForm);
    }

    private void initButtons(Form form) {
        AjaxLinkButton addButton = new AjaxLinkButton("add",
                new StringResourceModel("orgUnitPopup.button.add", this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //addPerformed(target, getSelectedRoles());
            }
        };
        form.add(addButton);
    }

    private List<UserRoleDto> getSelectedOrgUnits() {
    	// TODO: implement
        List<UserRoleDto> orgUnits = new ArrayList<UserRoleDto>();
        return orgUnits;
    }

    protected void addPerformed(AjaxRequestTarget target, List<UserRoleDto> selectedRoles) {

    }
}
