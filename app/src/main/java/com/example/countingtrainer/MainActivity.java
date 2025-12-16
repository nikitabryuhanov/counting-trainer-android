package com.example.countingtrainer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private RadioGroup difficultyGroup;
    private Button startButton, exitButton;
    private int selectedDifficulty = 1; // 1 = легко, 2 = средне, 3 = сложно
    private boolean preschoolMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        difficultyGroup = findViewById(R.id.difficulty_group);
        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);

        // Слушатель для выбора сложности
        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            preschoolMode = false;
            if (checkedId == R.id.radio_easy) {
                selectedDifficulty = 1;
            } else if (checkedId == R.id.radio_medium) {
                selectedDifficulty = 2;
            } else if (checkedId == R.id.radio_hard) {
                selectedDifficulty = 3;
            } else if (checkedId == R.id.radio_preschool) {
                preschoolMode = true;
                selectedDifficulty = 1; // базовый уровень как основа
            }
        });

        // Кнопка "Начать игру"
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("difficulty", selectedDifficulty);
            intent.putExtra("preschool_mode", preschoolMode);
            startActivity(intent);
        });

        // Кнопка "Выход"
        exitButton.setOnClickListener(v -> finish());
    }
}
