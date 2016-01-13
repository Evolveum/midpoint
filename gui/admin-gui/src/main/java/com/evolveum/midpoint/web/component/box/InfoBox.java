package com.evolveum.midpoint.web.component.box;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

public class InfoBox extends Panel{
	
	
	private static final long serialVersionUID = 1L;
	private static final String BACKGROUND_COLOR = "backgroundColor";
	private static final String IMAGE_ID = "imageId";
	private static final String DESCRIPTION = "description";

	public InfoBox(String id, String background, String image, List<String> values) {
		super(id);

		initLayout(background, image, values);
	}
	
	private void initLayout(String backgroundColor, String imageId, List<String> values){
		
		
        WebMarkupContainer background = new WebMarkupContainer(BACKGROUND_COLOR);
        background.add(new AttributeModifier("class", "info-box-icon " + backgroundColor));
        add(background);
        
        
        WebMarkupContainer image = new WebMarkupContainer(IMAGE_ID);
        image.add(new AttributeModifier("class", "fa " + imageId));
        background.add(image);
        
        RepeatingView description = new RepeatingView(DESCRIPTION);
        
        for (String value : values){
        	description.add(new Label(description.newChildId(), value));
        }
       
        add(description);
		
	}

	/**
	 * 
	 * <div class="col-md-3 col-sm-6 col-xs-12">
		<div class="info-box">
			<span class="info-box-icon bg-aqua" wicket:id="backgroundColor">
				<i class="fa fa-star-o" wicket:id="imageId"/>
			</span>

			<div class="info-box-content">
				<span class="info-box-text" wicket:id="description" /> 
			</div>

		</div>

	</div>
	 * 
	 */
	
	
}
