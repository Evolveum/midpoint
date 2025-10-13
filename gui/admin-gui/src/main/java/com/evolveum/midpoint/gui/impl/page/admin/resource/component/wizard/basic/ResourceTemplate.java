/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class ResourceTemplate implements Serializable {

    private final String oid;

    private final TemplateType templateType;

    public ResourceTemplate(String oid, TemplateType templateType) {
        this.oid = oid;
        this.templateType = templateType;
    }

    public String getOid() {
        return oid;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public enum TemplateType implements TileEnum {
        INHERIT_TEMPLATE(ResourceType.class, "fa fa-code-branch fa-rotate-180"),
        CONNECTOR(ConnectorType.class, "fa-solid fa-pencil"),
        COPY_FROM_TEMPLATE(ResourceType.class, "fa-regular fa-copy");

        private final Class<? extends AssignmentHolderType> type;
        private final String icon;

        TemplateType(Class<? extends AssignmentHolderType> type, String icon) {
            this.type = type;
            this.icon = icon;
        }

        public Class<? extends AssignmentHolderType> getType() {
            return type;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }
}
