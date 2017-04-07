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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

/**
 * This exception exists only for the purpose of passing checked exceptions where they are not allowed.
 * This comes handy e.g. in callbacks. This exception must never be shown to "outside".
 * 
 * @author Radovan Semancik
 *
 */
public class IntermediateException extends RuntimeException {

	public IntermediateException() {
		super();
	}

	public IntermediateException(String message, Throwable cause) {
		super(message, cause);
	}

	public IntermediateException(String message) {
		super(message);
	}

	public IntermediateException(Throwable cause) {
		super(cause);
	}

}
