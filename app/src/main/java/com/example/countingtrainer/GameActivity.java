package com.example.countingtrainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private boolean isAnswerButtonLocked = false;
    private long lastClickTime = 0;
    private static final long MIN_CLICK_INTERVAL = 500;
    private TextView questionText, feedbackText, timerText, correctAnswersText, countdownTimerText;
    private ProgressBar progressBar;
    private EditText answerInput;
    private Button checkButton, yesButton, noButton;
    private ImageButton pauseButton;
    private ImageView heart1, heart2, heart3;
    private LinearLayout answerInputLayout, answerButtonsLayout, pauseMenuLayout, mainContentLayout;
    private LinearLayout solutionOverlay;
    private TextView solutionExpression, solutionSteps, solutionAnswer;
    private Button solutionContinueButton;

    private SharedPreferences prefs;
    private Level currentLevel;
    private int consecutiveCorrect = 0;

    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private int questionsAnswered = 0;
    private long startTime;
    private int currentAnswer;
    private int questionType;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private ExpressionGenerator expressionGenerator = new ExpressionGenerator();
    private ExpressionGenerator.Expression lastExpression;

    private int timePerQuestion;
    private int timeLeft;
    private Runnable countdownRunnable;
    private MediaPlayer warningSound;

    private int lives = 3;
    private boolean isPaused = false;
    private boolean isGameEnded = false;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;
    private Runnable solutionAutoHideRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        int savedLevel = prefs.getInt("current_level", 1);
        int requestedLevel = getIntent().getIntExtra("difficulty", savedLevel);
        currentLevel = new Level(requestedLevel);
        // фиксируем выбранный уровень, чтобы он сохранялся между сессиями
        prefs.edit().putInt("current_level", currentLevel.getLevelNumber()).apply();

        questionText = findViewById(R.id.question_text);
        feedbackText = findViewById(R.id.feedback_text);
        timerText = findViewById(R.id.timer_text);
        countdownTimerText = findViewById(R.id.countdown_timer_text);
        correctAnswersText = findViewById(R.id.correct_answers_text);
        progressBar = findViewById(R.id.progress_bar);
        answerInput = findViewById(R.id.answer_input);
        checkButton = findViewById(R.id.check_button);
        yesButton = findViewById(R.id.yes_button);
        noButton = findViewById(R.id.no_button);
        pauseButton = findViewById(R.id.pause_button);
        heart1 = findViewById(R.id.heart1);
        heart2 = findViewById(R.id.heart2);
        heart3 = findViewById(R.id.heart3);
        answerInputLayout = findViewById(R.id.answer_input_layout);
        answerButtonsLayout = findViewById(R.id.answer_buttons_layout);
        pauseMenuLayout = findViewById(R.id.pause_menu_layout);
        mainContentLayout = findViewById(R.id.main_content_layout);
        solutionOverlay = findViewById(R.id.solution_overlay);
        solutionExpression = findViewById(R.id.solution_expression);
        solutionSteps = findViewById(R.id.solution_steps);
        solutionAnswer = findViewById(R.id.solution_answer);
        solutionContinueButton = findViewById(R.id.solution_continue_button);

        startTime = System.currentTimeMillis();
        updateHearts();

        setupProgressBar();
        generateNewQuestion();

        setupButtonListeners();
        updateTimer();
    }

    private void setupButtonListeners() {
        checkButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MIN_CLICK_INTERVAL || isAnswerButtonLocked || isPaused) {
                return;
            }
            lastClickTime = currentTime;

            if (!isAnswerButtonLocked) {
                isAnswerButtonLocked = true;
                checkInputAnswer();
            }
        });

        yesButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MIN_CLICK_INTERVAL || isAnswerButtonLocked || isPaused) {
                return;
            }
            lastClickTime = currentTime;

            if (!isAnswerButtonLocked) {
                isAnswerButtonLocked = true;
                checkYesNoAnswer(true);
            }
        });

        noButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MIN_CLICK_INTERVAL || isAnswerButtonLocked || isPaused) {
                return;
            }
            lastClickTime = currentTime;

            if (!isAnswerButtonLocked) {
                isAnswerButtonLocked = true;
                checkYesNoAnswer(false);
            }
        });

        pauseButton.setOnClickListener(v -> togglePause());

        // Кнопки в меню паузы
        findViewById(R.id.resume_button).setOnClickListener(v -> togglePause());
        findViewById(R.id.restart_button).setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, GameActivity.class);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.exit_to_menu_button).setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        solutionContinueButton.setOnClickListener(v -> proceedToNextQuestion());
    }

    private void togglePause() {
        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    private void pauseGame() {
        isPaused = true;
        pauseStartTime = System.currentTimeMillis();
        
        // Применяем эффект размытия/затемнения к основному контенту
        applyBlurEffect(true);
        
        pauseMenuLayout.setVisibility(View.VISIBLE);
        pauseButton.setImageResource(R.drawable.ic_play);
        stopCountdownTimer();
    }

    private void resumeGame() {
        isPaused = false;
        totalPausedTime += System.currentTimeMillis() - pauseStartTime;
        
        // Убираем эффект размытия
        applyBlurEffect(false);
        
        pauseMenuLayout.setVisibility(View.GONE);
        pauseButton.setImageResource(R.drawable.ic_pause);
        
        // Генерируем новый вопрос при возобновлении
        generateNewQuestion();
    }
    
    private void applyBlurEffect(boolean enable) {
        if (mainContentLayout == null) return;
        
        if (enable) {
            // Применяем эффект размытия через уменьшение alpha и затемнение
            // Это скрывает содержимое, делая его практически невидимым
            mainContentLayout.setAlpha(0.05f);
            // Дополнительно делаем view неинтерактивным
            mainContentLayout.setEnabled(false);
        } else {
            // Возвращаем нормальную прозрачность
            mainContentLayout.setAlpha(1.0f);
            mainContentLayout.setEnabled(true);
        }
    }

    private void updateHearts() {
        heart1.setImageResource(lives >= 1 ? R.drawable.heart_full : R.drawable.heart_empty);
        heart2.setImageResource(lives >= 2 ? R.drawable.heart_full : R.drawable.heart_empty);
        heart3.setImageResource(lives >= 3 ? R.drawable.heart_full : R.drawable.heart_empty);
    }

    private void loseLife() {
        lives--;
        updateHearts();

        if (lives <= 0) {
            endGame();
        }
    }

    private void setTimePerQuestion() {
        if (isFinishing() || isGameEnded) return;
        // Используем время из настроек сложности
        timePerQuestion = currentLevel.getTimePerQuestion();
        timeLeft = timePerQuestion;
        if (countdownTimerText != null) {
            countdownTimerText.setText("Время осталось: " + timeLeft + "с");
            countdownTimerText.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private void startCountdownTimer() {
        if (isPaused || isGameEnded || isFinishing()) return;
        if (countdownTimerText == null) return;

        handler.removeCallbacks(countdownRunnable);
        timeLeft = timePerQuestion;
        countdownTimerText.setText("Время осталось: " + timeLeft + "с");
        countdownTimerText.setTextColor(getResources().getColor(android.R.color.black));

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isGameEnded) return;
                if (countdownTimerText == null || correctAnswersText == null) return;

                if (isPaused) {
                    handler.postDelayed(this, 1000);
                    return;
                }

                if (timeLeft > 0) {
                    timeLeft--;
                    countdownTimerText.setText("Время осталось: " + timeLeft + "с");
                    if (timeLeft <= 5) {
                        countdownTimerText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        // Воспроизводим звук предупреждения в последние 5 секунд
                        playWarningSound();
                    }
                    handler.postDelayed(this, 1000);
                } else {
                    // При истечении времени отнимаем жизнь
                    wrongAnswers++;
                    consecutiveCorrect = 0;
                    if (correctAnswersText != null) {
                        correctAnswersText.setText("Правильно: " + correctAnswers + " | Ошибок: " + wrongAnswers);
                    }
                    loseLife();
                    if (lives > 0 && !isGameEnded) {
                        // Показываем решение только если остались жизни, затем генерируем новый вопрос
                        showSolutionOverlay("Время вышло!", lastExpression != null ? lastExpression : new ExpressionGenerator.Expression("—", currentAnswer, "—"), currentAnswer);
                    }
                    // Если жизни закончились, loseLife() вызовет endGame()
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void playWarningSound() {
        if (isFinishing() || isGameEnded) return;
        try {
            if (warningSound == null) {
                warningSound = MediaPlayer.create(this, R.raw.time_warning);
                if (warningSound != null) {
                    // Настраиваем зацикливание звука для непрерывного предупреждения
                    warningSound.setLooping(false);
                }
            }
            if (warningSound != null && !warningSound.isPlaying()) {
                warningSound.start();
            }
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }

    private void stopCountdownTimer() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        // Останавливаем звук предупреждения
        stopWarningSound();
    }
    
    private void stopWarningSound() {
        if (warningSound != null && warningSound.isPlaying()) {
            try {
                warningSound.stop();
                warningSound.reset();
            } catch (Exception e) {
                // Игнорируем ошибки остановки звука
            }
        }
    }

    private void generateNewQuestion() {
        if (isFinishing() || isGameEnded) return;
        // Разблокируем кнопки для нового вопроса
        isAnswerButtonLocked = false;
        
        // Останавливаем звук предупреждения при переходе к новому вопросу
        stopWarningSound();

        if (feedbackText != null) {
            feedbackText.setText("");
            feedbackText.setTextColor(getResources().getColor(android.R.color.black));
        }
        if (answerInput != null) {
            answerInput.setText("");
        }
        setTimePerQuestion();
        startCountdownTimer();
        updateProgressBar();

        questionType = random.nextInt(2);

        if (questionType == 0) {
            answerInputLayout.setVisibility(LinearLayout.VISIBLE);
            answerButtonsLayout.setVisibility(LinearLayout.GONE);
            generateArithmeticQuestion();
        } else {
            answerInputLayout.setVisibility(LinearLayout.GONE);
            answerButtonsLayout.setVisibility(LinearLayout.VISIBLE);
            generateTrueFalseQuestion();
        }
    }

    private void generateArithmeticQuestion() {
        int max = currentLevel.getMaxNumber();
        String[] ops = currentLevel.getOperations();
        int maxActions = currentLevel.getActionsCount();
        int minActions = currentLevel.getMinActions();
        boolean allowParentheses = currentLevel.getAllowParentheses();

        // Выбираем случайное количество действий от minActions до maxActions
        int actions = random.nextInt(maxActions - minActions + 1) + minActions;

        if (actions == 1) {
            generateSingleActionQuestion(max, ops);
        } else if (actions == 2) {
            generateTwoActionQuestion(max, ops, allowParentheses);
        } else if (actions == 3) {
            generateThreeActionQuestion(max, ops, allowParentheses);
        }
    }

    private void generateSingleActionQuestion(int max, String[] ops) {
        int num1 = random.nextInt(max) + 1;
        int num2 = random.nextInt(max) + 1;
        String op = ops[random.nextInt(ops.length)];

        // Для деления убеждаемся, что результат целый
        if (op.equals("/")) {
            if (num2 == 0) num2 = 1;
            num1 = num1 * num2; // Делаем деление нацело
        }

        // Для вычитания на легком уровне результат не должен быть отрицательным
        if (op.equals("-") && currentLevel.getLevelNumber() <= 2) {
            if (num1 < num2) {
                int temp = num1;
                num1 = num2;
                num2 = temp;
            }
        }

        String exprStr = num1 + " " + op + " " + num2;
        int res = calculate(num1, num2, op);
        lastExpression = new ExpressionGenerator.Expression(exprStr, res, exprStr + " = " + res);
        questionText.setText(exprStr + " = ?");
        currentAnswer = res;
    }

    private void generateTwoActionQuestion(int max, String[] ops, boolean allowParentheses) {
        int attempts = 0;
        int maxAttempts = 15;

        while (attempts < maxAttempts) {
            attempts++;

            String op1 = ops[random.nextInt(ops.length)];
            String op2 = ops[random.nextInt(ops.length)];

            int num1 = random.nextInt(max) + 1;
            int num2 = random.nextInt(max) + 1;
            int num3 = random.nextInt(max) + 1;

            // Проверяем ограничения для легких уровней
            if (currentLevel.getLevelNumber() <= 2) {
                // На легком уровне только + и -
                if (!op1.equals("+") && !op1.equals("-")) continue;
                if (!op2.equals("+") && !op2.equals("-")) continue;
            }

            // Проверяем деление
            if (op1.equals("/") && (num2 == 0 || num1 % num2 != 0)) {
                continue;
            }

            int intermediate;
            try {
                intermediate = calculate(num1, num2, op1);
            } catch (ArithmeticException e) {
                continue;
            }

            // Проверяем второе действие
            if (op2.equals("/") && (num3 == 0 || intermediate % num3 != 0)) {
                continue;
            }

            // Для вычитания на легких уровнях не должно быть отрицательных
            if (currentLevel.getLevelNumber() <= 2) {
                if (op1.equals("-") && num1 < num2) continue;
                if (op2.equals("-") && intermediate < num3) continue;
            }

            int result;
            try {
                result = calculate(intermediate, num3, op2);
            } catch (ArithmeticException e) {
                continue;
            }

            // Формируем выражение со скобками или без
            String exprStr;
            boolean needParentheses = allowParentheses &&
                    ((op1.equals("+") || op1.equals("-")) &&
                            (op2.equals("*") || op2.equals("/")));

            if (needParentheses) {
                exprStr = "(" + num1 + " " + op1 + " " + num2 + ") " + op2 + " " + num3;
            } else {
                exprStr = num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3;
            }

            String stepByStep = num1 + " " + op1 + " " + num2 + " = " + intermediate + "\n";
            stepByStep += intermediate + " " + op2 + " " + num3 + " = " + result;

            lastExpression = new ExpressionGenerator.Expression(exprStr, result, stepByStep);
            questionText.setText(exprStr + " = ?");
            currentAnswer = result;
            return;
        }

        // Если не удалось сгенерировать, создаем простой пример
        generateSimpleFallbackQuestion(max);
    }

    private void generateThreeActionQuestion(int max, String[] ops, boolean allowParentheses) {
        int attempts = 0;
        int maxAttempts = 20;

        while (attempts < maxAttempts) {
            attempts++;

            String op1 = ops[random.nextInt(ops.length)];
            String op2 = ops[random.nextInt(ops.length)];
            String op3 = ops[random.nextInt(ops.length)];

            int num1 = random.nextInt(max) + 1;
            int num2 = random.nextInt(max) + 1;
            int num3 = random.nextInt(max) + 1;
            int num4 = random.nextInt(max) + 1;

            // Проверяем первое действие
            if (op1.equals("/") && (num2 == 0 || num1 % num2 != 0)) {
                continue;
            }

            int intermediate1;
            try {
                intermediate1 = calculate(num1, num2, op1);
            } catch (ArithmeticException e) {
                continue;
            }

            // Проверяем второе действие
            if (op2.equals("/") && (num3 == 0 || intermediate1 % num3 != 0)) {
                continue;
            }

            int intermediate2;
            try {
                intermediate2 = calculate(intermediate1, num3, op2);
            } catch (ArithmeticException e) {
                continue;
            }

            // Проверяем третье действие
            if (op3.equals("/") && (num4 == 0 || intermediate2 % num4 != 0)) {
                continue;
            }

            int result;
            try {
                result = calculate(intermediate2, num4, op3);
            } catch (ArithmeticException e) {
                continue;
            }

            // Формируем выражение
            String exprStr = buildExpressionWithParentheses(num1, op1, num2, op2, num3, op3, num4, allowParentheses);

            String stepByStep = num1 + " " + op1 + " " + num2 + " = " + intermediate1 + "\n";
            stepByStep += intermediate1 + " " + op2 + " " + num3 + " = " + intermediate2 + "\n";
            stepByStep += intermediate2 + " " + op3 + " " + num4 + " = " + result;

            lastExpression = new ExpressionGenerator.Expression(exprStr, result, stepByStep);
            questionText.setText(exprStr + " = ?");
            currentAnswer = result;
            return;
        }

        // Если не удалось сгенерировать, создаем простой пример
        generateSimpleFallbackQuestion(max);
    }

    private void generateSimpleFallbackQuestion(int max) {
        int num1 = random.nextInt(max) + 1;
        int num2 = random.nextInt(max) + 1;
        int result = num1 + num2;
        String exprStr = num1 + " + " + num2;
        lastExpression = new ExpressionGenerator.Expression(exprStr, result, exprStr + " = " + result);
        questionText.setText(exprStr + " = ?");
        currentAnswer = result;
    }

    private String buildExpressionWithParentheses(int num1, String op1, int num2,
                                                  String op2, int num3, String op3,
                                                  int num4, boolean allowParentheses) {
        if (!allowParentheses) {
            return num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3 + " " + op3 + " " + num4;
        }

        // Логика расстановки скобок по приоритету операций
        boolean op1High = op1.equals("*") || op1.equals("/");
        boolean op2High = op2.equals("*") || op2.equals("/");
        boolean op3High = op3.equals("*") || op3.equals("/");

        if (!op1High && op2High && op3High) {
            return "(" + num1 + " " + op1 + " " + num2 + ") " + op2 + " " + num3 + " " + op3 + " " + num4;
        } else if (op1High && !op2High && op3High) {
            return "(" + num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3 + ") " + op3 + " " + num4;
        } else if (!op1High && !op2High && op3High) {
            return num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3 + " " + op3 + " " + num4;
        } else {
            return num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3 + " " + op3 + " " + num4;
        }
    }

    private int calculate(int a, int b, String op) {
        switch (op) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("Деление на ноль");
                return a / b;
            default:
                return 0;
        }
    }

    private void generateTrueFalseQuestion() {
        // Формируем арифметическое выражение слева
        int max = currentLevel.getMaxNumber();
        String[] ops = currentLevel.getOperations();

        // Используем один шаг для предсказуемости условий
        int num1 = random.nextInt(max) + 1;
        int num2 = random.nextInt(max) + 1;
        String op = ops[random.nextInt(ops.length)];

        // избегаем дробей
        if (op.equals("/") && num2 != 0) {
            num1 = num1 * num2;
        }

        int leftValue = calculate(num1, num2, op);
        String leftExpr = num1 + " " + op + " " + num2;

        String[] operators = new String[]{"<", ">", "=", "≠", "≤", "≥"};
        String operator = operators[random.nextInt(operators.length)];

        boolean targetTruth = random.nextBoolean();
        int rightValue = generateRightValue(leftValue, operator, targetTruth);

        boolean actualTruth = evaluateComparison(leftValue, rightValue, operator);
        // страховка: корректируем, если генерация дала противоположный результат
        if (actualTruth != targetTruth) {
            rightValue = adjustRightValue(leftValue, operator, targetTruth, rightValue);
            actualTruth = evaluateComparison(leftValue, rightValue, operator);
        }

        currentAnswer = actualTruth ? 1 : 0;
        lastExpression = new ExpressionGenerator.Expression(leftExpr, leftValue, leftExpr + " = " + leftValue);

        String question = leftExpr + " " + operator + " " + rightValue + " ?";
        questionText.setText(question);
    }

    private int generateRightValue(int leftValue, String operator, boolean targetTruth) {
        int delta = Math.max(1, leftValue / 5 + 1);
        switch (operator) {
            case "<":
                return targetTruth ? leftValue + delta : Math.max(0, leftValue - delta);
            case ">":
                return targetTruth ? Math.max(0, leftValue - delta) : leftValue + delta;
            case "=":
                return targetTruth ? leftValue : leftValue + delta;
            case "≠":
                return targetTruth ? leftValue + delta : leftValue;
            case "≤":
                return targetTruth ? leftValue : leftValue - delta - 1;
            case "≥":
                return targetTruth ? leftValue : leftValue + delta + 1;
            default:
                return leftValue;
        }
    }

    private int adjustRightValue(int leftValue, String operator, boolean targetTruth, int currentRight) {
        // Минимальная коррекция, чтобы соблюсти целевую истинность
        switch (operator) {
            case "<":
            case "≤":
                return targetTruth ? Math.max(leftValue - 1, 0) : leftValue + Math.max(1, leftValue / 3 + 1);
            case ">":
            case "≥":
                return targetTruth ? leftValue + Math.max(1, leftValue / 3 + 1) : Math.max(leftValue - 1, 0);
            case "=":
                return targetTruth ? leftValue : leftValue + Math.max(2, currentRight - leftValue + 1);
            case "≠":
                return targetTruth ? leftValue + Math.max(1, currentRight == leftValue ? 1 : 0) : leftValue;
            default:
                return currentRight;
        }
    }

    private boolean evaluateComparison(int leftValue, int rightValue, String operator) {
        switch (operator) {
            case "<":
                return leftValue < rightValue;
            case ">":
                return leftValue > rightValue;
            case "=":
                return leftValue == rightValue;
            case "≠":
                return leftValue != rightValue;
            case "≤":
                return leftValue <= rightValue;
            case "≥":
                return leftValue >= rightValue;
            default:
                return false;
        }
    }

    private void showSolutionOverlay(String title, ExpressionGenerator.Expression expression, int correctAnswer) {
        if (lives <= 0 || isFinishing() || isGameEnded) {
            return;
        }
        if (solutionOverlay == null || solutionExpression == null || solutionSteps == null || 
            solutionAnswer == null || feedbackText == null) {
            return;
        }
        isAnswerButtonLocked = true;
        stopCountdownTimer();
        if (solutionAutoHideRunnable != null) {
            handler.removeCallbacks(solutionAutoHideRunnable);
        }

        if (expression == null) {
            expression = new ExpressionGenerator.Expression("—", correctAnswer, "—");
        }

        solutionOverlay.setVisibility(View.VISIBLE);
        solutionExpression.setText("Пример: " + expression.getExpression());
        solutionSteps.setText("Решение: " + expression.getStepByStep());
        solutionAnswer.setText("Ответ: " + correctAnswer);
        feedbackText.setText(title);
        feedbackText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        solutionAutoHideRunnable = () -> {
            if (!isFinishing() && !isGameEnded && lives > 0) {
                proceedToNextQuestion();
            }
        };
        handler.postDelayed(solutionAutoHideRunnable, 3000);
    }

    private void proceedToNextQuestion() {
        if (solutionAutoHideRunnable != null) {
            handler.removeCallbacks(solutionAutoHideRunnable);
        }
        solutionOverlay.setVisibility(View.GONE);
        if (lives > 0) {
            generateNewQuestion();
        }
    }

    private void hideKeyboard() {
        if (answerInput != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(answerInput.getWindowToken(), 0);
            }
        }
    }

    private void checkInputAnswer() {
        hideKeyboard();
        stopCountdownTimer();

        if (answerInput == null || feedbackText == null) return;
        String userAnswer = answerInput.getText().toString();
        if (userAnswer.isEmpty()) {
            feedbackText.setText("Введи ответ!");
            startCountdownTimer();
            isAnswerButtonLocked = false;
            return;
        }
        int answer;
        try {
            answer = Integer.parseInt(userAnswer);
        } catch (NumberFormatException e) {
            feedbackText.setText("Введите число!");
            startCountdownTimer();
            isAnswerButtonLocked = false;
            return;
        }

        if (answer == currentAnswer) {
            if (feedbackText != null) {
                feedbackText.setText(getString(R.string.correct));
                feedbackText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            correctAnswers++;
            consecutiveCorrect++;
            if (consecutiveCorrect >= 10) {
                showLevelUpScreen();
                return;
            }
        } else {
            wrongAnswers++;
            consecutiveCorrect = 0;
            loseLife();
            showSolutionOverlay("Неправильно!", lastExpression, lastExpression != null ? lastExpression.getResult() : currentAnswer);
        }

        questionsAnswered++;
        if (correctAnswersText != null) {
            correctAnswersText.setText("Правильно: " + correctAnswers + " | Ошибок: " + wrongAnswers);
        }
        updateProgressBar();

        if (lives > 0 && answer == currentAnswer && !isFinishing() && !isGameEnded) {
            handler.postDelayed(() -> {
                if (!isFinishing() && !isGameEnded) {
                    generateNewQuestion();
                }
            }, 1200);
        }
    }

    private void checkYesNoAnswer(boolean userAnswered) {
        if (isFinishing() || isGameEnded) return;
        hideKeyboard();
        stopCountdownTimer();

        boolean correct = (currentAnswer == 1 && userAnswered) || (currentAnswer == 0 && !userAnswered);

        if (correct) {
            if (feedbackText != null) {
                feedbackText.setText(getString(R.string.correct));
                feedbackText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            correctAnswers++;
            consecutiveCorrect++;
            if (consecutiveCorrect >= 10) {
                showLevelUpScreen();
                return;
            }
        } else {
            wrongAnswers++;
            consecutiveCorrect = 0;
            loseLife();
            showSolutionOverlay("Неправильно!", lastExpression, lastExpression != null ? lastExpression.getResult() : currentAnswer);
        }

        questionsAnswered++;
        if (correctAnswersText != null) {
            correctAnswersText.setText("Правильно: " + correctAnswers + " | Ошибок: " + wrongAnswers);
        }
        updateProgressBar();

        if (lives > 0 && correct && !isFinishing() && !isGameEnded) {
            handler.postDelayed(() -> {
                if (!isFinishing() && !isGameEnded) {
                    generateNewQuestion();
                }
            }, 1200);
        }
    }

    private void showLevelUpScreen() {
        int lvlNum = currentLevel.getLevelNumber();
        if (lvlNum < 5 && !isGameEnded) {
            isGameEnded = true;
            stopCountdownTimer();
            
            long totalTime = (System.currentTimeMillis() - startTime - totalPausedTime) / 1000;
            
            // Сохраняем статистику перед переходом
            StatsManager statsManager = new StatsManager(this);
            statsManager.incrementSessionsCount();
            statsManager.addTotalTime(totalTime);
            
            int totalAnswers = correctAnswers + wrongAnswers;
            int percentage = totalAnswers > 0 ? (correctAnswers * 100) / totalAnswers : 0;
            if (percentage > statsManager.getBestScore()) {
                statsManager.setBestScore(percentage);
            }
            statsManager.setLastLevelResult(lvlNum, percentage);
            statsManager.setHighestLevel(lvlNum);
            
            Intent intent = new Intent(GameActivity.this, LevelUpActivity.class);
            intent.putExtra("current_level", lvlNum);
            intent.putExtra("correct_answers", correctAnswers);
            intent.putExtra("total_time", totalTime);
            startActivity(intent);
            finish();
        }
    }

    private void updateTimer() {
        if (isFinishing() || isGameEnded) return;
        handler.postDelayed(() -> {
            if (isFinishing() || isGameEnded) return;
            if (timerText == null) return;
            if (!isPaused) {
                long elapsed = (System.currentTimeMillis() - startTime - totalPausedTime) / 1000;
                timerText.setText("Время: " + elapsed + "с");
            }
            updateTimer();
        }, 1000);
    }

    private void setupProgressBar() {
        if (progressBar != null) {
            progressBar.setMax(10); // визуализируем блоками по 10 ответов
            progressBar.setProgress(questionsAnswered % 10);
        }
    }

    private void updateProgressBar() {
        if (progressBar != null) {
            progressBar.setProgress(questionsAnswered % progressBar.getMax());
        }
    }

    private void endGame() {
        isGameEnded = true;
        stopCountdownTimer();

        long totalTime = (System.currentTimeMillis() - startTime - totalPausedTime) / 1000;

        // Сохраняем статистику через SharedPreferences напрямую
        SharedPreferences statsPrefs = getSharedPreferences("game_stats", MODE_PRIVATE);
        SharedPreferences.Editor editor = statsPrefs.edit();

        // Увеличиваем количество сессий
        int sessions = statsPrefs.getInt("sessions_count", 0);
        editor.putInt("sessions_count", sessions + 1);

        // Увеличиваем общее время игры
        long totalPlayTime = statsPrefs.getLong("total_time", 0);
        editor.putLong("total_time", totalPlayTime + totalTime);

        // Рассчитываем процент правильных ответов
        int totalAnswers = correctAnswers + wrongAnswers;
        int percentage = 0;
        if (totalAnswers > 0) {
            percentage = (correctAnswers * 100) / totalAnswers;
        }

        // Обновляем лучший результат
        int bestScore = statsPrefs.getInt("best_score", 0);
        if (percentage > bestScore) {
            editor.putInt("best_score", percentage);
        }

        // Сохраняем результат для текущего уровня
        int currentLevelNum = currentLevel.getLevelNumber();
        editor.putInt("level_" + currentLevelNum + "_result", percentage);

        // Обновляем самый высокий достигнутый уровень
        int highestLevel = statsPrefs.getInt("highest_level", 1);
        if (currentLevelNum > highestLevel) {
            editor.putInt("highest_level", currentLevelNum);
        }

        editor.apply();

        // Переходим на экран результатов
        Intent intent = new Intent(GameActivity.this, ResultsActivity.class);
        intent.putExtra("correct_answers", correctAnswers);
        intent.putExtra("wrong_answers", wrongAnswers);
        intent.putExtra("total_answers", correctAnswers + wrongAnswers);
        intent.putExtra("total_time", totalTime);
        intent.putExtra("level", currentLevelNum);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Не показываем экран паузы, если игра уже завершена
        if (!isPaused && !isGameEnded) {
            pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Игра автоматически останется на паузе, пользователь сам решит, когда продолжить
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWarningSound();
        if (warningSound != null) {
            warningSound.release();
            warningSound = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}