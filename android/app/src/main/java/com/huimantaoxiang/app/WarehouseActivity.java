package com.huimantaoxiang.app;

import android.annotation.SuppressLint;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

public class WarehouseActivity extends AppCompatActivity {

    private ListView listViewInventory;
    private EditText editProductName, editQuantity, editCategory, editSearch;
    private Button btnAdd, btnSearch, btnViewAll;
    private TextView tvTotalItems, tvTotalCategories, tvLowStock, tvCategoryCount;
    private InventoryAdapter adapter;
    private List<WarehouseItem> items;
    private boolean isSearchMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse);

        // 初始化视图
        initViews();

        // 初始化数据
        initData();

        // 设置监听器
        setupListeners();
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
        items = new ArrayList<>();

        // 添加示例数据
        items.add(new WarehouseItem("水蜜桃", "120", "鲜桃", 120, "#FF6B6B"));
        items.add(new WarehouseItem("黄桃", "85", "鲜桃", 85, "#FFD93D"));
        items.add(new WarehouseItem("油桃", "45", "鲜桃", 45, "#FF8B3D"));
        items.add(new WarehouseItem("桃罐头", "200", "加工品", 200, "#FF6B9D"));
        items.add(new WarehouseItem("桃干", "67", "加工品", 67, "#FFB6D9"));
        items.add(new WarehouseItem("桃种子", "500", "种子", 500, "#4ECDC4"));

        // 初始化自定义适配器
        adapter = new InventoryAdapter(this, items);
        listViewInventory.setAdapter(adapter);

        updateStatistics();
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> addInventory());
        btnSearch.setOnClickListener(v -> searchInventory());
        btnViewAll.setOnClickListener(v -> viewAllInventory());

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
            items.add(new WarehouseItem(name, quantity, category, qty, getRandomColor()));

            // 如果在搜索模式下，添加后返回全部显示
            if (isSearchMode) {
                isSearchMode = false;
                btnViewAll.setVisibility(View.GONE);
                editSearch.setText("");
                adapter.updateData(items);
            } else {
                adapter.notifyDataSetChanged();
            }

            updateStatistics();
            clearInputs();
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
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
