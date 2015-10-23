package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;
import com.evolveum.midpoint.web.page.PageTemplate;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public class AssignableSelectionPage extends PageDialog {

    public static final String ID_ASSIGNABLE_SELECTION_PANEL = "assignableSelectionPanel";

    public AssignableSelectionPage(AssignableSelectionPanel innerPanel, PageBase callingPage) {
        super(callingPage);
        innerPanel.setOutputMarkupId(true);
        add(innerPanel);
    }

    public static void prepareDialog(ModalWindow dialog, final AssignableSelectionPanel.Context context,
                                     final Component callingComponent, String titleResourceKey,
                                     final String... idsToRefresh) {
        dialog.setPageCreator(new AssignableSelectionPage.PageCreator(context));
        dialog.setInitialWidth(1100);
        dialog.setInitialHeight(560);
        dialog.setTitle(PageTemplate.createStringResourceStatic(callingComponent, titleResourceKey));
        dialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            public void onClose(AjaxRequestTarget target) {
                for (String idToRefresh : idsToRefresh) {
                    Component c = /* context.getRealParent(). */ callingComponent.get(idToRefresh);
                    if (c != null) {
                        target.add(c);
                    }
                }
                // we add feedback in any case
                Component feedback = callingComponent.getPage().get(PageTemplate.ID_FEEDBACK_CONTAINER);
                if (feedback != null) {     // just to be sure
                    target.add(feedback);
                }
            }
        });
        dialog.showUnloadConfirmation(false);
        dialog.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        dialog.setCookieName(AssignableSelectionPanel.class.getSimpleName() + ((int) (Math.random() * 100)));
        dialog.setWidthUnit("px");
    }

    public static class PageCreator<T extends AbstractAssignableSelectionPanel> implements ModalWindow.PageCreator {

        private AssignableSelectionPanel.Context context;

        public PageCreator(AssignableSelectionPanel.Context context) {
            this.context = context;
        }

        @Override
        public Page createPage() {
            context.callingPageReference = context.getCallingPage().getPageReference();

            AssignableSelectionPanel selectionPanel = new AssignableSelectionPanel(AssignableSelectionPage.ID_ASSIGNABLE_SELECTION_PANEL, context);

            Page page = new AssignableSelectionPage(selectionPanel, context.getCallingPage());
            context.setModalWindowPageReference(page.getPageReference());

            selectionPanel.setType(context.getDefaultType());
            selectionPanel.setSearchParameter(context.getDefaultSearchParameter());

            return page;
        }
    }

}
