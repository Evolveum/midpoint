/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;

public class DetailsFragment extends Fragment {

    public DetailsFragment(String id, String markupId, MarkupContainer markupProvider) {
        super(id, markupId, markupProvider);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initFragmentLayout();
    }

    protected void initFragmentLayout() {

    }
}
