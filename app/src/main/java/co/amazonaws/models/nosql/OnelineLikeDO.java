package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Like")

public class OnelineLikeDO {
    private String likeDateAndId;
    private String contentDateAndId;
    private long contentTimestamp;

    @DynamoDBHashKey(attributeName = "likeDateAndId")
    public String getLikeDateAndId() {
        return likeDateAndId;
    }

    public void setLikeDateAndId(String likeDateAndId) {
        this.likeDateAndId = likeDateAndId;
    }

    @DynamoDBAttribute(attributeName = "contentDateAndId")
    @DynamoDBIndexHashKey(attributeName = "contentDateAndId", globalSecondaryIndexName = "contentDateAndId-index")
    public String getContentDateAndId() {
        return contentDateAndId;
    }

    public void setContentDateAndId(String contentDateAndId) {
        this.contentDateAndId = contentDateAndId;
    }

    @DynamoDBAttribute(attributeName = "contentTimestamp")
    public long getContentTimestamp() {
        return contentTimestamp;
    }

    public void setContentTimestamp(long contentTimestamp) {
        this.contentTimestamp = contentTimestamp;
    }
}
