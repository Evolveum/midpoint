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

package com.evolveum.midpoint.web.component.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.request.Response;

/**
 *  @author shood
 * */
public class Form<T> extends org.apache.wicket.markup.html.form.Form<T>{

    private static final String ID_CONTAINER = "fake";

    public Form(String id){
        this(id, false);
    }

    /**
     *  Use this constructor when a form needs to display empty input field:
     *  &lt;input style="display:none"&gt;
     *  &lt;input type="password" style="display:none"&gt;
     *
     *  To overcome Chrome auto-completion of password and other form fields
     * */
    public Form(String id, boolean addFakeInputFields){
        super(id);
    }

    @Override
    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        super.onComponentTagBody(markupStream, openTag);

        final Response response = getResponse();

        response.write("<input style=\"display:none\">\n" +
                "<input type=\"password\" style=\"display:none\">");
    }
}
