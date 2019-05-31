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

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

/**
 * @author skublik
 */
public class CompositedIconPanel extends Panel {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_LAYERED_ICON = "layeredIcon";
    private static final String ID_BASIC_ICON = "basicIcon";
    private static final String ID_LAYER_ICONS = "layerIcons";

    private CompositedIcon compositedIcon;

    public CompositedIconPanel(String id, CompositedIcon compositedIcon){
        super(id);
        this.compositedIcon = compositedIcon;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
    	WebMarkupContainer layeredIcon = new WebMarkupContainer(ID_LAYERED_ICON);
    	if(org.apache.commons.lang3.StringUtils.isNotBlank(compositedIcon.getTitle())) {
    		layeredIcon.add(AttributeAppender.append("title", compositedIcon.getTitle()));
    	}
    	add(layeredIcon);
    	WebComponent basicIcon = new WebComponent(ID_BASIC_ICON);
    	if(compositedIcon.hasBasicIcon()) {
    		basicIcon.add(AttributeAppender.append("class", compositedIcon.getBasicIcon()));
        	if (compositedIcon.hasBasicIconHtmlColor()){
        		basicIcon.add(AttributeAppender.append("style", compositedIcon.getBasicIconHtmlColor()));
            }
    	}
    	layeredIcon.add(basicIcon);
    	
    	
    	RepeatingView listItems = new RepeatingView(ID_LAYER_ICONS);
    	for(IconType layerIcon : compositedIcon.getLayerIcons()) {
    		if (layerIcon == null){
    	        continue;
            }
    		if (StringUtils.isNotEmpty(layerIcon.getCssClass())) {
    			WebComponent icon = new WebComponent(listItems.newChildId());
    			icon.add(AttributeAppender.append("class", layerIcon.getCssClass()));
                if (StringUtils.isNotEmpty(layerIcon.getColor())) {
                	icon.add(AttributeAppender.append("style", layerIcon.getColor()));
                }
                listItems.add(icon);
            }
    	}
    	layeredIcon.add(listItems);
    	
//    	ListView<IconType> layerIcons = new ListView<IconType>(ID_LAYER_ICONS, new IModel<List<IconType>>(){
//
//				@Override
//				public List<IconType> getObject() {
//					return compositedIcon.getLayerIcons();
//				}
//    		
//			}) 
//    	{
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void populateItem(ListItem<IconType> item) {
//				if (item.getModelObject() == null){
//        	        return;
//                }
//        		if (StringUtils.isNotEmpty(item.getModelObject().getCssClass())) {
//        			add(AttributeAppender.append("class", item.getModelObject().getCssClass()));
//                    if (StringUtils.isNotEmpty(item.getModelObject().getColor())) {
//                    	add(AttributeAppender.append("style", item.getModelObject().getColor()));
//                    }
//                }
//			}
//		};
    }
}
