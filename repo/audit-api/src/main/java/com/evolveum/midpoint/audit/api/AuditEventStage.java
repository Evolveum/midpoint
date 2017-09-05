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

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;

/**
 * @author semancik
 *
 */
public enum AuditEventStage {

	REQUEST,

	EXECUTION;

	public static AuditEventStage toAuditEventStage(AuditEventStageType stage){
		if (stage == null){
			return null;
		}

		switch (stage){
			case EXECUTION :
				return AuditEventStage.EXECUTION;
			case REQUEST:
				return AuditEventStage.REQUEST;
			default:
				throw new IllegalArgumentException("Unknown audit event stage: " + stage);
		}
	}

	public static AuditEventStageType fromAuditEventStage(AuditEventStage stage){
		if (stage == null){
			return null;
		}

		switch (stage){
			case EXECUTION :
				return AuditEventStageType.EXECUTION;
			case REQUEST:
				return AuditEventStageType.REQUEST;
			default:
				throw new IllegalArgumentException("Unknown audit event stage: " + stage);
		}
	}

}
