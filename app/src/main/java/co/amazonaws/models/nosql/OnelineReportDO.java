package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Oneline-Report")

public class OnelineReportDO {
    private String reportDateAndId;
    private String contentDateAndId;
    private long contentTimestamp;

    @DynamoDBHashKey(attributeName = "reportDateAndId")
    public String getReportDateAndId() {
        return reportDateAndId;
    }

    public void setReportDateAndId(String reportDateAndId) {
        this.reportDateAndId = reportDateAndId;
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
