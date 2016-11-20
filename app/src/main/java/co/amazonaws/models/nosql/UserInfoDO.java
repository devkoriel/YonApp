package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-User-Info")

public class UserInfoDO {
    private String _userId;
    private String _gcmToken;
    private String _gcmTokenKey;
    private String _studentId;
    private String _studentPasswd;
    private String _numOfNotice;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "DateSorted")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "gcmToken")
    @DynamoDBIndexRangeKey(attributeName = "gcmToken", globalSecondaryIndexName = "DateSorted")
    public String getGcmToken() {
        return "activated";
    }

    public void setGcmToken(final String _gcmToken) {
        this._gcmToken = _gcmToken;
    }
    @DynamoDBAttribute(attributeName = "gcmTokenKey")
    public String getGcmTokenKey() { return _gcmTokenKey; }

    public void setGcmTokenKey(final String _gcmTokenKey) {
        this._gcmTokenKey = _gcmTokenKey;
    }
    @DynamoDBAttribute(attributeName = "studentId")
    public String getStudentId() {
        return _studentId;
    }

    public void setStudentId(final String _studentId) {
        this._studentId = _studentId;
    }
    @DynamoDBAttribute(attributeName = "studentPasswd")
    public String getStudentPasswd() {
        return _studentPasswd;
    }

    public void setStudentPasswd(final String _studentPasswd) {
        this._studentPasswd = _studentPasswd;
    }

    @DynamoDBAttribute(attributeName = "numOfNotice")
    public String getNumOfNotice() { return _numOfNotice; }

    public void setNumOfNotice(final String _numOfNotice) {
        this._numOfNotice = _numOfNotice;
    }

}
