package com.evolveum.midpoint.util.exception;

/**
 *
 * @author tony
 *
 */
public abstract class ExceptionMessageHelper {

    /*
     } catch (ObjectNotFoundException e) {
            String msg = "Object not found (unexpected error, probably a bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (SchemaException e) {
            String msg = "Schema processing error (probably connector bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (ExpressionEvaluationException e) {
            String msg = "Expression error: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (RuntimeException e) {
            String msg = "Unspecified exception: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        }
     */

    protected final Exception exception;

    public ExceptionMessageHelper(Exception exception) {
        super();
        this.exception = exception;
    }


    public static ExceptionMessageHelper from(Exception e) {
        return new Unmapped(e);
    }


    public String orElse(String string) {
        return onUnmatched(string).getMessage();
    }


    public abstract Mapped onUnmatched(String string);


    public ExceptionMessageHelper on(Class<? extends Exception> clazz, String message) {
        return this;
    }

    private static class Unmapped extends ExceptionMessageHelper {

        public Unmapped(Exception exception) {
            super(exception);
        }

        @Override
        public ExceptionMessageHelper on(Class<? extends Exception> clazz, String message) {
            if(clazz.isInstance(exception)) {
                return new Mapped(exception, message);
            }

            return this;
        }

        @Override
        public Mapped onUnmatched(String string) {
            return new Mapped(exception, string);
        }
    }

    public static class Mapped extends ExceptionMessageHelper {

        private String message;

        public Mapped(Exception exception, String message) {
            super(exception);
            this.message = message;
        }

        public String getMessage() {
            return message + exception.getMessage();
        }

        @Override
        public Mapped onUnmatched(String string) {
            return this;
        }

    }

    static String example(Exception e) {
        String msg = ExceptionMessageHelper.from(e)
            .on(ObjectNotFoundException.class, "Object not found (unexpected error, probably a bug): ")
            .on(SchemaException.class, "Schema processing error (probably connector bug): ")
            .on(ExpressionEvaluationException.class, "Expression error: ")
            .orElse("Unspecified exception: ");
        return "failed while completing resource. " + msg;
    }
}
