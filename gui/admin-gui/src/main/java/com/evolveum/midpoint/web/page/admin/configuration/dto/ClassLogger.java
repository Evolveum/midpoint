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
 * @author lazyman
 */
public class ClassLogger extends LoggerConfiguration {

    private String _package;

    public ClassLogger(ClassLoggerConfigurationType config) {
        Validate.notNull(config, "Subsystem logger configuration must not be null.");
//        Validate.notNull(config.getPackage(), "Subsystem component is not defined.");

        _package = config.getPackage();
        setLevel(config.getLevel());
        setAppenders(config.getAppender());
    }

    @Override
    public String getName() {
        return _package;
    }

	@Override
	public void setName(String name) {
		this._package = name;
	}

	public ClassLoggerConfigurationType toXmlType() {
		ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
		type.setPackage(_package);
		type.setLevel(getLevel());
		if (!(getAppenders().isEmpty())){
        	type.getAppender().addAll(getAppenders());
        }
		return type;
	}
}
