package javax.security.sasl;

import javax.security.auth.callback.ChoiceCallback;

public class RealmChoiceCallback extends ChoiceCallback {
    private static final long serialVersionUID = -858814732703835777L;

    public RealmChoiceCallback(String prompt, String[] choices, int defaultChoice, boolean multipleSelectionsAllowed) {
        super(prompt, choices, defaultChoice, multipleSelectionsAllowed);
    }
}
