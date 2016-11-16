/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import org.jetbrains.annotations.NotNull;

/**
 * Description of a situation that caused a trigger of the policy rule.
 * 
 * @author semancik
 */
public class EvaluatedPolicyRuleTrigger {
	
	@NotNull private final PolicyConstraintKindType constraintKind;
	@NotNull private final AbstractPolicyConstraintType constraint;
	private final String message;
	
	public EvaluatedPolicyRuleTrigger(@NotNull PolicyConstraintKindType constraintKind, @NotNull AbstractPolicyConstraintType constraint, String message) {
		this.constraintKind = constraintKind;
		this.constraint = constraint;
		this.message = message;
	}

	/**
	 * The kind of constraint that caused the trigger.
	 * @return
	 */
	@NotNull
	public PolicyConstraintKindType getConstraintKind() {
		return constraintKind;
	}

	@NotNull
	public AbstractPolicyConstraintType getConstraint() {
		return constraint;
	}

	/**
	 * Human-readable message associated with this trigger. The message
	 * explain why the rule was triggered. It can be used
	 * in the logs, as an error message, in the audit trail
	 * and so on.
	 */
	public String getMessage() {
		return message;
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
		result = prime * result + ((constraintKind == null) ? 0 : constraintKind.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EvaluatedPolicyRuleTrigger other = (EvaluatedPolicyRuleTrigger) obj;
		if (constraint == null) {
			if (other.constraint != null) {
				return false;
			}
		} else if (!constraint.equals(other.constraint)) {
			return false;
		}
		if (constraintKind != other.constraintKind) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "EvaluatedPolicyRuleTrigger(" + constraintKind + ": " + message + ")";
	}

}
