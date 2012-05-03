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

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
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
        //todo wtf?
//        showEmptyImg.add(new AttributeAppender("title", ""));
//        if(model.getObject().isShowEmpty()){
//        	showEmptyImg.add(new AttributeModifier("title", getString("prismOptionButtonPanel.hideEmpty")));
//        } else {
//        	showEmptyImg.add(new AttributeModifier("title", getString("prismOptionButtonPanel.showEmpty")));
//        }

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
        //todo wtf?
//        minimizeImg.add(new AttributeAppender("title", ""));
//        if(model.getObject().isMinimalized()){
//        	minimizeImg.add(new AttributeModifier("title", getString("prismOptionButtonPanel.maximize")));
//        } else {
//        	minimizeImg.add(new AttributeModifier("title", getString("prismOptionButtonPanel.minimize")));
//        }
        minimize.add(minimizeImg);
    }

    public void minimizeOnClick(AjaxRequestTarget target) {
    }

    public void showEmptyOnClick(AjaxRequestTarget target) {
    }

    public void checkBoxOnUpdate(AjaxRequestTarget target) {
    }
}
