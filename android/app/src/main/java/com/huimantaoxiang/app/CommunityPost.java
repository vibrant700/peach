package com.huimantaoxiang.app;

public class CommunityPost {
    private String author;      // 作者
    private String time;        // 发布时间
    private String content;     // 内容
    private int likeCount;      // 点赞数
    private int commentCount;   // 评论数

    public CommunityPost(String author, String time, String content, int likeCount, int commentCount) {
        this.author = author;
        this.time = time;
        this.content = content;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    // Getter 和 Setter 方法
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
}