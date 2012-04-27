/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import java.awt.LayoutManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mserbak
 */
public class PrismOptionButtonPanel extends Panel {
	Boolean showCheckBox = false;

	public PrismOptionButtonPanel(String id, final IModel<ObjectWrapper> model, Boolean showCheckBox) {
		super(id);
		this.showCheckBox = showCheckBox;
		initButtons(model);
	}
	
	private void initButtons(final IModel<ObjectWrapper> model){
		WebMarkupContainer headerPanel = new WebMarkupContainer("header");
		AjaxLink showEmpty = new AjaxLink("showEmptyButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setShowEmpty(!wrapper.isShowEmpty());
                //TODO: add link
                target.add(PrismObjectPanel.class);
            }
        };
        headerPanel.add(showEmpty);
        
        Image showEmptyImg = new Image("showEmptyImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isShowEmpty()) {
                    return new PackageResourceReference(PrismObjectPanel.class,
                            "ShowEmptyFalse.png");
                }

                return new PackageResourceReference(PrismObjectPanel.class,
                        "ShowEmptyTrue.png");
            }
        });
        showEmpty.add(showEmptyImg);

        AjaxLink minimize = new AjaxLink("minimizeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setMinimalized(!wrapper.isMinimalized());
                //TODO: add link
                target.add(PrismObjectPanel.this);
            }
        };
        headerPanel.add(minimize);

        Image minimizeImg = new Image("minimizeImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isMinimalized()) {
                    return new PackageResourceReference(PrismObjectPanel.class,
                            "Maximize.png");
                }

                return new PackageResourceReference(PrismObjectPanel.class,
                        "Minimize.png");
            }
        });
        minimize.add(minimizeImg);
	}
}
