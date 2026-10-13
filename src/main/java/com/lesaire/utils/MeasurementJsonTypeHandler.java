package com.lesaire.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.Map;
import org.apache.ibatis.type.*;

/**
 * 在 MyBatis 和数据库 JSON 字段之间转换量体快照，并保留小数精度。
 */
public class MeasurementJsonTypeHandler extends BaseTypeHandler<Map<String,Object>> {
    private static final ObjectMapper JSON=new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final TypeReference<Map<String,Object>> TYPE=new TypeReference<>() {};
    /**
     * 把量体键值映射序列化为 JSON，通过 JDBC 参数绑定写入数据库。
     */
    @Override public void setNonNullParameter(PreparedStatement statement,int index,Map<String,Object> value,JdbcType type) throws SQLException {
        try { statement.setString(index,JSON.writeValueAsString(value)); } catch(Exception ex) { throw new SQLException("量体快照格式不正确",ex); }
    }
    /**
     * 兼容正常 JSON 对象与 H2 包装成字符串的 JSON 表示。
     */
    private Map<String,Object> read(String value) throws SQLException {
        if(value==null) return null;
        try {
            var node=JSON.readTree(value);
            // H2 会把绑定的 VARCHAR 保存为 JSON 字符串，测试读取时需还原为量体对象。
            if(node.isTextual()) return JSON.readValue(node.textValue(),TYPE);
            return JSON.convertValue(node,TYPE);
        } catch(Exception ex) { throw new SQLException("量体快照读取失败",ex); }
    }
    @Override public Map<String,Object> getNullableResult(ResultSet result,String column) throws SQLException { return read(result.getString(column)); }
    @Override public Map<String,Object> getNullableResult(ResultSet result,int column) throws SQLException { return read(result.getString(column)); }
    @Override public Map<String,Object> getNullableResult(CallableStatement result,int column) throws SQLException { return read(result.getString(column)); }
}
