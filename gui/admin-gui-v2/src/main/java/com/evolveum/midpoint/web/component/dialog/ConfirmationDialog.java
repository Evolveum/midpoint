/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class ConfirmationDialog extends ModalWindow {

    public ConfirmationDialog(String id, IModel<String> title, IModel<String> message) {
        super(id);
        setTitle(title);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(ConfirmationDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setResizable(false);
        setInitialWidth(350);
        setInitialHeight(150);
        setWidthUnit("px");

        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                ConfirmationDialog.this.close(target);
            }
        });

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
        initLayout(content, message);
    }

    private void initLayout(WebMarkupContainer content, IModel<String> message) {
        content.add(new Label("confirmText", message));

        AjaxLinkButton yesButton = new AjaxLinkButton("yes", new StringResourceModel("confirmationDialog.yes",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                yesPerformed(target);
            }
        };
        content.add(yesButton);

        AjaxLinkButton noButton = new AjaxLinkButton("no", new StringResourceModel("confirmationDialog.no",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        content.add(noButton);
    }

    public void yesPerformed(AjaxRequestTarget target) {

    }

    public void noPerformed(AjaxRequestTarget target) {

    }
}
