package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;

public class ShadowRefAttributeItemDeltaProcessor extends ItemDeltaValueProcessor<ObjectReferenceType> {

    private final QName name;
    private final QShadowReferenceAttributeMapping referenceMapping;
    private final Integer nameId;
    private final SqaleUpdateContext<?, QShadow, MShadow> shadowContext;

    public ShadowRefAttributeItemDeltaProcessor(QName name, SqaleUpdateContext<?, QShadow, MShadow> context, QShadowReferenceAttributeMapping referenceMapping) {
        super(context);
        this.shadowContext = context;
        this.name = name;
        this.nameId = context.repositoryContext().processCacheableUri(name);
        this.referenceMapping = referenceMapping;
    }

    @Override
    public void delete() {
    }

    @Override
    protected void addRealValue(ObjectReferenceType realValue) throws SchemaException {
        var ref = SqaleUtils.referenceWithTypeFixed(realValue);
        referenceMapping.insert(nameId, ref, shadowContext.row(), shadowContext.jdbcSession());
    }

    @Override
    protected void deleteRealValue(ObjectReferenceType realValue) {

        super.deleteRealValue(realValue);
    }
}
