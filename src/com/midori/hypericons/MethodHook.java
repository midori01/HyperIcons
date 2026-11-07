package com.midori.hypericons;

import java.lang.reflect.Member;

/**
 * Before/after callback run inside a libxposed interceptor by {@link HookEnv}.
 * It keeps the shape of the legacy XC_MethodHook so hook bodies only deal
 * with arguments, the result and the thrown exception.
 */
public abstract class MethodHook {

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static final class MethodHookParam {
        public final Member method;
        public final Object thisObject;
        /** Arguments passed on to the original; before-hooks may replace entries. */
        public final Object[] args;

        private Object result;
        private Throwable throwable;
        boolean returnEarly;

        MethodHookParam(Member method, Object thisObject, Object[] args) {
            this.method = method;
            this.thisObject = thisObject;
            this.args = args;
        }

        public Object getResult() {
            return result;
        }

        /** In a before-hook this skips the original call. */
        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        /** In a before-hook this skips the original call. */
        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        void setOutcome(Object result, Throwable throwable) {
            this.result = result;
            this.throwable = throwable;
        }
    }
}
