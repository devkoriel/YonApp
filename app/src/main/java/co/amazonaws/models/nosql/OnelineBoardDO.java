package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Board")

public class OnelineBoardDO {
    private String _DateAndId;
    private String _Date;
    private String _Content;
    private String _Ip;
    private String _Like;
    private String _writerGcmTokenKey;
    private long _Timestamp;

    @DynamoDBHashKey(attributeName = "DateAndId")
    @DynamoDBIndexHashKey(attributeName = "DateAndId", globalSecondaryIndexName = "DateSorted")
    public String getDateAndId() {
        return _DateAndId;
    }
    public void setDateAndId(final String _DateAndId) {
        this._DateAndId = _DateAndId;
    }

    @DynamoDBAttribute(attributeName = "Date")
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
    public long getTimestamp() { return _Timestamp; }
    public void setTimestamp(final long _Timestamp) {
        this._Timestamp = _Timestamp;
    }

    @DynamoDBAttribute(attributeName = "writerGcmTokenKey")
    public String getWriterGcmTokenKey() { return _writerGcmTokenKey; }
    public void setWriterGcmTokenKey(final String _writerGcmTokenKey) {
        this._writerGcmTokenKey = _writerGcmTokenKey;
    }
}
