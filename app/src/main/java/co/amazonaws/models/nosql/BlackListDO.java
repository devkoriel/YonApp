package co.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "yonapp-mobilehub-329555121-Black-List")

public class BlackListDO {
    private String _studentId;

    @DynamoDBHashKey(attributeName = "studentId")
    @DynamoDBIndexHashKey(attributeName = "studentId", globalSecondaryIndexName = "DateSorted")
    public String getStudentId() {
        return _studentId;
    }
    public void setStudentId(final String _studentId) {
        this._studentId = _studentId;
    }
}
