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

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ReportDto implements Serializable {

    public static enum Type {USERS, RECONCILIATION, AUDIT}

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";

    private Type type;
    private String name;
    private String description;
    private String author;
    private String timeString;
    private String fileType;


    public ReportDto(Type type, String name, String description) {
        this.type = type;
        this.description = description;
        this.name = name;
    }

    public ReportDto(Type type, String name, String description, String author, String timeString){
        this.type = type;
        this.name = name;
        this.description = description;
        this.author = author;
        this.timeString = timeString;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getAuthor(){
        return author;
    }

    public String getTimeString(){
        return timeString;
    }

    public String getFileType(){
        return fileType;
    }

    public void setFileType(String fileType){
        this.fileType = fileType;
    }
}
