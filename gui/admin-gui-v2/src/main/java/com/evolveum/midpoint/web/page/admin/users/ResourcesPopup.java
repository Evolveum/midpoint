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
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDtoProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.web.page.admin.users.dto.UserResourceDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

public class ResourcesPopup extends Panel {

    public ResourcesPopup(String id, PageBase page) {
        super(id);

        initLayout(page);
    }

    private void initLayout(PageBase page) {
        TablePanel resources = new TablePanel<ResourceDto>("table",
                new ResourceDtoProvider(page), initResourceColumns());
        resources.setOutputMarkupId(true);
        add(resources);

        AjaxLinkButton addButton = new AjaxLinkButton("add",
                new StringResourceModel("resourcePopup.button.add", this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedResources());
            }
        };
        add(addButton);
    }

    private List<IColumn<ResourceDto>> initResourceColumns() {
        List<IColumn<ResourceDto>> columns = new ArrayList<IColumn<ResourceDto>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceDto>();
        columns.add(column);

        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.name", this, null), "name"));
        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.bundle", this, null), "bundle"));
        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.version", this, null), "version"));

        return columns;
    }

    private List<UserResourceDto> getSelectedResources() {
        List<UserResourceDto> list = new ArrayList<UserResourceDto>();

        TablePanel table = (TablePanel)get("table");
        ResourceDtoProvider provider = (ResourceDtoProvider)table.getDataTable().getDataProvider();
        for (ResourceDto bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            list.add(new UserResourceDto(bean.getOid(), bean.getName()));
        }

        return list;
    }

    protected void addPerformed(AjaxRequestTarget target, List<UserResourceDto> newResources) {

    }
}
