/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.getImageScaleScript;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.Mode;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getClusterTypeObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningOperationChunk;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_IMAGE = "image";

    String clusterOid;
    String mode;

    OperationResult result = new OperationResult("GetObject");

    public ImageDetailsPanel(String id, IModel<String> messageModel, String clusterOid, String mode) {
        super(id, messageModel);
        this.mode = mode;
        this.clusterOid = clusterOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {


        ClusterType cluster = getClusterTypeObject((PageBase) getPage(), clusterOid).asObjectable();
        MiningOperationChunk miningOperationChunk = new MiningOperationChunk(cluster, (PageBase) getPage(),
                Mode.valueOf(mode), result, false);

        CustomImageResource imageResource;

        imageResource = new CustomImageResource(miningOperationChunk, mode);

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

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1000;
    }

    @Override
    public int getHeight() {
        return 800;
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
        return createStringResource("");
    }

}
