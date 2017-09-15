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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mserbak
 * @author lazyman
 */
public class PrismOptionButtonPanel extends Panel {

    public PrismOptionButtonPanel(String id, IModel<ObjectWrapper> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<ObjectWrapper> model) {
        AjaxCheckBox check = new AjaxCheckBox("check", new PropertyModel<Boolean>(model, "selected")) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                PrismOptionButtonPanel.this.checkBoxOnUpdate(target);
            }
        };
        check.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return model.getObject().isSelectable();
            }
        });
        check.setOutputMarkupId(true);
        add(check);

        initButtons(model);
    }

    private void initButtons(final IModel<ObjectWrapper> model) {
        AjaxLink showEmpty = new AjaxLink("showEmptyButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyOnClick(target);
            }

        };
        add(showEmpty);

        showEmpty.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !model.getObject().isReadonly();
            }
        });

        Image showEmptyImg = new Image("showEmptyImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                ObjectWrapper wrapper = model.getObject();
//                if (wrapper.isShowEmpty()) {
//                    return new PackageResourceReference(PrismObjectPanel.class,
//                            "ShowEmptyFalse.png");
//                }
                return new PackageResourceReference(PrismObjectPanel.class,
                        "ShowEmptyTrue.png");
            }
        });

        showEmptyImg.add(new AttributeAppender("title", new AbstractReadOnlyModel() {

			@Override
			public Object getObject() {
//				ObjectWrapper wrapper = model.getObject();
//                if (wrapper.isShowEmpty()) {
//                    return getString("prismOptionButtonPanel.hideEmpty");
//                }
                return getString("prismOptionButtonPanel.showEmpty");
			}
		}, ""));

        showEmpty.add(showEmptyImg);

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
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isMinimalized()) {
                    return new PackageResourceReference(PrismObjectPanel.class,
                            "Maximize.png");
                }

                return new PackageResourceReference(PrismObjectPanel.class,
                        "Minimize.png");
            }
        });
        minimizeImg.add(new AttributeAppender("title", new AbstractReadOnlyModel() {

			@Override
			public Object getObject() {
				ObjectWrapper wrapper = model.getObject();
                if (wrapper.isMinimalized()) {
                    return getString("prismOptionButtonPanel.maximize");
                }

                return getString("prismOptionButtonPanel.minimize");
			}
		}, ""));
        
        minimize.add(minimizeImg);
    }

    public void minimizeOnClick(AjaxRequestTarget target) {
    }

    public void showEmptyOnClick(AjaxRequestTarget target) {
    }

    public void checkBoxOnUpdate(AjaxRequestTarget target) {
    }
}
