package de.liebki.myollamaenhancer.types;

import org.json.JSONObject;

public class Entry {

    public String prompt;
    public String result;
    public long timestamp;
    public boolean favorite;

    public Entry(final String prompt, final String result, final long timestamp, final boolean favorite) {
        this.prompt = prompt;
        this.result = result;
        this.timestamp = timestamp;
        this.favorite = favorite;
    }

    public static Entry fromJson(final JSONObject obj) {
        return new Entry(
                obj.optString("prompt", ""),
                obj.optString("result", ""),
                obj.optLong("timestamp", 0),
                obj.optBoolean("favorite", false)
        );
    }

    public JSONObject toJson() {
        final JSONObject obj = new JSONObject();
        obj.put("prompt", this.prompt);
        obj.put("result", this.result);
        obj.put("timestamp", this.timestamp);
        obj.put("favorite", this.favorite);
        return obj;
    }
}