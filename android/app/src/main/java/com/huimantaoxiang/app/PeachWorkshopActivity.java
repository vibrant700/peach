package com.huimantaoxiang.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class PeachWorkshopActivity extends AppCompatActivity {

    private EditText etPrompt;
    private Button btnGenerate;
    private ImageView ivResult;
    private ProgressBar progressBar;

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peach_workshop);

        etPrompt = findViewById(R.id.et_prompt);
        btnGenerate = findViewById(R.id.btn_generate);
        ivResult = findViewById(R.id.iv_result);
        progressBar = findViewById(R.id.progress_bar);

        apiKey = BuildConfig.DASHSCOPE_API_KEY;

        btnGenerate.setOnClickListener(v -> {
            String prompt = etPrompt.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "请输入描述内容", Toast.LENGTH_SHORT).show();
                return;
            }
            if (apiKey == null || apiKey.isEmpty()) {
                Toast.makeText(this, "未配置 API Key", Toast.LENGTH_SHORT).show();
                return;
            }
            generateImage(prompt);
        });
    }

    private void generateImage(String prompt) {
        setLoading(true);
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                // 添加 X-DashScope-WorkSpace 如果需要，通常不需要

                JSONObject root = new JSONObject();
                root.put("model", "qwen-image-max");

                JSONObject input = new JSONObject();
                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                JSONArray content = new JSONArray();
                JSONObject textObj = new JSONObject();
                textObj.put("text", prompt);
                content.put(textObj);
                message.put("content", content);
                messages.put(message);
                input.put("messages", messages);
                root.put("input", input);

                JSONObject parameters = new JSONObject();
                parameters.put("size", "1024*1024"); // 使用标准比例，或者 "1664*928"
                parameters.put("n", 1);
                root.put("parameters", parameters);

                conn.setDoOutput(true);
                try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                    writer.write(root.toString());
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    handleResponse(response.toString());
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line);
                    }
                    reader.close();
                    handleError("Error " + code + ": " + err.toString());
                }

            } catch (Exception e) {
                handleError("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleResponse(String jsonStr) {
        try {
            Log.d("PeachWorkshop", "Response: " + jsonStr);
            JSONObject json = new JSONObject(jsonStr);
            // 解析 DashScope 的返回结构
            
            if (json.has("output")) {
                JSONObject output = json.optJSONObject("output");
                if (output != null) {
                    // 1. 尝试解析标准的 results (Wanx 等)
                    if (output.has("results")) {
                        JSONArray results = output.optJSONArray("results");
                        if (results != null && results.length() > 0) {
                            JSONObject res = results.getJSONObject(0);
                            String imageUrl = res.optString("url");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                showImage(imageUrl);
                                return;
                            }
                        }
                    } 
                    // 2. 尝试解析 choices (通常是文本生成或多模态理解的返回，但也可能包含信息)
                    else if (output.has("choices")) {
                         JSONArray choices = output.optJSONArray("choices");
                         if (choices != null && choices.length() > 0) {
                             JSONObject choice = choices.getJSONObject(0);
                             JSONObject message = choice.optJSONObject("message");
                             if (message != null) {
                                 // 检查 content 是字符串还是数组
                                 Object contentObj = message.opt("content");
                                 if (contentObj instanceof String) {
                                     String content = (String) contentObj;
                                     // 尝试在文本中提取 Markdown 图片链接 ![](url)
                                     if (content.contains("](") && content.contains("http")) {
                                         int start = content.indexOf("](") + 2;
                                         int end = content.indexOf(")", start);
                                         if (start > 1 && end > start) {
                                              String url = content.substring(start, end);
                                              if (url.startsWith("http")) {
                                                  showImage(url);
                                                  return;
                                              }
                                         }
                                     }
                                     // 如果只是文本，可能是错误提示或拒绝生成
                                     handleError("模型返回文本: " + content);
                                     return;
                                 } else if (contentObj instanceof JSONArray) {
                                     // 如果是多模态内容数组
                                     JSONArray contentArr = (JSONArray) contentObj;
                                     for (int i = 0; i < contentArr.length(); i++) {
                                         JSONObject item = contentArr.getJSONObject(i);
                                         if (item.has("image")) {
                                             String img = item.optString("image");
                                              if (img != null && !img.isEmpty()) {
                                                  showImage(img);
                                                  return;
                                              }
                                         }
                                     }
                                 }
                             }
                         }
                    }
                    // 3. 异步任务状态
                    else if (output.has("task_status")) {
                         String status = output.optString("task_status");
                         handleError("Task Submitted: " + status + " (Async handling not implemented)");
                         return;
                    }
                }
            }
            
            // 兜底错误
            handleError("无法解析图片地址: " + jsonStr);

        } catch (Exception e) {
            handleError("Parse Error: " + e.getMessage());
        }
    }

    private void showImage(String imageUrl) {
        runOnUiThread(() -> {
            setLoading(false);
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivResult);
        });
    }

    private void handleError(String msg) {
        runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.e("PeachWorkshop", msg);
        });
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnGenerate.setEnabled(!loading);
            if (loading) {
                ivResult.setImageDrawable(null);
            }
        });
    }
}
