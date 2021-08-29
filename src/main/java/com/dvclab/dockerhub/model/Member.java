package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.txt.StringUtil;

@DBName("dockerhub")
@DatabaseTable(tableName = "members")
public class Member extends ModelD {
    // 数据及成员角色类型
    public enum Status {
        Normal,
        Deleted
    }

    // 数据集id
    @DatabaseField(dataType = DataType.STRING, width = 64, index = true)
    public String did;

    @DatabaseField(dataType = DataType.STRING, width = 64, index = true)
    public String uid;

    @DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
    public Status status = Status.Normal;

    // 用户获取数据集成员列表时，获取对应成员的信息并存储
    public User user;

    public Member (String did, String uid) {
        this.uid = uid;
        this.did = did;
        this.id = StringUtil.md5(did + "::" + uid);
    }

}