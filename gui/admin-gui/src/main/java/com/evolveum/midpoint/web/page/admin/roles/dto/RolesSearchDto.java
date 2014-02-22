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

import java.io.Serializable;

/**
 *  @author shood
 * */
public class RolesSearchDto implements Serializable{

    public enum Requestable{
        ALL("All"), REQUESTABLE("Requestable"), NON_REQUESTABLE("Non-Requestable");

        Requestable(String key){
            this.key = key;
        }

        public String getKey(){
            return key;
        }

        private String key;
    }

    public static final String F_SEARCH_TEXT = "text";
    public static final String F_REQUESTABLE = "requestable";

    private String text;
    private Requestable requestable = Requestable.ALL;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Requestable getRequestable() {
        if(requestable == null){
            requestable = Requestable.ALL;
        }
        return requestable;
    }

    public Boolean getRequestableValue(){
        switch(requestable){
            case ALL:
                return null;
            case REQUESTABLE:
                return true;
            case NON_REQUESTABLE:
                return false;
        }
        return null;
    }

    public void setRequestable(Requestable requestable) {
        this.requestable = requestable;
    }
}
