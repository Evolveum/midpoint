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
package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 *
 */
public class ValidationItem implements DebugDumpable, ShortDumpable {

	private OperationResultStatus status;
	private LocalizableMessage message;
	private UniformItemPath itemPath;
	// TODO? line number?
	
	public OperationResultStatus getStatus() {
		return status;
	}
	
	public void setStatus(OperationResultStatus status) {
		this.status = status;
	}

	public LocalizableMessage getMessage() {
		return message;
	}

	public void setMessage(LocalizableMessage message) {
		this.message = message;
	}

	public UniformItemPath getItemPath() {
		return itemPath;
	}

	public void setItemPath(UniformItemPath itemPath) {
		this.itemPath = itemPath;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ValidationItem.class, indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
		DebugUtil.debugDumpWithLabelShortDumpLn(sb, "message", message, indent + 1);
		DebugUtil.debugDumpWithLabelShortDump(sb, "itemPath", itemPath, indent + 1);
		return sb.toString();
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (status != null) {
			sb.append(status);
			sb.append(" ");
		}
		if (itemPath != null) {
			sb.append(itemPath);
			sb.append(" ");
		}
		if (message != null) {
			sb.append(message.getFallbackMessage());
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ValidationItem(");
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}
	
	
	
}
