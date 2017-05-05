/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractConstruction<F extends FocusType, T extends AbstractConstructionType> implements DebugDumpable, Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractConstruction.class);

	private AssignmentPathImpl assignmentPath;
	private T constructionType;
	private ObjectType source;
	private OriginType originType;
	private String channel;
	private LensContext<F> lensContext;
	private ObjectDeltaObject<F> focusOdo;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;

	private boolean isValid = true;

	public AbstractConstruction(T constructionType, ObjectType source) {
		this.constructionType = constructionType;
		this.source = source;
		this.assignmentPath = null;
	}

	public void setSource(ObjectType source) {
		this.source = source;
	}

	public ObjectType getSource() {
		return source;
	}

	public OriginType getOriginType() {
		return originType;
	}

	public void setOriginType(OriginType originType) {
		this.originType = originType;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public LensContext<F> getLensContext() {
		return lensContext;
	}

	public void setLensContext(LensContext<F> lensContext) {
		this.lensContext = lensContext;
	}
	
	public T getConstructionType() {
		return constructionType;
	}

	public ObjectDeltaObject<F> getFocusOdo() {
		return focusOdo;
	}

	public void setFocusOdo(ObjectDeltaObject<F> focusOdo) {
		this.focusOdo = focusOdo;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	PrismContext getPrismContext() {
		return prismContext;
	}

	void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public String getDescription() {
		return constructionType.getDescription();
	}
	
	public boolean isWeak() {
		return constructionType.getStrength() == ConstructionStrengthType.WEAK;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public AssignmentPathImpl getAssignmentPath() {
		return assignmentPath;
	}

	public void setAssignmentPath(AssignmentPathImpl assignmentPath) {
		this.assignmentPath = assignmentPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignmentPath == null) ? 0 : assignmentPath.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((constructionType == null) ? 0 : constructionType.hashCode());
		result = prime * result + ((focusOdo == null) ? 0 : focusOdo.hashCode());
		result = prime * result + (isValid ? 1231 : 1237);
		result = prime * result + ((lensContext == null) ? 0 : lensContext.hashCode());
		result = prime * result + ((objectResolver == null) ? 0 : objectResolver.hashCode());
		result = prime * result + ((originType == null) ? 0 : originType.hashCode());
		result = prime * result + ((prismContext == null) ? 0 : prismContext.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		AbstractConstruction other = (AbstractConstruction) obj;
		if (assignmentPath == null) {
			if (other.assignmentPath != null) {
				return false;
			}
		} else if (!assignmentPath.equals(other.assignmentPath)) {
			return false;
		}
		if (channel == null) {
			if (other.channel != null) {
				return false;
			}
		} else if (!channel.equals(other.channel)) {
			return false;
		}
		if (constructionType == null) {
			if (other.constructionType != null) {
				return false;
			}
		} else if (!constructionType.equals(other.constructionType)) {
			return false;
		}
		if (focusOdo == null) {
			if (other.focusOdo != null) {
				return false;
			}
		} else if (!focusOdo.equals(other.focusOdo)) {
			return false;
		}
		if (isValid != other.isValid) {
			return false;
		}
		if (lensContext == null) {
			if (other.lensContext != null) {
				return false;
			}
		} else if (!lensContext.equals(other.lensContext)) {
			return false;
		}
		if (objectResolver == null) {
			if (other.objectResolver != null) {
				return false;
			}
		} else if (!objectResolver.equals(other.objectResolver)) {
			return false;
		}
		if (originType != other.originType) {
			return false;
		}
		if (prismContext == null) {
			if (other.prismContext != null) {
				return false;
			}
		} else if (!prismContext.equals(other.prismContext)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		return true;
	}

}
