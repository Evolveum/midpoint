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

import com.evolveum.midpoint.web.component.util.Selectable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *  @author shood
 * */
public class ReportOutputDto extends Selectable implements Serializable{

    public static enum SearchType{
        NAME("SearchType.NAME"),
        DESCRIPTION("SearchType.DESCRIPTION"),
        AUTHOR("SearchType.AUTHOR");

        private String key;

        private SearchType(String key){this.key = key;}

        private String getKey(){return key;}
    }

    public static final String F_TYPE = "type";
    public static final String F_DESCRIPTION = "description";
    public static final String F_TEXT = "text";
    public static final String F_NAME = "name";

    private ReportDto.Type reportType;
    private String name;
    private String description;
    private String time;
    private String author;
    private String fileType;
    private String text;

    private Collection<SearchType> type;

    public ReportOutputDto(){}

    public ReportOutputDto(ReportDto.Type type, String name, String description){
        this.reportType = type;
        this.name = name;
        this.description = description;
    }

    public ReportOutputDto(ReportDto.Type type, String name, String description,
                           String author, String time){
        this.reportType = type;
        this.name = name;
        this.description = description;
        this.author = author;
        this.time = time;
    }

    public Collection<SearchType> getType() {
        if (type == null) {
            type = new ArrayList<SearchType>();
            type.add(SearchType.NAME);
        }
        return type;
    }

    public void setType(Collection type) {
        this.type = type;
    }

    public boolean hasType(SearchType type) {
        if (getType().contains(type)) {
            return true;
        }
        return false;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ReportDto.Type getReportType() {
        return reportType;
    }

    public void setReportType(ReportDto.Type reportType) {
        this.reportType = reportType;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
