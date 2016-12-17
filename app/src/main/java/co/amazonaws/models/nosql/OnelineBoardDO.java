package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.io.Serializable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Board")

public class OnelineBoardDO implements Serializable {
    private String _DateAndId;
    private String _Date;
    private String _Content;
    private String _Ip;
    private String _Like;
    private String _writerGcmTokenKey;
    private boolean isNickStatic;
    private boolean isPicture;
    private long _Timestamp;

    @DynamoDBHashKey(attributeName = "DateAndId")
    public String getDateAndId() {
        return _DateAndId;
    }
    public void setDateAndId(final String _DateAndId) {
        this._DateAndId = _DateAndId;
    }

    @DynamoDBAttribute(attributeName = "Date")
    @DynamoDBIndexHashKey(attributeName = "Date", globalSecondaryIndexName = "Date-Timestamp-index")
    public String getDate() { return _Date; }
    public void setDate(final String _Date) {
        this._Date = _Date;
    }

    @DynamoDBAttribute(attributeName = "Content")
    public String getContent() { return _Content; }
    public void setContent(final String _Content) {
        this._Content = _Content;
    }

    @DynamoDBAttribute(attributeName = "Ip")
    public String getIp() { return _Ip; }
    public void setIp(final String _Ip) {
        this._Ip = _Ip;
    }

    @DynamoDBAttribute(attributeName = "Like")
    public String getLike() { return _Like; }
    public void setLike(final String _Like) {
        this._Like = _Like;
    }

    @DynamoDBAttribute(attributeName = "Timestamp")
    @DynamoDBIndexRangeKey(attributeName = "Timestamp", globalSecondaryIndexName = "Date-Timestamp-index")
    public long getTimestamp() { return _Timestamp; }
    public void setTimestamp(final long _Timestamp) {
        this._Timestamp = _Timestamp;
    }

    @DynamoDBAttribute(attributeName = "writerGcmTokenKey")
    public String getWriterGcmTokenKey() { return _writerGcmTokenKey; }
    public void setWriterGcmTokenKey(final String _writerGcmTokenKey) {
        this._writerGcmTokenKey = _writerGcmTokenKey;
    }

    @DynamoDBAttribute(attributeName = "isNickStatic")
    public boolean isNickStatic() {
        return isNickStatic;
    }

    public void setNickStatic(boolean nickStatic) {
        isNickStatic = nickStatic;
    }

    @DynamoDBAttribute(attributeName = "isPicture")
    public boolean isPicture() {
        return isPicture;
    }

    public void setPicture(boolean picture) {
        isPicture = picture;
    }
}
