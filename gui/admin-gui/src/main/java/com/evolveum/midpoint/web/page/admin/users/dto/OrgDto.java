package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public interface OrgDto {

    String getOid();

    String getName();

    QName getRelation();

    Class<? extends ObjectType> getType();
}
