/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.panels.details;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.MiningSet;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.TableJCF;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

public class DetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    List<PrismObject<UserType>> users;
    List<MiningSet> miningSets;

    List<MiningSet> selectedMiningSet;

    public DetailsPanel(String id, IModel<String> messageModel, List<PrismObject<UserType>> users, List<MiningSet> miningSets) {
        super(id, messageModel);
        this.users = users;
        this.miningSets = miningSets;

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<PrismObject<UserType>> listview = new ListView<>("listview", users) {
            @Override
            protected void populateItem(ListItem<PrismObject<UserType>> listItem) {
                listItem.add(new AjaxLinkPanel("label", createStringResource(listItem.getModel().getObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        String oid = listItem.getModel().getObject().asObjectable().getOid();
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                        ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                    }
                });

            }
        };
        add(listview);

        if (miningSets != null) {
            listview.setVisible(false);
            TableJCF tableJCF = new TableJCF("datatable_extra", miningSets,
                    new RoleMiningFilter().filterRoles(getPageBase()), true, false);
            tableJCF.setOutputMarkupId(true);
            add(tableJCF);

        } else {
            WebMarkupContainer emptyContainer = new WebMarkupContainer("datatable_extra");
            emptyContainer.setVisible(false);
            add(emptyContainer);
        }

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);

    }

    private List<MiningSet> getSelectedItems() {
        return ((TableJCF) get(((PageBase) getPage()).createComponentPath("datatable_extra"))).getSelectedItems();
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        if (miningSets != null) {
            selectedMiningSet = getSelectedItems();
        }
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1000;
    }

    @Override
    public int getHeight() {
        return 500;
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
        return new StringResourceModel("Details.panel");
    }

}
