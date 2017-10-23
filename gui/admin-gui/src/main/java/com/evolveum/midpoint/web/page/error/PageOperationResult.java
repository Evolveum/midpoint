/*
 * Copyright (c) 2016 Evolveum
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

package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Page that displays just the operation result. Comes handy
 * for places where the operation result cannot be displayed
 * on the main page (e.g. object list warnings, projection list, etc.)
 *
 * @author semancik
 */
@PageDescriptor(url = "/result")
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
