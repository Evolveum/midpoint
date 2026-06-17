/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

import org.apache.wicket.model.IModel;

/**
 * Object, which can be described by its title and description.
 */
public interface Describable extends Serializable {

    /**
     * Title of the object.
     */
    IModel<String> title();

    /**
     * Description of the object.
     */
    IModel<String> description();
}
