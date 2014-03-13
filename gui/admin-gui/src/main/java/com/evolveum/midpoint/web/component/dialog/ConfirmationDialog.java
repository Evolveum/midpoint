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

import com.evolveum.midpoint.web.component.AjaxButton;
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

    private static final String ID_CONFIRM_TEXT = "confirmText";
    private static final String ID_YES = "yes";
    private static final String ID_NO = "no";

    private int confirmType;

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
        showUnloadConfirmation(false);
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

    public boolean getLabelEscapeModelStrings(){
        return true;
    }

    public void setMessage(IModel<String> message) {
        Label label = (Label) getContent().get(ID_CONFIRM_TEXT);
        label.setDefaultModel(message);
    }

    private void initLayout(WebMarkupContainer content, IModel<String> message) {
        Label label = new Label(ID_CONFIRM_TEXT, message);
        label.setEscapeModelStrings(getLabelEscapeModelStrings());
        content.add(label);

        AjaxButton yesButton = new AjaxButton(ID_YES, new StringResourceModel("confirmationDialog.yes",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                yesPerformed(target);
            }
        };
        content.add(yesButton);

        AjaxButton noButton = new AjaxButton(ID_NO, new StringResourceModel("confirmationDialog.no",
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

    /**
     * @return confirmation type identifier
     */
    public int getConfirmType() {
        return confirmType;
    }

    /**
     * This method provides solution for reusing one confirmation dialog for more messages/actions
     * by using confirmType identifier. See for example {@link com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel}
     *
     * @param confirmType
     */
    public void setConfirmType(int confirmType) {
        this.confirmType = confirmType;
    }
}
