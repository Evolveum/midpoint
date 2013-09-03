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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.prism.HeaderStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
        unlinkImg.add(new AttributeModifier("title", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                String key = HeaderStatus.UNLINKED == wrapper.getHeaderStatus() ?
                        "prismOperationButtonPanel.undoUnlink" : "prismOperationButtonPanel.unlink";

                return AccountOperationButtons.this.getString(key);
            }
        }));
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
        deleteImg.add(new AttributeModifier("title", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                String key = HeaderStatus.DELETED == wrapper.getHeaderStatus() ?
                        "prismOperationButtonPanel.undoDelete" : "prismOperationButtonPanel.delete";

                return AccountOperationButtons.this.getString(key);
            }
        }));
        delete.add(deleteImg);
        
        
        }

    public void deletePerformed(AjaxRequestTarget target) {
    }

    public void unlinkPerformed(AjaxRequestTarget target) {
    }
  
    
}
