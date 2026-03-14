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
        setupFunctionButton(R.id.btn_identify, R.drawable.ic_menu_identify,
                R.string.feature_identify_title, IdentifyPeachActivity.class,
                R.drawable.bg_icon_gradient);

        // 桃宝按钮 - 蓝色渐变
        setupFunctionButton(R.id.btn_shop, R.drawable.ic_menu_shop,
                R.string.feature_taobao_title, TaobaoAIActivity.class,
                R.drawable.bg_icon_gradient2);

        // 桃农社区按钮 - 黄色渐变
        setupFunctionButton(R.id.btn_community, R.drawable.ic_menu_community,
                R.string.feature_community_title, CommunityActivity.class,
                R.drawable.bg_icon_gradient3);

        // 桃源工坊按钮 - 绿色渐变
        setupFunctionButton(R.id.btn_workshop, R.drawable.ic_menu_workshop,
                R.string.feature_market_title, PeachWorkshopActivity.class,
                R.drawable.bg_icon_gradient4);

        // 仓库管理按钮 - 紫色渐变
        setupFunctionButton(R.id.btn_irrigation, R.drawable.ic_menu_warehouse,
                R.string.feature_irrigation_title, WarehouseActivity.class,
                R.drawable.bg_icon_gradient5);

        // 碳汇市场按钮 - 浅黄渐变
        setupFunctionButton(R.id.btn_price, R.drawable.ic_menu_carbon,
                R.string.feature_price_title, CarbonMarketActivity.class,
                R.drawable.bg_icon_gradient6);
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
                iconContainer.setBackgroundResource(iconBgRes);
            }

            final Class<?> activity = targetActivity;
            button.setOnClickListener(v -> {
                if (activity != null) {
                    // 跳转到对应的Activity
                    Intent intent = new Intent(MainActivity.this, activity);
                    startActivity(intent);
                } else {
                    // 显示开发中提示
                    String title = getString(titleRes);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.function_dev, title),
                            Toast.LENGTH_SHORT).show();
                }
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
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#FFD93D"),
                Color.parseColor("#FF8B3D"),
                Color.parseColor("#FF6B9D"),
                Color.parseColor("#FFB6D9"),
                Color.parseColor("#4ECDC4")
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
