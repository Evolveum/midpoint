/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
