/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface Editable extends Serializable {

    boolean isEditing();

    void setEditing(boolean editing);
}
