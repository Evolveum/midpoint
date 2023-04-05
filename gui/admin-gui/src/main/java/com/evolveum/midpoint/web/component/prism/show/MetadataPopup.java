/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismValueMetadataPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MetadataPopup extends SimplePopupable<ValueMetadataWrapperImpl> {

    private static final long serialVersionUID = 1L;

    private static final String ID_METADATA = "metadata";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    public MetadataPopup(String id, IModel<ValueMetadataWrapperImpl> model) {
        super(id, model, 400, 200, PageBase.createStringResourceStatic("MetadataPopupable.title"));

        initLayout();
    }

    public @NotNull Component getFooter() {
        return footer;
    }

    private void initLayout() {
        PrismValueMetadataPanel metadata = new PrismValueMetadataPanel(ID_METADATA, getModel());
        metadata.add(new VisibleBehaviour(() -> getModelObject() != null));
        metadata.setOutputMarkupId(true);
        add(metadata);

        footer = initFooter();
    }

    private Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxLink<Void> close = new AjaxLink<>(ID_CLOSE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCloseClicked(target);
            }
        };
        footer.add(close);

        return footer;
    }

    protected void onCloseClicked(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }
}
