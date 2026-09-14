package javax.security.auth.callback;

public class UnsupportedCallbackException extends Exception {
    private static final long serialVersionUID = -8290812977790396417L;
    private final Callback callback;

    public UnsupportedCallbackException(Callback callback) {
        super();
        this.callback = callback;
    }

    public UnsupportedCallbackException(Callback callback, String msg) {
        super(msg);
        this.callback = callback;
    }

    public Callback getCallback() {
        return callback;
    }
}
