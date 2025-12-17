package com.example.countingtrainer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private RadioGroup difficultyGroup;
    private RadioButton radioEasy, radioMedium, radioHard;
    private Button startButton, exitButton;
    private int selectedDifficulty = 1; // 1 = легко, 2 = средне, 3 = сложно

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        difficultyGroup = findViewById(R.id.difficulty_group);
        radioEasy = findViewById(R.id.radio_easy);
        radioMedium = findViewById(R.id.radio_medium);
        radioHard = findViewById(R.id.radio_hard);
        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);

        // Устанавливаем начальный цвет для выбранной опции
        updateDifficultyColors();

        // Слушатель для выбора сложности
        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_easy) {
                selectedDifficulty = 1;
            } else if (checkedId == R.id.radio_medium) {
                selectedDifficulty = 2;
            } else if (checkedId == R.id.radio_hard) {
                selectedDifficulty = 3;
            }
            updateDifficultyColors();
        });

        // Кнопка "Начать игру"
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("difficulty", selectedDifficulty);
            startActivity(intent);
        });

        // Кнопка "Выход"
        exitButton.setOnClickListener(v -> finish());
    }

    private void updateDifficultyColors() {
        // Сбрасываем цвета всех кнопок
        radioEasy.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        radioMedium.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        radioHard.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        // Устанавливаем цвет для выбранной кнопки
        if (radioEasy.isChecked()) {
            radioEasy.setTextColor(ContextCompat.getColor(this, R.color.easy_color));
        } else if (radioMedium.isChecked()) {
            radioMedium.setTextColor(ContextCompat.getColor(this, R.color.medium_color));
        } else if (radioHard.isChecked()) {
            radioHard.setTextColor(ContextCompat.getColor(this, R.color.hard_color));
        }
    }
}
