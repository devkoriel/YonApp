package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Nickname")

public class OnelineNicknameDO {
    private String userId;
    private String nickname;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "userId-index")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBAttribute(attributeName = "nickname")
    @DynamoDBIndexHashKey(attributeName = "nickname", globalSecondaryIndexName = "nickname-index")
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
