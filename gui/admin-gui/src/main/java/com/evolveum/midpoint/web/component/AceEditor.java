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

package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AceEditor extends TextArea<String> {

    private IModel<Boolean> readonly = new Model(false);

    private boolean resizeToMaxHeight = true;

    private int minHeight = 200;
    private int height = minHeight;

    public AceEditor(String id, IModel<String> model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initEditor('").append(getMarkupId());
        sb.append("',").append(readonly.getObject());
        sb.append(",").append(resizeToMaxHeight);
        sb.append(",").append(height);
        sb.append(",").append(minHeight);
        sb.append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public boolean isResizeToMaxHeight() {
        return resizeToMaxHeight;
    }

    public void setResizeToMaxHeight(boolean resizeToMaxHeight) {
        this.resizeToMaxHeight = resizeToMaxHeight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setReadonly(boolean readonly) {
        this.readonly.setObject(readonly);
    }

    public void setReadonly(IModel<Boolean> readonly) {
        this.readonly = readonly;
    }

    public void refreshReadonly(AjaxRequestTarget target) {
        StringBuilder sb = new StringBuilder();
        sb.append("refreshReadonly('").append(getMarkupId()).append("',").append(readonly.getObject()).append(");");

        target.appendJavaScript(sb.toString());
    }
}
