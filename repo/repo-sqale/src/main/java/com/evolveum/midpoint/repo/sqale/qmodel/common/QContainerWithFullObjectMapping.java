package com.evolveum.midpoint.repo.sqale.qmodel.common;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;

import com.evolveum.midpoint.repo.sqale.qmodel.assignment.MAssignment;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

public abstract class QContainerWithFullObjectMapping<S extends Containerable, Q extends QContainerWithFullObject<R, OR>, R extends MContainerWithFullObject, OR> extends QContainerMapping<S,Q,R,OR> {

    protected QContainerWithFullObjectMapping(@NotNull String tableName, @NotNull String defaultAliasName, @NotNull Class<S> schemaType, @NotNull Class<Q> queryType, @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);
    }

    abstract protected ItemPath getContainerPath();

    public R initRowObjectWithFullObject(S schemaObject, OR ownerRow) throws SchemaException {
        R row =  super.initRowObject(schemaObject, ownerRow);
        row.fullObject = createFullObject(schemaObject);
        return row;
    }

    @Override
    public void afterModify(SqaleUpdateContext<S, Q, R> updateContext) throws SchemaException {
        super.afterModify(updateContext);
            // insert fullObject here
        PrismContainer<AssignmentType> identityContainer =
                updateContext.findValueOrItem(getContainerPath());
        // row in context already knows its CID
        PrismContainerValue<AssignmentType> pcv = identityContainer.findValue(updateContext.row().cid);
        byte[] fullObject = createFullObject(pcv.asContainerable());
        updateContext.set(updateContext.entityPath().fullObject, fullObject);
    }


}
