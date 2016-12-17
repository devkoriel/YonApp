package co.koriel.yonapp.fragment.adapter;

import java.io.Serializable;

public class OneLineBoardItem implements Serializable {
    private String id;
    private String timeBefore;
    private String content;
    private int likeCount;
    private int commentCount;
    private boolean isNickStatic;
    private boolean isPicture;

    private String contentDateAndId;
    private long timestamp;
    private String writerGcmTokenKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimeBefore() {
        return timeBefore;
    }

    public void setTimeBefore(String timeBefore) {
        this.timeBefore = timeBefore;
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

    public boolean isNickStatic() {
        return isNickStatic;
    }

    public void setNickStatic(boolean nickStatic) {
        isNickStatic = nickStatic;
    }

    public boolean isPicture() {
        return isPicture;
    }

    public void setPicture(boolean picture) {
        isPicture = picture;
    }

    public String getContentDateAndId() {
        return contentDateAndId;
    }

    public void setContentDateAndId(String contentDateAndId) {
        this.contentDateAndId = contentDateAndId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getWriterGcmTokenKey() {
        return writerGcmTokenKey;
    }

    public void setWriterGcmTokenKey(String writerGcmTokenKey) {
        this.writerGcmTokenKey = writerGcmTokenKey;
    }
}
