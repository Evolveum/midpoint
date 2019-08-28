package com.evolveum.midpoint.web.page.admin.orgs;

import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import org.apache.wicket.markup.html.navigation.paging.IPageable;

public class OrgTreePagingPanel extends NavigatorPanel {
    public OrgTreePagingPanel(String id, IPageable pageable, boolean showPageListing) {
        super(id, pageable, showPageListing);
    }
}
