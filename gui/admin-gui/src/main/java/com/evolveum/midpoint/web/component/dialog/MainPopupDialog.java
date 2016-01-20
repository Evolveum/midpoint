/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainPopupDialog extends ModalWindow {

    public MainPopupDialog(String id) {
        super(id);

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
                MainPopupDialog.this.close(target);
            }
        });
    }
}
