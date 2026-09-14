package javax.security.auth.callback;

import java.io.IOException;

public interface CallbackHandler {
    void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException;
}
