package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;

public class DropdownButtonPanel extends BasePanel<DropdownButtonDto>{

	private static final String ID_BUTTON = "button";
	private static final String ID_INFO = "info";
	private static final String ID_ICON = "icon";
	private static final String ID_LABEL = "label";
	private static final String ID_MENU = "menu";
	
	private static String ID_MENU_ITEM = "menuItem";
    private static String ID_MENU_ITEM_BODY = "menuItemBody";

	
	public DropdownButtonPanel(String id, DropdownButtonDto model) {
		super(id);
		initLayout(model);
	}
	
	private void initLayout(DropdownButtonDto model){
		
//		AjaxButton button = new AjaxButton(ID_BUTTON) {
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				ApplicationButtonPanel.this.onClick(target);
//			}
//		};
//		add(button);
		
		Label info = new Label(ID_INFO, model.getInfo());
		add(info);
//		button.add(info);
		
		Label label = new Label(ID_LABEL, model.getLabel());
		add(label);
//		button.add(label);
		
		WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
		icon.add(AttributeModifier.append("class", model.getIcon()));
		add(icon);
//		button.add(icon);
		
		
		 ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_MENU_ITEM, new Model((Serializable) model.getMenuItems())) {

	            @Override
	            protected void populateItem(ListItem<InlineMenuItem> item) {
	                initMenuItem(item);
	            }
	        };
	        
	        add(li);
		
//		InlineMenu assignmentMenu = new InlineMenu(ID_MENU, new Model((Serializable) model.getMenuItems()));
//		add(assignmentMenu);
	}
	
	 private void initMenuItem(ListItem<InlineMenuItem> menuItem) {
	        final InlineMenuItem item = menuItem.getModelObject();

//	        menuItem.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
//
//	            @Override
//	            public String getObject() {
//	                if (item.isMenuHeader()) {
//	                    return "dropdown-header";
//	                } else if (item.isDivider()) {
//	                    return "divider";
//	                }
//
//	                return getBoolean(item.getEnabled(), true) ? null : "disabled";
//	            }
//	        }));

//	        if (item.getEnabled() != null || item.getVisible() != null) {
//	            menuItem.add(new VisibleEnableBehaviour() {
//
//	                @Override
//	                public boolean isEnabled() {
//	                    return getBoolean(item.getEnabled(), true);
//	                }
//
//	                @Override
//	                public boolean isVisible() {
//	                    return getBoolean(item.getVisible(), true);
//	                }
//	            });
//	        }

	        WebMarkupContainer menuItemBody;
//	        if (item.isMenuHeader() || item.isDivider()) {
//	            menuItemBody = new MenuDividerPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
//	        } else {
	            menuItemBody = new MenuLinkPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
//	        }
	        menuItemBody.setRenderBodyOnly(true);
	        menuItem.add(menuItemBody);
	    }
	

}
