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
package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

import org.apache.commons.lang.Validate;

/**
 *  @author shood
 * */
public class StandardLogger extends LoggerConfiguration{

    private StandardLoggerType logger;

    public StandardLogger(ClassLoggerConfigurationType config){
        Validate.notNull(config, "Standard logger configuration must not be null.");

        if(config.getPackage() != null && !config.getPackage().isEmpty()){
           logger = StandardLoggerType.fromValue(config.getPackage());
        }

        setLevel(config.getLevel());
        setAppenders(config.getAppender());
    }

    @Override
    public String getName(){
        if(logger == null){
            return null;
        }

        return logger.getValue();
    }

    @Override
    public void setName(String name){}

    public StandardLoggerType getLogger() {
        return logger;
    }

    public void setLogger(StandardLoggerType logger) {
        this.logger = logger;
    }

    @Override
    public ClassLoggerConfigurationType toXmlType(){
        ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
        type.setPackage(logger.getValue());
        type.setLevel(getLevel());
        if (!(getAppenders().isEmpty())){
        	type.getAppender().addAll(getAppenders());
        }

        return type;
    }

    public static boolean isStandardLogger(String pkg){
        for(StandardLoggerType s: StandardLoggerType.values()){
            if(s.getValue().equals(pkg)){
                return true;
            }
        }
        return false;
    }
}
