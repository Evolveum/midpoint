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

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class SimplePieChartDto implements Serializable {

    private String label;
    private int base;
    private int entryValue;
    private int percent;

    public SimplePieChartDto(String label, int base, int entryValue){
        this.label = label;
        this.entryValue = entryValue;
        this.base = base;
        this.percent = calculatePercentage();
    }

    public String getLabel() {
        return label;
    }

    public int getBase() {
        return base;
    }

    public int getEntryValue() {
        return entryValue;
    }

    public int getPercent() {
        return percent;
    }

    private int calculatePercentage(){
        return (this.entryValue*100)/this.base;
    }
}
