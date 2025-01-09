/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeleteRepositoryAction extends RepositoryAction<DeleteOptions, Void> {

    private static final String DOT_CLASS = DeleteRepositoryAction.class.getName() + ".";

    private static final String OPERATION_DELETE = DOT_CLASS + "delete";

    private enum State {

        DELETE, SKIP, STOP
    }

    @Override
    public String getOperationName() {
        return "delete objects";
    }

    @Override
    public Void execute() throws Exception {
        String oid = options.getOid();

        if (oid != null) {
            deleteByOid();
            return null;
        }

        ObjectTypes type = options.getType();
        if (type == null) {
            type = ObjectTypes.OBJECT;
        }

        ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context, type.getClassDefinition());
        deleteByFilter(query);

        return null;
    }

    private void deleteByOid() throws SchemaException {
        ObjectQuery query = context.getPrismContext().queryFor(ObjectType.class).id(options.getOid()).build();

        deleteByFilter(query);
    }

    private void deleteByFilter(ObjectQuery query) throws SchemaException {
        OperationResult result = new OperationResult(OPERATION_DELETE);

        OperationStatus operation = new OperationStatus(context, result);
        operation.start();

        log.info("Starting delete");

        ObjectTypes type = options.getType();
        if (type != null) {
            deleteByFilter(type, query, operation, result);
        } else {
            for (ObjectTypes t : ObjectTypes.values()) {
                if (Modifier.isAbstract(t.getClassDefinition().getModifiers())) {
                    continue;
                }

                deleteByFilter(t, query, operation, result);
            }
        }

        operation.finish();
        handleResultOnFinish(null, operation, "Delete finished");
    }

    private void deleteByFilter(ObjectTypes type, ObjectQuery query, OperationStatus operation, OperationResult result)
            throws SchemaException {

        ResultHandler<?> handler = (prismObject, operationResult) -> {
            try {
                State state = options.isAsk() ? askForState(prismObject) : State.DELETE;

                switch (state) {
                    case SKIP:
                        operation.incrementSkipped();
                        return true;
                    case STOP:
                        return false;
                    case DELETE:
                    default:
                }

                RepositoryService repository = context.getRepository();
                repository.deleteObject(prismObject.getCompileTimeClass(), prismObject.getOid(), operationResult);

                operation.incrementTotal();
            } catch (ObjectNotFoundException ex) {
                // object was already gone
            } catch (IOException ex) {
                context.getLog().error("Couldn't delete object {}, reason: {}", ex, prismObject, ex.getMessage());
                operation.incrementError();
            }

            return true;
        };

        Collection<SelectorOptions<GetOperationOptions>> opts = new ArrayList<>();
        if (options.isRaw()) {
            opts.add(new SelectorOptions<>(GetOperationOptions.createRaw()));
        }

        RepositoryService repository = context.getRepository();
        repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, true, result);
    }

    private State askForState(PrismObject<?> object) throws IOException {
        log.info("Do you really want to delete object '" + object.toDebugName() + "'? Yes/No/Cancel");

        State state = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (state == null) {
                String strState = br.readLine();

                if (StringUtils.isEmpty(strState)) {
                    continue;
                }

                strState = strState.toLowerCase();

                if ("y".equalsIgnoreCase(strState) || "yes".equalsIgnoreCase(strState)) {
                    state = State.DELETE;
                }

                if ("n".equalsIgnoreCase(strState) || "no".equalsIgnoreCase(strState)) {
                    state = State.SKIP;
                }

                if ("c".equalsIgnoreCase(strState) || "cancel".equalsIgnoreCase(strState)) {
                    state = State.STOP;
                }
            }
        }

        return state;
    }
}
