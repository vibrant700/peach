package com.huimantaoxiang.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 淘宝 AI 辅助页面
 * - 提供三种模式（通用问答、农业助手、种桃推荐）
 * - 根据配置选择不同后端（OpenAI 兼容 / 本地大模型服务）
 * - 通过简单的聊天气泡形式展示用户和 AI 的对话
 */
public class TaobaoAIActivity extends AppCompatActivity {
    // 顶部模式选择下拉框
    private Spinner spinnerMode;
    // 聊天消息容器（垂直线性布局，每条消息是一个 TextView 气泡）
    private LinearLayout messagesContainer;
    private android.widget.ScrollView scrollView;
    // 底部输入框
    private EditText input;
    // 发送按钮
    private Button sendBtn;
    // 调用模型时显示的进度条
    private ProgressBar progress;

    // 后端服务的基础地址（例如 http://10.0.2.2:11434 或云端地址）
    private String baseUrl;
    // 后端类型：openai / 其他（本地 ollama 等）
    private String backendType;
    // 使用的模型名称
    private String modelName;
    // 若使用 OpenAI 兼容后端时的 API Key（DeepSeek 等）
    private String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taobao_ai);

        spinnerMode = findViewById(R.id.spinner_mode);
        messagesContainer = findViewById(R.id.container_messages);
        scrollView = findViewById(R.id.scroll);
        input = findViewById(R.id.input_message);
        sendBtn = findViewById(R.id.btn_send);
        progress = findViewById(R.id.progress);
        Button backBtn = findViewById(R.id.btn_back);

        // 从 BuildConfig 中读取在构建阶段写入的 AI 配置信息
        baseUrl = BuildConfig.AI_BASE_URL;
        backendType = BuildConfig.AI_BACKEND_TYPE;
        modelName = BuildConfig.AI_MODEL;
        apiKey = BuildConfig.DEEPSEEK_API_KEY;

        // 模式选择下拉框适配器：通用对话 / 农业助手 / 种植推荐
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{
                getString(R.string.mode_general),
                getString(R.string.mode_agro),
                getString(R.string.mode_recommend)
        });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(adapter);

        // 返回按钮：关闭当前页面
        backBtn.setOnClickListener(v -> {
            finish();
        });

        // 发送按钮点击：发送用户输入到模型，并展示结果
        sendBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            // 空输入直接返回
            if (text.isEmpty()) return;
            // 先把用户输入追加到聊天区域（右侧绿色气泡）
            appendMessage(text, true);
            input.setText("");
            // 调用过程中禁止重复点击，显示进度条
            sendBtn.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            // 当前选择的业务模式，用于拼接不同的 system prompt
            String mode = spinnerMode.getSelectedItem().toString();
            // 在子线程中访问网络，避免阻塞 UI 线程
            new Thread(() -> {
                // 调用 DeepSeek 模型生成回复
                String result = callDeepSeek(mode, text);
                // 回到主线程更新 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    progress.setVisibility(View.GONE);
                    sendBtn.setEnabled(true);
                    if (result == null || result.isEmpty()) {
                        appendMessage(getString(R.string.ai_error), false);
                    } else {
                        appendMessage(result, false);
                    }
                });
            }).start();
        });

        // 添加开场白
        appendMessage(getString(R.string.ai_welcome), false);
    }

    // 往聊天区域追加一条消息，根据 self 区分用户和 AI 样式
    private void appendMessage(String msg, boolean self) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextSize(16);
        // 增加内边距，使气泡更舒适
        tv.setPadding(32, 24, 32, 24);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 24; // 增加消息间距
        
        if (self) {
            // 自己的消息：右对齐，使用圆角气泡
            lp.gravity = Gravity.END;
            lp.leftMargin = 100; // 限制最大宽度，防止占满屏幕
            tv.setBackgroundResource(R.drawable.bg_chat_bubble_me);
            tv.setTextColor(0xFFFFFFFF);
        } else {
            // AI 回复：左对齐，使用圆角气泡
            lp.gravity = Gravity.START;
            lp.rightMargin = 100; // 限制最大宽度
            tv.setBackgroundResource(R.drawable.bg_chat_bubble_other);
            tv.setTextColor(0xFF000000);
        }
        tv.setLayoutParams(lp);
        messagesContainer.addView(tv);
        
        // 自动滚动到底部
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    // 根据当前选中的模式返回对应的提示词，用作 system prompt
    private String systemPrompt(String mode) {
        if (mode.equals(getString(R.string.mode_agro))) {
            return getString(R.string.prompt_agro);
        } else if (mode.equals(getString(R.string.mode_recommend))) {
            return getString(R.string.prompt_recommend);
        } else {
            return getString(R.string.prompt_general);
        }
    }

    /**
     * 调用 DeepSeek 大模型 (OpenAI 兼容接口)
     */
    private String callDeepSeek(String mode, String userMsg) {
        try {
            String sys = systemPrompt(mode);
            // 使用 DeepSeek 官方 API 地址或配置的地址
            // 如果 baseUrl 配置为 https://api.deepseek.com，则拼接 /chat/completions
            String finalUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

            // 简单纠错：如果用户配置了 deepseek.com 域名但路径不对，尝试强制修正
            if (baseUrl.contains("deepseek.com") && !baseUrl.contains("/chat/completions")) {
                finalUrl = "https://api.deepseek.com/chat/completions";
            }

            URL url = new URL(finalUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey); // 必须有 API Key

            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("model", modelName); // 例如 "deepseek-chat" 或 "deepseek-reasoner"
            body.put("stream", false);

            JSONObject m1 = new JSONObject();
            m1.put("role", "system");
            m1.put("content", sys);

            JSONObject m2 = new JSONObject();
            m2.put("role", "user");
            m2.put("content", userMsg);

            org.json.JSONArray messages = new org.json.JSONArray();
            messages.put(m1);
            messages.put(m2);
            body.put("messages", messages);

            try (java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                w.write(body.toString());
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readStream(conn.getErrorStream());
                return "HTTP " + code + ": " + (err != null ? err : "无错误详情");
            }

            String ok = readStream(conn.getInputStream());
            JSONObject resp = new JSONObject(ok);
            org.json.JSONArray choices = resp.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return "无回复内容";

            JSONObject first = choices.getJSONObject(0);
            JSONObject msg = first.optJSONObject("message");
            return msg != null ? msg.optString("content") : "回复格式异常";

        } catch (Exception e) {
            return "调用异常: " + e.getMessage();
        }
    }

    // 将输入流内容完整读取为字符串（UTF-8），用于处理 HTTP 响应
    private String readStream(java.io.InputStream is) {
        if (is == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
