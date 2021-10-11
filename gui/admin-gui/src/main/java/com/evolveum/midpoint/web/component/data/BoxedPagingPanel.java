/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import org.apache.wicket.markup.html.navigation.paging.IPageable;

/**
 * @author Viliam Repan (lazyman)
 */
public class BoxedPagingPanel extends NavigatorPanel {

    public BoxedPagingPanel(String id, IPageable pageable, boolean showPageListing) {
        super(id, pageable, showPageListing);
    }
}
