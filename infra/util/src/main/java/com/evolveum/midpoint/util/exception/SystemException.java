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

package com.evolveum.midpoint.util.exception;

public class SystemException extends RuntimeException {

	private static final long serialVersionUID = -611042093339023362L;

	public SystemException() {
	}

	public SystemException(String message) {
		super(message);
	}

	public SystemException(Throwable throwable) {
		super(throwable);
	}

	public SystemException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
