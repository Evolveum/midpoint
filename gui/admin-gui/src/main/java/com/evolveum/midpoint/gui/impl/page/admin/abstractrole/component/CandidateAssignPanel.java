/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class CandidateAssignPanel extends BasePanel<String> implements Popupable {

    //TODO
    private static final String ID_PANELS = "table";
    Set<String> existMembersOid = new HashSet<>();

    public CandidateAssignPanel(String id, List<BusinessRoleDto> patternDeltas) {
        super(id);

        for (BusinessRoleDto bsApplicationDto : patternDeltas) {
            String oid = bsApplicationDto.getPrismObjectUser().getOid();
            existMembersOid.add(oid);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        intiLayout();

    }

    boolean enable = true;

    private MainObjectListPanel<UserType> candidateAssignPanel() {
        MainObjectListPanel<UserType> table = new MainObjectListPanel<>(CandidateAssignPanel.ID_PANELS, UserType.class) {
            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return null;
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<UserType>, String>> defaultColumns = super.createDefaultColumns();

                defaultColumns.add(new AbstractColumn<>(createStringResource("Add as candidate")) {
                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> item, String s,
                            IModel<SelectableBean<UserType>> iModel) {

                        String oid = iModel.getObject().getValue().getOid();

                        enable = !existMembersOid.contains(oid);

                        AjaxIconButton ajaxButton = new AjaxIconButton(s, new LoadableDetachableModel<>() {
                            @Override
                            protected String load() {
                                if (enable) {
                                    return " fa fa-plus";
                                } else {
                                    return " fa fa-ban";
                                }
                            }
                        },
                                createStringResource("idk")) {

                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                this.setEnabled(false);
                                enable = false;
                                ajaxRequestTarget.add(this);
                                performAddOperation(ajaxRequestTarget, iModel);
                            }
                        };
                        ajaxButton.setOutputMarkupId(true);
                        ajaxButton.setEnabled(enable);
                        item.add(ajaxButton);

                    }
                });
                return defaultColumns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    protected void intiLayout() {
        add(candidateAssignPanel());
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public void performAddOperation(AjaxRequestTarget ajaxRequestTarget, IModel<SelectableBean<UserType>> iModel) {

    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 60;
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
        return new StringResourceModel("RoleMining.modification.details.panel.title");
    }
}
