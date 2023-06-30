/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.getImageScaleScript;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.CustomImageResource;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_IMAGE = "image";

    ClusterType cluster;
    String mode;

    OperationResult result = new OperationResult("GetObject");

    public ImageDetailsPanel(String id, IModel<String> messageModel, ClusterType cluster, String mode) {
        super(id, messageModel);
        this.mode = mode;
        this.cluster = cluster;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        List<ClusteringObjectMapped> clusteringObjectMapped = null;
        if (mode.equals(ClusterObjectUtils.Mode.ROLE.getDisplayString())) {
            clusteringObjectMapped = generateClusterMappedStructureRoleMode(cluster, getPageBase());
        } else if (mode.equals(ClusterObjectUtils.Mode.USER.getDisplayString())) {
            clusteringObjectMapped = generateClusterMappedStructure(cluster, getPageBase(), result);
        }

        List<String> clusterPoints = cluster.getPoints();

        CustomImageResource imageResource;

        imageResource = new CustomImageResource(clusteringObjectMapped, clusterPoints);

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
