package javax.security.auth.callback;

import java.io.Serializable;

public class TextInputCallback implements Callback, Serializable {
    private static final long serialVersionUID = -806346635828564405L;

    private String prompt;
    private String defaultText;
    private String inputText;

    public TextInputCallback(String prompt) {
        this.prompt = prompt;
    }

    public TextInputCallback(String prompt, String defaultText) {
        this.prompt = prompt;
        this.defaultText = defaultText;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setText(String text) {
        this.inputText = text;
    }

    public String getText() {
        return inputText;
    }
}
