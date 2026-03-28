package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.predicate.AbstractPredicate;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.log.LogService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExecutionSupportTest {

    @Test
    void createPredicateShouldReturnNullWhenExpressionMissing() {
        ExecutionSupport support =
            new ExecutionSupport(
                Mockito.mock(PageAccess.class),
                Mockito.mock(LogService.class),
                Mockito.mock(LockService.class),
                new PredicateFactory());

        assertNull(support.createPredicate(null, new Schema(List.of(new Column("id", DataType.INT)))));
    }

    @Test
    void createPredicateShouldDelegateToPredicateFactory() throws Exception {
        ExecutionSupport support =
            new ExecutionSupport(
                Mockito.mock(PageAccess.class),
                Mockito.mock(LogService.class),
                Mockito.mock(LockService.class),
                new PredicateFactory());
        Schema schema = new Schema(List.of(new Column("id", DataType.INT)));

        AbstractPredicate predicate =
            support.createPredicate(
                new BinaryExpressionNode(
                    new IdentifierNode("id"),
                    token(TokenType.EQUAL, "="),
                    new LiteralNode(token(TokenType.INTEGER_CONST, "1"))),
                schema);

        assertNotNull(predicate);
        assertTrue(predicate.evaluate(new Tuple(List.of(new Value(1)))));
    }

    @Test
    void createTableHeapShouldWrapGivenInfrastructureAndTableInfo() {
        PageAccess pageAccess = Mockito.mock(PageAccess.class);
        LogService logService = Mockito.mock(LogService.class);
        LockService lockService = Mockito.mock(LockService.class);
        ExecutionSupport support = new ExecutionSupport(pageAccess, logService, lockService, new PredicateFactory());
        TableInfo tableInfo =
            new TableInfo("users", new Schema(List.of(new Column("id", DataType.INT))), new PageId(4));

        TableHeap tableHeap = support.createTableHeap(tableInfo);

        assertNotNull(tableHeap);
        assertSame(tableInfo, tableHeap.getTableInfo());
    }

    private static Token token(TokenType type, String lexeme) {
        return new Token(type, lexeme, 1, 1);
    }
}
