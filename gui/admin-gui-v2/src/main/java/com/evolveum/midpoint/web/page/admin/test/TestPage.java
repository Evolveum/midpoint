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
		final ModalWindow resourceWindow = createResourceWindow();
		final ModalWindow accountWindow = createAccountWindow();
		
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
        
        Image showEmpty2 = new Image("icon5", new PackageResourceReference(TestPage.class,
        		"ShowEmptyTrue.png"));
        accounts.add(showEmpty2);
        
        Image maximize = new Image("icon2", new PackageResourceReference(TestPage.class,"Maximize.png"));
        accounts.add(maximize);
        
        Image delete = new Image("icon3", new PackageResourceReference(TestPage.class,
        		"Delete.png"));
        accounts.add(delete);
        
        Image disableEnable = new Image("icon4", new PackageResourceReference(TestPage.class,"Link.png"));
        accounts.add(disableEnable);
        
        resources.add(new AjaxLinkButton("addResource", new Model<String>("Add resource")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				resourceWindow.show(target);
			}
		});
		
		accounts.add(new AjaxLinkButton("addAccount", new Model<String>("Add account")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				accountWindow.show(target);
			}
		});

	}
	
	private ModalWindow createResourceWindow(){
		final ModalWindow popupWindow;
		add(popupWindow = new ModalWindow("resourceWindow"));

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
		
		return popupWindow;
	}
	
	private ModalWindow createAccountWindow(){
		final ModalWindow popupWindow;
		add(popupWindow = new ModalWindow("accountWindow"));

		popupWindow.setContent(new AccountPopupWindow(popupWindow.getContentId(), popupWindow));
		popupWindow.setResizable(false);
		popupWindow.setTitle("Select Account");
		popupWindow.setCookieName("Account popup window");

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
		
		return popupWindow;
	}
	
	@Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(PrismObjectPanel.class, "PrismObjectPanel.css"));
    }
}
