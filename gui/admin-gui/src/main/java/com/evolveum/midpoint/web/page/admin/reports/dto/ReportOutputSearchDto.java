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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *  @author shood
 * */
public class ReportOutputSearchDto extends Selectable implements Serializable{

    public static enum SearchType{
        NAME("SearchType.NAME");
        //DESCRIPTION("SearchType.DESCRIPTION");
        //AUTHOR("SearchType.AUTHOR");

        private String key;

        private SearchType(String key){this.key = key;}

        private String getKey(){return key;}
    }

    public static final String F_REPORT_TYPE = "reportType";
    public static final String F_FILE_TYPE = "fileType";
    public static final String F_TYPE = "type";
    public static final String F_DESCRIPTION = "description";
    public static final String F_TEXT = "text";
    public static final String F_NAME = "name";

    private Map<String, String> reportTypeMap = new HashMap<String, String>();
    private String name;
    private String description;
    private String time;
    private String author;
    private ExportType fileType;
    private String reportType = "Report Type";
    private String text;

    private Collection<SearchType> type;

    public ReportOutputSearchDto(){}

    public ReportOutputSearchDto(String name, String description){
        this.name = name;
        this.description = description;
    }

    public Map<String, String> getReportTypeMap() {
        return reportTypeMap;
    }

    public void setReportTypeMap(Map<String, String> reportTypeMap) {
        this.reportTypeMap = reportTypeMap;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public ReportOutputSearchDto(String name, String description,
                                 String author, String time){
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

    public ExportType getFileType() {
        return fileType;
    }

    public void setFileType(ExportType fileType) {
        this.fileType = fileType;
    }
}
