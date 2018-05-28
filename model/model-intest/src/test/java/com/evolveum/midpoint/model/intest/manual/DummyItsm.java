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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Dummy ITSM system for testing.
 * 
 * @author semancik
 */
public class DummyItsm implements DebugDumpable {
	
	private static DummyItsm instance = new DummyItsm();
	private List<DummyItsmTicket> tickets = new ArrayList<>();
	private int nextId = 1;
	private Class<? extends CommonException> failureClass;
	
	public static DummyItsm getInstance() {
		return instance;
	}
	
	public Class<? extends CommonException> getFailureClass() {
		return failureClass;
	}

	public void setFailureClass(Class<? extends CommonException> failureClass) {
		this.failureClass = failureClass;
	}
	
	public void clearFailureClass() {
		this.failureClass = null;
	}

	public List<DummyItsmTicket> getTickets() {
		return tickets;
	}

	private synchronized String nextIdentifier() {
		String identifier = String.format("%04d", nextId);
		nextId++;
		return identifier;
	}
	
	public String createTicket(String body) throws CommonException {
		tryToFail();
		DummyItsmTicket ticket = new DummyItsmTicket(nextIdentifier());
		ticket.setBody(body);
		tickets.add(ticket);
		return ticket.getIdentifier();
	}
	
	public DummyItsmTicket findTicket(String identifier) throws CommonException {
		tryToFail();
		for (DummyItsmTicket ticket: tickets) {
			if (ticket.getIdentifier().equals(identifier)) {
				return ticket;
			}
		}
		return null;
	}

	private void tryToFail() throws CommonException {
		if (failureClass == null) {
			return;
		}
		CommonException exception;
		try {
			Constructor<? extends CommonException> constructor = failureClass.getConstructor(String.class);
			exception = constructor.newInstance("Simulated error: "+failureClass.getSimpleName());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Failed to fail: "+e.getMessage(), e);
		}
		throw exception;
	}

	public void test() throws CommonException {
		tryToFail();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(DummyItsm.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "tickets", tickets, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "failureClass", failureClass, indent + 1);
		return sb.toString();
	}
	
}
