package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.metadata.QValueMetadataMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class QAssignmentMetadataMapping extends QValueMetadataMapping<MAssignment, MAssignmentMetadata, QAssignmentMetadata> {

    private static QAssignmentMetadataMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAssignmentMetadataMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAssignmentMetadataMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAssignmentMetadataMapping get() {
        return Objects.requireNonNull(instance);
    }

    protected QAssignmentMetadataMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAssignmentMetadata.TABLE_NAME, QAssignmentMetadata.ALIAS, QAssignmentMetadata.class, repositoryContext);
        addNestedMapping(ValueMetadataType.F_PROCESS, ProcessMetadataType.class)
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        QAssignmentMetadataReferenceMapping.initForAssignmentCreateApprover(
                                repositoryContext))
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        QAssignmentMetadataReferenceMapping.initForAssignmentModifyApprover(
                                repositoryContext));
    }

    @Override
    protected QAssignmentMetadata newAliasInstance(String alias) {
        return new QAssignmentMetadata(alias);
    }

    @Override
    public MAssignmentMetadata newRowObject() {
        return new MAssignmentMetadata();
    }

    @Override
    public MAssignmentMetadata newRowObject(MAssignment ownerRow) {
        var ret = new MAssignmentMetadata();
        ret.ownerOid = ownerRow.ownerOid;
        ret.ownerType = ownerRow.ownerType;
        ret.assignmentCid = ownerRow.cid;
        return ret;
    }

    @Override
    public MAssignmentMetadata insert(ValueMetadataType schemaObject, MAssignment ownerRow, JdbcSession jdbcSession) {
        var row  = initRowObject(schemaObject, ownerRow);

        insert(row, jdbcSession);
        // FIXME: Add refs to process metadata

        var process = schemaObject.getProcess();
        if (process != null) {
            storeRefs(row, process.getCreateApproverRef(),
                    QAssignmentMetadataReferenceMapping.getForAssignmentCreateApprover(), jdbcSession);
            storeRefs(row, process.getModifyApproverRef(),
                    QAssignmentMetadataReferenceMapping.getForAssignmentModifyApprover(), jdbcSession);
        }
        return row;
    }
}
