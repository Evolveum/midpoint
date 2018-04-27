/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.box;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressbarPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author katkav
 * @author semancik
 */
public class InfoBoxPanel extends Panel{
	private static final long serialVersionUID = 1L;

	private static final String ID_INFO_BOX = "infoBox";
	private static final String ID_INFO_BOX_ICON = "infoBoxIcon";
	private static final String ID_IMAGE_ID = "imageId";
	private static final String ID_MESSAGE = "message";
	private static final String ID_NUMBER = "number";
	private static final String ID_PROGRESS = "progress";
	private static final String ID_PROGRESS_BAR = "progressBar";
	private static final String ID_DESCRIPTION = "description";

	public InfoBoxPanel(String id, IModel<InfoBoxType> model) {
		super(id, model);

		add(AttributeModifier.append("class", "dashboard-info-box"));

		initLayout(model, null);
	}

	public InfoBoxPanel(String id, IModel<InfoBoxType> model, Class<? extends IRequestablePage> linkPage) {
		super(id, model);
		add(AttributeModifier.append("class", "dashboard-info-box"));
		initLayout(model, linkPage);
	}

	private void initLayout(final IModel<InfoBoxType> model, final Class<? extends IRequestablePage> linkPage) {

		WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
		add(infoBox);
		infoBox.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.BOX_BACKGROUND_COLOR)));

		WebMarkupContainer infoBoxIcon = new WebMarkupContainer(ID_INFO_BOX_ICON);
		infoBox.add(infoBoxIcon);
		infoBoxIcon.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.ICON_BACKGROUND_COLOR)));


        WebMarkupContainer image = new WebMarkupContainer(ID_IMAGE_ID);
        image.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.IMAGE_ID)));
        infoBoxIcon.add(image);

        Label message = new Label(ID_MESSAGE, new PropertyModel<String>(model, InfoBoxType.MESSAGE));
        infoBox.add(message);

        Label number = new Label(ID_NUMBER, new PropertyModel<String>(model, InfoBoxType.NUMBER));
        infoBox.add(number);

        WebMarkupContainer progress = new WebMarkupContainer(ID_PROGRESS);
        infoBox.add(progress);
        progress.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return model.getObject().getProgress() != null;
			}
        });
        ProgressbarPanel progressBar = new ProgressbarPanel(ID_PROGRESS_BAR, new PropertyModel<>(model, InfoBoxType.PROGRESS));
        progress.add(progressBar);

        Label description = new Label(ID_DESCRIPTION, new PropertyModel<String>(model, InfoBoxType.DESCRIPTION));
        infoBox.add(description);

        if (linkPage != null) {
	        add(new AjaxEventBehavior("click") {
				private static final long serialVersionUID = 1L;

				@Override
				protected void onEvent(AjaxRequestTarget target) {
					setResponsePage(linkPage);
				}
			});
        }
	}


}
