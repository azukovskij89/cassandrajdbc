/* Copyright © 2020 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.cassandrajdbc.translator.impl;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.cassandrajdbc.expressions.AssignmentParser;
import com.cassandrajdbc.expressions.ClauseParser;
import com.cassandrajdbc.statement.StatementOptions;
import com.cassandrajdbc.statement.StatementOptions.Collation;
import com.cassandrajdbc.translator.SqlToClqTranslator.CqlBuilder;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update.Assignments;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

public class Update implements CqlBuilder<net.sf.jsqlparser.statement.update.Update> {
    
    @Override
    public Class<net.sf.jsqlparser.statement.update.Update> getInputType() {
        return net.sf.jsqlparser.statement.update.Update.class;
    }

    @Override
    public RegularStatement buildCql(net.sf.jsqlparser.statement.update.Update stmt, StatementOptions config) {
        if(stmt.getJoins() != null) {
            throw new UnsupportedOperationException("joins not supported " + stmt);
        }
        if(stmt.getSelect() != null) {
            throw new UnsupportedOperationException("subselect not supported" + stmt);
        }
        if(stmt.getReturningExpressionList() != null) {
            throw new UnsupportedOperationException("returning not supported" + stmt);
        }
        Function<Expression, Stream<Clause>> clauseParser = ClauseParser.instance(config);

        RegularStatement[] updates = stmt.getTables().stream()
            .map(table -> clauseParser.apply(stmt.getWhere())
                .collect(() -> createAssignment(stmt, table, config), 
                    Assignments::where, (a,b) -> { throw new IllegalStateException("no parallel"); }))
            .toArray(RegularStatement[]::new);
        return updates.length == 1 ? updates[0] : QueryBuilder.unloggedBatch(updates);
    }

    private Assignments createAssignment(net.sf.jsqlparser.statement.update.Update stmt, Table table, StatementOptions config) {
        BiFunction<String, Expression, Stream<Assignment>> assignmentParser = AssignmentParser.instance();
        return IntStream.range(0, stmt.getColumns().size()).boxed()
            .flatMap(i -> assignmentParser.apply(getName(stmt, i, config), getValue(stmt, i)))
            .collect(() -> QueryBuilder.update(table.getSchemaName(), escape(table.getName(), config)).with(),
                Assignments::and, (a,b) -> { throw new IllegalStateException("no parallel"); });
    }

    private String getName(net.sf.jsqlparser.statement.update.Update stmt, Integer i, StatementOptions config) {
        return escape(stmt.getColumns().get(i).getColumnName(), config);
    }

    private Expression getValue(net.sf.jsqlparser.statement.update.Update stmt, int idx) {
        return stmt.getExpressions().get(idx);
    }
    
    private String escape(String value, StatementOptions config) {
        return config.getCollation() == Collation.CASE_SENSITIVE ? "\"" + value + "\"" : value;
    }

}