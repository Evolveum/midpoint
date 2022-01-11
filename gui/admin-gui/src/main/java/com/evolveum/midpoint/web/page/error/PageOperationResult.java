/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Page that displays just the operation result. Comes handy
 * for places where the operation result cannot be displayed
 * on the main page (e.g. object list warnings, projection list, etc.)
 *
 * @author semancik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/result", matchUrlForSecurity = "/result")
        }, permitAll = true)
public class PageOperationResult extends PageBase {

    private static final long serialVersionUID = 1L;

    private static final String ID_BACK = "back";

    private static final Trace LOGGER = TraceManager.getTrace(PageOperationResult.class);

    private OperationResult result = null;

    public PageOperationResult() {
        super();
        initLayout();
    }

    public PageOperationResult(OperationResult result) {
        super();
        this.result = result;
        initLayout();
    }

    private void initLayout() {
        if (result != null) {
            OpResult opresult = showResult(result);
            if (opresult != null) {
                opresult.setShowMoreAll(true);
            } else {
                warn(getString("PageOperationResult.noResultAvailable"));
            }
        }

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageError.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }
        };
        add(back);
    }

    private void backPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

}
