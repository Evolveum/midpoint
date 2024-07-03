/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class ResponseSelectablePanel extends BasePanel<AccessCertificationResponseType> {

        private static final Trace LOGGER = TraceManager.getTrace(ResponseSelectablePanel.class);

        private static final String ID_IMAGE = "imageId";
        private static final String ID_LINK = "link";
        private static final String ID_LABEL = "labelId";
        private static final String ID_DESCRIPTION = "descriptionId";

        private CertificationItemResponseHelper responseHelper;

        public ResponseSelectablePanel(String id, IModel<AccessCertificationResponseType> model) {
            super(id, model);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();

            responseHelper = new CertificationItemResponseHelper(getModelObject());

            initLayout();
        }

        private void initLayout() {

            WebMarkupContainer linkItem = new WebMarkupContainer(ID_LINK);

            AjaxEventBehavior linkClick = new AjaxEventBehavior("click") {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    responseSelectedPerformed(getModelObject(), target);
                }
            };
            linkItem.add(linkClick);
            linkItem.add(AttributeModifier.append("class", getAdditionalLinkStyle(getModelObject())));
            add(linkItem);

            Label icon = new Label(ID_IMAGE);
            icon.add(AttributeModifier.append("class", getIconClassModel()));
            icon.add(AttributeAppender.append("style", "--bs-bg-opacity: .5;"));
            linkItem.add(icon);

            linkItem.add(new Label(ID_LABEL, getResponseLabelModel()));

            Label description = new Label(ID_DESCRIPTION, getResponseDescriptionModel());
            linkItem.add(description);

        }

        private IModel<String> getIconClassModel() {
            return () -> {
                String iconCssClass = GuiDisplayTypeUtil.getIconCssClass(responseHelper.getResponseDisplayType());
                String iconBgColor = responseHelper.getBackgroundCssClass();
                return iconCssClass + " " + iconBgColor;
            };
        }

        private IModel<String> getIconStyleModel() {
            return () -> responseHelper.getBackgroundCssClass();
        }

        private IModel<String> getResponseLabelModel() {
            return () -> GuiDisplayTypeUtil.getTranslatedLabel(responseHelper.getResponseDisplayType());
        }

        private IModel<String> getResponseDescriptionModel() {
            return () -> GuiDisplayTypeUtil.getHelp(responseHelper.getResponseDisplayType());
        }

        protected void responseSelectedPerformed(AccessCertificationResponseType response, AjaxRequestTarget target) {
        }

        protected IModel<String> getAdditionalLinkStyle(AccessCertificationResponseType response) {
            return () -> "";
        }
}
