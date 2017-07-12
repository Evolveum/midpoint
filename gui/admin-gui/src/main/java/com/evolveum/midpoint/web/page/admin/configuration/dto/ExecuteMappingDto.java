/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.io.Serializable;

/**
 * @author mederly
 */
public class ExecuteMappingDto implements Serializable {

    public static final String F_MAPPING = "mapping";
    public static final String F_REQUEST = "request";
    public static final String F_RESULT_TEXT = "resultText";

    private String mapping = "";
    private String request = "";
    private String resultText = "";

	public String getMapping() {
		return mapping;
	}

	public void setMapping(String mapping) {
		this.mapping = mapping;
	}

	public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

	public String getResultText() {
		return resultText;
	}

	public void setResultText(String resultText) {
		this.resultText = resultText;
	}
}
