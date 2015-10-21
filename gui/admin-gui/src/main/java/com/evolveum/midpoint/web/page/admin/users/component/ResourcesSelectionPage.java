package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

/**
 * A page hosting the resources selection panel. Although these are a bit coupled, we decided
 * to work with them separately to make panel reusable.
 */
public class ResourcesSelectionPage extends PageDialog {

    public static final String ID_RESOURCES_SELECTION_PANEL = "resourcesSelectionPanel";

    public ResourcesSelectionPage(ResourcesSelectionPanel innerPanel, PageBase callingPage) {
        super(callingPage);
        innerPanel.setOutputMarkupId(true);
        add(innerPanel);
    }

    public static <T extends ObjectType> void prepareDialog(ModalWindow dialog, final ResourcesSelectionPanel.Context context,
                                                            final Component callingComponent, String titleResourceKey, final String... idsToRefresh) {
        dialog.setPageCreator(new ResourcesSelectionPage.PageCreator(context));
        dialog.setInitialWidth(1100);
        dialog.setInitialHeight(560);
        dialog.setTitle(PageTemplate.createStringResourceStatic(callingComponent, titleResourceKey));
        dialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            public void onClose(AjaxRequestTarget target) {
                // it is not possible to refresh components in the context of the calling panel -- this is the correct place to do it
                for (String idToRefresh : idsToRefresh) {
                    Component c = callingComponent.get(idToRefresh);
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
        dialog.setCookieName(ResourcesSelectionPanel.class.getSimpleName() + ((int) (Math.random() * 100)));
        dialog.setWidthUnit("px");
    }

    public static class PageCreator implements ModalWindow.PageCreator {

        private ResourcesSelectionPanel.Context context;

        public PageCreator(ResourcesSelectionPanel.Context context) {
            this.context = context;
        }

        @Override
        public Page createPage() {
            context.callingPageReference = context.getCallingPage().getPageReference();

            ResourcesSelectionPanel selectionPanel = new ResourcesSelectionPanel(
                    ResourcesSelectionPage.ID_RESOURCES_SELECTION_PANEL,
                    context);

            return new ResourcesSelectionPage(selectionPanel, context.getCallingPage());
        }
    }

}
