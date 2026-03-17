package com.huimantaoxiang.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifyPeachActivity extends AppCompatActivity {

    // UI组件 - 简洁版本
    private LinearLayout btnBack;
    private ImageView ivPreview;
    private ImageView ivResult;        // 新增：识别结果图片
    private Button btnUpload;          // 改为Button，不是View
    private Spinner spinnerModel;
    private Button btnIdentify;        // 改为Button，不是View
    private LinearLayout llResults;
    private ScrollView scrollResults;
    private ProgressBar progressLoading;  // 新增：加载进度条
    private LinearLayout layoutLoading;   // 新增：加载布局
    private TextView tvLoadingHint;       // 新增：加载提示文本
    private LinearLayout emptyStateContainer; // 空状态容器
    private TextView tvEmptyResult;

    // 数据
    private Bitmap selectedImage;
    private Bitmap resultImage;        // 新增：识别结果图片
    private String selectedModel = "YOLOv5标准模型";
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    // 服务器配置
    private static final String SERVER_URL = "http://10.0.2.2:8081/upload";

    // 模拟数据
    private Map<String, PeachResult> mockResults = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_peach);

        initViews();
        initImagePicker();
        setupListeners();
        setupSpinner();
        initMockData();
    }

    private void initImagePicker() {
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadSelectedImage(uri);
                }
        );
    }

    private void loadSelectedImage(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "图片解析失败", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedImage = bitmap;
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setAlpha(1.0f);

            resultImage = null;
            ivResult.setImageResource(R.drawable.ic_upload);
            ivResult.setAlpha(0.5f);

            llResults.removeAllViews();
            scrollResults.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            tvEmptyResult.setText("📸 已选择图片，点击开始识别");
        } catch (Exception e) {
            Log.e("IdentifyPeach", "loadSelectedImage error: " + e.getMessage(), e);
            Toast.makeText(this, "读取图片失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivPreview = findViewById(R.id.iv_preview);
        ivResult = findViewById(R.id.iv_result);              // 新增：初始化识别结果图片
        btnUpload = findViewById(R.id.btn_upload);            // Button，不是View
        spinnerModel = findViewById(R.id.spinner_model);
        btnIdentify = findViewById(R.id.btn_identify);        // Button，不是View
        llResults = findViewById(R.id.ll_results);
        tvEmptyResult = findViewById(R.id.tv_empty_result);
        scrollResults = findViewById(R.id.scroll_results);
        progressLoading = findViewById(R.id.progress_loading);  // 新增
        layoutLoading = findViewById(R.id.layout_loading);      // 新增
        tvLoadingHint = findViewById(R.id.tv_loading_hint);     // 新增
        emptyStateContainer = findViewById(R.id.empty_state_container); // 新增
    }

    private void setupSpinner() {
        List<String> models = new ArrayList<>();
        models.add("YOLOv5标准模型");
        models.add("改进YOLOv5模型（高精度）");
        models.add("轻量化模型（快速）");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);

        spinnerModel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedModel = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void initMockData() {
        mockResults.put("北京27号", new PeachResult("北京27号", "特级果", 12.5f, 78, 235, 82, 95.2f, 92));
        mockResults.put("春蜜", new PeachResult("春蜜", "一级果", 11.8f, 82, 210, 78, 93.5f, 88));
        mockResults.put("白风", new PeachResult("白风", "特级果", 13.2f, 85, 245, 84, 96.8f, 94));
    }

    private void setupListeners() {
        // 返回按钮 - 先检查是否为空
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> {
                if (pickMediaLauncher == null) {
                    Toast.makeText(this, "图片选择器未初始化", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickMediaLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );
            });
        }

        // 识别按钮 - 添加空值检查
        if (btnIdentify != null) {
            btnIdentify.setOnClickListener(v -> {
                if (selectedImage == null) {
                    Toast.makeText(this, "请先上传桃子照片", Toast.LENGTH_SHORT).show();
                    return;
                }
                identifyPeach();
            });
        }
    }

    private void identifyPeach() {
        btnIdentify.setText("识别中...");
        btnIdentify.setEnabled(false);

        // 显示加载进度
        showLoading("正在上传图片...");

        // 使用异步任务上传图片并获取识别结果
        new Thread(() -> {
            try {
                // 上传图片到服务器
                updateLoadingHint("正在上传图片...");
                String response = uploadImageToServer(selectedImage);

                updateLoadingHint("正在识别中...");

                // 检查服务器返回的错误信息
                if (response.contains("\"msg\":\"error\"") || response.contains("\"error\":")) {
                    runOnUiThread(() -> {
                        hideLoading();
                        btnIdentify.setText("重新识别");
                        btnIdentify.setEnabled(true);
                        try {
                            JSONObject errorJson = new JSONObject(response);
                            String errorMsg = errorJson.optString("error", "服务器返回错误");
                            Toast.makeText(this, "识别失败：" + errorMsg, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "识别失败：服务器错误\n" + response, Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                JSONObject jsonResponse = new JSONObject(response);

                // 获取识别结果的图片URL
                String imageUrl = jsonResponse.optString("imgs_url", "");
                // 修复模拟器无法访问宿主机IP的问题：如果服务器返回的是局域网IP，替换为10.0.2.2
                if (!imageUrl.isEmpty()) {
                     imageUrl = imageUrl.replaceAll("http://[^:]+:8081", "http://10.0.2.2:8081");
                }
                String messages = jsonResponse.optString("messages", "");

                // 下载识别结果图片
                if (!imageUrl.isEmpty()) {
                    updateLoadingHint("正在下载结果...");
                    resultImage = downloadImage(imageUrl);

                    // 解析识别结果（从 messages 中提取）
                    String detectedClass = extractDetectedClass(messages);

                    // 在主线程更新UI
                    runOnUiThread(() -> {
                        hideLoading();

                        // 更新识别结果图片
                        if (resultImage != null) {
                            ivResult.setImageBitmap(resultImage);
                            ivResult.setAlpha(1.0f);
                        }

                        // 显示识别结果
                        llResults.removeAllViews();
                        emptyStateContainer.setVisibility(View.GONE);
                        scrollResults.setVisibility(View.VISIBLE);

                        // 1. 尝试匹配模拟数据（精确或模糊）
                        PeachResult match = mockResults.get(detectedClass);
                        if (match == null) {
                            for (String key : mockResults.keySet()) {
                                if (detectedClass.contains(key)) {
                                    match = mockResults.get(key);
                                    break;
                                }
                            }
                        }

                        // 2. 如果未找到匹配项，使用默认模拟数据（如"北京27号"）作为模板
                        if (match == null && !mockResults.isEmpty()) {
                            match = mockResults.get("北京27号");
                            if (match == null) {
                                match = mockResults.values().iterator().next();
                            }
                        }

                        if (match != null) {
                            // 3. 构造最终显示结果：
                            // - 品种名称：使用实际识别出的 detectedClass
                            // - 其他参数：使用模拟数据的参数
                            String finalVariety = (detectedClass != null && !detectedClass.isEmpty()) ? detectedClass : match.variety;
                            
                            PeachResult finalResult = new PeachResult(
                                    finalVariety,
                                    match.grade,
                                    match.sweetness,
                                    match.colorPercentage,
                                    match.weight,
                                    match.diameter,
                                    match.confidence,
                                    match.lightScore
                            );
                            
                            displayResults(finalResult);
                        } else {
                            // 兜底显示（理论上不应触发，除非mockResults为空）
                            if (!detectedClass.isEmpty()) {
                                addResultCard("识别结果", detectedClass, "检测成功", 95);
                            } else {
                                addResultCard("识别结果", messages, "检测完成", 85);
                            }
                        }

                        setSuccessState();

                        Toast.makeText(this, "识别完成！", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        hideLoading();
                        btnIdentify.setText("重新识别");
                        btnIdentify.setEnabled(true);
                        Toast.makeText(this, "识别失败，请重试", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoading();
                    btnIdentify.setText("重新识别");
                    btnIdentify.setEnabled(true);
                    Toast.makeText(this, "识别失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 显示加载进度
     */
    private void showLoading(String hint) {
        emptyStateContainer.setVisibility(View.GONE);
        scrollResults.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
        tvLoadingHint.setText(hint);

        // 更新按钮状态为加载中
        if (btnIdentify != null) {
            btnIdentify.setText("识别中...");
            btnIdentify.setEnabled(false);
        }
    }

    /**
     * 更新加载提示文本
     */
    private void updateLoadingHint(String hint) {
        runOnUiThread(() -> {
            if (tvLoadingHint != null) {
                tvLoadingHint.setText(hint);
            }
        });
    }

    /**
     * 隐藏加载进度
     */
    private void hideLoading() {
        layoutLoading.setVisibility(View.GONE);

        // 恢复按钮状态
        if (btnIdentify != null) {
            btnIdentify.setText("开始识别");
            btnIdentify.setEnabled(true);
        }
    }

    /**
     * 设置识别成功状态
     */
    private void setSuccessState() {
        if (btnIdentify != null) {
            btnIdentify.setText("识别完成");
            btnIdentify.setEnabled(true);
        }
    }

    /**
     * 上传图片到服务器
     */
    private String uploadImageToServer(Bitmap bitmap) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL(SERVER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // 将 Bitmap 转换为字节数组
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        DataOutputStream out = new DataOutputStream(conn.getOutputStream());

        // 写入文件数据
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"\r\n");
        out.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        out.write(imageData);
        out.writeBytes("\r\n");
        out.writeBytes("--" + boundary + "--\r\n");
        out.flush();

        // 读取响应
        int responseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    /**
     * 从 URL 下载图片
     */
    private Bitmap downloadImage(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream input = conn.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        return bitmap;
    }

    /**
     * 从识别结果中提取检测类别
     */
    private String extractDetectedClass(String messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // 去掉末尾的逗号，返回类别名称
        if (messages.endsWith(",")) {
            return messages.substring(0, messages.length() - 1);
        }
        return messages;
    }

    private void displayResults(PeachResult result) {
        addResultCard("品种识别", result.variety, String.format("置信度：%.1f%%", result.confidence), (int) result.confidence);
        addResultCard("综合评级", result.grade, "符合" + result.grade + "标准",
                result.grade.equals("特级果") ? 95 : result.grade.equals("一级果") ? 85 : 75);
        addResultCard("甜度预测", String.format("%.1f°Bx", result.sweetness), "误差范围：±0.3°Bx", (int) (result.sweetness * 7));
        addResultCard("成熟度", getMaturityLevel(result.colorPercentage),
                String.format("着色面积：%d%%", result.colorPercentage), result.colorPercentage);
        addResultCard("果实尺寸", getSizeLevel(result.weight),
                String.format("重量：%dg，果径：%dmm", result.weight, result.diameter), (int) ((float) result.weight / 300 * 100));
        addResultCard("光照情况", getLightLevel(result.lightScore),
                String.format("光照评分：%d/100", result.lightScore), result.lightScore);

        // 添加加工建议
        String[] processingRecommendation = getProcessingRecommendation(result);
        addProcessingCard(processingRecommendation[0], processingRecommendation[1], processingRecommendation[2]);
    }

    private String getMaturityLevel(int colorPercentage) {
        if (colorPercentage >= 80) return "完全成熟";
        else if (colorPercentage >= 60) return "基本成熟";
        else return "未完全成熟";
    }

    private String getSizeLevel(int weight) {
        if (weight >= 220) return "大果";
        else if (weight >= 180) return "中果";
        else return "小果";
    }

    private String getLightLevel(int score) {
        if (score >= 90) return "光照充足";
        else if (score >= 70) return "光照良好";
        else return "光照一般";
    }

    /**
     * 根据桃子指标判断适合的加工方式
     * @param result 桃子检测结果
     * @return [加工类型, 适合原因, 推荐指数]
     */
    private String[] getProcessingRecommendation(PeachResult result) {
        String processingType;
        String reason;
        String recommendation;
        int score = 0;

        // 综合评分计算
        float sweetness = result.sweetness;
        int maturity = result.colorPercentage;
        int weight = result.weight;
        String grade = result.grade;

        // 判断逻辑
        if (sweetness >= 12.0 && maturity >= 70 && weight >= 200) {
            // 高甜度+高成熟度+大果 → 适合做桃干
            processingType = "🍑 桃干加工";
            reason = "高甜度且果肉厚实，脱水后口感浓郁，糖分保存率高";
            score = 95;
            recommendation = "强烈推荐";
        } else if (sweetness >= 11.0 && maturity >= 80 && grade.equals("特级果")) {
            // 高甜度+完全成熟+特级果 → 适合做果脯
            processingType = "🍬 糖渍果脯";
            reason = "完全成熟的特级果，糖渍后色泽诱人，口感软糯香甜";
            score = 92;
            recommendation = "强烈推荐";
        } else if (sweetness >= 10.0 && maturity >= 60) {
            // 中高甜度+基本成熟 → 适合做桃酒/桃醋
            if (result.lightScore >= 80) {
                processingType = "🍷 桃子酒";
                reason = "糖度适中且光照充足，发酵后酒香浓郁，口感醇厚";
                score = 88;
                recommendation = "推荐";
            } else {
                processingType = "🥫 桃子醋";
                reason = "酸甜适中，适合酿制果醋，营养价值高";
                score = 85;
                recommendation = "推荐";
            }
        } else if (maturity >= 85 && weight >= 180) {
            // 高成熟度+中大果 → 适合做桃汁
            processingType = "🧃 鲜榨桃汁";
            reason = "完全成熟且汁水丰富，出汁率高，口感新鲜香甜";
            score = 90;
            recommendation = "推荐";
        } else if (weight >= 220 && grade.equals("特级果")) {
            // 大果+特级果 → 适合做罐头
            processingType = "🥫 糖水罐头";
            reason = "特级大果，果形完整，制作罐头卖相佳，口感脆嫩";
            score = 87;
            recommendation = "推荐";
        } else if (sweetness >= 13.0 && maturity >= 75) {
            // 超高甜度 → 适合做桃酱
            processingType = "🍯 桃子果酱";
            reason = "超高甜度，制作果酱时无需额外加糖，口感纯正";
            score = 93;
            recommendation = "强烈推荐";
        } else if (maturity >= 80) {
            // 高成熟度 → 适合做冷冻桃肉
            processingType = "❄️ 冷冻桃肉";
            reason = "完全成熟，冷冻后保持鲜嫩口感，适合烘焙和甜品";
            score = 82;
            recommendation = "较适合";
        } else {
            // 其他情况 → 适合做综合制品
            processingType = "🍧 综合加工";
            reason = "各项指标均衡，可制作桃子派、桃子冰淇淋等综合制品";
            score = 78;
            recommendation = "较适合";
        }

        return new String[]{processingType, reason, recommendation + " (匹配度" + score + "/100)"};
    }

    /**
     * 添加加工建议卡片
     */
    private void addProcessingCard(String type, String reason, String recommendation) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_peach_result, llResults, false);

        TextView tvTitle = cardView.findViewById(R.id.tv_item_title);
        TextView tvValue = cardView.findViewById(R.id.tv_item_value);
        TextView tvDesc = cardView.findViewById(R.id.tv_item_description);
        ProgressBar progressBar = cardView.findViewById(R.id.pb_item_progress);

        tvTitle.setText("💡 加工建议");
        tvValue.setText(type);
        tvDesc.setText(reason + "\n" + recommendation);

        // 设置进度条颜色（加工建议用不同颜色）
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            // 从recommendation中提取分数
            int score = 75; // 默认值
            try {
                String scoreStr = recommendation.substring(recommendation.indexOf("匹配度") + 3, recommendation.indexOf("/100"));
                score = Integer.parseInt(scoreStr);
            } catch (Exception e) {
                // 解析失败使用默认值
            }
            progressBar.setProgress(score);
        }

        llResults.addView(cardView);
    }

    private void addResultCard(String title, String value, String description, int progress) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_peach_result, llResults, false);

        TextView tvTitle = cardView.findViewById(R.id.tv_item_title);
        TextView tvValue = cardView.findViewById(R.id.tv_item_value);
        TextView tvDesc = cardView.findViewById(R.id.tv_item_description);
        ProgressBar progressBar = cardView.findViewById(R.id.pb_item_progress);

        tvTitle.setText(title);
        tvValue.setText(value);
        tvDesc.setText(description);

        if (progress > 0) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
        }

        llResults.addView(cardView);
    }

    private static class PeachResult {
        String variety;
        String grade;
        float sweetness;
        int colorPercentage;
        int weight;
        int diameter;
        float confidence;
        int lightScore;

        PeachResult(String variety, String grade, float sweetness, int colorPercentage,
                    int weight, int diameter, float confidence, int lightScore) {
            this.variety = variety;
            this.grade = grade;
            this.sweetness = sweetness;
            this.colorPercentage = colorPercentage;
            this.weight = weight;
            this.diameter = diameter;
            this.confidence = confidence;
            this.lightScore = lightScore;
        }
    }
}
