/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.CustomImageResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.getImageScaleScript;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_IMAGE = "image";

    List<PrismObject<UserType>> members;
    String identifier;
    List<String> occupiedRoles;

    public ImageDetailsPanel(String id, IModel<String> messageModel,
            List<PrismObject<UserType>> members, List<String> occupiedRoles, String identifier) {
        super(id, messageModel);
        this.members = members;
        this.identifier = identifier;
        this.occupiedRoles = occupiedRoles;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        CustomImageResource imageResource;

        imageResource = new CustomImageResource(occupiedRoles, members);

        Image image = new Image(ID_IMAGE, imageResource);

        image.add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                target.appendJavaScript(getImageScaleScript());

            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(getImageScaleScript()));

            }
        });

        add(image);

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

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1700;
    }

    @Override
    public int getHeight() {
        return 1100;
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
