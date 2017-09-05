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

/**
 * Exception used for tunneling checked exceptions through places where checked exceptipons are not allowed (e.g. callbacks).
 *
 * This exception must not appear to the "outside", it must be caught and transformed back to the original form.
 *
 * @author Radovan Semancik
 *
 */
public class TunnelException extends RuntimeException {
	private static final long serialVersionUID = -3745473492409029661L;

	public TunnelException() {
		super();
	}

	public TunnelException(String message, Throwable cause) {
		super(message, cause);
	}

	public TunnelException(String message) {
		super(message);
	}

	public TunnelException(Throwable cause) {
		super(cause);
	}

}
