package javax.security.auth.callback;

import java.io.Serializable;

public class ConfirmationCallback implements Callback, Serializable {
    private static final long serialVersionUID = -9095656433782481668L;

    public static final int UNSPECIFIED_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    public static final int YES = 0;
    public static final int NO = 1;
    public static final int CANCEL = 2;
    public static final int OK = 3;

    public static final int INFORMATION = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    private String prompt;
    private int messageType;
    private int optionType = UNSPECIFIED_OPTION;
    private int defaultOption;
    private String[] options;
    private int selection;

    public ConfirmationCallback(int messageType, int optionType, int defaultOption) {
        this.messageType = messageType;
        this.optionType = optionType;
        this.defaultOption = defaultOption;
    }

    public ConfirmationCallback(int messageType, String[] options, int defaultOption) {
        this.messageType = messageType;
        this.options = options;
        this.defaultOption = defaultOption;
    }

    public ConfirmationCallback(String prompt, int messageType, int optionType, int defaultOption) {
        this.prompt = prompt;
        this.messageType = messageType;
        this.optionType = optionType;
        this.defaultOption = defaultOption;
    }

    public ConfirmationCallback(String prompt, int messageType, String[] options, int defaultOption) {
        this.prompt = prompt;
        this.messageType = messageType;
        this.options = options;
        this.defaultOption = defaultOption;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getOptionType() {
        return optionType;
    }

    public String[] getOptions() {
        return options;
    }

    public int getDefaultOption() {
        return defaultOption;
    }

    public void setSelectedIndex(int selection) {
        this.selection = selection;
    }

    public int getSelectedIndex() {
        return selection;
    }
}
