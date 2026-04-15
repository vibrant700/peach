package com.huimantaoxiang.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置功能按钮
        setupFunctionButtons();

        // 设置扇形图数据
        setupPieChart();
    }

    /**
     * 设置功能按钮
     */
    private void setupFunctionButtons() {
        // 识桃按钮 - 橙色渐变
        setupFunctionButton(R.id.btn_identify, R.drawable.ic_peach_1,
                R.string.feature_identify_title, IdentifyPeachActivity.class,
                0);

        // 桃宝按钮 - 蓝色渐变
        setupFunctionButton(R.id.btn_shop, R.drawable.ic_peach_5,
                R.string.feature_taobao_title, TaobaoAIActivity.class,
                0);

        // 桃农社区按钮 - 黄色渐变
        setupFunctionButton(R.id.btn_community, R.drawable.ic_peach_3,
                R.string.feature_community_title, CommunityActivity.class,
                0);

        // 桃源工坊按钮 - 绿色渐变
        setupFunctionButton(R.id.btn_workshop, R.drawable.ic_peach_2,
                R.string.feature_market_title, PeachWorkshopActivity.class,
                0);

        // 仓库管理按钮 - 紫色渐变
        setupFunctionButton(R.id.btn_irrigation, R.drawable.ic_peach_4,
                R.string.feature_irrigation_title, WarehouseActivity.class,
                0);

        // 碳汇市场按钮 - 浅黄渐变
        setupFunctionButton(R.id.btn_price, R.drawable.ic_peach_6,
                R.string.feature_price_title, CarbonMarketActivity.class,
                0);
    }

    /**
     * 设置单个功能按钮
     */
    private void setupFunctionButton(int buttonId, int iconRes, int titleRes,
                                     Class<?> targetActivity, int iconBgRes) {
        LinearLayout button = findViewById(buttonId);
        if (button != null) {
            ImageView iconView = button.findViewById(R.id.iv_function_icon);
            TextView titleView = button.findViewById(R.id.tv_function_name);
            FrameLayout iconContainer = (FrameLayout) iconView.getParent();

            iconView.setImageResource(iconRes);
            titleView.setText(titleRes);

            // 设置不同的图标背景
            if (iconContainer != null) {
                if (iconBgRes != 0) {
                    iconContainer.setBackgroundResource(iconBgRes);
                } else {
                    iconContainer.setBackground(null);
                }
            }

            // 添加波纹效果
            button.setBackgroundResource(R.drawable.ripple_function_button);

            final Class<?> activity = targetActivity;
            button.setOnClickListener(v -> {
                // 点击动画效果
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                    })
                    .start();

                // 延迟跳转，让动画完成
                v.postDelayed(() -> {
                    if (activity != null) {
                        // 跳转到对应的Activity
                        Intent intent = new Intent(MainActivity.this, activity);
                        // singleTask模式会自动复用已存在的Activity实例
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        // 显示开发中提示
                        String title = getString(titleRes);
                        Toast.makeText(MainActivity.this,
                                getString(R.string.function_dev, title),
                                Toast.LENGTH_SHORT).show();
                    }
                }, 150);
            });
        }
    }

    /**
     * 设置扇形图数据
     */
    private void setupPieChart() {
        PieChartView pieChart = findViewById(R.id.pieChart);
        LinearLayout legendContainer = findViewById(R.id.legendContainer);

        if (pieChart == null || legendContainer == null) {
            return;
        }

        // 模拟库存数据（产品名称和数量）
        Map<String, Integer> inventoryData = new HashMap<>();
        inventoryData.put("水蜜桃", 120);
        inventoryData.put("黄桃", 85);
        inventoryData.put("油桃", 45);
        inventoryData.put("桃罐头", 200);
        inventoryData.put("桃干", 67);
        inventoryData.put("桃种子", 500);

        // 计算总数
        int total = 0;
        for (int value : inventoryData.values()) {
            total += value;
        }

        // 准备扇形图数据
        List<PieChartView.PieSlice> slices = new ArrayList<>();
        int colorIndex = 0;
        int[] colors = {
                Color.parseColor("#FF8A9B"),  // 桃红 - 水蜜桃
                Color.parseColor("#FFD166"),  // 种子黄 - 桃罐头
                Color.parseColor("#D4A373"),  // 枝干棕 - 桃干
                Color.parseColor("#FFAE42"),  // 油桃橙 - 油桃
                Color.parseColor("#FFC857"),  // 黄桃金 - 黄桃
                Color.parseColor("#83E8B3")   // 水蜜桃绿 - 桃种子
        };

        // 清空图例容器
        legendContainer.removeAllViews();

        for (Map.Entry<String, Integer> entry : inventoryData.entrySet()) {
            String name = entry.getKey();
            int value = entry.getValue();
            float percentage = (value * 100f) / total;
            float sweepAngle = (value * 360f) / total;

            PieChartView.PieSlice slice = new PieChartView.PieSlice(name, value, colors[colorIndex % colors.length]);
            slice.percentage = percentage;
            slice.sweepAngle = sweepAngle;
            slices.add(slice);

            // 添加图例
            View legendView = LayoutInflater.from(this).inflate(R.layout.item_legend, legendContainer, false);
            TextView legendLabel = legendView.findViewById(R.id.tvLegendLabel);
            View legendColor = legendView.findViewById(R.id.viewLegendColor);

            legendLabel.setText(name + " " + String.format("%.1f%%", percentage));
            legendColor.setBackgroundColor(colors[colorIndex % colors.length]);

            legendContainer.addView(legendView);

            colorIndex++;
        }

        // 设置扇形图数据
        pieChart.setData(slices);
    }
}
