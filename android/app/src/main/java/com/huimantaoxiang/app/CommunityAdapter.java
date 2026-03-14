package com.huimantaoxiang.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<CommunityPost> postList;
    private OnItemClickListener onItemClickListener;

    // 点击监听器接口
    public interface OnItemClickListener {
        void onItemClick(int position, CommunityPost post);
    }

    public CommunityAdapter(List<CommunityPost> postList) {
        this.postList = postList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityPost post = postList.get(position);

        holder.tvAuthor.setText(post.getAuthor());
        holder.tvTime.setText(post.getTime());
        holder.tvContent.setText(post.getContent());
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, post);
            }
        });

        // 点赞按钮点击
        holder.btnLike.setOnClickListener(v -> {
            // 实现点赞逻辑
            post.setLikeCount(post.getLikeCount() + 1);
            notifyItemChanged(position);
        });

        // 评论按钮点击
        holder.btnComment.setOnClickListener(v -> {
            // 实现评论逻辑
            post.setCommentCount(post.getCommentCount() + 1);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvAuthor;
        TextView tvTime;
        TextView tvContent;
        TextView tvLikeCount;
        TextView tvCommentCount;
        ImageView btnLike;
        ImageView btnComment;
        ImageView btnShare;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnShare = itemView.findViewById(R.id.btn_share);
        }
    }
}