/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {

    public PageResources() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<UserType>> columns = new ArrayList<IColumn<UserType>>();

        IColumn column = new CheckBoxColumn<UserType>();
        columns.add(column);

        column = new LinkColumn(createStringResource("pageResources.name"), "name", "value.name");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageResources.givenName"), "givenName", "value.givenName");
        columns.add(column);

        add(new TablePanel<UserType>("table", UserType.class, columns));
    }
}
