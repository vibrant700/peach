package com.huimantaoxiang.util;

import android.content.Context;
import android.util.Log;

import com.huimantaoxiang.app.IdentifyPeachActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PeachDataLoader {
    private static final String TAG = "PeachDataLoader";
    private static final String DATA_FILE = ".cache/app_config.json";

    private static Map<String, PeachParams> cachedData = null;

    public static class PeachParams {
        public String code;
        public String name;
        public String grade;
        public float sweetness;
        public int maturity;
        public int weight;
        public int diameter;
        public float confidence;
        public int lightScore;

        public PeachParams(String code, String name, String grade,
                         float sweetness, int maturity, int weight, int diameter,
                         float confidence, int lightScore) {
            this.code = code;
            this.name = name;
            this.grade = grade;
            this.sweetness = sweetness;
            this.maturity = maturity;
            this.weight = weight;
            this.diameter = diameter;
            this.confidence = confidence;
            this.lightScore = lightScore;
        }
    }

    public static Map<String, PeachParams> loadPeachParams(Context context) {
        if (cachedData != null) {
            return cachedData;
        }

        Map<String, PeachParams> data = new HashMap<>();

        try {
            InputStream is = context.getAssets().open(DATA_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);
            JSONArray varieties = jsonObject.getJSONArray("varieties");

            for (int i = 0; i < varieties.length(); i++) {
                JSONObject item = varieties.getJSONObject(i);
                String code = item.getString("code");
                String name = item.getString("name");
                JSONObject params = item.getJSONObject("params");

                PeachParams peachParams = new PeachParams(
                    code,
                    name,
                    params.getString("grade"),
                    (float) params.getDouble("sweetness"),
                    params.getInt("maturity"),
                    params.getInt("weight"),
                    params.getInt("diameter"),
                    (float) params.getDouble("confidence"),
                    params.getInt("light_score")
                );

                data.put(code, peachParams);
            }

            cachedData = data;
            Log.d(TAG, "成功加载" + data.size() + "个品种参数");

        } catch (Exception e) {
            Log.e(TAG, "加载桃子参数数据失败", e);
        }

        return data;
    }

    public static PeachParams getPeachParams(Context context, String code) {
        Map<String, PeachParams> allData = loadPeachParams(context);
        return allData.get(code);
    }

    public static void clearCache() {
        cachedData = null;
    }
}