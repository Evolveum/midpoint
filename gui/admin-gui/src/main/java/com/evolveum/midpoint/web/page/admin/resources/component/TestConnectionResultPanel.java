/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.component;

import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

/**
 * @author katkav
 */
public class TestConnectionResultPanel extends BasePanel<List<OpResult>> implements Popupable {

    private String resourceOid;
    private boolean isLoaded = false;

    public TestConnectionResultPanel(String id, String resourceOid, Page parentPage) {
        super(id);
        this.resourceOid = resourceOid;
        initLayout(parentPage);
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_RESULT = "result";
    private static final String ID_MESSAGE = "message";
    private static final String ID_CONTENT_PANEL = "contentPanel";
    private static final String ID_OK = "ok";


    private void initLayout(Page parentPage) {
        WebMarkupContainer contentPanel = new WebMarkupContainer(ID_CONTENT_PANEL);
        contentPanel.setOutputMarkupId(true);
        add(contentPanel);

        Label messageLabel = new Label(ID_MESSAGE, ((PageBase) parentPage).createStringResource("TestConnectionResultPanel.message"));
        messageLabel.setOutputMarkupId(true);
        contentPanel.add(messageLabel);
        messageLabel.add(new VisibleEnableBehaviour() {
            public boolean isVisible() {
                return !isLoaded;
            }
        });

        AjaxLazyLoadPanel resultsPanel = new AjaxLazyLoadPanel(ID_RESULT) {
            @Override
            public Component getLazyLoadComponent(String id) {
                return new TestConnectionMessagesPanel(id, resourceOid, (PageBase) parentPage);
            }

            @Override
            protected void onContentLoaded(Component content, Optional optionalTarget) {
                isLoaded = true;
                AjaxRequestTarget target = (AjaxRequestTarget) optionalTarget.get();
                target.add(content);
                target.add(messageLabel);
            }

        };
        contentPanel.add(resultsPanel);

        AjaxButton ok = new AjaxButton(ID_OK) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                okPerformed(target);

            }

        };

        contentPanel.add(ok);
    }

    protected void okPerformed(AjaxRequestTarget target) {

    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit(){
        return "%";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("TestConnectionResultPanel.testConnection.result");
    }

    public WebMarkupContainer getContentPanel() {
        return (WebMarkupContainer) get(ID_CONTENT_PANEL);
    }
}
