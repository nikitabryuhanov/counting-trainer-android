package com.example.countingtrainer;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class ExpressionGenerator {
    private Random random = new Random();

    // Класс для хранения выражения и результата
    public static class Expression {
        private final String expression;
        private final int result;
        private final String stepByStep;

        public Expression(String expression, int result, String stepByStep) {
            this.expression = expression;
            this.result = result;
            this.stepByStep = stepByStep;
        }

        public String getExpression() {
            return expression;
        }

        public int getResult() {
            return result;
        }

        public String getStepByStep() {
            return stepByStep;
        }
    }

    // Генерация для уровня 4 (2 действия)
    public Expression generateLevel4(int maxNumber, String[] operations) {
        // Генерируем выражение с двумя действиями
        int num1 = random.nextInt(maxNumber - 1) + 2; // от 2 до maxNumber
        int num2 = random.nextInt(maxNumber - 1) + 2;
        int num3 = random.nextInt(maxNumber - 1) + 2;

        String op1 = getRandomOperation(operations);
        String op2 = getRandomOperation(operations);

        // Убедимся, что деление без остатка
        if (op1.equals("/")) {
            num1 = adjustForDivision(num1, num2);
        }

        // Вычисляем с учетом приоритета операций
        int result = calculateWithPriority(num1, num2, num3, op1, op2);
        String expression = num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3;
        String steps = generateSteps(num1, num2, num3, op1, op2, result);

        return new Expression(expression, result, steps);
    }

    // Генерация для уровня 5 (3 действия)
    public Expression generateLevel5(int maxNumber, String[] operations) {
        // Генерируем выражение с тремя действиями
        int num1 = random.nextInt(maxNumber - 2) + 3; // от 3 до maxNumber
        int num2 = random.nextInt(maxNumber - 2) + 3;
        int num3 = random.nextInt(maxNumber - 2) + 3;
        int num4 = random.nextInt(maxNumber - 2) + 3;

        String op1 = getRandomOperation(operations);
        String op2 = getRandomOperation(operations);
        String op3 = getRandomOperation(operations);

        // Убедимся, что деление без остатка
        if (op1.equals("/")) {
            num1 = adjustForDivision(num1, num2);
        }

        // Вычисляем с учетом приоритета операций
        int result = calculateWithPriority(num1, num2, num3, num4, op1, op2, op3);
        String expression = num1 + " " + op1 + " " + num2 + " " + op2 + " " + num3 + " " + op3 + " " + num4;
        String steps = generateSteps(num1, num2, num3, num4, op1, op2, op3, result);

        return new Expression(expression, result, steps);
    }

    private String getRandomOperation(String[] operations) {
        return operations[random.nextInt(operations.length)];
    }

    private int adjustForDivision(int a, int b) {
        // Делаем деление без остатка
        if (b == 0) b = 1;
        int result = a * b;
        return result;
    }

    private int calculateWithPriority(int a, int b, int c, String op1, String op2) {
        // Вычисляем с учетом приоритета (* и / перед + и -)
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) {
            int firstResult = calculate(a, b, op1);
            return calculate(firstResult, c, op2);
        } else if ((op2.equals("*") || op2.equals("/")) && (op1.equals("+") || op1.equals("-"))) {
            int secondResult = calculate(b, c, op2);
            return calculate(a, secondResult, op1);
        } else {
            int firstResult = calculate(a, b, op1);
            return calculate(firstResult, c, op2);
        }
    }

    private int calculateWithPriority(int a, int b, int c, int d, String op1, String op2, String op3) {
        // Более сложный расчет для 3 действий
        // Простая реализация - последовательно с приоритетом
        List<Integer> numbers = new ArrayList<>();
        List<String> ops = new ArrayList<>();

        numbers.add(a);
        numbers.add(b);
        numbers.add(c);
        numbers.add(d);
        ops.add(op1);
        ops.add(op2);
        ops.add(op3);

        // Сначала выполняем умножение и деление
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i).equals("*") || ops.get(i).equals("/")) {
                int result = calculate(numbers.get(i), numbers.get(i + 1), ops.get(i));
                numbers.set(i, result);
                numbers.remove(i + 1);
                ops.remove(i);
                i--; // Уменьшаем счетчик, так как список уменьшился
            }
        }

        // Затем сложение и вычитание
        int result = numbers.get(0);
        for (int i = 0; i < ops.size(); i++) {
            result = calculate(result, numbers.get(i + 1), ops.get(i));
        }

        return result;
    }

    private String generateSteps(int a, int b, int c, String op1, String op2, int result) {
        StringBuilder steps = new StringBuilder();
        steps.append(a).append(" ").append(op1).append(" ").append(b).append(" ").append(op2).append(" ").append(c);

        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) {
            int first = calculate(a, b, op1);
            steps.append(" = ").append(first).append(" ").append(op2).append(" ").append(c);
            steps.append(" = ").append(result);
        } else if ((op2.equals("*") || op2.equals("/")) && (op1.equals("+") || op1.equals("-"))) {
            int second = calculate(b, c, op2);
            steps.append(" = ").append(a).append(" ").append(op1).append(" ").append(second);
            steps.append(" = ").append(result);
        } else {
            int first = calculate(a, b, op1);
            int second = calculate(first, c, op2);
            steps.append(" = ").append(first).append(" ").append(op2).append(" ").append(c);
            steps.append(" = ").append(result);
        }

        return steps.toString();
    }

    private String generateSteps(int a, int b, int c, int d, String op1, String op2, String op3, int result) {
        // Упрощенная версия для демонстрации
        return a + " " + op1 + " " + b + " " + op2 + " " + c + " " + op3 + " " + d + " = " + result;
    }

    private int calculate(int x, int y, String op) {
        switch (op) {
            case "+": return x + y;
            case "-": return x - y;
            case "*": return x * y;
            case "/": return y != 0 ? x / y : 0;
            default: return 0;
        }
    }
}