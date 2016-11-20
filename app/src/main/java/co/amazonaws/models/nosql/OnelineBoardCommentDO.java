package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Board-Comment")

public class OnelineBoardCommentDO {
    private String _CommentDateAndId;
    private String _ContentDateAndId;
    private String _Comment;
    private String _Ip;
    private String _writerGcmTokenKey;
    private String _Content;
    private String _ContentTimestamp;
    private long _Timestamp;

    @DynamoDBHashKey(attributeName = "CommentDateAndId")
    @DynamoDBIndexHashKey(attributeName = "CommentDateAndId", globalSecondaryIndexName = "DateSorted")
    public String getCommentDateAndId() {
        return _CommentDateAndId;
    }
    public void setCommentDateAndId(final String _CommentDateAndId) {
        this._CommentDateAndId = _CommentDateAndId;
    }

    @DynamoDBAttribute(attributeName = "_ContentDateAndId")
    public String getContentDateAndId() { return _ContentDateAndId; }
    public void setContentDateAndId(final String _ContentDateAndId) {
        this._ContentDateAndId = _ContentDateAndId;
    }

    @DynamoDBAttribute(attributeName = "Comment")
    public String getComment() { return _Comment; }
    public void setComment(final String _Comment) {
        this._Comment = _Comment;
    }

    @DynamoDBAttribute(attributeName = "Ip")
    public String getIp() { return _Ip; }
    public void setIp(final String _Ip) {
        this._Ip = _Ip;
    }

    @DynamoDBAttribute(attributeName = "Timestamp")
    public long getTimestamp() { return _Timestamp; }
    public void setTimestamp(final long _Timestamp) {
        this._Timestamp = _Timestamp;
    }

    @DynamoDBAttribute(attributeName = "writerGcmTokenKey")
    public String getWriterGcmTokenKey() { return _writerGcmTokenKey; }
    public void setWriterGcmTokenKey(final String _writerGcmTokenKey) {
        this._writerGcmTokenKey = _writerGcmTokenKey;
    }

    @DynamoDBAttribute(attributeName = "Content")
    public String getContent() {
        return _Content;
    }
    public void setContent(final String _Content) {
        this._Content = _Content;
    }

    @DynamoDBAttribute(attributeName = "ContentTimestamp")
    public String getContentTimestamp() {
        return _ContentTimestamp;
    }
    public void setContentTimestamp(final String _ContentTimestamp) {
        this._ContentTimestamp = _ContentTimestamp;
    }
}
