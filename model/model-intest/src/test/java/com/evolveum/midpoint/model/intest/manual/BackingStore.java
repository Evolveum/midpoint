/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.intest.manual;

import java.io.IOException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;

/**
 * @author semancik
 *
 */
public interface BackingStore {
	
	void initialize() throws IOException;
	
	void provisionWill(String interest) throws IOException;
	
	void updateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException;
	
	void deprovisionWill() throws IOException;
	
	void addJack() throws IOException;
	
	void deleteJack() throws IOException;
	
	void addPhantom()  throws IOException;
	
	void deleteAccount(String username) throws IOException;
	
	void displayContent() throws IOException;

}
