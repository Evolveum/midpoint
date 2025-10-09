/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author skublik
 */

public class LayerIcon implements Serializable {
    private static final long serialVersionUID = 1L;

    private final IconType iconType;
    private final IModel<String> labelModel;

    public LayerIcon(IconType iconType) {
        this(iconType, null);
    }

    public LayerIcon(IconType iconType, IModel<String> labelModel) {
        this.iconType = iconType;
        this.labelModel = labelModel;
    }

    public IconType getIconType() {
        return iconType;
    }

    public IModel<String> getLabelModel() {
        return labelModel;
    }

    public boolean hasLabel() {
        return labelModel != null && StringUtils.isNotBlank(labelModel.getObject());
    }
}
