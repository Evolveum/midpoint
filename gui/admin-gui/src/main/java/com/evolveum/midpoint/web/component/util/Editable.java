/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
