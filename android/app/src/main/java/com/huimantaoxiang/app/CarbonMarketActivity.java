package com.huimantaoxiang.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CarbonMarketActivity extends AppCompatActivity {

    private TextView tvCurrentPrice, tvPriceChange, tvTotalCarbon, tvMyCarbon, tvMarketTrend;
    private EditText editAmount;
    private Button btnBuy, btnSell, btnRefresh;
    private CardView cardMarketStatus;
    private View viewPriceChart;

    private double currentPrice = 45.8;
    private double myCarbonCredits = 1250.0;
    private double totalCarbonTraded = 158600.0;
    private double priceChange = 2.3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_market);

        initViews();

        // 恢复保存的状态
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            initData();
        }

        setupListeners();
        animatePriceUpdate();
        animateChart();
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

        // 保存市场数据
        outState.putDouble("currentPrice", currentPrice);
        outState.putDouble("myCarbonCredits", myCarbonCredits);
        outState.putDouble("totalCarbonTraded", totalCarbonTraded);
        outState.putDouble("priceChange", priceChange);

        // 保存输入框内容
        if (editAmount != null) {
            outState.putString("amount", editAmount.getText().toString());
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
        // 恢复市场数据
        currentPrice = savedInstanceState.getDouble("currentPrice", 45.8);
        myCarbonCredits = savedInstanceState.getDouble("myCarbonCredits", 1250.0);
        totalCarbonTraded = savedInstanceState.getDouble("totalCarbonTraded", 158600.0);
        priceChange = savedInstanceState.getDouble("priceChange", 2.3);

        updateMarketData();
        updateCarbonData();
        updateMarketTrend();

        // 恢复输入框内容
        String amount = savedInstanceState.getString("amount", "");
        if (editAmount != null) {
            editAmount.setText(amount);
        }
    }

    private void initViews() {
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        tvPriceChange = findViewById(R.id.tvPriceChange);
        tvTotalCarbon = findViewById(R.id.tvTotalCarbon);
        tvMyCarbon = findViewById(R.id.tvMyCarbon);
        tvMarketTrend = findViewById(R.id.tvMarketTrend);
        editAmount = findViewById(R.id.editAmount);
        btnBuy = findViewById(R.id.btnBuy);
        btnSell = findViewById(R.id.btnSell);
        btnRefresh = findViewById(R.id.btnRefresh);
        cardMarketStatus = findViewById(R.id.cardMarketStatus);
        viewPriceChart = findViewById(R.id.viewPriceChart);
    }

    private void initData() {
        updateMarketData();
        updateCarbonData();
        updateMarketTrend();
    }

    private void setupListeners() {
        btnBuy.setOnClickListener(v -> executeTrade(true));
        btnSell.setOnClickListener(v -> executeTrade(false));
        btnRefresh.setOnClickListener(v -> refreshMarketData());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void executeTrade(boolean isBuy) {
        String amountStr = editAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入交易数量", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            double totalCost = amount * currentPrice;

            if (isBuy) {
                if (amount > 0) {
                    myCarbonCredits += amount;
                    totalCarbonTraded += amount;
                    Toast.makeText(this,
                            String.format("成功购买 %.1f 碳汇，花费 ¥%.2f", amount, totalCost),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "请输入有效的数量", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (amount <= myCarbonCredits) {
                    myCarbonCredits -= amount;
                    totalCarbonTraded += amount;
                    Toast.makeText(this,
                            String.format("成功出售 %.1f 碳汇，获得 ¥%.2f", amount, totalCost),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "碳汇余额不足", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 模拟价格变化
            priceChange = (Math.random() - 0.5) * 10;
            currentPrice += priceChange;

            updateMarketData();
            updateCarbonData();
            animatePriceUpdate();

            editAmount.setText("");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数量", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshMarketData() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("刷新中...");

        // 模拟数据刷新
        new android.os.Handler().postDelayed(() -> {
            priceChange = (Math.random() - 0.5) * 10;
            currentPrice += priceChange;
            totalCarbonTraded += Math.random() * 1000;

            updateMarketData();
            updateMarketTrend();
            animatePriceUpdate();
            animateChart();

            btnRefresh.setEnabled(true);
            btnRefresh.setText("刷新数据");

            Toast.makeText(this, "数据已更新", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void updateMarketData() {
        tvCurrentPrice.setText(String.format("¥%.2f", currentPrice));

        String changeText = String.format("%+.2f%%", priceChange);
        tvPriceChange.setText(changeText);

        if (priceChange >= 0) {
            tvPriceChange.setTextColor(0xFF4ECDC4);
        } else {
            tvPriceChange.setTextColor(0xFFFF6B6B);
        }
    }

    private void updateCarbonData() {
        tvMyCarbon.setText(String.format("%.1f 吨", myCarbonCredits));
        tvTotalCarbon.setText(String.format("%.1f 吨", totalCarbonTraded));
    }

    private void updateMarketTrend() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());

        String trendText;
        if (priceChange > 0) {
            trendText = "市场趋势: 上升 ↗";
            tvMarketTrend.setTextColor(0xFF4ECDC4);
        } else if (priceChange < 0) {
            trendText = "市场趋势: 下降 ↘";
            tvMarketTrend.setTextColor(0xFFFF6B6B);
        } else {
            trendText = "市场趋势: 平稳 →";
            tvMarketTrend.setTextColor(0xFF666666);
        }

        tvMarketTrend.setText(trendText + " | " + time);
    }

    private void animatePriceUpdate() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(500);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float scale = 1 + 0.1f * (1 - animation.getAnimatedFraction());
            tvCurrentPrice.setScaleX(scale);
            tvCurrentPrice.setScaleY(scale);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tvCurrentPrice.setScaleX(1);
                tvCurrentPrice.setScaleY(1);
            }
        });

        animator.start();
    }

    private void animateChart() {
        // 简单的图表高度动画
        final View[] bars = {
                findViewById(R.id.bar1),
                findViewById(R.id.bar2),
                findViewById(R.id.bar3),
                findViewById(R.id.bar4),
                findViewById(R.id.bar5),
                findViewById(R.id.bar6),
                findViewById(R.id.bar7)
        };

        for (int i = 0; i < bars.length; i++) {
            final View bar = bars[i];
            if (bar != null) {
                final float targetHeight = 50 + (float) (Math.random() * 150);

                bar.postDelayed(() -> {
                    ValueAnimator animator = ValueAnimator.ofFloat(0, targetHeight);
                    animator.setDuration(800);
                    animator.setInterpolator(new DecelerateInterpolator());

                    animator.addUpdateListener(animation -> {
                        float height = (float) animation.getAnimatedValue();
                        bar.getLayoutParams().height = (int) height;
                        bar.requestLayout();
                    });

                    animator.start();
                }, i * 100);
            }
        }
    }
}
