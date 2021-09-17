/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
