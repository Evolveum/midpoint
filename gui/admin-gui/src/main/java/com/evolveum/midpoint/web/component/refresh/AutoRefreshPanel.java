/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.refresh;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;

/**
 * Provides simple "auto refresh" panel: buttons for start/stop auto refreshing, requesting manual refresh, and status label.
 *
 * @author mederly
 * @see Refreshable
 */
public class AutoRefreshPanel extends BasePanel<AutoRefreshDto> {

    private static final String ID_REFRESH_NOW = "refreshNow";
    private static final String ID_START = "startPause";
    private static final String ID_STATUS = "status";

    public AutoRefreshPanel(String id, IModel<AutoRefreshDto> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {

        final LinkIconPanel refreshNow = new LinkIconPanel(ID_REFRESH_NOW, new Model("fa fa-refresh"), createStringResource("autoRefreshPanel.refreshNow")) {
            @Override
            protected void onClickPerformed(AjaxRequestTarget target) {
                refreshPerformed(target);
            }
        };
        add(refreshNow);

        final LinkIconPanel resumePauseRefreshing = new LinkIconPanel(ID_START, (IModel<String>) () -> createResumePauseButton(), createStringResource("autoRefreshPanel.resumeRefreshing")) {
            @Override
            protected void onClickPerformed(AjaxRequestTarget target) {
                getModelObject().setEnabled(!getModelObject().isEnabled());
                refreshPerformed(target);
            }
        };

        add(resumePauseRefreshing);

        final Label status = new Label(ID_STATUS, (IModel<String>) () -> {
            AutoRefreshDto dto = getModelObject();
            if (dto.isEnabled()) {
                return createStringResource("autoRefreshPanel.refreshingEach", dto.getInterval() / 1000).getString();
            } else {
                return createStringResource("autoRefreshPanel.noRefreshing").getString();
            }
        });
        add(status);

    }

    private String createResumePauseButton() {
        if (isRefreshEnabled()) {
            return "fa fa-pause";
        }
        return "fa fa-play";
    }

    protected void refreshPerformed(AjaxRequestTarget target) {
    }

    protected boolean isRefreshEnabled() {
        return getModelObject().isEnabled();
    }

}
