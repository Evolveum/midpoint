/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

/**
 * A page hosting the object selection panel. Although these are a bit coupled, we decided
 * to work with them separately to make panel reusable.
 */
public class ObjectSelectionPage extends PageDialog {

    public static final String ID_OBJECT_SELECTION_PANEL = "objectSelectionPanel";

    public ObjectSelectionPage(ObjectSelectionPanel innerPanel, PageBase callingPage) {
        super(callingPage);
        innerPanel.setOutputMarkupId(true);
        add(innerPanel);
    }

    public static <T extends ObjectType> void prepareDialog(ModalWindow dialog, ObjectSelectionPanel.Context context,
                                                            final Component callingComponent, String titleResourceKey, final String idToRefresh) {
        dialog.setPageCreator(new ObjectSelectionPage.PageCreator(dialog, context));
        dialog.setInitialWidth(800);
        dialog.setInitialHeight(500);
        dialog.setTitle(PageBase.createStringResourceStatic(callingComponent, titleResourceKey));
        dialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            // We are not able to refresh targets residing in the parent page
            // from inside the modal window -> so we have to do it in this
            // context, when the modal window is being closed.
            public void onClose(AjaxRequestTarget target) {
                target.add(callingComponent.get(idToRefresh));
            }
        });
        dialog.showUnloadConfirmation(false);
        dialog.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        dialog.setCookieName(ObjectSelectionPanel.class.getSimpleName() + ((int) (Math.random() * 100)));
        dialog.setWidthUnit("px");
    }

    // Page creator is used to eliminate "form in a form problem" (MID-2589) - when
    // an object hierarchy contains form1 -> modal window -> form2, then while submitting
    // form2, the form1 gets updated with inappropriate search query, leading to zeroing
    // nullable dropdown boxes. The solution can be factoring out modal window out of form1,
    // but this is quite problematic when there is a rich hierarchy of panels between
    // form1 -> ... (panels) ... -> modal window. So, the other solution is
    // to break the hierarchy by introducing a page hosting the modal window content.
    public static class PageCreator implements ModalWindow.PageCreator {

        private ModalWindow modalWindow;
        private ObjectSelectionPanel.Context context;

        public PageCreator(ModalWindow dialog, ObjectSelectionPanel.Context context) {
            this.modalWindow = dialog;
            this.context = context;
        }

        @Override
        public Page createPage() {
            context.callingPageReference = context.getCallingPage().getPageReference();

            ObjectSelectionPanel selectionPanel = new ObjectSelectionPanel(
                    ObjectSelectionPage.ID_OBJECT_SELECTION_PANEL,
                    context.getObjectTypeClass(), modalWindow, context);

            return new ObjectSelectionPage(selectionPanel, context.getCallingPage());
        }
    }

}
