package co.koriel.yonapp.fragment.adapter;

import java.io.Serializable;

public class OneLineBoardCommentItem implements Serializable {
    private String id;
    private String timeBefore;
    private int index;
    private String comment;
    private boolean isNickStatic;

    private String commentDateAndId;
    private long Timestamp;

    public OneLineBoardCommentItem() {
        this.id = "";
    }

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCommentDateAndId() {
        return commentDateAndId;
    }

    public void setCommentDateAndId(String commentDateAndId) {
        this.commentDateAndId = commentDateAndId;
    }

    public boolean isNickStatic() {
        return isNickStatic;
    }

    public void setNickStatic(boolean nickStatic) {
        isNickStatic = nickStatic;
    }

    public long getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(long timestamp) {
        Timestamp = timestamp;
    }
}
