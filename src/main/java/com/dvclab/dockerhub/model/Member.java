package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;
import one.rewind.txt.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DBName("dockerhub")
@DatabaseTable(tableName = "members")
public class Member extends ModelD {
    // 数据及成员状态
    public enum Status {
        Normal,
        Deleted
    }
    // 数据集成员的角色
    public enum Roles {
        Viewer,
        Admin
    }

    // 数据集id
    @DatabaseField(dataType = DataType.STRING, width = 64, index = true)
    public String did;

    @DatabaseField(dataType = DataType.STRING, width = 64, index = true)
    public String uid;

    @DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
    public Status status = Status.Normal;

    @DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
    public Roles role = Roles.Viewer;

    // 用户获取数据集成员列表时，获取对应成员的信息并存储
    public User user;

    public Member () {}

    public Member (String did, String uid) {
        this.uid = uid;
        this.did = did;
        this.id = genId(did, uid);
    }

    public static String genId(String did, String uid) {
        return StringUtil.md5(did + "::" + uid);
    }


    /**
     * 根据数据集id获取成员列表
     * @param dids
     * @return
     */
    public static Map<String, List<Member>> getMembers(List<String> dids) throws SQLException, DBInitException {
        if(dids.size() == 0) {
            return new HashMap<>();
        }

        return Daos.get(Member.class).queryBuilder()
                .where().in("did", dids).query()
                .stream().collect(Collectors.groupingBy(c -> c.did));
    }

}
