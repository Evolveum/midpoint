/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathSegmentPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class RoleAnalysisPathTableSelector extends BasePanel<String> implements Popupable {

    private static final String ID_SELECTOR_ROLE = "selector_role";
    private static final String ID_SELECTOR_USER = "selector_user";

    private static final String ID_SAVE_BUTTON = "save_button";

    LoadableDetachableModel<DisplayValueOption> option;
    LoadableDetachableModel<ItemPathDto> itemPathRoleDto = new LoadableDetachableModel<>() {
        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected ItemPathDto load() {
            ItemPathDto itemPathDto = new ItemPathDto();
            itemPathDto.setObjectType(RoleType.COMPLEX_TYPE);
            return itemPathDto;
        }
    };

    LoadableDetachableModel<ItemPathDto> itemPathUserDto = new LoadableDetachableModel<>() {
        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected ItemPathDto load() {
            ItemPathDto itemPathDto = new ItemPathDto();
            itemPathDto.setObjectType(UserType.COMPLEX_TYPE);
            return itemPathDto;        }
    };

    public RoleAnalysisPathTableSelector(String id,
            IModel<String> messageModel,
            LoadableDetachableModel<DisplayValueOption> option) {
        super(id, messageModel);
        this.option = option;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void initLayout() {
        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON, new Model<>("Save")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                performAfterFinish(target);
                onClose(target);
            }
        };
        saveButton.setOutputMarkupId(true);
        add(saveButton);

        ItemPathPanel componentsRolePath = new ItemPathPanel(ID_SELECTOR_ROLE, itemPathRoleDto) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                if(option.getObject() == null) {
                    option.setObject(new DisplayValueOption());
                }
                option.getObject().setRoleItemValuePath(new ItemPathType(itemPathDto.toItemPath()).getItemPath());
            }

            @Override
            protected ItemPathSegmentPanel getItemPathSegmentPanel() {
                return super.getItemPathSegmentPanel();
            }

            @Override
            protected boolean setDefaultItemPath() {
                return super.setDefaultItemPath();
            }

            @Override
            protected boolean isRoleAnalysisSimple() {
                return true;
            }

            @Override
            protected boolean isNamespaceEnable() {
                return false;
            }

        };
        componentsRolePath.setOutputMarkupId(true);
        add(componentsRolePath);

        ItemPathPanel componentsUserPath = new ItemPathPanel(ID_SELECTOR_USER, itemPathUserDto) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                if(option.getObject() == null) {
                    option.setObject(new DisplayValueOption());
                }
                option.getObject().setUserItemValuePath(new ItemPathType(itemPathDto.toItemPath()).getItemPath());
            }

            @Override
            protected boolean isRoleAnalysisSimple() {
                return true;
            }

            @Override
            protected boolean isNamespaceEnable() {
                return false;
            }

        };
        componentsUserPath.setOutputMarkupId(true);
        add(componentsUserPath);
    }

    public void performAfterFinish(AjaxRequestTarget target) {
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public boolean isActive() {
        return false;
    }

    public RoleAnalysisProgressIncrement getHandler() {
        return null;
    }

    @Override
    public int getWidth() {
        return 30;
    }

    @Override
    public int getHeight() {
        return 30;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleAnalysisPathTableSelector.title");
    }
}
