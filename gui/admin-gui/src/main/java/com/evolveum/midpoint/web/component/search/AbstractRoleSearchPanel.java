/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

/**
 * @author honchar
 */
public class AbstractRoleSearchPanel<ART extends AbstractRoleType> extends TypeSearchPanel<ART> {
    private static final long serialVersionUID = 1L;

    public AbstractRoleSearchPanel(String id, IModel<Search<ART>> model) {
        super(id, model);
    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {

    }
}
