package javax.security.auth.callback;

import java.io.Serializable;
import java.util.Arrays;

public class PasswordCallback implements Callback, Serializable {
    private static final long serialVersionUID = 2267422647454909926L;

    private String prompt;
    private boolean echoOn;
    private char[] inputPassword;

    public PasswordCallback(String prompt, boolean echoOn) {
        this.prompt = prompt;
        this.echoOn = echoOn;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isEchoOn() {
        return echoOn;
    }

    public void setPassword(char[] password) {
        this.inputPassword = password;
    }

    public char[] getPassword() {
        return inputPassword;
    }

    public void clearPassword() {
        if (inputPassword != null) {
            Arrays.fill(inputPassword, ' ');
        }
    }
}
