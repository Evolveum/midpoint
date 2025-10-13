/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.request.Response;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MyNodeBorder extends Behavior {

    private static final long serialVersionUID = 1L;

    private boolean[] branches;

    public MyNodeBorder(boolean[] branches) {
        this.branches = branches;
    }

    @Override
    public void beforeRender(Component component) {
        Response response = component.getResponse();

        for (int i = 0; i < branches.length; i++) {
            if (i > 0) {
                response.write("<div class=\"ml-4\">");
            }
        }
    }

    @Override
    public void afterRender(Component component) {
        Response response = component.getResponse();

        for (int i = 0; i < branches.length; i++) {
            if (i > 0) {
                response.write("</div>");
            }
        }
    }
}
