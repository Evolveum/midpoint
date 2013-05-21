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
package com.evolveum.midpoint.audit.api;

/**
 * @author semancik
 *
 */
public enum AuditEventType {
	
	GET_OBJECT,
	
	ADD_OBJECT,
	
	MODIFY_OBJECT,
	
	DELETE_OBJECT,
	
	SYNCHRONIZATION,
	
	EXECUTE_CHANGES_RAW,
	//  ....
		
	/**
	 * E.g. login
	 */
	CREATE_SESSION,
	
	/**
	 * E.g. logout
	 */
	TERMINATE_SESSION,
	
	/**
	 * Queury session, modify session
	 */
	
	/**
	 * Task states??? 
	 */
	
	/**
	 * Workflow actions? e.g. approvals?
	 */
	
	/**
	 * Startup, shutdown, critical failure (whole system)
	 */
	
	// backup, restores

}
