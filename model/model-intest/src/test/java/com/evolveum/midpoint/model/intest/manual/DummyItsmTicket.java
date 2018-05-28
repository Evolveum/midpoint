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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class DummyItsmTicket implements DebugDumpable {
	
	private String identifier;
	private DummyItsmTicketStatus status;
	private String body;
	
	public DummyItsmTicket(String identifier) {
		this.identifier = identifier;
		this.status = DummyItsmTicketStatus.OPEN;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public DummyItsmTicketStatus getStatus() {
		return status;
	}

	public void setStatus(DummyItsmTicketStatus status) {
		this.status = status;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(DummyItsmTicket.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "identifier", identifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "body", body, indent + 1);
		return sb.toString();
	}

}
