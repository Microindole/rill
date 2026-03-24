package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.sql.ast.AstNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowTablesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UpdateStatementNode;
import com.indolyn.rill.core.session.Session;

import java.util.LinkedHashMap;
import java.util.Map;

public class SemanticAnalyzer {

    private final Map<Class<? extends AstNode>, SemanticRule<? extends AstNode>> semanticRules =
        new LinkedHashMap<>();
    private final SelectSemanticValidator selectSemanticValidator;
    private final InsertSemanticValidator insertSemanticValidator;
    private final DeleteSemanticValidator deleteSemanticValidator;
    private final UpdateSemanticValidator updateSemanticValidator;
    private final CreateUserSemanticValidator createUserSemanticValidator;
    private final GrantSemanticValidator grantSemanticValidator;
    private final CreateTableSemanticValidator createTableSemanticValidator;
    private final DropTableSemanticValidator dropTableSemanticValidator;
    private final AlterTableSemanticValidator alterTableSemanticValidator;
    private final ShowTablesSemanticValidator showTablesSemanticValidator;

    public SemanticAnalyzer(Catalog catalog) {
        SemanticValidationSupport validationSupport = new SemanticValidationSupport(catalog);
        DefinitionValidationSupport definitionValidationSupport = new DefinitionValidationSupport();
        this.selectSemanticValidator = new SelectSemanticValidator(catalog);
        this.insertSemanticValidator = new InsertSemanticValidator(validationSupport);
        this.deleteSemanticValidator = new DeleteSemanticValidator(validationSupport);
        this.updateSemanticValidator = new UpdateSemanticValidator(validationSupport);
        this.createUserSemanticValidator =
            new CreateUserSemanticValidator(catalog, definitionValidationSupport);
        this.grantSemanticValidator =
            new GrantSemanticValidator(catalog, definitionValidationSupport);
        this.createTableSemanticValidator =
            new CreateTableSemanticValidator(catalog, definitionValidationSupport);
        this.dropTableSemanticValidator =
            new DropTableSemanticValidator(definitionValidationSupport, validationSupport);
        this.alterTableSemanticValidator =
            new AlterTableSemanticValidator(definitionValidationSupport, validationSupport);
        this.showTablesSemanticValidator = new ShowTablesSemanticValidator();
        registerSemanticRules();
    }

    public void analyze(AstNode node, Session session) {
        if (session == null || !session.isAuthenticated()) {
            throw new SemanticException("Access denied. User is not authenticated.");
        }

        SemanticRule<AstNode> rule = resolveRule(node);
        if (rule != null) {
            rule.analyze(node, session);
        }
    }

    private void registerSemanticRules() {
        registerRule(CreateTableStatementNode.class, createTableSemanticValidator::analyze);
        registerRule(InsertStatementNode.class, insertSemanticValidator::analyze);
        registerRule(SelectStatementNode.class, selectSemanticValidator::analyze);
        registerRule(DeleteStatementNode.class, deleteSemanticValidator::analyze);
        registerRule(UpdateStatementNode.class, updateSemanticValidator::analyze);
        registerRule(DropTableStatementNode.class, dropTableSemanticValidator::analyze);
        registerRule(AlterTableStatementNode.class, alterTableSemanticValidator::analyze);
        registerRule(ShowTablesStatementNode.class, showTablesSemanticValidator::analyze);
        registerRule(CreateUserStatementNode.class, createUserSemanticValidator::analyze);
        registerRule(GrantStatementNode.class, grantSemanticValidator::analyze);
    }

    private <T extends AstNode> void registerRule(Class<T> nodeType, SemanticRule<T> rule) {
        semanticRules.put(nodeType, rule);
    }

    @SuppressWarnings("unchecked")
    private SemanticRule<AstNode> resolveRule(AstNode node) {
        SemanticRule<? extends AstNode> exactRule = semanticRules.get(node.getClass());
        if (exactRule != null) {
            return (SemanticRule<AstNode>) exactRule;
        }

        for (Map.Entry<Class<? extends AstNode>, SemanticRule<? extends AstNode>> entry :
            semanticRules.entrySet()) {
            if (entry.getKey().isInstance(node)) {
                return (SemanticRule<AstNode>) entry.getValue();
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface SemanticRule<T extends AstNode> {
        void analyze(T node, Session session);
    }
}

