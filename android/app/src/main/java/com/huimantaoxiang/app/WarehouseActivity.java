package com.huimantaoxiang.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WarehouseActivity extends AppCompatActivity {

    private ListView listViewInventory;
    private long lastAddedItemId = -1; // 记录最后添加的项ID
    private boolean hasShownHighlight = false; // 是否已显示过高亮
    private EditText editProductName, editQuantity, editCategory, editSearch;
    private Button btnAdd, btnSearch, btnViewAll;
    private TextView tvTotalItems, tvTotalCategories, tvLowStock, tvCategoryCount;
    private InventoryAdapter adapter;
    private List<WarehouseItem> items;
    private boolean isSearchMode = false;

    // SharedPreferences用于持久化保存库存数据
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "warehouse_prefs";
    private static final String KEY_INVENTORY = "inventory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse);

        android.util.Log.d("WarehouseActivity", "onCreate开始");

        // 先初始化数据列表
        items = new ArrayList<>();

        // 初始化SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 初始化视图
        initViews();
        android.util.Log.d("WarehouseActivity", "视图初始化完成");

        // 初始化适配器
        adapter = new InventoryAdapter(this, items);
        listViewInventory.setAdapter(adapter);
        android.util.Log.d("WarehouseActivity", "适配器初始化完成");

        // 设置监听器
        setupListeners();

        // 加载保存的库存数据
        loadInventoryFromPrefs();

        // 如果没有库存数据，初始化示例数据
        if (items.isEmpty()) {
            android.util.Log.d("WarehouseActivity", "没有库存数据，初始化示例数据");
            initData();
        } else {
            android.util.Log.d("WarehouseActivity", "有库存数据，直接显示");
        }

        android.util.Log.d("WarehouseActivity", "onCreate完成，当前库存数量：" + items.size());
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
     * 从SharedPreferences加载库存数据
     */
    private void loadInventoryFromPrefs() {
        try {
            String inventoryJson = prefs.getString(KEY_INVENTORY, "");
            android.util.Log.d("WarehouseActivity", "加载库存数据，JSON长度：" + inventoryJson.length());

            if (!inventoryJson.isEmpty()) {
                JSONArray jsonArray = new JSONArray(inventoryJson);
                android.util.Log.d("WarehouseActivity", "解析到 " + jsonArray.length() + " 个库存项目");

                // 清空现有列表并添加新数据
                items.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemObj = jsonArray.getJSONObject(i);
                    WarehouseItem item = new WarehouseItem(
                        itemObj.getString("name"),
                        itemObj.getString("quantityDisplay"),
                        itemObj.getString("category"),
                        itemObj.getInt("quantity"),
                        itemObj.getString("color")
                    );
                    items.add(item);
                    android.util.Log.d("WarehouseActivity", "加载库存：" + item.name + ", 数量：" + item.quantity);
                }

                // 更新适配器
                if (adapter != null) {
                    adapter.updateData(items);
                    android.util.Log.d("WarehouseActivity", "适配器已更新，显示 " + items.size() + " 个项目");
                }

                updateStatistics();
            } else {
                android.util.Log.d("WarehouseActivity", "没有保存的库存数据");
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("WarehouseActivity", "加载库存数据失败：" + e.getMessage());
            // 如果加载失败，清空列表
            items.clear();
        }
    }

    /**
     * 保存库存数据到SharedPreferences
     */
    private void saveInventoryToPrefs() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (WarehouseItem item : items) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("name", item.name);
                itemObj.put("quantityDisplay", item.quantityDisplay);
                itemObj.put("category", item.category);
                itemObj.put("quantity", item.quantity);
                itemObj.put("color", item.color);
                jsonArray.put(itemObj);
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_INVENTORY, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("WarehouseActivity", "保存库存数据失败：" + e.getMessage());
        }
    }

    private void initViews() {
        listViewInventory = findViewById(R.id.listViewInventory);
        editProductName = findViewById(R.id.editProductName);
        editQuantity = findViewById(R.id.editQuantity);
        editCategory = findViewById(R.id.editCategory);
        editSearch = findViewById(R.id.editSearch);
        btnAdd = findViewById(R.id.btnAdd);
        btnSearch = findViewById(R.id.btnSearch);
        btnViewAll = findViewById(R.id.btnViewAll);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvTotalCategories = findViewById(R.id.tvTotalCategories);
        tvLowStock = findViewById(R.id.tvLowStock);
        tvCategoryCount = findViewById(R.id.tvCategoryCount);
    }

    private void initData() {
        android.util.Log.d("WarehouseActivity", "初始化示例数据");

        // 添加示例数据
        items.add(new WarehouseItem("水蜜桃", "120", "鲜桃", 120, "#FF6B6B"));
        items.add(new WarehouseItem("黄桃", "85", "鲜桃", 85, "#FFD93D"));
        items.add(new WarehouseItem("油桃", "45", "鲜桃", 45, "#FF8B3D"));
        items.add(new WarehouseItem("桃罐头", "200", "加工品", 200, "#FF6B9D"));
        items.add(new WarehouseItem("桃干", "67", "加工品", 67, "#FFB6D9"));
        items.add(new WarehouseItem("桃种子", "500", "种子", 500, "#4ECDC4"));

        android.util.Log.d("WarehouseActivity", "示例数据添加完成，共 " + items.size() + " 个项目");

        // 更新适配器
        if (adapter != null) {
            adapter.updateData(items);
            android.util.Log.d("WarehouseActivity", "适配器已更新");
        } else {
            android.util.Log.e("WarehouseActivity", "适配器为null！");
        }

        updateStatistics();

        // 确保按钮可用
        btnAdd.setEnabled(true);
        btnAdd.setAlpha(1.0f);

        android.util.Log.d("WarehouseActivity", "initData完成");
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> addInventory());
        btnSearch.setOnClickListener(v -> searchInventory());
        btnViewAll.setOnClickListener(v -> viewAllInventory());

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 移除TextWatcher输入验证，改为在添加时验证
        // 避免UI阻塞和输入延迟
    }

    // 验证输入并启用/禁用添加按钮（移除自动验证，改用手动触发）
    private void validateInputs() {
        boolean isValid = !editProductName.getText().toString().trim().isEmpty() &&
                         !editQuantity.getText().toString().trim().isEmpty() &&
                         !editCategory.getText().toString().trim().isEmpty();
        btnAdd.setEnabled(isValid);
        btnAdd.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void addInventory() {
        String name = editProductName.getText().toString().trim();
        String quantity = editQuantity.getText().toString().trim();
        String category = editCategory.getText().toString().trim();

        if (name.isEmpty() || quantity.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int qty = Integer.parseInt(quantity);

            // 检查是否已存在相同产品名称和类别的项目
            boolean found = false;
            int existingIndex = -1;

            for (int i = 0; i < items.size(); i++) {
                WarehouseItem item = items.get(i);
                if (item.name.equals(name) && item.category.equals(category)) {
                    found = true;
                    existingIndex = i;
                    break;
                }
            }

            if (found) {
                // 合并库存：更新现有项目的数量
                WarehouseItem existingItem = items.get(existingIndex);
                int oldQty = existingItem.quantity;
                int newQty = oldQty + qty;

                // 创建更新后的项目
                WarehouseItem updatedItem = new WarehouseItem(
                    name,
                    String.valueOf(newQty),
                    category,
                    newQty,
                    existingItem.color
                );

                // 替换现有项目
                items.set(existingIndex, updatedItem);

                // 记录最后添加的项ID，用于高亮显示
                lastAddedItemId = updatedItem.id;
                hasShownHighlight = false;

                // 如果在搜索模式下，返回全部显示
                if (isSearchMode) {
                    isSearchMode = false;
                    btnViewAll.setVisibility(View.GONE);
                    editSearch.setText("");
                }

                // 统一使用updateData刷新列表
                adapter.updateData(items);

                // 保存到SharedPreferences
                saveInventoryToPrefs();

                // 滚动到更新的项
                int finalIndex = existingIndex;
                listViewInventory.post(() -> {
                    listViewInventory.smoothScrollToPosition(finalIndex);
                });

                updateStatistics();
                clearInputs();
                validateInputs();
                Toast.makeText(this, "库存已合并：" + name + " (" + category + ")，从 " + oldQty + " 增加到 " + newQty, Toast.LENGTH_SHORT).show();
            } else {
                // 添加新项目
                WarehouseItem newItem = new WarehouseItem(name, quantity, category, qty, getRandomColor());
                items.add(newItem);

                // 记录最后添加的项ID，用于高亮显示
                lastAddedItemId = newItem.id;
                hasShownHighlight = false;

                // 如果在搜索模式下，添加后返回全部显示
                if (isSearchMode) {
                    isSearchMode = false;
                    btnViewAll.setVisibility(View.GONE);
                    editSearch.setText("");
                }

                // 统一使用updateData刷新列表
                adapter.updateData(items);

                // 保存到SharedPreferences
                saveInventoryToPrefs();

                // 滚动到最新添加的项
                listViewInventory.post(() -> {
                    listViewInventory.smoothScrollToPosition(items.size() - 1);
                });

                updateStatistics();
                clearInputs();
                validateInputs();
                Toast.makeText(this, "添加成功：" + name + " (库存: " + qty + ")", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数量", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchInventory() {
        String keyword = editSearch.getText().toString().trim().toLowerCase();

        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        List<WarehouseItem> filteredList = new ArrayList<>();
        for (WarehouseItem item : items) {
            if (item.name.toLowerCase().contains(keyword) ||
                item.category.toLowerCase().contains(keyword)) {
                filteredList.add(item);
            }
        }

        adapter.updateData(filteredList);

        // 显示查看全部按钮
        isSearchMode = true;
        btnViewAll.setVisibility(View.VISIBLE);
    }

    private void viewAllInventory() {
        // 清除搜索框
        editSearch.setText("");

        // 显示全部库存
        adapter.updateData(items);

        // 隐藏查看全部按钮
        isSearchMode = false;
        btnViewAll.setVisibility(View.GONE);

        Toast.makeText(this, "已显示全部库存", Toast.LENGTH_SHORT).show();
    }

    private void updateStatistics() {
        int totalItems = 0;
        int lowStockCount = 0;
        List<String> categories = new ArrayList<>();
        List<String> productNames = new ArrayList<>(); // 统计不同产品名称

        for (WarehouseItem item : items) {
            totalItems += item.quantity;
            if (item.quantity < 50) {
                lowStockCount++;
            }
            if (!categories.contains(item.category)) {
                categories.add(item.category);
            }
            // 统计不同产品名称（如水蜜桃、油桃等）
            if (!productNames.contains(item.name)) {
                productNames.add(item.name);
            }
        }

        tvTotalItems.setText(String.valueOf(totalItems));
        tvTotalCategories.setText(String.valueOf(categories.size()));
        tvLowStock.setText(String.valueOf(lowStockCount));

        // 更新产品种类数量显示 - 按产品名称统计
        tvCategoryCount.setText("共 " + productNames.size() + " 种桃子");
    }

    private void clearInputs() {
        editProductName.setText("");
        editQuantity.setText("");
        editCategory.setText("");
    }

    private String getRandomColor() {
        String[] colors = {"#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF", "#FF6B9D", "#C9B1FF"};
        return colors[(int) (Math.random() * colors.length)];
    }

    // 自定义适配器
    private class InventoryAdapter extends ArrayAdapter<WarehouseItem> {
        private List<WarehouseItem> itemList;
        private Context context;

        public InventoryAdapter(Context context, List<WarehouseItem> items) {
            super(context, R.layout.item_inventory, items);
            this.context = context;
            this.itemList = new ArrayList<>(items);
        }

        public void updateData(List<WarehouseItem> newItems) {
            this.itemList = new ArrayList<>(newItems);
            clear();
            addAll(this.itemList);
            notifyDataSetChanged();
            android.util.Log.d("InventoryAdapter", "适配器已更新，显示 " + newItems.size() + " 个项目");
        }

        @Override
        public WarehouseItem getItem(int position) {
            return itemList.get(position);
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
                holder = new ViewHolder();
                holder.tvProductName = convertView.findViewById(R.id.tvProductName);
                holder.tvProductDetails = convertView.findViewById(R.id.tvProductDetails);
                holder.viewColorIndicator = convertView.findViewById(R.id.viewColorIndicator);
                holder.btnDelete = convertView.findViewById(R.id.btnDelete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            WarehouseItem item = itemList.get(position);

            holder.tvProductName.setText(item.name);
            holder.tvProductDetails.setText(item.category + " | 库存: " + item.quantityDisplay);

            // 设置颜色指示器的背景
            try {
                holder.viewColorIndicator.setBackgroundColor(android.graphics.Color.parseColor(item.color));
            } catch (Exception e) {
                holder.viewColorIndicator.setBackgroundColor(0xFF6B6B);
            }

            // 高亮显示新添加的项（只在第一次显示时执行动画）
            if (item.id == lastAddedItemId && !hasShownHighlight) {
                convertView.setBackgroundColor(0xFFE8F5E9); // 浅绿色背景
                // 标记已显示高亮
                hasShownHighlight = true;
                // 延迟重置标志和恢复背景
                final View itemView = convertView;
                convertView.postDelayed(() -> {
                    itemView.setBackgroundColor(0xFFFFFFFF);
                }, 1000); // 1秒后恢复背景
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF);
            }

            // 存储当前项的ID到tag中，避免position复用问题
            holder.btnDelete.setTag(item.id);
            holder.btnDelete.setOnClickListener(v -> {
                Long itemId = (Long) v.getTag();
                deleteItemById(itemId);
            });

            return convertView;
        }

        private class ViewHolder {
            TextView tvProductName;
            TextView tvProductDetails;
            View viewColorIndicator;
            ImageButton btnDelete;
        }
    }

    // 新的删除方法，通过ID删除
    private void deleteItemById(long itemId) {
        // 先找到要删除的项的名称
        String itemName = null;
        for (WarehouseItem item : items) {
            if (item.id == itemId) {
                itemName = item.name;
                break;
            }
        }

        if (itemName == null) {
            Toast.makeText(this, "删除失败，未找到该物品", Toast.LENGTH_SHORT).show();
            return;
        }

        final String nameToDelete = itemName;
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除 " + nameToDelete + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 直接通过ID删除，使用索引避免并发问题
                    int indexToRemove = -1;
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).id == itemId) {
                            indexToRemove = i;
                            break;
                        }
                    }

                    if (indexToRemove >= 0) {
                        items.remove(indexToRemove);

                        if (isSearchMode) {
                            searchInventory();
                        } else {
                            adapter.updateData(items);
                        }

                        // 保存到SharedPreferences
                        saveInventoryToPrefs();

                        updateStatistics();
                        Toast.makeText(WarehouseActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(WarehouseActivity.this, "删除失败，未找到该物品", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 仓库物品数据类
    private static class WarehouseItem {
        String name;
        String quantityDisplay;
        String category;
        int quantity;
        String color;
        long id; // 唯一ID

        private static long nextId = 1;

        WarehouseItem(String name, String quantityDisplay, String category, int quantity, String color) {
            this.name = name;
            this.quantityDisplay = quantityDisplay;
            this.category = category;
            this.quantity = quantity;
            this.color = color;
            this.id = nextId++;
        }
    }
}
