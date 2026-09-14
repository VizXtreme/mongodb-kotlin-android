package javax.security.auth.callback;

import java.io.Serializable;

public class TextOutputCallback implements Callback, Serializable {
    private static final long serialVersionUID = 0L;

    public static final int INFORMATION = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;

    private int messageType;
    private String message;

    public TextOutputCallback(int messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }
}
