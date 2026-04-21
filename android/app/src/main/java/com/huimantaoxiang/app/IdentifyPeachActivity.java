package com.huimantaoxiang.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import java.io.InputStream;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
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

import com.huimantaoxiang.util.PeachDataLoader;

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
    private String selectedModel = "YOLOv11标准模型";
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private AlertDialog dialog;

    // 服务器配置
    private static final String SERVER_URL = "https://foyer-freestyle-ditzy.ngrok-free.dev/upload";

    // 模拟数据 - 使用模型输出的英文类别代码作为key
    private Map<String, PeachResult> mockResults = new HashMap<>();

    // 类别代码到中文品种名称的映射（根据5-best.pt模型的实际类别）
    private static final Map<String, String> CATEGORY_TO_VARIETY = new HashMap<>();
    static {
        // 5-best.pt模型实际识别的4个桃子品种
        CATEGORY_TO_VARIETY.put("chunxue", "春雪");
        CATEGORY_TO_VARIETY.put("huangpantao", "黄蟠桃");
        CATEGORY_TO_VARIETY.put("wangchun", "晚春");
        CATEGORY_TO_VARIETY.put("ruipan", "瑞蟠");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_peach);

        initViews();
        initImagePicker();
        setupListeners();
        setupSpinner();
        initMockData();

        // 恢复保存的状态
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    /**
     * 处理singleTask模式下新的Intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // singleTask模式下，Activity实例被复用时会调用此方法
        // 不需要做任何处理，因为我们希望保持当前状态
    }

    /**
     * 保存Activity状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // 保存选择的图片
        if (selectedImage != null) {
            outState.putParcelable("selectedImage", selectedImage);
        }

        // 保存识别结果图片
        if (resultImage != null) {
            outState.putParcelable("resultImage", resultImage);
        }

        // 保存选择的模型
        outState.putString("selectedModel", selectedModel);

        // 保存识别按钮状态
        Button btnIdentify = findViewById(R.id.btn_identify);
        if (btnIdentify != null) {
            outState.putString("identifyButtonText", btnIdentify.getText().toString());
            outState.putBoolean("identifyButtonEnabled", btnIdentify.isEnabled());
        }
    }

    /**
     * 恢复Activity状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreState(savedInstanceState);
    }

    /**
     * 恢复状态的通用方法
     */
    private void restoreState(Bundle savedInstanceState) {
        // 恢复选择的图片
        selectedImage = savedInstanceState.getParcelable("selectedImage");
        if (selectedImage != null && ivPreview != null) {
            ivPreview.setImageBitmap(selectedImage);
            ivPreview.setAlpha(1.0f);
        }

        // 恢复识别结果图片
        resultImage = savedInstanceState.getParcelable("resultImage");
        if (resultImage != null && ivResult != null) {
            ivResult.setImageBitmap(resultImage);
            ivResult.setAlpha(1.0f);
        }

        // 恢复选择的模型
        String savedModel = savedInstanceState.getString("selectedModel");
        if (savedModel != null) {
            selectedModel = savedModel;
            // 恢复Spinner选择
            if (spinnerModel != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerModel.getAdapter();
                if (adapter != null) {
                    int position = adapter.getPosition(savedModel);
                    if (position >= 0) {
                        spinnerModel.setSelection(position);
                    }
                }
            }
        }

        // 恢复识别按钮状态
        String buttonText = savedInstanceState.getString("identifyButtonText");
        boolean buttonEnabled = savedInstanceState.getBoolean("identifyButtonEnabled", true);
        if (btnIdentify != null) {
            if (buttonText != null) {
                btnIdentify.setText(buttonText);
            }
            btnIdentify.setEnabled(buttonEnabled);
        }
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

    private void showTestImagePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择测试图片");

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(2);
        gridLayout.setPadding(40, 20, 40, 20);

        int[] imageIds = {
            R.drawable.test_peach_1,
            R.drawable.test_peach_2,
            R.drawable.test_peach_3,
            R.drawable.test_peach_4
        };

        String[] imageNames = {"测试图片 1", "测试图片 2", "测试图片 3", "测试图片 4"};

        for (int i = 0; i < imageIds.length; i++) {
            final int index = i;
            ImageView imageView = new ImageView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 300;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(10, 10, 10, 10);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(imageIds[i]);
            imageView.setOnClickListener(v -> {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imageIds[index]);
                selectedImage = bitmap;
                ivPreview.setImageBitmap(bitmap);
                ivPreview.setAlpha(1.0f);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(this, "已选择: " + imageNames[index], Toast.LENGTH_SHORT).show();
            });
            gridLayout.addView(imageView);
        }

        builder.setView(gridLayout);
        dialog = builder.create();
        dialog.show();
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
        models.add("YOLOv11标准模型");
        models.add("改进YOLOv11模型（高精度）");
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
        java.util.Map<String, PeachDataLoader.PeachParams> paramsData =
            PeachDataLoader.loadPeachParams(this);

        for (PeachDataLoader.PeachParams params : paramsData.values()) {
            PeachResult result = new PeachResult(
                params.name,
                params.grade,
                params.sweetness,
                params.maturity,
                params.weight,
                params.diameter,
                params.confidence,
                params.lightScore
            );
            mockResults.put(params.code, result);
        }

        Log.d("IdentifyPeach", "从配置文件加载了" + mockResults.size() + "个品种数据");
    }

    private void setupListeners() {
        // 返回按钮 - 先检查是否为空
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> showTestImagePicker());
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
                // 使用公网IP，不需要替换IP地址
                // imageUrl = imageUrl.replaceAll("http://[^:]+:8081", "http://10.0.2.2:8081");
                String messages = jsonResponse.optString("messages", "");

                // 下载识别结果图片
                if (!imageUrl.isEmpty()) {
                    updateLoadingHint("正在下载结果...");
                    String imageUrlWithTimestamp = imageUrl + "?t=" + System.currentTimeMillis();
                    resultImage = downloadImage(imageUrlWithTimestamp);

                    // 解析识别结果（从 messages 中提取）
                    String detectedClass = extractDetectedClass(messages);

                    // 🔍 调试信息：显示实际识别的类别代码
                    android.util.Log.d("IdentifyPeach", "========== 识别结果调试 ==========");
                    android.util.Log.d("IdentifyPeach", "原始messages: " + messages);
                    android.util.Log.d("IdentifyPeach", "解析后类别代码: " + detectedClass);
                    android.util.Log.d("IdentifyPeach", "结果图片URL: " + imageUrl);
                    android.util.Log.d("IdentifyPeach", "================================");

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

                        // 1. 尝试匹配模拟数据（使用英文类别代码）
                        android.util.Log.d("IdentifyPeach", "模型识别类别代码: " + detectedClass);
                        PeachResult match = mockResults.get(detectedClass);

                        // 2. 如果直接匹配失败，尝试通过映射表转换后匹配
                        if (match == null) {
                            String chineseVariety = CATEGORY_TO_VARIETY.get(detectedClass);
                            android.util.Log.d("IdentifyPeach", "映射尝试: " + detectedClass + " -> " + chineseVariety);
                            if (chineseVariety != null) {
                                // 查找是否有对应中文名称的数据
                                for (PeachResult result : mockResults.values()) {
                                    if (result.variety.equals(chineseVariety)) {
                                        match = result;
                                        android.util.Log.d("IdentifyPeach", "通过映射找到匹配: " + chineseVariety);
                                        break;
                                    }
                                }
                            }
                        }

                        // 3. 如果还是找不到，尝试模糊匹配（处理特殊情况）
                        if (match == null) {
                            for (String key : mockResults.keySet()) {
                                if (detectedClass.contains(key) || key.contains(detectedClass)) {
                                    match = mockResults.get(key);
                                    break;
                                }
                            }
                        }

                        // 4. 如果未找到匹配项，使用默认模拟数据作为模板
                        if (match == null && !mockResults.isEmpty()) {
                            match = mockResults.get("cx_aru");  // 使用第一个品种作为默认
                            if (match == null) {
                                match = mockResults.values().iterator().next();
                            }
                        }

                        if (match != null) {
                            // 5. 构造最终显示结果：
                            // - 品种名称：优先使用映射后的中文名称，如果映射失败则使用原始类别代码
                            // - 其他参数：使用模拟数据的参数
                            String finalVariety;
                            if (detectedClass != null && !detectedClass.isEmpty()) {
                                // 尝试将英文类别代码映射为中文名称
                                String mappedVariety = CATEGORY_TO_VARIETY.get(detectedClass);
                                finalVariety = (mappedVariety != null) ? mappedVariety : match.variety;
                            } else {
                                finalVariety = match.variety;
                            }
                            
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
        // ✨ 首先显示加工建议（作为主要内容）
        String[] processingRecommendation = getProcessingRecommendation(result);
        addProcessingRecommendationCard(result, processingRecommendation);

        // 添加分割线
        addDivider();

        // 然后显示其他检测指标
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
     * @return [加工类型, 适合原因, 推荐等级, 匹配分数, 优势说明, 加工提示, 关键指标数组]
     */
    private String[] getProcessingRecommendation(PeachResult result) {
        String processingType;
        String reason;
        String recommendation;
        String advantages = "";
        String tips = "";
        String[] keyIndicators;
        int score = 0;

        // 综合评分计算
        float sweetness = result.sweetness;
        int maturity = result.colorPercentage;
        int weight = result.weight;
        String grade = result.grade;
        int lightScore = result.lightScore;

        // 判断逻辑
        if (sweetness >= 12.0 && maturity >= 70 && weight >= 200) {
            // 高甜度+高成熟度+大果 → 适合做桃干
            processingType = "🍑 桃干加工";
            reason = "高甜度且果肉厚实，脱水后口感浓郁，糖分保存率高";
            score = 95;
            recommendation = "强烈推荐";
            advantages = "营养丰富 | 保存期长 | 运输方便";
            tips = "建议选用完全成熟的果实，去皮去核后切片晾晒，控制温度在50-60℃";
            keyIndicators = new String[]{
                "🍬 甜度 " + sweetness + "°Bx",
                "⚖️ 重量 " + weight + "g",
                "🌈 成熟度 " + maturity + "%"
            };
        } else if (sweetness >= 11.0 && maturity >= 80 && grade.equals("特级果")) {
            // 高甜度+完全成熟+特级果 → 适合做果脯
            processingType = "🍬 糖渍果脯";
            reason = "完全成熟的特级果，糖渍后色泽诱人，口感软糯香甜";
            score = 92;
            recommendation = "强烈推荐";
            advantages = "色泽鲜艳 | 口感软糯 | 易于保存";
            tips = "采用分次糖渍工艺，使果肉充分吸收糖分，保持果形完整";
            keyIndicators = new String[]{
                "🏆 特级果",
                "🍬 甜度 " + sweetness + "°Bx",
                "🌈 成熟度 " + maturity + "%"
            };
        } else if (sweetness >= 10.0 && maturity >= 60) {
            // 中高甜度+基本成熟 → 适合做桃酒/桃醋
            if (lightScore >= 80) {
                processingType = "🍷 桃子酒";
                reason = "糖度适中且光照充足，发酵后酒香浓郁，口感醇厚";
                score = 88;
                recommendation = "推荐";
                advantages = "酒香浓郁 | 营养保留 | 度数适中";
                tips = "选用新鲜完好果实，清洗干净后破碎发酵，控制发酵温度在20-25℃";
                keyIndicators = new String[]{
                    "🍬 甜度 " + sweetness + "°Bx",
                    "☀️ 光照 " + lightScore + "/100",
                    "🌈 成熟度 " + maturity + "%"
                };
            } else {
                processingType = "🥫 桃子醋";
                reason = "酸甜适中，适合酿制果醋，营养价值高";
                score = 85;
                recommendation = "推荐";
                advantages = "酸甜可口 | 消化助益 | 天然健康";
                tips = "采用醋酸发酵工艺，发酵时间约30-45天，定期搅拌通风";
                keyIndicators = new String[]{
                    "🍬 甜度 " + sweetness + "°Bx",
                    "🌈 成熟度 " + maturity + "%",
                    "⚖️ 重量 " + weight + "g"
                };
            }
        } else if (maturity >= 85 && weight >= 180) {
            // 高成熟度+中大果 → 适合做桃汁
            processingType = "🧃 鲜榨桃汁";
            reason = "完全成熟且汁水丰富，出汁率高，口感新鲜香甜";
            score = 90;
            recommendation = "推荐";
            advantages = "新鲜原味 | 维生素丰富 | 即饮便捷";
            tips = "采用冷压榨取工艺，避免高温加热，保留更多营养成分";
            keyIndicators = new String[]{
                "🌈 成熟度 " + maturity + "%",
                "⚖️ 重量 " + weight + "g",
                "💧 汁水丰富"
            };
        } else if (weight >= 220 && grade.equals("特级果")) {
            // 大果+特级果 → 适合做罐头
            processingType = "🥫 糖水罐头";
            reason = "特级大果，果形完整，制作罐头卖相佳，口感脆嫩";
            score = 87;
            recommendation = "推荐";
            advantages = "果形完整 | 卖相极佳 | 保质期长";
            tips = "选用八成熟果实，去皮去核后保持果形完整，糖水浓度40-50%";
            keyIndicators = new String[]{
                "🏆 特级果",
                "⚖️ 重量 " + weight + "g",
                "📏 果径 " + result.diameter + "mm"
            };
        } else if (sweetness >= 13.0 && maturity >= 75) {
            // 超高甜度 → 适合做桃酱
            processingType = "🍯 桃子果酱";
            reason = "超高甜度，制作果酱时无需额外加糖，口感纯正";
            score = 93;
            recommendation = "强烈推荐";
            advantages = "口感纯正 | 天然甜味 | 用途广泛";
            tips = "小火慢熬，不停搅拌至浓稠状，可添加柠檬汁增香防腐";
            keyIndicators = new String[]{
                "🍬 甜度 " + sweetness + "°Bx",
                "🌈 成熟度 " + maturity + "%",
                "✨ 无需加糖"
            };
        } else if (maturity >= 80) {
            // 高成熟度 → 适合做冷冻桃肉
            processingType = "❄️ 冷冻桃肉";
            reason = "完全成熟，冷冻后保持鲜嫩口感，适合烘焙和甜品";
            score = 82;
            recommendation = "较适合";
            advantages = "保持新鲜 | 方便储存 | 用途多样";
            tips = "快速冷冻处理，使用前提前冷藏解冻，适合制作烘焙甜品";
            keyIndicators = new String[]{
                "🌈 成熟度 " + maturity + "%",
                "❄️ 冷冻保鲜",
                "🍃 适合烘焙"
            };
        } else {
            // 其他情况 → 适合做综合制品
            processingType = "🍧 综合加工";
            reason = "各项指标均衡，可制作桃子派、桃子冰淇淋等综合制品";
            score = 78;
            recommendation = "较适合";
            advantages = "灵活多样 | 创意空间 | 适合研发";
            tips = "可根据具体指标选择合适的加工方式，建议混合多种制品提高利用率";
            keyIndicators = new String[]{
                "📊 指标均衡",
                "🔧 灵活加工",
                "💡 多种选择"
            };
        }

        return new String[]{processingType, reason, recommendation, String.valueOf(score), advantages, tips};
    }

    /**
     * 添加分割线
     */
    private void addDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        ));
        divider.setBackgroundColor(getResources().getColor(R.color.color_card_identify));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
        params.setMargins(40, 20, 40, 20);
        divider.setLayoutParams(params);
        llResults.addView(divider);
    }

    /**
     * 添加加工建议卡片（使用新布局）
     */
    private void addProcessingRecommendationCard(PeachResult result, String[] recommendation) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_processing_recommendation, llResults, false);

        // 绑定视图
        TextView tvProcessingType = cardView.findViewById(R.id.tv_processing_type);
        TextView tvRecommendationLevel = cardView.findViewById(R.id.tv_recommendation_level);
        TextView tvRecommendationReason = cardView.findViewById(R.id.tv_recommendation_reason);
        TextView tvMatchScore = cardView.findViewById(R.id.tv_match_score);
        TextView tvAdvantages = cardView.findViewById(R.id.tv_advantages);
        TextView tvProcessingTips = cardView.findViewById(R.id.tv_processing_tips);
        LinearLayout llKeyIndicators = cardView.findViewById(R.id.ll_key_indicators);

        // 设置内容
        tvProcessingType.setText(recommendation[0]); // 加工类型

        // 设置推荐等级（根据分数显示星星）
        String level = recommendation[2]; // 推荐等级
        int score = Integer.parseInt(recommendation[3]); // 匹配分数
        String stars = "";
        if (score >= 90) stars = "⭐⭐⭐⭐⭐";
        else if (score >= 80) stars = "⭐⭐⭐⭐";
        else if (score >= 70) stars = "⭐⭐⭐";
        else stars = "⭐⭐";

        tvRecommendationLevel.setText(stars + " " + level);
        tvRecommendationReason.setText(recommendation[1]); // 原因
        tvMatchScore.setText("匹配度 " + score + "%");
        tvAdvantages.setText("✨ 优势：" + recommendation[4]); // 优势
        tvProcessingTips.setText("💡 加工提示：" + recommendation[5]); // 加工提示

        // 添加关键指标
        String[] keyIndicators = getKeyIndicators(result, recommendation[0]);
        for (String indicator : keyIndicators) {
            TextView indicatorView = new TextView(this);
            indicatorView.setText(indicator);
            indicatorView.setTextSize(13);
            indicatorView.setTextColor(getResources().getColor(R.color.deep_purple_text));
            indicatorView.setPadding(20, 6, 0, 6);
            llKeyIndicators.addView(indicatorView);
        }

        llResults.addView(cardView);
    }

    /**
     * 根据加工类型返回关键指标
     */
    private String[] getKeyIndicators(PeachResult result, String processingType) {
        if (processingType.contains("桃干")) {
            return new String[]{
                "🍬 甜度：" + result.sweetness + "°Bx",
                "⚖️ 重量：" + result.weight + "g",
                "🌈 成熟度：" + result.colorPercentage + "%"
            };
        } else if (processingType.contains("果脯")) {
            return new String[]{
                "🏆 等级：" + result.grade,
                "🍬 甜度：" + result.sweetness + "°Bx",
                "🌈 成熟度：" + result.colorPercentage + "%"
            };
        } else if (processingType.contains("桃酒")) {
            return new String[]{
                "🍬 甜度：" + result.sweetness + "°Bx",
                "☀️ 光照：" + result.lightScore + "/100",
                "🌈 成熟度：" + result.colorPercentage + "%"
            };
        } else if (processingType.contains("桃汁")) {
            return new String[]{
                "🌈 成熟度：" + result.colorPercentage + "%",
                "⚖️ 重量：" + result.weight + "g",
                "💧 汁水丰富"
            };
        } else if (processingType.contains("罐头")) {
            return new String[]{
                "🏆 等级：" + result.grade,
                "⚖️ 重量：" + result.weight + "g",
                "📏 果径：" + result.diameter + "mm"
            };
        } else if (processingType.contains("果酱")) {
            return new String[]{
                "🍬 甜度：" + result.sweetness + "°Bx",
                "🌈 成熟度：" + result.colorPercentage + "%",
                "✨ 无需加糖"
            };
        } else if (processingType.contains("冷冻")) {
            return new String[]{
                "🌈 成熟度：" + result.colorPercentage + "%",
                "❄️ 冷冻保鲜",
                "🍃 适合烘焙"
            };
        } else {
            return new String[]{
                "📊 指标均衡",
                "🔧 灵活加工",
                "💡 多种选择"
            };
        }
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
