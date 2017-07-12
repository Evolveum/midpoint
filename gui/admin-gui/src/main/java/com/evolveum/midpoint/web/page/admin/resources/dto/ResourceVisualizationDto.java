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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;

/**
 * @author mederly
 */
public class ResourceVisualizationDto implements Serializable {

	public static final String F_DOT = "dot";
	public static final String F_SVG = "svg";
	public static final String F_EXCEPTION_AS_STRING = "exceptionAsString";

	private final String dot;
	private final String svg;
	private final Exception exception;

	public ResourceVisualizationDto(String dot, String svg, Exception exception) {
		this.dot = dot;
		this.svg = svg;
		this.exception = exception;
	}

	public String getDot() {
		return dot;
	}

	public String getSvg() {
		return svg;
	}

	public Exception getException() {
		return exception;
	}

	public String getExceptionAsString() {
		return exception != null ? exception.getMessage() : null;
	}
}
