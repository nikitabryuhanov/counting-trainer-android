package com.example.countingtrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class StatsManager {
    private static final String PREFS_NAME = "game_stats";
    private SharedPreferences prefs;

    private static final String KEY_HIGHEST_LEVEL = "highest_level";
    private static final String KEY_SESSIONS_COUNT = "sessions_count";
    private static final String KEY_BEST_SCORE = "best_score";
    private static final String KEY_TOTAL_TIME = "total_time";
    private static final String KEY_LAST_RESULTS = "last_results";

    // Формат хранения результатов каждого уровня: "level_1_result", "level_2_result", ...
    private static String levelResultKey(int level) {
        return "level_" + level + "_result";
    }

    public StatsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getHighestLevel() {
        return prefs.getInt(KEY_HIGHEST_LEVEL, 1);
    }

    public void setHighestLevel(int level) {
        if (level > getHighestLevel()) {
            prefs.edit().putInt(KEY_HIGHEST_LEVEL, level).apply();
        }
    }

    public int getSessionsCount() {
        return prefs.getInt(KEY_SESSIONS_COUNT, 0);
    }

    public void incrementSessionsCount() {
        prefs.edit().putInt(KEY_SESSIONS_COUNT, getSessionsCount() + 1).apply();
    }

    public int getBestScore() {
        return prefs.getInt(KEY_BEST_SCORE, 0);
    }

    public void setBestScore(int score) {
        if (score > getBestScore()) {
            prefs.edit().putInt(KEY_BEST_SCORE, score).apply();
        }
    }

    public long getTotalTime() {
        return prefs.getLong(KEY_TOTAL_TIME, 0);
    }

    public void addTotalTime(long seconds) {
        prefs.edit().putLong(KEY_TOTAL_TIME, getTotalTime() + seconds).apply();
    }

    public int getLastLevelResult(int level) {
        return prefs.getInt(levelResultKey(level), 0);
    }

    public void setLastLevelResult(int level, int score) {
        prefs.edit().putInt(levelResultKey(level), score).apply();
    }

    public void addResult(int score) {
        List<Integer> list = getLastResultsList();
        list.add(score);
        // сохраняем только последние 10
        while (list.size() > 10) {
            list.remove(0);
        }
        saveLastResults(list);
    }

    public int[] getLastResults() {
        List<Integer> list = getLastResultsList();
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private List<Integer> getLastResultsList() {
        String stored = prefs.getString(KEY_LAST_RESULTS, "");
        List<Integer> list = new ArrayList<>();
        if (stored != null && !stored.isEmpty()) {
            String[] parts = stored.split(",");
            for (String p : parts) {
                try {
                    list.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ignored) { }
            }
        }
        return list;
    }

    private void saveLastResults(List<Integer> list) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i));
            if (i < list.size() - 1) {
                builder.append(",");
            }
        }
        prefs.edit().putString(KEY_LAST_RESULTS, builder.toString()).apply();
    }

    // Новый метод: сброс всей статистики
    public void resetAllStats() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Очищает все сохраненные данные
        editor.apply();
    }
}