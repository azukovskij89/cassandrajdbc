/* Copyright © 2020 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.cassandrajdbc.translator;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

public class SqlParser {

    public static SqlStatement parse(String sql) throws JSQLParserException {
        return new SqlStatement(sql, CCJSqlParserUtil.parse(normalizeSql(sql)));
    }
    
    private static String normalizeSql(String sql) {
        if(sql.startsWith("ALTER TABLE") && sql.contains("ADD ") &&
            !sql.contains("ADD COLUMN")) {
            return sql.replace("ADD ", "ADD COLUMN ");
        }
        return sql.replace("ALLOW FILTERING", "");
    }
    
    public static class SqlStatement<T extends Statement> {
        
        private String sql;
        private T statement;
        
        public SqlStatement(T statement) {
            this(null, statement);
        }

        SqlStatement(String sql, T statement) {
            this.sql = sql;
            this.statement = statement;
        }
        
        public T getStatement() {
            return statement;
        }
        
        public String getSql() {
            return sql;
        }
        
        @Override
        public String toString() {
            return sql == null ? statement.toString() : sql;
        }
    }
    
}
