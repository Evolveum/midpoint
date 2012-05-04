/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

public class ConfirmPopup extends PageAdmin {

	public ConfirmPopup(final ModalWindow window) {
		init(window);
	}
	
	public void init(final ModalWindow window) {
		add(new Label("confirmText", createStringResource("confirmPopup.confirmDeleteAccounts")));
		
		
		AjaxLinkButton yesButton = new AjaxLinkButton("yes",
                createStringResource("confirmPopup.yes")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
            	//TODO execute operation
                window.close(target);
            }
        };
        add(yesButton);
        
        AjaxLinkButton noButton = new AjaxLinkButton("no",
                createStringResource("confirmPopup.no")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
            	//TODO execute operation
                window.close(target);
            }
        };
        add(noButton);
        
    }
}
