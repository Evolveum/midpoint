package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.RefTableItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.UUID;

public class ShadowRefAttributeItemDeltaProcessor extends RefTableItemDeltaProcessor<QShadowReferenceAttribute, QShadow, MShadow> {

    private final QName name;
    private final QShadowReferenceAttributeMapping referenceMapping;
    private final Integer nameId;
    public ShadowRefAttributeItemDeltaProcessor(QName name, SqaleUpdateContext<?, QShadow, MShadow> context, QShadowReferenceAttributeMapping referenceMapping) {
        super(context, referenceMapping);
        this.name = name;
        this.nameId = context.repositoryContext().processCacheableUri(name);
        this.referenceMapping = referenceMapping;
    }

    @Override
    public void delete() {
        var r = referenceMapping.defaultAlias();
        context.jdbcSession().newDelete(r)
                .where(r.pathId.eq(nameId).and(r.isOwnedBy(context.row())))
                .execute();
    }

    @Override
    protected void addRealValue(ObjectReferenceType realValue) throws SchemaException {
        var ref = SqaleUtils.referenceWithTypeFixed(realValue);
        referenceMapping.insert(nameId, ref, context.row(), context.jdbcSession());
    }

    @Override
    protected void deleteRealValue(ObjectReferenceType ref) {
        var r = referenceMapping.defaultAlias();
        Integer relId = context.repositoryContext().searchCachedRelationId(ref.getRelation());
        context.jdbcSession().newDelete(r)
                .where(r.isOwnedBy(context.row())
                        .and(r.targetOid.eq(UUID.fromString(ref.getOid())))
                        .and(r.relationId.eq(relId))
                        .and(r.pathId.eq(nameId))
                )
                .execute();
    }


}
