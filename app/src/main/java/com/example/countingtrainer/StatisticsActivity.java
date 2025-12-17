package com.example.countingtrainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private TextView statsTextView;
    private StatsManager statsManager;
    private SharedPreferences prefs;
    private Button backButton, resetStatsButton;
    private LinearLayout achievementsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        statsTextView = findViewById(R.id.stats_text_view);
        backButton = findViewById(R.id.back_button);
        resetStatsButton = findViewById(R.id.reset_stats_button);
        achievementsLayout = findViewById(R.id.achievements_layout);

        statsManager = new StatsManager(this);
        prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);

        displayStatistics();
        displayAchievements();

        setupButtonListeners();
    }

    private void displayStatistics() {
        StringBuilder stats = new StringBuilder();

        stats.append("📊 ПОДРОБНАЯ СТАТИСТИКА\n\n");

        // Общая информация
        stats.append("🎮 ОБЩАЯ ИНФОРМАЦИЯ\n");
        stats.append("• Сессий сыграно: ").append(statsManager.getSessionsCount()).append("\n");
        stats.append("• Общее время игры: ").append(formatTime(statsManager.getTotalTime())).append("\n");
        stats.append("• Среднее время на игру: ").append(formatTime(calculateAverageTime())).append("\n");
        stats.append("• Лучший результат: ").append(statsManager.getBestScore()).append("%\n");
        stats.append("• Высший достигнутый уровень: ").append(statsManager.getHighestLevel()).append("\n");
        stats.append("• Текущий уровень: ").append(prefs.getInt("current_level", 1)).append("\n\n");

        // Результаты по уровням
        stats.append("📈 РЕЗУЛЬТАТЫ ПО УРОВНЯМ\n");
        int highestLevel = statsManager.getHighestLevel();
        if (highestLevel > 0) {
            for (int i = 1; i <= Math.min(highestLevel, 5); i++) {
                int result = statsManager.getLastLevelResult(i);
                String progressBar = createProgressBar(result);

                stats.append("Уровень ").append(i).append(": ").append(getLevelDescription(i)).append("\n");
                stats.append("  Последний результат: ").append(result > 0 ? result + "%" : "Еще не пройден").append("\n");
                if (result > 0) {
                    stats.append("  ").append(progressBar).append("\n");
                }
                stats.append("\n");
            }
        } else {
            stats.append("  Еще нет пройденных уровней\n\n");
        }

        // Прогресс обучения
        stats.append("🎯 ПРОГРЕСС ОБУЧЕНИЯ\n");
        int progressPercentage = calculateLearningProgress();
        stats.append("  ").append(createProgressBar(progressPercentage)).append(" ").append(progressPercentage).append("%\n\n");

        // Последние 10 результатов
        stats.append("📝 ПОСЛЕДНИЕ РЕЗУЛЬТАТЫ\n");
        int[] last = statsManager.getLastResults();
        if (last.length == 0) {
            stats.append("  Пока нет данных\n\n");
        } else {
            for (int i = last.length - 1, idx = 1; i >= 0; i--, idx++) {
                stats.append("  #").append(idx).append(": ").append(last[i]).append("%\n");
            }
            stats.append("\n");
        }

        // Советы
        stats.append("💡 СОВЕТЫ ДЛЯ УЛУЧШЕНИЯ\n");
        stats.append(getImprovementTips()).append("\n");

        // Дата последней игры
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        stats.append("\n📅 Статистика обновлена: ").append(sdf.format(new Date()));

        statsTextView.setText(stats.toString());
    }

    private void displayAchievements() {
        achievementsLayout.removeAllViews();

        int sessions = statsManager.getSessionsCount();
        int bestScore = statsManager.getBestScore();
        int highestLevel = statsManager.getHighestLevel();
        long totalTime = statsManager.getTotalTime();

        if (bestScore >= 100) {
            addAchievementView("🏆 ИДЕАЛЬНЫЙ РЕЗУЛЬТАТ", "Получено 100% правильных ответов");
        }
        if (bestScore >= 90) {
            addAchievementView("⭐ ОТЛИЧНИК", "Результат 90% или выше");
        }
        if (sessions >= 10) {
            addAchievementView("🎮 ПОСТОЯННЫЙ ИГРОК", "10+ игровых сессий");
        }
        if (sessions >= 25) {
            addAchievementView("👑 МАСТЕР ИГРЫ", "25+ игровых сессий");
        }
        if (totalTime >= 300) { // 5 минут
            addAchievementView("⏱ МАСТЕР ВРЕМЕНИ", "5+ минут в игре");
        }
        if (totalTime >= 1800) { // 30 минут
            addAchievementView("🧠 ПРОФЕССИОНАЛ", "30+ минут в игре");
        }
        if (highestLevel >= 3) {
            addAchievementView("📚 СРЕДНИЙ УРОВЕНЬ", "Достигнут 3 уровень");
        }
        if (highestLevel >= 5) {
            addAchievementView("🚀 ЭКСПЕРТ", "Достигнут максимальный уровень");
        }

        if (achievementsLayout.getChildCount() == 0) {
            LinearLayout noAchievementsCard = new LinearLayout(this);
            noAchievementsCard.setOrientation(LinearLayout.VERTICAL);
            noAchievementsCard.setPadding(32, 32, 32, 32);
            noAchievementsCard.setBackgroundResource(R.drawable.stats_card);
            noAchievementsCard.setGravity(android.view.Gravity.CENTER);
            
            TextView noAchievements = new TextView(this);
            noAchievements.setText("🏁 Достижения появятся здесь после игры");
            noAchievements.setTextSize(16);
            noAchievements.setTextColor(getResources().getColor(R.color.text_secondary));
            noAchievements.setGravity(android.view.Gravity.CENTER);
            noAchievementsCard.addView(noAchievements);
            
            achievementsLayout.addView(noAchievementsCard);
        }
    }

    private void addAchievementView(String title, String description) {
        LinearLayout achievementCard = new LinearLayout(this);
        achievementCard.setOrientation(LinearLayout.VERTICAL);
        achievementCard.setPadding(20, 16, 20, 16);
        achievementCard.setBackgroundResource(R.drawable.achievement_card);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        achievementCard.setLayoutParams(params);
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(getResources().getColor(android.R.color.black));
        titleView.setPadding(0, 0, 0, 8);
        achievementCard.addView(titleView);
        
        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(14);
        descView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        achievementCard.addView(descView);
        
        achievementsLayout.addView(achievementCard);
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + " сек";

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes < 60) return minutes + " мин " + remainingSeconds + " сек";

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + " ч " + remainingMinutes + " мин";
    }

    private long calculateAverageTime() {
        int sessions = statsManager.getSessionsCount();
        return sessions > 0 ? statsManager.getTotalTime() / sessions : 0;
    }

    private String createProgressBar(int percentage) {
        int filled = percentage / 10;
        int empty = 10 - filled;
        return "[" + "█".repeat(filled) + "░".repeat(empty) + "]";
    }

    private String getLevelDescription(int level) {
        switch (level) {
            case 1: return "Начинающий (+, -)";
            case 2: return "Базовый (+, -, ×)";
            case 3: return "Средний (+, -, ×, ÷)";
            case 4: return "Продвинутый (2 действия)";
            case 5: return "Эксперт (3 действия)";
            default: return "Неизвестный уровень";
        }
    }

    private int calculateLearningProgress() {
        int highestLevel = statsManager.getHighestLevel();
        int totalResults = 0;
        int count = 0;

        for (int i = 1; i <= highestLevel; i++) {
            int result = statsManager.getLastLevelResult(i);
            if (result > 0) {
                totalResults += result;
                count++;
            }
        }

        if (count == 0) return 0;
        return totalResults / count;
    }

    private String getImprovementTips() {
        int bestScore = statsManager.getBestScore();

        if (bestScore < 60) {
            return "• Тренируйте базовые операции сложения и вычитания\n• Не спешите, время у вас есть\n• Проверяйте ответы перед отправкой";
        } else if (bestScore < 80) {
            return "• Освойте умножение и деление\n• Обращайте внимание на приоритет операций\n• Используйте пошаговое решение при ошибках";
        } else if (bestScore < 90) {
            return "• Практикуйте сложные выражения\n• Улучшайте скорость счета\n• Развивайте внимательность";
        } else {
            return "• Вы отлично справляетесь!\n• Помогайте другим учиться\n• Создайте свои сложные примеры";
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> finish());

        resetStatsButton.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Сброс статистики")
                    .setMessage("Вы уверены, что хотите сбросить всю статистику? Это действие нельзя отменить.")
                    .setPositiveButton("Сбросить", (dialog, which) -> {
                        statsManager.resetAllStats();
                        prefs.edit().putInt("current_level", 1).apply();
                        displayStatistics();
                        displayAchievements();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Долгое нажатие на кнопку "Назад" возвращает в главное меню
        backButton.setOnLongClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}