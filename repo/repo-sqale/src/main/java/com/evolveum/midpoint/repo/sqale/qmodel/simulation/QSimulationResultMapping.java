package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;


public class QSimulationResultMapping extends QObjectMapping<SimulationResultType, QSimulationResult, MSimulationResult>{

    public static final String DEFAULT_ALIAS_NAME = "sr";

    private static QSimulationResultMapping instance;



    public static QSimulationResultMapping initSimulationResultMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QSimulationResultMapping(repositoryContext);
        return instance;
    }

    public static QSimulationResultMapping getSimulationResultMapping() {
        return Objects.requireNonNull(instance);
    }


    private QSimulationResultMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSimulationResult.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SimulationResultType.class, QSimulationResult.class, repositoryContext);
    }

    @Override
    protected QSimulationResult newAliasInstance(String alias) {
        return new QSimulationResult(alias);
    }

    @Override
    public MSimulationResult newRowObject() {
        return new MSimulationResult();
    }
}
