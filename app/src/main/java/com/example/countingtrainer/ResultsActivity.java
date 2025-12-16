package com.example.countingtrainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class ResultsActivity extends AppCompatActivity {

    private TextView correctAnswersText, timeSpentText, scoreText, levelText, congratsText;
    private Button newGameButton, statsButton, mainMenuButton;
    private ProgressBar resultProgressBar;
    private StatsManager statsManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Инициализация всех View
        correctAnswersText = findViewById(R.id.correct_answers_final);
        timeSpentText = findViewById(R.id.time_spent_final);
        scoreText = findViewById(R.id.score_text);
        levelText = findViewById(R.id.level_text);
        congratsText = findViewById(R.id.congrats_text);
        newGameButton = findViewById(R.id.new_game_button);
        statsButton = findViewById(R.id.stats_button);
        mainMenuButton = findViewById(R.id.main_menu_button);
        resultProgressBar = findViewById(R.id.result_progress_bar);

        statsManager = new StatsManager(this);
        prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);

        // Получаем данные из Intent
        int correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        int wrongAnswers = getIntent().getIntExtra("wrong_answers", 0);
        int totalAnswers = getIntent().getIntExtra("total_answers", correctAnswers + wrongAnswers);
        long totalTime = getIntent().getLongExtra("total_time", 0);
        int currentLevel = getIntent().getIntExtra("level", prefs.getInt("current_level", 1));

        if (totalAnswers == 0) totalAnswers = 1; // защита от деления на ноль

        // Рассчитываем оценку и рейтинг
        int score = calculateScore(correctAnswers, totalAnswers);
        String rating = getRating(score);

        // Обновляем UI
        updateUI(correctAnswers, wrongAnswers, totalAnswers, totalTime, score, rating, currentLevel);

        // Обновляем статистику
        updateStatistics(correctAnswers, totalTime, score, currentLevel);

        // Показываем достижения
        showAchievements(score, correctAnswers, totalTime);

        // Настраиваем обработчики кнопок
        setupButtonListeners();
    }

    private int calculateScore(int correctAnswers, int totalAnswers) {
        if (totalAnswers <= 0) return 0;
        return (correctAnswers * 100) / totalAnswers;
    }

    private String getRating(int score) {
        if (score >= 90) return "Отлично!";
        else if (score >= 70) return "Хорошо";
        else if (score >= 50) return "Удовлетворительно";
        else return "Попробуйте еще";
    }

    private void updateUI(int correctAnswers, int wrongAnswers, int totalAnswers, long totalTime, int score, String rating, int level) {
        correctAnswersText.setText("✅ Правильных: " + correctAnswers + " / " + totalAnswers +
                "  |  Ошибок: " + wrongAnswers);
        timeSpentText.setText("⏱ Время: " + formatTime(totalTime));
        scoreText.setText("🎯 Оценка: " + score + "% (" + rating + ")");
        levelText.setText("📊 Текущий уровень: " + level);
        resultProgressBar.setProgress(score);

        // Динамическое приветствие
        if (score == 100) {
            congratsText.setText("🎉 Идеально! Вы - гений математики!");
            congratsText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (score >= 90) {
            congratsText.setText("👍 Отличный результат!");
            congratsText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (score >= 70) {
            congratsText.setText("👏 Хорошая работа!");
            congratsText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            congratsText.setText("💪 Практика делает мастера!");
            congratsText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " секунд";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + " мин " + remainingSeconds + " сек";
        }
    }

    private void updateStatistics(int correctAnswers, long totalTime, int score, int currentLevel) {
        // Обновляем статистику
        statsManager.incrementSessionsCount();
        statsManager.addTotalTime(totalTime);

        // Обновляем лучший результат, если текущий лучше
        if (score > statsManager.getBestScore()) {
            statsManager.setBestScore(score);
        }

        // Сохраняем результат для текущего уровня
        statsManager.setLastLevelResult(currentLevel, score);
        statsManager.addResult(score);

        // Автоматическое повышение уровня при отличных результатах
        if (score >= 90 && currentLevel < 5) {
            int newLevel = currentLevel + 1;
            prefs.edit().putInt("current_level", newLevel).apply();
            statsManager.setHighestLevel(newLevel);

            // Показываем сообщение о повышении уровня
            levelText.append("\n🎊 Повышен до уровня " + newLevel + "!");
        }

        // Обновляем высший уровень, если текущий выше
        if (currentLevel > statsManager.getHighestLevel()) {
            statsManager.setHighestLevel(currentLevel);
        }
    }

    private void showAchievements(int score, int correctAnswers, long totalTime) {
        StringBuilder achievements = new StringBuilder("\n🏆 Достижения:\n");

        if (score == 100) {
            achievements.append("⭐ Идеальный результат (100%)\n");
        }
        if (score >= 90) {
            achievements.append("⭐ Отличник (90%+)\n");
        }
        if (correctAnswers >= 8) {
            achievements.append("⭐ Серия правильных ответов\n");
        }
        if (totalTime < 60) {
            achievements.append("⭐ Быстрая игра (< 1 минуты)\n");
        }
        if (statsManager.getSessionsCount() >= 5) {
            achievements.append("⭐ Постоянный игрок\n");
        }

        // Добавляем достижения в levelText
        if (achievements.length() > "\n🏆 Достижения:\n".length()) {
            levelText.append(achievements.toString());
        }
    }

    private void setupButtonListeners() {
        newGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, GameActivity.class);
            startActivity(intent);
            finish();
        });

        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        mainMenuButton.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Долгое нажатие на кнопку "Новая игра" сбрасывает уровень к 1
        newGameButton.setOnLongClickListener(v -> {
            prefs.edit().putInt("current_level", 1).apply();
            Intent intent = new Intent(ResultsActivity.this, GameActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}