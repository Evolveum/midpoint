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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.page.PageBase;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class SimplePanel<T> extends BaseSimplePanel<T> {

    public SimplePanel(String id) {
        this(id, null);
    }

    public SimplePanel(String id, IModel<T> model) {
        super(id, model);
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

//    public PrismContext getPrismContext(){
//    	return getPageBase().getPrismContext();
//    }
    
//    public WebMarkupContainer getFeedbackPanel(){
//    	return getPageBase().getFeedbackPanel();
//    }
    /*
    *   TODO - this is the exact copy method as the one on PageBase. Refactor if possible.
    * */
    protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
        final ModalWindow modal = new ModalWindow(id);
        add(modal);

        modal.setResizable(false);
        modal.setTitle(title);
        modal.setCookieName(PageBase.class.getSimpleName() + ((int) (Math.random() * 100)));

        modal.setInitialWidth(width);
        modal.setWidthUnit("px");
        modal.setInitialHeight(height);
        modal.setHeightUnit("px");

        modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                modal.close(target);
            }
        });

        modal.add(new AbstractDefaultAjaxBehavior() {

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                response.render(OnDomReadyHeaderItem.forScript("Wicket.Window.unloadConfirmation = false;"));
                response.render(JavaScriptHeaderItem.forScript("$(document).ready(function() {\n" +
                        "  $(document).bind('keyup', function(evt) {\n" +
                        "    if (evt.keyCode == 27) {\n" +
                        getCallbackScript() + "\n" +
                        "        evt.preventDefault();\n" +
                        "    }\n" +
                        "  });\n" +
                        "});", id));
            }

            @Override
            protected void respond(AjaxRequestTarget target) {
                modal.close(target);

            }
        });

        return modal;
    }
}
