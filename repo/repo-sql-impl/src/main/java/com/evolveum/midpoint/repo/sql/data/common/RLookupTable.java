package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@ForeignKey(name = "fk_lookup_table")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_lookup_name", columnNames = {"name_norm"}))
public class RLookupTable extends RObject<LookupTableType> {

    private RPolyString name;
    private Set<RLookupTableRow> rows;

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RLookupTableRow> getRows() {
        return rows;
    }

    public void setRows(Set<RLookupTableRow> rows) {
        this.rows = rows;
    }

    @Override
    public RPolyString getName() {
        return name;
    }

    @Override
    public void setName(RPolyString name) {
        this.name = name;
    }

    @Override
    public LookupTableType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options) throws DtoTranslationException {
        return null;
    }
}
