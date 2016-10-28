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
package com.evolveum.midpoint.web.page.admin.roles.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import javax.xml.datatype.XMLGregorianCalendar;

import java.io.Serializable;
import java.util.List;

/**
 *  @author shood
 *
 *  Deprecated for now. May find usage in future
 * */

@Deprecated
public class RoleDto implements Serializable{

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_TYPE = "type";
    public static final String F_REQUESTABLE = "requestable";
    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_ADMIN_STATUS = "adminStatus";
    public static final String F_INDUCEMENTS = "inducements";
    public static final String F_ASSIGNMENTS = "assignments";

    private PrismObject<RoleType> role;
    private String name;
    private String description;
    private String type;
    private Boolean requestable;
    private XMLGregorianCalendar from;
    private XMLGregorianCalendar to;
    private ActivationStatusType adminStatus;
    private List<AssignmentEditorDto> inducements;
    private List<AssignmentEditorDto> assignments;

    public RoleDto(){}

    public RoleDto(String name, String description, String type, Boolean requestable,
                   XMLGregorianCalendar from, XMLGregorianCalendar to, ActivationStatusType adminStatus){

        this.name = name;
        this.description = description;
        this.type = type;
        this.requestable = requestable;
        this.from = from;
        this.to = to;
        this.adminStatus = adminStatus;
    }

    public PrismObject<RoleType> getRole() {
        return role;
    }

    public void setRole(PrismObject<RoleType> role) {
        this.role = role;
    }

    public List<AssignmentEditorDto> getInducements() {
        return inducements;
    }

    public void setInducements(List<AssignmentEditorDto> inducements) {
        this.inducements = inducements;
    }

    public List<AssignmentEditorDto> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentEditorDto> assignments) {
        this.assignments = assignments;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRequestable() {
        return requestable;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }

    public XMLGregorianCalendar getFrom() {
        return from;
    }

    public void setFrom(XMLGregorianCalendar from) {
        this.from = from;
    }

    public XMLGregorianCalendar getTo() {
        return to;
    }

    public void setTo(XMLGregorianCalendar to) {
        this.to = to;
    }

    public ActivationStatusType getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(ActivationStatusType adminStatus) {
        this.adminStatus = adminStatus;
    }
}
