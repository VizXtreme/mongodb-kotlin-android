package javax.security.auth.callback;

import java.io.Serializable;

public class NameCallback implements Callback, Serializable {
    private static final long serialVersionUID = 3779186147307058644L;

    private String prompt;
    private String defaultName;
    private String inputName;

    public NameCallback(String prompt) {
        this.prompt = prompt;
    }

    public NameCallback(String prompt, String defaultName) {
        this.prompt = prompt;
        this.defaultName = defaultName;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public void setName(String name) {
        this.inputName = name;
    }

    public String getName() {
        return inputName;
    }
}
