/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * @author Viliam Repan (lazyman)
 */
public class DownloadButtonPanel extends BasePanel {

    private static final String ID_DOWNLOAD = "download";
    private static final String ID_DELETE = "delete";

    public DownloadButtonPanel(String id) {
        super(id);
        initLayout();
    }

    private void initLayout() {
        AjaxButton download = new AjaxButton(ID_DOWNLOAD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                downloadPerformed(target);
            }
        };
        add(download);

        AjaxButton delete = new AjaxButton(ID_DELETE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        add(delete);
    }

    protected void downloadPerformed(AjaxRequestTarget target) {

    }

    protected void deletePerformed(AjaxRequestTarget target) {

    }
}
