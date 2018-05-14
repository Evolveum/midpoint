package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.opts.DeleteOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeleteRepositoryAction extends RepositoryAction<DeleteOptions> {

    private static final String DOT_CLASS = DeleteRepositoryAction.class.getName() + ".";

    private static final String OPERATION_DELETE = DOT_CLASS + "delete";

    private enum State {

        DELETE, SKIP, STOP;
    }

    @Override
    public void execute() throws Exception {
        String oid = options.getOid();

        if (oid != null) {
            deleteByOid();
        } else {
            ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context);

            deleteByFilter(query);
        }
    }

    private void deleteByOid() throws SchemaException, IOException {
        InOidFilter filter = InOidFilter.createInOid(options.getOid());
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        deleteByFilter(query);
    }

    private void deleteByFilter(ObjectQuery query) throws SchemaException, IOException {
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

        handleResultOnFinish(operation, "Delete finished");
    }

    private void deleteByFilter(ObjectTypes type, ObjectQuery query, OperationStatus operation, OperationResult result)
            throws SchemaException, IOException {

        ResultHandler handler = (prismObject, operationResult) -> {

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
            opts.add(new SelectorOptions(GetOperationOptions.createRaw()));
        }

        RepositoryService repository = context.getRepository();
        repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, false, result);
    }

    private State askForState(PrismObject object) throws IOException {
        log.info("Do you really want to delete object '" + object.toDebugName() + "'? Yes/No/Cancel");

        State state = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (state == null) {
                String strState = br.readLine();

                if (StringUtils.isEmpty(strState)) {
                    continue;
                }

                strState = strState.toLowerCase();

                if ("y".equals(strState) || "yes".equals(strState)) {
                    state = State.DELETE;
                }

                if ("n".equals(strState) || "no".equals(strState)) {
                    state = State.SKIP;
                }

                if ("c".equals(strState) || "cancel".equals(strState)) {
                    state = State.STOP;
                }
            }
        }

        return state;
    }
}
