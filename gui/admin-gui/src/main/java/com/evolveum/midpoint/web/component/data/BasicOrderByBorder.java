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

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByLink;

/**
 * @author lazyman
 */
public abstract class BasicOrderByBorder extends OrderByBorder {

    protected BasicOrderByBorder(String id, String property, ISortStateLocator stateLocator) {
        super(id, property, stateLocator, BasicCssProvider.getInstance());
    }

    @Override
    protected OrderByLink newOrderByLink(String id, String property, ISortStateLocator stateLocator) {
        return new OrderByLink(id, property, stateLocator, OrderByLink.VoidCssProvider.getInstance()) {

            @Override
            protected void onSortChanged() {
                BasicOrderByBorder.this.onSortChanged();
            }
        };
    }

    public static class BasicCssProvider extends OrderByLink.CssProvider {

        private static BasicCssProvider instance = new BasicCssProvider();

        private BasicCssProvider() {
            super("sortable asc", "sortable desc", "sortable");
        }

        public static BasicCssProvider getInstance() {
            return instance;
        }
    }
}
