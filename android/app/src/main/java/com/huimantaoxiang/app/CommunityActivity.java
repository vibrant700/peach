package com.huimantaoxiang.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    // UI 组件
    private EditText etPostContent;
    private Button btnPost;
    private RecyclerView rvPosts;
    private View emptyStateContainer;
    private TextView tvPostCount;
    private TextView tvMemberCount;
    private TextView tvPostCountLabel;
    private LinearLayout btnBack;
    private ImageButton btnRefresh;

    // 数据
    private List<CommunityPost> postList = new ArrayList<>();
    private CommunityAdapter adapter;

    // SharedPreferences用于持久化保存帖子数据
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "community_prefs";
    private static final String KEY_POSTS = "posts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_community);

        // 初始化SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        // 初始化视图
        initViews();

        // 加载保存的帖子数据
        loadPostsFromPrefs();

        // 设置事件监听
        setupListeners();

        // 如果没有帖子，添加示例数据
        if (postList.isEmpty()) {
            initData();
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
     * 从SharedPreferences加载帖子数据
     */
    private void loadPostsFromPrefs() {
        try {
            String postsJson = prefs.getString(KEY_POSTS, "");
            if (!postsJson.isEmpty()) {
                JSONArray jsonArray = new JSONArray(postsJson);
                postList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject postObj = jsonArray.getJSONObject(i);
                    CommunityPost post = new CommunityPost(
                        postObj.getString("author"),
                        postObj.getString("time"),
                        postObj.getString("content"),
                        postObj.getInt("likeCount"),
                        postObj.getInt("commentCount")
                    );
                    postList.add(post);
                }

                // 更新适配器
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                updateStats();

                // 隐藏空状态提示
                if (!postList.isEmpty() && emptyStateContainer != null) {
                    emptyStateContainer.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存帖子数据到SharedPreferences
     */
    private void savePostsToPrefs() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (CommunityPost post : postList) {
                JSONObject postObj = new JSONObject();
                postObj.put("author", post.getAuthor());
                postObj.put("time", post.getTime());
                postObj.put("content", post.getContent());
                postObj.put("likeCount", post.getLikeCount());
                postObj.put("commentCount", post.getCommentCount());
                jsonArray.put(postObj);
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_POSTS, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存Activity状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // 保存帖子列表数据
        ArrayList<String> authors = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>();
        ArrayList<String> contents = new ArrayList<>();
        ArrayList<Integer> likes = new ArrayList<>();
        ArrayList<Integer> comments = new ArrayList<>();

        for (CommunityPost post : postList) {
            authors.add(post.getAuthor());
            times.add(post.getTime());
            contents.add(post.getContent());
            likes.add(post.getLikeCount());
            comments.add(post.getCommentCount());
        }

        outState.putStringArrayList("authors", authors);
        outState.putStringArrayList("times", times);
        outState.putStringArrayList("contents", contents);
        outState.putIntegerArrayList("likes", likes);
        outState.putIntegerArrayList("comments", comments);

        // 保存输入框内容
        if (etPostContent != null) {
            outState.putString("postContent", etPostContent.getText().toString());
        }

        // 保存空状态
        outState.putBoolean("isEmptyState", emptyStateContainer != null && emptyStateContainer.getVisibility() == View.VISIBLE);
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
        // 恢复帖子列表数据
        ArrayList<String> authors = savedInstanceState.getStringArrayList("authors");
        ArrayList<String> times = savedInstanceState.getStringArrayList("times");
        ArrayList<String> contents = savedInstanceState.getStringArrayList("contents");
        ArrayList<Integer> likes = savedInstanceState.getIntegerArrayList("likes");
        ArrayList<Integer> comments = savedInstanceState.getIntegerArrayList("comments");

        if (authors != null && times != null && contents != null &&
            likes != null && comments != null) {

            postList.clear();
            for (int i = 0; i < authors.size(); i++) {
                CommunityPost post = new CommunityPost(
                    authors.get(i),
                    times.get(i),
                    contents.get(i),
                    likes.get(i),
                    comments.get(i)
                );
                postList.add(post);
            }

            // 更新适配器
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            updateStats();
        }

        // 恢复输入框内容
        String postContent = savedInstanceState.getString("postContent", "");
        if (etPostContent != null) {
            etPostContent.setText(postContent);
        }

        // 恢复空状态
        boolean isEmptyState = savedInstanceState.getBoolean("isEmptyState", false);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(isEmptyState ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 初始化界面组件
     */
    private void initViews() {
        etPostContent = findViewById(R.id.et_post_content);
        btnPost = findViewById(R.id.btn_post);
        rvPosts = findViewById(R.id.rv_posts);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvPostCountLabel = findViewById(R.id.tv_post_count_label);
        btnBack = findViewById(R.id.btn_back);
        btnRefresh = findViewById(R.id.btn_refresh);

        // 设置 RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPosts.setLayoutManager(layoutManager);

        adapter = new CommunityAdapter(postList);
        rvPosts.setAdapter(adapter);
    }

    /**
     * 初始化示例数据
     */
    private void initData() {
        // 添加一些示例帖子
        postList.add(new CommunityPost("张三丰", "2小时前", "今年的桃子收成不错，大家有什么好的销售渠道吗？", 15, 8));
        postList.add(new CommunityPost("李桃农", "5小时前", "分享一个防治桃树病虫害的小技巧...", 32, 12));
        postList.add(new CommunityPost("王技术员", "1天前", "新品种'秋月红'开始上市了，口感非常好！", 45, 23));
        postList.add(new CommunityPost("赵收购商", "2天前", "大量收购优质水蜜桃，价格从优，有意的联系。", 28, 5));

        // 更新适配器
        adapter.notifyDataSetChanged();

        // 更新统计数据
        updateStats();

        // 如果有数据，隐藏空状态提示
        if (!postList.isEmpty()) {
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 发布按钮点击事件
        btnPost.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(CommunityActivity.this, "请输入帖子内容", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建新帖子
            CommunityPost newPost = new CommunityPost(
                    "当前用户",
                    "刚刚",
                    content,
                    0,
                    0
            );

            // 添加到列表开头
            postList.add(0, newPost);
            adapter.notifyItemInserted(0);
            rvPosts.scrollToPosition(0);

            // 保存到SharedPreferences
            savePostsToPrefs();

            // 清空输入框
            etPostContent.setText("");

            // 更新统计数据
            updateStats();

            // 隐藏空状态提示
            emptyStateContainer.setVisibility(View.GONE);

            Toast.makeText(CommunityActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
        });
        //返回按钮点击事件
       btnBack.setOnClickListener(v->{
           finish();

       });
       //刷新按钮点击事件
        btnRefresh.setOnClickListener(v->{
            refreshPosts();

        });
        // 帖子点击事件（通过适配器设置）
        adapter.setOnItemClickListener((position, post) -> {
            // 跳转到帖子详情页或显示更多操作
            Toast.makeText(CommunityActivity.this,
                    "点击了帖子：" + post.getAuthor(),
                    Toast.LENGTH_SHORT).show();
        });
    }
    /**
     * 刷新帖子列表
     */
    private void refreshPosts() {
        // 显示加载提示
        Toast.makeText(this, "刷新中...", Toast.LENGTH_SHORT).show();

        // 可以添加旋转动画效果
        btnRefresh.animate().rotationBy(360).setDuration(500).start();

        // 模拟网络延迟（实际开发中这里应该是网络请求）
        new android.os.Handler().postDelayed(() -> {
            // 清空当前列表
            int oldSize = postList.size();
            postList.clear();
            adapter.notifyItemRangeRemoved(0, oldSize);

            // 重新加载数据（这里调用 initData() 重新初始化）
            initData();

            // 显示刷新完成提示
            Toast.makeText(this, "刷新完成，共" + postList.size() + "条帖子",
                    Toast.LENGTH_SHORT).show();

            // 滚动到顶部
            if (postList.size() > 0) {
                rvPosts.scrollToPosition(0);
            }

            // 检查空状态
            if (postList.isEmpty()) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            } else {
                emptyStateContainer.setVisibility(View.GONE);
            }

            // 更新统计数据
            updateStats();
        }, 1000); // 延迟1秒模拟网络请求
    }

    /**
     * 更新统计数据
     */
    private void updateStats() {
        int postCount = postList.size();
        tvPostCount.setText(String.valueOf(postCount));
        tvPostCountLabel.setText("共 " + postCount + " 条");
        // 成员数量可以设为固定值或从服务器获取
        tvMemberCount.setText("128");
    }

}