package co.koriel.yonapp.db;

import co.amazonaws.models.nosql.BlackListDO;
import co.amazonaws.models.nosql.OnelineBoardCommentDO;
import co.amazonaws.models.nosql.OnelineBoardDO;
import co.amazonaws.models.nosql.UserInfoDO;

public class DataBase {
    public static UserInfoDO userInfo = new UserInfoDO();
    public static OnelineBoardDO onelineBoard = new OnelineBoardDO();
    public static OnelineBoardCommentDO onelineBoardComment = new OnelineBoardCommentDO();
    public static BlackListDO blackList = new BlackListDO();
}
