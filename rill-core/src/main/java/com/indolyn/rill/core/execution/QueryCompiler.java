package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.parser.Parser;
import com.indolyn.rill.core.sql.planner.Planner;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.semantic.SemanticAnalyzer;

class QueryCompiler {

    private final Catalog catalog;
    private final Planner planner;

    QueryCompiler(Catalog catalog, Planner planner) {
        this.catalog = catalog;
        this.planner = planner;
    }

    StatementNode parse(String sql) {
        Lexer lexer = new Lexer(sql);
        Parser parser = new Parser(lexer.tokenize());
        return parser.parse();
    }

    CompiledStatement compile(String sql, Session session) {
        StatementNode ast = parse(sql);
        if (ast == null) {
            return null;
        }
        return compile(ast, session);
    }

    CompiledStatement compile(StatementNode ast, Session session) {
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(catalog);
        semanticAnalyzer.analyze(ast, session);
        PlanNode plan = planner.createPlan(ast);
        return new CompiledStatement(ast, plan);
    }

    PlanNode compileSystemStatement(StatementNode ast) {
        return planner.createPlan(ast);
    }
}
