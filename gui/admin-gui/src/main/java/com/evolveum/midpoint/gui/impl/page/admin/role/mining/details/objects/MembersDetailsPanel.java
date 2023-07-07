/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkIconPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MembersDetailsPanel extends BasePanel<String> implements Popupable {

    List<PrismObject<FocusType>> elements;
    String mode;

    public MembersDetailsPanel(String id, IModel<String> messageModel, List<PrismObject<FocusType>> elements, String pageParameterMode) {
        super(id, messageModel);
        this.elements = elements;
        this.mode = pageParameterMode;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        DisplayType displayType;
        if (mode.equals("ROLE")) {
            displayType = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
        } else {
            displayType = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
        }
        ListView<PrismObject<FocusType>> listView = new ListView<>("list", elements) {
            @Override
            protected void populateItem(ListItem<PrismObject<FocusType>> listItem) {
                PrismObject<FocusType> modelObject = listItem.getModelObject();
                listItem.add(new AjaxLinkIconPanel("object",
                        createStringResource(modelObject.getName()),
                        createStringResource(modelObject.getName()), displayType) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, modelObject.getOid());

                        if (mode.equals("ROLE")) {
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }else {
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                        }
                    }

                });
            }
        };
        add(listView);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 200;
    }


    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.members.details.panel.title");
    }
}
