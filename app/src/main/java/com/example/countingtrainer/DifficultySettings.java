package com.example.countingtrainer;

public class DifficultySettings {
    private int maxNumber;
    private String[] operations;
    private int maxActions;
    private int minActions;
    private boolean allowParentheses;
    private int timePerQuestion;

    public DifficultySettings(int maxNumber, String[] operations, int maxActions,
                              boolean allowParentheses, int timePerQuestion) {
        this(maxNumber, operations, maxActions, 1, allowParentheses, timePerQuestion);
    }

    public DifficultySettings(int maxNumber, String[] operations, int maxActions,
                              int minActions, boolean allowParentheses, int timePerQuestion) {
        this.maxNumber = maxNumber;
        this.operations = operations;
        this.maxActions = maxActions;
        this.minActions = minActions;
        this.allowParentheses = allowParentheses;
        this.timePerQuestion = timePerQuestion;
    }

    public int getMaxNumber() { return maxNumber; }
    public String[] getOperations() { return operations; }
    public int getMaxActions() { return maxActions; }
    public int getMinActions() { return minActions; }
    public boolean getAllowParentheses() { return allowParentheses; }
    public int getTimePerQuestion() { return timePerQuestion; }
}