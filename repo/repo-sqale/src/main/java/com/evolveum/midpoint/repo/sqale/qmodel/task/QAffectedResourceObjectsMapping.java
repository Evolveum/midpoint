package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

public class QAffectedResourceObjectsMapping extends QContainerMapping<BasicResourceObjectSetType,QAffectedResourceObjects,MAffectedResourceObjects,MTask> {

    public static final String DEFAULT_ALIAS_NAME = "aro";

    private static QAffectedResourceObjectsMapping instance;


    // Explanation in class Javadoc for SqaleTableMapping
    public static QAffectedResourceObjectsMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAffectedResourceObjectsMapping(repositoryContext);
        }
        return instance;
    }

    protected QAffectedResourceObjectsMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAffectedResourceObjects.TABLE_NAME, DEFAULT_ALIAS_NAME, BasicResourceObjectSetType.class, QAffectedResourceObjects.class, repositoryContext);

        addItemMapping(ShadowType.F_OBJECT_CLASS, uriMapper(q -> q.objectClassId));
        addRefMapping(F_RESOURCE_REF,
                q -> q.resourceRefTargetOid,
                q -> q.resourceRefTargetType,
                q -> q.resourceRefRelationId,
                QResourceMapping::get);
        addItemMapping(F_INTENT, stringMapper(q -> q.intent));
        addItemMapping(F_KIND, enumMapper(q -> q.kind));
    }

    public static QAffectedResourceObjectsMapping get() {
        return instance;
    }

    @Override
    public MAffectedResourceObjects newRowObject() {
        return new MAffectedResourceObjects();
    }

    @Override
    public MAffectedResourceObjects newRowObject(MTask ownerRow) {
        var row = super.newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    protected QAffectedResourceObjects newAliasInstance(String alias) {
        return new QAffectedResourceObjects(alias);
    }

    @Override
    public MAffectedResourceObjects insert(BasicResourceObjectSetType object, MTask ownerRow, JdbcSession jdbcSession) throws SchemaException {
        MAffectedResourceObjects row = initRowObject(object, ownerRow);
        setReference(object.getResourceRef(),
                o -> row.resourceRefTargetOid = o ,
                t -> row.resourceRefTargetType = t,
                r -> row.resourceRefRelationId = r
                );
        row.kind = object.getKind();
        row.intent = object.getIntent();
        row.objectClassId = processCacheableUri(object.getObjectclass());
        insert(row, jdbcSession);
        return  row;
    }
}
