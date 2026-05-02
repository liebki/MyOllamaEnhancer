package de.liebki.myollamaenhancer.utils;

import de.liebki.myollamaenhancer.types.Entry;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public enum CustomPromptHistoryManager {
    ;

    private static final String HISTORY_PATH = System.getProperty("user.home") + "/.myollamaenhancer_custom_history.json";

    public static void addEntry(final String prompt, final String result) {
        final Entry entry = new Entry(prompt, result, System.currentTimeMillis(), false);
        try (final FileWriter fw = new FileWriter(CustomPromptHistoryManager.HISTORY_PATH, true)) {
            fw.write(entry.toJson().toString() + "\n");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Entry> getHistory() {
        final List<Entry> history = new ArrayList<>();
        if (!Files.exists(Paths.get(CustomPromptHistoryManager.HISTORY_PATH))) return history;

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CustomPromptHistoryManager.HISTORY_PATH), StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = br.readLine())) {
                try {
                    final JSONObject obj = new JSONObject(line);
                    history.add(Entry.fromJson(obj));
                } catch (final Exception ignore) {
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return history;
    }

    public static void toggleFavorite(final int index) {
        final List<Entry> history = CustomPromptHistoryManager.getHistory();
        if (0 > index || index >= history.size()) return;

        history.get(index).favorite = !history.get(index).favorite;
        CustomPromptHistoryManager.saveAll(history);
    }

    public static List<Entry> getFavorites() {
        final List<Entry> all = CustomPromptHistoryManager.getHistory();
        final List<Entry> favs = new ArrayList<>();

        for (final Entry e : all) if (e.favorite) favs.add(e);
        return favs;
    }

    public static void deleteEntry(final int index) {
        final List<Entry> history = CustomPromptHistoryManager.getHistory();
        if (0 > index || index >= history.size()) return;
        history.remove(index);
        CustomPromptHistoryManager.saveAll(history);
    }

    private static void saveAll(final List<Entry> entries) {
        try (final FileWriter fw = new FileWriter(CustomPromptHistoryManager.HISTORY_PATH, false)) {
            for (final Entry e : entries) {
                fw.write(e.toJson().toString() + "\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
} 