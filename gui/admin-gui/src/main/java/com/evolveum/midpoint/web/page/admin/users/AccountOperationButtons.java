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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mserbak
 */
public class AccountOperationButtons extends Panel {

    public AccountOperationButtons(String id, final IModel<ObjectWrapper> model) {
        super(id);

        initButtons(model);
    }

    private void initButtons(final IModel<ObjectWrapper> model) {
        AjaxLink unlink = new AjaxLink("unlink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
            	unlinkPerformed(target);
                
            }
        };
        add(unlink);

        Image unlinkImg = new Image("unlinkImg", new PackageResourceReference(AccountOperationButtons.class,
                "UnLink.png"));
        unlink.add(unlinkImg);


        AjaxLink delete = new AjaxLink("delete") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        add(delete);

        Image deleteImg = new Image("deleteImg", new PackageResourceReference(AccountOperationButtons.class,
                "Delete.png"));
        delete.add(deleteImg);
        
        
        }

    public void deletePerformed(AjaxRequestTarget target) {
    }

    public void unlinkPerformed(AjaxRequestTarget target) {
    }
  
    
}
