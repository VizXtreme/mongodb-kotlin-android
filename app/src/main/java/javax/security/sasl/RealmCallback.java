package javax.security.sasl;

import javax.security.auth.callback.TextInputCallback;

public class RealmCallback extends TextInputCallback {
    private static final long serialVersionUID = -434227291534777247L;

    public RealmCallback(String prompt) {
        super(prompt);
    }

    public RealmCallback(String prompt, String defaultRealmInfo) {
        super(prompt, defaultRealmInfo);
    }
}
