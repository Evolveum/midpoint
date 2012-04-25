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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageRoles extends PageAdminRoles {

    public PageRoles() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<RoleType>> columns = new ArrayList<IColumn<RoleType>>();

        IColumn column = new CheckBoxHeaderColumn<RoleType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<RoleType>>(createStringResource("pageRoles.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                RoleType role = rowModel.getObject().getValue();
                roleDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageRoles.description"), "value.description");
        columns.add(column);

        add(new TablePanel<RoleType>("table", new ObjectDataProvider(PageRoles.class), columns));
        
        
        
        ///////////////////////// POPUP MODAL WINDOW //////////////////////////////////
        final ModalWindow popupWindow;
        add(popupWindow = new ModalWindow("popupWindow"));
        
        popupWindow.setContent(new ResourcePopupWindow(popupWindow.getContentId(), popupWindow));
        popupWindow.setResizable(false);
        popupWindow.setTitle("Select resource");
        popupWindow.setCookieName("Resrource popup window");
        
        popupWindow.setInitialWidth(1100);
        popupWindow.setWidthUnit("px");
        
        
        popupWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
			
			@Override
			public boolean onCloseButtonClicked(AjaxRequestTarget target) {
				return true;
			}
		});
        
        popupWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
			
			@Override
			public void onClose(AjaxRequestTarget target) {
				popupWindow.close(target);
			}
		});
        
        add(new AjaxLinkButton("popup", new Model<String>("Popup")){
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				popupWindow.show(target);
			}
		});
        ///////////////////////////////////////////////////////////////////////////////
    }

    public void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, oid);
        setResponsePage(PageUser.class, parameters);
    }
}
