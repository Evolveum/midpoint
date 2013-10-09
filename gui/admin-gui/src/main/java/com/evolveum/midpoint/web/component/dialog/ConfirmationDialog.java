/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class ConfirmationDialog extends ModalWindow {

    public ConfirmationDialog(String id) {
        this(id, null, null);
    }

    public ConfirmationDialog(String id, IModel<String> title, IModel<String> message) {
        super(id);
        if (title != null) {
            setTitle(title);
        }
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

        if (message == null) {
            message = new Model();
        }
        initLayout(content, message);
    }

    public void setMessage(IModel<String> message) {
        Label label = (Label) getContent().get("confirmText");
        label.setDefaultModel(message);
    }

    public void setEscapeModelStringsByCaller(boolean value){
        Label label = (Label)getContent().get("confirmText").setEscapeModelStrings(value);
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
        close(target);
    }
}
