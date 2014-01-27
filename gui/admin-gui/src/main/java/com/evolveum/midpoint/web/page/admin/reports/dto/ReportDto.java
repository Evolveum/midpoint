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

package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ReportDto implements Serializable {

    public static enum Type {
        USERS("User"), RECONCILIATION("Reconciliation"), AUDIT("Audit");

        private final String name;

        private Type(final String s){name = s;}
        public String toString(){return name;}
    }

    public static final String F_OID = "oid";
    public static final String F_XML = "xml";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_EXPORT_TYPE = "exportType";

    private String oid;
    private String xml;
    private Type type;
    private String name;
    private String description;
    private ExportType exportType;
    private PrismObject<ReportType> object;

    public ReportDto(){}

    public ReportDto(Type type, String name, String description) {
        this.type = type;
        this.description = description;
        this.name = name;
    }

    public ReportDto(String name, String description, String xml, ExportType export){
        this.name = name;
        this.description = description;
        this.xml = xml;
        this.exportType = export;
    }

    public PrismObject<ReportType> getObject() {
        return object;
    }

    public void setObject(PrismObject<ReportType> object) {
        this.object = object;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }
}
