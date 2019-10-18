/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.form;

import com.evolveum.midpoint.web.security.SecurityUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.request.Response;


/**
 * @author Viliam Repan (lazyman)
 * @author shood
 * @author Radovan Semancik
 */
public class Form<T> extends org.apache.wicket.markup.html.form.Form<T> {

    private boolean addFakeInputFields = false;

    public Form(String id) {
        super(id);
    }

    /**
     * Use this constructor when a form needs to display empty input field:
     * &lt;input style="display:none"&gt;
     * &lt;input type="password" style="display:none"&gt;
     * <p>
     * To overcome Chrome auto-completion of password and other form fields
     */
    public Form(String id, boolean addFakeInputFields) {
        super(id);
        this.addFakeInputFields = addFakeInputFields;
    }

    @Override
    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        super.onComponentTagBody(markupStream, openTag);

        Response resp = getResponse();

        // add hidden input for CSRF
        SecurityUtils.appendHiddenInputForCsrf(resp);

        if (addFakeInputFields) {
            resp.write("<input style=\"display:none\">");
        }
    }
}
