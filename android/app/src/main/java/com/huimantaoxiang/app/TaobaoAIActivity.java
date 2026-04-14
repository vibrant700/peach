package com.huimantaoxiang.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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
    // 聊天消息容器（垂直线性布局，每条消息是一个 TextView 气泡）
    private LinearLayout messagesContainer;
    private android.widget.ScrollView scrollView;
    // 底部输入框
    private EditText input;
    // 发送按钮
    private Button sendBtn;
    // 调用模型时显示的进度条
    private ProgressBar progress;
    private String currentMode;

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

        messagesContainer = findViewById(R.id.container_messages);
        scrollView = findViewById(R.id.scroll);
        input = findViewById(R.id.input_message);
        sendBtn = findViewById(R.id.btn_send);
        progress = findViewById(R.id.progress);
        ImageView inputAvatar = findViewById(R.id.iv_ai_avatar);

        LinearLayout backBtn = findViewById(R.id.btn_back);
        View modeBtn = findViewById(R.id.btn_mode);

        // 从 BuildConfig 中读取在构建阶段写入的 AI 配置信息
        baseUrl = BuildConfig.AI_BASE_URL;
        backendType = BuildConfig.AI_BACKEND_TYPE;
        modelName = BuildConfig.AI_MODEL;
        apiKey = BuildConfig.DEEPSEEK_API_KEY;

        currentMode = getString(R.string.mode_general);
        if (modeBtn != null) {
            modeBtn.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(this, v);
                menu.getMenu().add(0, 1, 0, getString(R.string.mode_general));
                menu.getMenu().add(0, 2, 1, getString(R.string.mode_agro));
                menu.getMenu().add(0, 3, 2, getString(R.string.mode_recommend));
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 2) {
                        currentMode = getString(R.string.mode_agro);
                    } else if (item.getItemId() == 3) {
                        currentMode = getString(R.string.mode_recommend);
                    } else {
                        currentMode = getString(R.string.mode_general);
                    }
                    return true;
                });
                menu.show();
            });
        }

        if (inputAvatar != null) {
            loadAvatar(inputAvatar);
        }

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
            String mode = currentMode;
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

        // 检查API配置，如果API Key为空，提示用户
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            appendMessage("⚠️ 提示：您尚未配置DeepSeek API Key，AI功能可能无法正常使用。\n\n配置方法：\n1. 打开 app/build.gradle\n2. 在 buildConfigField 中添加 DEEPSEEK_API_KEY\n3. 填入您的 API Key\n\n或者使用本地Ollama部署DeepSeek模型。", false);
        }
    }

    // 往聊天区域追加一条消息，根据 self 区分用户和 AI 样式
    private void appendMessage(String msg, boolean self) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(self ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowLp.topMargin = dpToPx(12);
        row.setLayoutParams(rowLp);

        ImageView avatar = new ImageView(this);
        int avatarSize = dpToPx(34);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarLp.topMargin = dpToPx(2);
        avatar.setLayoutParams(avatarLp);
        avatar.setBackgroundResource(R.drawable.circle_background_white);
        int avatarPadding = dpToPx(2);
        avatar.setPadding(avatarPadding, avatarPadding, avatarPadding, avatarPadding);

        TextView bubble = new TextView(this);
        bubble.setText(msg);
        bubble.setTextSize(16);
        bubble.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.68f));

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (self) {
            bubbleLp.rightMargin = dpToPx(10);
            bubble.setBackgroundResource(R.drawable.bg_chat_bubble_me);
            bubble.setTextColor(0xFFFFFFFF);
        } else {
            bubbleLp.leftMargin = dpToPx(10);
            bubble.setBackgroundResource(R.drawable.bg_chat_bubble_other);
            bubble.setTextColor(0xFF000000);
        }
        bubble.setLayoutParams(bubbleLp);

        loadAvatar(avatar);

        if (self) {
            row.addView(bubble);
            row.addView(avatar);
        } else {
            row.addView(avatar);
            row.addView(bubble);
        }

        messagesContainer.addView(row);

        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void loadAvatar(ImageView imageView) {
        Glide.with(this)
                .load(R.drawable.avatar)
                .circleCrop()
                .into(imageView);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
                String errorMsg = "抱歉，AI服务暂时不可用（HTTP " + code + "）\n";
                if (err != null && err.length() > 0) {
                    // 简化错误信息，避免显示原始错误
                    if (err.contains("401") || err.contains("Unauthorized")) {
                        errorMsg += "请检查API Key配置";
                    } else if (err.contains("404") || err.contains("Not Found")) {
                        errorMsg += "请检查模型名称配置";
                    } else {
                        errorMsg += "请稍后重试";
                    }
                } else {
                    errorMsg += "请检查网络连接";
                }
                return errorMsg;
            }

            String ok = readStream(conn.getInputStream());
            if (ok == null || ok.isEmpty()) {
                return "抱歉，AI服务未返回任何内容";
            }

            JSONObject resp = new JSONObject(ok);
            org.json.JSONArray choices = resp.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return "抱歉，AI未能生成回复";
            }

            JSONObject first = choices.getJSONObject(0);
            JSONObject msg = first.optJSONObject("message");
            if (msg == null) {
                return "抱歉，回复格式异常";
            }

            String content = msg.optString("content");
            if (content == null || content.isEmpty()) {
                return "抱歉，AI返回了空内容";
            }

            // 清理Markdown格式，让回复更自然
            content = cleanMarkdown(content);

            // 再次检查清理后的内容
            content = content.trim();
            if (content.isEmpty() || content.equals("***") || content.equals("**") || content.equals("*")) {
                return "我理解了您的问题，但需要更多信息才能准确回答。能否详细说明一下？";
            }

            return content;

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                // 简化错误信息
                if (errorMsg.contains("UnknownHostException") || errorMsg.contains("ConnectException")) {
                    return "抱歉，无法连接到AI服务\n请检查：\n1. DeepSeek服务是否已启动\n2. 网络连接是否正常";
                } else if (errorMsg.contains("Timeout")) {
                    return "抱歉，AI服务响应超时，请稍后重试";
                } else if (errorMsg.contains("JSON")) {
                    return "抱歉，AI返回数据格式异常，请重新尝试";
                }
            }
            return "抱歉，AI服务出现问题：" + (errorMsg != null ? errorMsg : "未知错误");
        }
    }

    /**
     * 清理Markdown格式，让AI回复更自然
     * @param content 原始内容
     * @return 清理后的内容
     */
    private String cleanMarkdown(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 移除Markdown标题标记
        content = content.replaceAll("^#+\\s*", ""); // 移除开头的 #, ##, ### 等
        content = content.replaceAll("\\n#+\\s*", "\n"); // 移除换行后的 #, ## 等

        // 移除加粗标记
        content = content.replaceAll("\\*\\*", ""); // 移除 **加粗**
        content = content.replaceAll("\\*", ""); // 移除 *斜体*

        // 移除代码块标记
        content = content.replaceAll("```[\\s\\S]*?```", ""); // 移除代码块
        content = content.replaceAll("`", ""); // 移除行内代码

        // 移除链接标记
        content = content.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1"); // [文本](链接) → 文本

        // 移除列表标记（保留缩进）
        content = content.replaceAll("^\\s*[-*+]\\s+", ""); // - 或 * 或 + 开头的列表
        content = content.replaceAll("^\\s*\\d+\\.\\s+", ""); // 1. 2. 3. 开头的列表

        // 移除引用标记
        content = content.replaceAll("^>\\s*", ""); // > 开头的引用

        // 移除分割线
        content = content.replaceAll("^-{3,}\\s*$", ""); // --- 开头的分割线
        content = content.replaceAll("^\\*{3,}\\s*$", ""); // *** 开头的分割线

        // 清理多余的空行
        content = content.replaceAll("\\n{3,}", "\n\n"); // 多个连续换行压缩为2个

        // 移除开头和结尾的空白
        content = content.trim();

        // 如果清理后为空，返回友好提示
        if (content.isEmpty()) {
            return "我理解了您的问题，但需要更多信息才能准确回答。能否详细说明一下？";
        }

        return content;
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
