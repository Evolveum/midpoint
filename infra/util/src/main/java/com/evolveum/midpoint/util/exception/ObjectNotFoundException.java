/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.util.exception;

/**
 * Object with specified criteria (OID) has not been found in the repository.
 * 
 * @author Radovan Semancik
 * 
 */
public class ObjectNotFoundException extends CommonException {
	private static final long serialVersionUID = -9003686713018111855L;

	private String oid = null;;
	
	public ObjectNotFoundException() {
		super();
	}

	public ObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectNotFoundException(String message, Throwable cause, String oid) {
		super(message, cause);
		this.oid = oid;
	}
	
	public ObjectNotFoundException(String message) {
		super(message);
	}

	public ObjectNotFoundException(Throwable cause) {
		super(cause);
	}

	public String getOid() {
		return oid;
	}

	@Override
	public String getOperationResultMessage() {
		return "Object not found";
	}
}
