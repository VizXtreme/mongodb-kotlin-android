package javax.security.auth.callback;

import java.io.Serializable;

public class ChoiceCallback implements Callback, Serializable {
    private static final long serialVersionUID = -3975664071579892167L;

    private String prompt;
    private String[] choices;
    private int defaultChoice;
    private boolean multipleSelectionsAllowed;
    private int[] selections;

    public ChoiceCallback(String prompt, String[] choices, int defaultChoice, boolean multipleSelectionsAllowed) {
        this.prompt = prompt;
        this.choices = choices;
        this.defaultChoice = defaultChoice;
        this.multipleSelectionsAllowed = multipleSelectionsAllowed;
    }

    public String getPrompt() {
        return prompt;
    }

    public String[] getChoices() {
        return choices;
    }

    public int getDefaultChoice() {
        return defaultChoice;
    }

    public boolean allowMultipleSelections() {
        return multipleSelectionsAllowed;
    }

    public void setSelectedIndex(int selection) {
        this.selections = new int[]{selection};
    }

    public void setSelectedIndexes(int[] selections) {
        this.selections = selections;
    }

    public int[] getSelectedIndexes() {
        return selections;
    }
}
