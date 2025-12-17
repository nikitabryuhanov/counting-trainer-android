package com.example.countingtrainer;

public class Level {
    private int levelNumber;
    private DifficultySettings difficulty;

    public Level(int levelNumber) {
        this.levelNumber = levelNumber;
        this.difficulty = getDifficultyForLevel(levelNumber);
    }

    private DifficultySettings getDifficultyForLevel(int levelNum) {
        switch (levelNum) {
            case 1: // Базовый: + и - до 10, одно действие
                return new DifficultySettings(
                        10,
                        new String[]{"+", "-"},
                        1,
                        false,
                        30
                );

            case 2: // Расширенный: +, -, * до 20, одно действие
                return new DifficultySettings(
                        20,
                        new String[]{"+", "-", "*"},
                        1,
                        false,
                        25
                );

            case 3: // Продвинутый: +, -, *, / до 30, одно действие
                return new DifficultySettings(
                        30,
                        new String[]{"+", "-", "*", "/"},
                        1,
                        false,
                        20
                );

            case 4: // Два действия с приоритетами
                return new DifficultySettings(
                        40,
                        new String[]{"+", "-", "*", "/"},
                        2,
                        2, // Минимум 2 действия
                        true,
                        15
                );

            case 5: // Три действия, большие числа
                return new DifficultySettings(
                        60,
                        new String[]{"+", "-", "*", "/"},
                        3,
                        3, // Минимум 3 действия
                        true,
                        10
                );

            default: // По умолчанию - уровень 1
                return new DifficultySettings(10, new String[]{"+", "-"}, 1, false, 30);
        }
    }

    public int getLevelNumber() { return levelNumber; }
    public int getMaxNumber() { return difficulty.getMaxNumber(); }
    public String[] getOperations() { return difficulty.getOperations(); }
    public int getActionsCount() { return difficulty.getMaxActions(); }
    public int getMinActions() { return difficulty.getMinActions(); }
    public boolean getAllowParentheses() { return difficulty.getAllowParentheses(); }
    public int getTimePerQuestion() { return difficulty.getTimePerQuestion(); }
}