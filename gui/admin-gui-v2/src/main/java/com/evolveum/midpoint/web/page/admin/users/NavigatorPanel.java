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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class NavigatorPanel extends PagingNavigator {

    private static final Trace LOGGER = TraceManager.getTrace(NavigatorPanel.class);

    public NavigatorPanel(String id, IPageable pageable) {
        super(id, pageable);

        int from = 0;
        int to = 0;
        int count = 0;

        if (pageable instanceof DataViewBase) {
            DataViewBase view = (DataViewBase) pageable;

            from = view.getFirstItemOffset() + 1;
            to = from + view.getItemsPerPage() - 1;
            count = view.getItemCount();
        } else {
            LOGGER.warn("Navigator panel, missing implementation... TODO");
        }

        add(new Label("label", createModel(from, to, count)));

        //todo if count == 0 hide << >> stuff
    }

    private StringResourceModel createModel(int from, int to, int count) {
        if (count > 0) {
            return new StringResourceModel("navigatorPanel.label", this, null,
                    new Object[]{from, to, count});
        }

        return new StringResourceModel("navigatorPanel.noFound", this, null);
    }
}
