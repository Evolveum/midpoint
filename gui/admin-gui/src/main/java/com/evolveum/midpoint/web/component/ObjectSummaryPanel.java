package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.IModel;

public abstract class ObjectSummaryPanel <O extends ObjectType> extends AbstractSummaryPanel<O> {
	private static final long serialVersionUID = -3755521482914447912L;
	
	public ObjectSummaryPanel(String id, final IModel<PrismObject<O>> model) {
		super(id, model);
		initLayoutCommon();
	}

}
