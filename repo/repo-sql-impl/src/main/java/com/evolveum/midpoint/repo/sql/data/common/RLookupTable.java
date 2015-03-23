package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@ForeignKey(name = "fk_lookup_table")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_lookup_name", columnNames = {"name_norm"}))
public class RLookupTable extends RObject<LookupTableType> {

    private RPolyString name;
    private Set<RLookupTableRow> rows;

    @Override
    @Embedded
    public RPolyString getName() {
        return name;
    }

    @Transient
    public Set<RLookupTableRow> getRows() {
        return rows;
    }

    public void setRows(Set<RLookupTableRow> rows) {
        this.rows = rows;
    }

    @Override
    public void setName(RPolyString name) {
        this.name = name;
    }

    public static void copyFromJAXB(LookupTableType jaxb, RLookupTable repo, PrismContext prismContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext, generatorResult);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));

        List<LookupTableTableType> rows = jaxb.getTable();
        if (!rows.isEmpty()) {
            repo.setRows(new HashSet<RLookupTableRow>());
        }

        for (LookupTableTableType row : rows) {
            RLookupTableRow rRow = new RLookupTableRow();
            rRow.setOwner(repo);
            rRow.setTransient(generatorResult.isTransient(row.asPrismContainerValue()));
            rRow.setId(RUtil.toShort(row.getId()));
            rRow.setKey(row.getKey());
            rRow.setLabel(RPolyString.copyFromJAXB(row.getLabel()));
            rRow.setLastChangeTimestamp(row.getLastChangeTimestamp());
            if (rRow.getLastChangeTimestamp() == null) {
                XMLGregorianCalendar cal = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
                rRow.setLastChangeTimestamp(cal);
                row.setLastChangeTimestamp(cal);
            }
            rRow.setValue(row.getValue());

            repo.getRows().add(rRow);
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
