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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mserbak
 * @author lazyman
 */
public class SceneButtonPanel extends Panel {

    public SceneButtonPanel(String id, IModel<DataSceneDto> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<DataSceneDto> model) {
        AjaxLink minimize = new AjaxLink("minimizeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                minimizeOnClick(target);
            }
        };
        add(minimize);

        Image minimizeImg = new Image("minimizeImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                DataSceneDto dto = model.getObject();
                if (dto.isMinimized()) {
                    return new PackageResourceReference(PrismObjectPanel.class, "Maximize.png");
                }
                return new PackageResourceReference(PrismObjectPanel.class, "Minimize.png");
            }
        });
        minimizeImg.add(new AttributeAppender("title", new AbstractReadOnlyModel() {

			@Override
			public Object getObject() {
				DataSceneDto dto = model.getObject();
                if (dto.isMinimized()) {
                    return getString("prismOptionButtonPanel.maximize");
                }
                return getString("prismOptionButtonPanel.minimize");
			}
		}, ""));
        minimize.add(minimizeImg);
    }

    public void minimizeOnClick(AjaxRequestTarget target) {
    }
}
