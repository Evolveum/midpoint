package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@ForeignKey(name = "fk_lookup_table")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_lookup_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
public class RLookupTable extends RObject<LookupTableType> {

    private RPolyString nameCopy;
    private Set<RLookupTableRow> rows;

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @Transient
    public Set<RLookupTableRow> getRows() {
        return rows;
    }

    public void setRows(Set<RLookupTableRow> rows) {
        this.rows = rows;
    }

    public static void copyFromJAXB(LookupTableType jaxb, RLookupTable repo, RepositoryContext repositoryContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException, SchemaException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));

        List<LookupTableRowType> rows = jaxb.getRow();
        if (!rows.isEmpty()) {
            repo.setRows(new HashSet<>());
            for (LookupTableRowType row : rows) {
                RLookupTableRow rRow = RLookupTableRow.toRepo(repo, row);
                rRow.setTransient(generatorResult.isTransient(row.asPrismContainerValue()));
                repo.getRows().add(rRow);
            }
        }
    }

    @Override
    public LookupTableType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options) throws DtoTranslationException {
        LookupTableType object = new LookupTableType();
        RUtil.revive(object, prismContext);
        RLookupTable.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
