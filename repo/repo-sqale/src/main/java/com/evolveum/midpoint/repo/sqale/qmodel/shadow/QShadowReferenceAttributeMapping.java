package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

public class QShadowReferenceAttributeMapping extends QReferenceMapping<QShadowReferenceAttribute,MShadowReferenceAttribute,QShadow, MShadow> {
    private static QShadowReferenceAttributeMapping instance;

    public static QShadowReferenceAttributeMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QShadowReferenceAttributeMapping(repositoryContext);
        }
        return get();
    }

    public static QShadowReferenceAttributeMapping get() {
        return  Objects.requireNonNull(instance);
    }

    public static final String DEFAULT_ALIAS_NAME = "sh";

    protected <OS, TQ extends QObject<TR>, TR extends MObject> QShadowReferenceAttributeMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QShadowReferenceAttribute.TABLE_NAME,
                DEFAULT_ALIAS_NAME,
                QShadowReferenceAttribute.class,
                repositoryContext,
                QShadowMapping::getShadowMapping,
                QShadowMapping::getShadowMapping,
                (refAttr, shadow) -> refAttr.ownerOid.eq(shadow.oid), // Add objectClass and resourceRef
                ShadowType.class,
                ShadowType.F_REFERENCE_ATTRIBUTES);
    }

    @Override
    public QShadowReferenceAttribute newAliasInstance(String alias) {
        return new QShadowReferenceAttribute(alias);
    }

    @Override
    public MShadowReferenceAttribute newRowObject(MShadow owner) {
        var ref = new MShadowReferenceAttribute();
        ref.ownerOid = owner.oid;
        ref.ownerType = owner.objectType;
        ref.resourceOid = owner.resourceRefTargetOid;
        ref.ownerObjectClassId = owner.objectClassId;
        return ref;
    }

    public MShadowReferenceAttribute insert(Integer pathId, ObjectReferenceType ref, MShadow ownerRow, JdbcSession jdbcSession) throws SchemaException {
        MShadowReferenceAttribute row = newRowObject(ownerRow);
        initRowObject(row, ref);
        row.pathId = pathId;
        insert(row, jdbcSession);

        return row;

    }

    @Override
    public BiFunction<QShadow, QShadowReferenceAttribute, Predicate> correlationPredicate() {
        return (owner, ref) -> owner.oid.eq(ref.ownerOid);
    }
}
