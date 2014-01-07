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

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class ImagePanel extends Panel {

    private static final String ID_IMAGE = "image";

    public ImagePanel(String id, IModel<String> model, IModel<String> titleModel) {
        super(id);

        Label image = new Label(ID_IMAGE);
        add(image);

        image.add(AttributeModifier.replace("class", model));
        if (titleModel != null) {
            image.add(AttributeModifier.replace("title", titleModel));
        }
    }
}
