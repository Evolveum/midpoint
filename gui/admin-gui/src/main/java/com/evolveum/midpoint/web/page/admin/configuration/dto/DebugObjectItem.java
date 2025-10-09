/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
public class DebugObjectItem extends Selectable implements SelectableRow {

    public static final String F_OID = "oid";
    public static final String F_NAME = "name";

    public static final String F_RESOURCE_NAME = "resourceName";
    public static final String F_RESOURCE_TYPE = "resourceType";
    public static final String F_FULL_NAME = "fullName";
    public static final String F_DESCRIPTION = "description";
    public static final String F_STATUS = "status";

    private String oid;
    private String name;
    private String description;
    //todo create subclasses
    private String resourceName;
    private String resourceType;
    private OperationResultStatusType status;            // TODO store full operation result here

    private Class<? extends ObjectType> type;
    private String fullName;

    public DebugObjectItem(String oid, String name, String description, Class<? extends ObjectType> type) {
        this.name = name;
        this.oid = oid;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public static DebugObjectItem createDebugObjectItem(PrismObject<? extends ObjectType> object) {
        DebugObjectItem item = new DebugObjectItem(object.getOid(), WebComponentUtil.getName(object),
                object.getPropertyRealValue(ObjectType.F_DESCRIPTION, String.class), object.getCompileTimeClass());

        if (object.asObjectable().getFetchResult() != null) {
            item.setStatus(object.asObjectable().getFetchResult().getStatus());
        }

        if (UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
            PolyString fullName = WebComponentUtil.getValue(object, UserType.F_FULL_NAME, PolyString.class);
            item.setFullName((fullName != null ? fullName.getOrig() : null));
        }

        return item;
    }

    public OperationResultStatusType getStatus() {
        return status;
    }

    public void setStatus(OperationResultStatusType status) {
        this.status = status;
    }

}
