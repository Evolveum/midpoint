package com.evolveum.midpoint.web.component.box;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class InfoBoxPanel extends Panel{
	
	
	private static final long serialVersionUID = 1L;
	private static final String BACKGROUND_COLOR = "backgroundColor";
	private static final String IMAGE_ID = "imageId";
	private static final String DESCRIPTION = "description";
	

	public InfoBoxPanel(String id, IModel<InfoBoxType> model) {
		super(id, model);
		initLayout(model);
	}
	
	private void initLayout(IModel<InfoBoxType> model){
		
		
        WebMarkupContainer background = new WebMarkupContainer(BACKGROUND_COLOR);
        background.add(AttributeModifier.append("class", new PropertyModel<String>(model, BACKGROUND_COLOR)));
        add(background);
        
        
        WebMarkupContainer image = new WebMarkupContainer(IMAGE_ID);
        image.add(AttributeModifier.append("class", new PropertyModel<String>(model, IMAGE_ID)));
        background.add(image);
        
        ListView<String> description = new ListView<String>(DESCRIPTION, new PropertyModel<List<String>>(model, DESCRIPTION)) {
        	
        	@Override
        	protected void populateItem(ListItem<String> item) {
        		Label l = new Label("desc", item.getModel());
        		l.setOutputMarkupId(true);
        		item.add(l);
        	}
		};
		description.setOutputMarkupId(true);
       
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
