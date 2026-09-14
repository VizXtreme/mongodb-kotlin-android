package javax.security.sasl;

import java.io.IOException;

public class SaslException extends IOException {
    private static final long serialVersionUID = 4579784287297943939L;

    public SaslException() {
        super();
    }

    public SaslException(String detail) {
        super(detail);
    }

    public SaslException(String detail, Throwable ex) {
        super(detail);
        if (ex != null) {
            initCause(ex);
        }
    }

    public Throwable getException() {
        return getCause();
    }
}
