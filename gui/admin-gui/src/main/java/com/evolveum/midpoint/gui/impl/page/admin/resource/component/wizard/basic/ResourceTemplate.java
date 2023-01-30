/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class ResourceTemplate implements Serializable {

    private final String oid;
    private final QName type;

    public ResourceTemplate(String oid, QName type) {
        this.oid = oid;
        this.type = type;
    }

    public QName getType() {
        return type;
    }

    public String getOid() {
        return oid;
    }
}
