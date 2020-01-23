/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.box;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressbarPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author katkav
 * @author semancik
 * @author skublik
 */
public class BasicInfoBoxPanel extends InfoBoxPanel{
    private static final long serialVersionUID = 1L;

    private static final String ID_PROGRESS = "progress";
    private static final String ID_PROGRESS_BAR = "progressBar";
    private static final String ID_DESCRIPTION = "description";

    public BasicInfoBoxPanel(String id, IModel<InfoBoxType> model) {
        super(id, model);
    }

    public BasicInfoBoxPanel(String id, IModel<InfoBoxType> model, Class<? extends IRequestablePage> linkPage) {
        super(id, model, linkPage);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer parentInfoBox, IModel<InfoBoxType> model,
            Class<? extends IRequestablePage> linkPage) {
        WebMarkupContainer progress = new WebMarkupContainer(ID_PROGRESS);
        parentInfoBox.add(progress);
        progress.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return model.getObject().getProgress() != null;
            }
        });
        ProgressbarPanel progressBar = new ProgressbarPanel(ID_PROGRESS_BAR, new PropertyModel<>(model, InfoBoxType.PROGRESS));
        progress.add(progressBar);

        Label description = new Label(ID_DESCRIPTION, new PropertyModel<String>(model, InfoBoxType.DESCRIPTION));
        parentInfoBox.add(description);

        if (linkPage != null) {
            add(new AjaxEventBehavior("click") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    setResponsePage(linkPage);
                }
            });
        }
    }

}
