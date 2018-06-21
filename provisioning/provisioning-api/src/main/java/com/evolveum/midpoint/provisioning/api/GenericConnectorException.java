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
package com.evolveum.midpoint.provisioning.api;

/**
 * Generic indistinguishable error of a connector framework.
 *
 * Please do not use this exception if possible!
 * Only errors that cannot be categorized or that are not
 * expected at all should use this exception.
 *
 * This is RUNTIME exception. As this error is generic and
 * we cannot distinguish any details, there is no hope that
 * an interface client can do anything with it. So don't even
 * bother catching it.
 *
 * @author Radovan Semancik
 */
public class GenericConnectorException extends RuntimeException {
	private static final long serialVersionUID = 4718501022689239025L;

	public GenericConnectorException() {
	}

	public GenericConnectorException(String message) {
		super(message);
	}

	public GenericConnectorException(Throwable cause) {
		super(cause);
	}

	public GenericConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

}
