/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ObjectViewDto<T extends ObjectType> implements Serializable {

    public static final String BAD_OID = "==BAD_OID==";

    public static final String F_OID = "oid";
    public static final String F_NAME = "name";
    public static final String F_XML = "xml";

    private String oid;
    private String name;
    private String xml;
    private PrismObject<T> object;       //todo why this?
    Class<T> type;

    public ObjectViewDto() {
        this.name = null;
        this.oid = null;
    }

    public ObjectViewDto(String oid){
        this.oid = oid;
    }

    public ObjectViewDto(String oid, String name){
        this.name = name;
        this.oid = oid;
    }

    public ObjectViewDto(String oid, String name, PrismObject<T> object, String xml) {
        this.name = name;
        this.oid = oid;
        this.object = object;
        this.xml = xml;
    }

    public PrismObject<T> getObject() {
        return object;
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid){
        this.oid = oid;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public Class<T> getType() {
        return type;
    }

    public void setType(Class<T> type) {
        this.type = type;
    }
}
