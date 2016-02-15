/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public class AssignableOrgSelectionPage extends PageDialog {

    public static final String ID_ASSIGNABLE_ORG_SELECTION_PANEL = "assignableOrgSelectionPanel";

    public AssignableOrgSelectionPage(AssignableOrgSelectionPanel innerPanel, PageBase callingPage) {
        super(callingPage);
        innerPanel.setOutputMarkupId(true);
        add(innerPanel);
    }

    public static void prepareDialog(ModalWindow dialog, final AbstractAssignableSelectionPanel.Context context,
                                     final Component callingComponent, String titleResourceKey,
                                     final String... idsToRefresh) {
        dialog.setPageCreator(new AssignableOrgSelectionPage.PageCreator(context));
        dialog.setInitialWidth(1100);
        dialog.setInitialHeight(560);
        dialog.setTitle(PageBase.createStringResourceStatic(callingComponent, titleResourceKey));
        dialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            public void onClose(AjaxRequestTarget target) {
                for (String idToRefresh : idsToRefresh) {
                    Component c = /* context.getRealParent(). */ callingComponent.get(idToRefresh);
                    if (c != null) {
                        target.add(c);
                    }
                }
                // we add feedback in any case
                Component feedback = callingComponent.getPage().get(PageBase.ID_FEEDBACK_CONTAINER);
                if (feedback != null) {     // just to be sure
                    target.add(feedback);
                }
            }
        });
        dialog.showUnloadConfirmation(false);
        dialog.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        dialog.setCookieName(AssignableOrgSelectionPanel.class.getSimpleName() + ((int) (Math.random() * 100)));
        dialog.setWidthUnit("px");
    }

    public static class PageCreator<T extends AbstractAssignableSelectionPanel> implements ModalWindow.PageCreator {

        private AbstractAssignableSelectionPanel.Context context;

        public PageCreator(AbstractAssignableSelectionPanel.Context context) {
            this.context = context;
        }

        @Override
        public Page createPage() {
            context.callingPageReference = context.getCallingPage().getPageReference();

            AssignableOrgSelectionPanel selectionPanel = new AssignableOrgSelectionPanel(AssignableOrgSelectionPage.ID_ASSIGNABLE_ORG_SELECTION_PANEL,
                    context);

            Page page = new AssignableOrgSelectionPage(selectionPanel, context.getCallingPage());
            context.setModalWindowPageReference(page.getPageReference());

            selectionPanel.setType(context.getDefaultType());
            return page;
        }
    }

}
