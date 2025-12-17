package com.example.countingtrainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LevelUpActivity extends AppCompatActivity {

    private TextView levelUpText, congratsText, statsText;
    private Button nextLevelButton, mainMenuButton, statsButton;
    private SharedPreferences prefs;
    private StatsManager statsManager;
    private int newLevel;
    private int currentLevel;
    private int correctAnswers;
    private long totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_up);

        prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        statsManager = new StatsManager(this);

        // Получаем данные из Intent
        currentLevel = getIntent().getIntExtra("current_level", 1);
        correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        totalTime = getIntent().getLongExtra("total_time", 0);
        newLevel = currentLevel + 1;

        if (newLevel > 5) {
            newLevel = 5; // Максимальный уровень
        }

        // Инициализация View
        levelUpText = findViewById(R.id.level_up_text);
        congratsText = findViewById(R.id.level_up_congrats);
        statsText = findViewById(R.id.level_up_stats);
        nextLevelButton = findViewById(R.id.next_level_button);
        mainMenuButton = findViewById(R.id.level_up_main_menu_button);
        statsButton = findViewById(R.id.level_up_stats_button);

        updateUI();
        setupButtonListeners();
    }

    private void updateUI() {
        if (newLevel <= 5) {
            levelUpText.setText("🎊 Уровень " + currentLevel + " пройден!");
            congratsText.setText("Поздравляем! Вы переходите на уровень " + newLevel + "!");
            statsText.setText("✅ Правильных ответов: " + correctAnswers + "\n" +
                    "⏱ Время: " + formatTime(totalTime));
        } else {
            levelUpText.setText("🎊 Все уровни пройдены!");
            congratsText.setText("Вы достигли максимального уровня!");
            statsText.setText("✅ Правильных ответов: " + correctAnswers + "\n" +
                    "⏱ Время: " + formatTime(totalTime));
            nextLevelButton.setEnabled(false);
            nextLevelButton.setText("МАКСИМАЛЬНЫЙ УРОВЕНЬ");
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " сек";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + " мин " + remainingSeconds + " сек";
        }
    }

    private void setupButtonListeners() {
        nextLevelButton.setOnClickListener(v -> {
            // Сохраняем новый уровень
            if (newLevel <= 5) {
                prefs.edit().putInt("current_level", newLevel).apply();
                statsManager.setHighestLevel(newLevel);
            }
            
            // Запускаем новую игру с новым уровнем
            Intent intent = new Intent(LevelUpActivity.this, GameActivity.class);
            intent.putExtra("difficulty", newLevel);
            startActivity(intent);
            finish();
        });

        mainMenuButton.setOnClickListener(v -> {
            // Сохраняем новый уровень
            if (newLevel <= 5) {
                prefs.edit().putInt("current_level", newLevel).apply();
                statsManager.setHighestLevel(newLevel);
            }
            
            Intent intent = new Intent(LevelUpActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(LevelUpActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // При нажатии назад возвращаемся в главное меню
        if (newLevel <= 5) {
            prefs.edit().putInt("current_level", newLevel).apply();
            statsManager.setHighestLevel(newLevel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}

