package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.metadata.QValueMetadataMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
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
        ret.assignmentCid = ownerRow.cid;
        return ret;
    }

    @Override
    public MAssignmentMetadata insert(ValueMetadataType schemaObject, MAssignment ownerRow, JdbcSession jdbcSession) {
        var row  = initRowObject(schemaObject, ownerRow);
        insert(row, jdbcSession);
        // FIXME: Add refs to process metadata
        return row;
    }
}
