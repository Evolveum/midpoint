package com.evolveum.midpoint.web.page.admin.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

public class TestPage extends PageAdmin {

	@Override
	public List<BottomMenuItem> getBottomMenuItems() {
		return new ArrayList<BottomMenuItem>();
	}

	public TestPage() {
		initLayout();
	}

	private void initLayout() {
		final ModalWindow popupWindow;
		add(popupWindow = new ModalWindow("popupWindow"));

		popupWindow.setContent(new ResourcePopupWindow(popupWindow.getContentId(), popupWindow));
		popupWindow.setResizable(false);
		popupWindow.setTitle("Select resource");
		popupWindow.setCookieName("Resrource popup window");

		popupWindow.setInitialWidth(1100);
		popupWindow.setWidthUnit("px");

		popupWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

			@Override
			public boolean onCloseButtonClicked(AjaxRequestTarget target) {
				return true;
			}
		});

		popupWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

			@Override
			public void onClose(AjaxRequestTarget target) {
				popupWindow.close(target);
			}
		});

		add(new AjaxLinkButton("popup", new Model<String>("Popup")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				popupWindow.show(target);
			}
		});
		
		add(new AjaxLinkButton("save", new Model<String>("Save")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				//
			}
		});
		
		add(new AjaxLinkButton("cancel", new Model<String>("Cancel")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				//
			}
		});

        Accordion accordion = new Accordion("accordion");
        accordion.setOpenedPanel(0);
        add(accordion);
        
        
        AccordionItem accounts = new AccordionItem("accounts", new Model<String>("Accounts"));
        accordion.getBodyContainer().add(accounts);
        
        AccordionItem assignments = new AccordionItem("assignments", new Model<String>("Assignments"));
        accordion.getBodyContainer().add(assignments);
        
        AccordionItem resources = new AccordionItem("resources", new Model<String>("Resources"));
        accordion.getBodyContainer().add(resources);
        
        Image showEmpty = new Image("icon1", new PackageResourceReference(TestPage.class,
        		"ShowEmptyTrue.png"));
        accounts.add(showEmpty);
        
        Image maximize = new Image("icon2", new PackageResourceReference(TestPage.class,"Maximize.png"));
        accounts.add(maximize);

	}
	
	@Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(PrismObjectPanel.class, "PrismObjectPanel.css"));
    }
}
