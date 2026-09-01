package uk.ac.ebi.uniprot.urml.engine.drools.compiler;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.drools.compiler.lang.DRL6Lexer;
import org.drools.compiler.lang.DRL6Parser;
import org.drools.compiler.lang.descr.AndDescr;
import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.lang.descr.CollectDescr;
import org.drools.compiler.lang.descr.ExistsDescr;
import org.drools.compiler.lang.descr.ExprConstraintDescr;
import org.drools.compiler.lang.descr.NotDescr;
import org.drools.compiler.lang.descr.OrDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.compiler.lang.descr.RuleDescr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assertions for comparing generated DRL against expected fragments. Both sides are parsed
 * with the Drools DRL parser and compared as ASTs, not text, so assertions pin semantics
 * while ignoring formatting, redundant parentheses, and constant boilerplate (package/imports
 * header, {@code dialect}/{@code no-loop}). Use {@link #assertConditionsEquivalent(String, String)}
 * for the {@code when} part, {@link #assertConsequencesEquivalent(String, String)} for the
 * {@code then} part (one statement per line, no semicolons), and
 * {@link #assertRulesEquivalent(String, String)} when attributes or multiple rules matter.
 * {@link #assertDrlContains(String, String)} remains for intentionally unparseable output.
 *
 * @author Muhammad Aditya Hilmy
 */
public final class DrlAssertions {

    private DrlAssertions() {
    }

    public static void assertConditionsEquivalent(String expectedWhen, String actualDrl) {
        assertEquals(serializeLhs(singleRule(parseDrl("rule \"EXPECTED\" when\n" + expectedWhen + "\nthen\nend")).getLhs()),
                serializeLhs(singleRule(parseDrl(actualDrl)).getLhs()));
    }

    public static void assertConsequencesEquivalent(String expectedThen, String actualDrl) {
        Object consequence = singleRule(parseDrl(actualDrl)).getConsequence();
        assertEquals(normalizeMvel(expectedThen), normalizeMvel(consequence == null ? "" : consequence.toString()));
    }

    public static void assertRulesEquivalent(String expectedRules, String actualDrl) {
        assertEquals(canonicalRules(parseDrl(expectedRules)), canonicalRules(parseDrl(actualDrl)));
    }

    public static void assertDrlContains(String expectedFragment, String actualDrl) {
        assertTrue(normalizeDrl(actualDrl).contains(normalizeDrl(expectedFragment)),
                () -> "Expected DRL fragment not found.\nExpected fragment: " + normalizeDrl(expectedFragment)
                        + "\nActual DRL: " + normalizeDrl(actualDrl));
    }

    static PackageDescr parseDrl(String drl) {
        try {
            DRL6Lexer lexer = new DRL6Lexer(new ANTLRStringStream(drl));
            DRL6Parser parser = new DRL6Parser(new CommonTokenStream(lexer));
            PackageDescr packageDescr = parser.compilationUnit();
            assertFalse(parser.hasErrors(),
                    () -> "DRL failed to parse: " + parser.getErrorMessages() + "\nDRL:\n" + drl);
            return packageDescr;
        } catch (RecognitionException e) {
            return fail("DRL failed to parse: " + e.getMessage() + "\nDRL:\n" + drl);
        }
    }

    static String normalizeDrl(String drl) {
        StringBuilder normalized = new StringBuilder();
        boolean insideLiteral = false;
        boolean pendingWhitespace = false;
        for (int i = 0; i < drl.length(); i++) {
            char c = drl.charAt(i);
            if (insideLiteral) {
                normalized.append(c);
                if (c == '\\' && i + 1 < drl.length()) {
                    normalized.append(drl.charAt(++i));
                } else if (c == '"') {
                    insideLiteral = false;
                }
            } else if (c == '"') {
                if (pendingWhitespace && normalized.length() > 0) {
                    normalized.append(' ');
                }
                pendingWhitespace = false;
                normalized.append(c);
                insideLiteral = true;
            } else if (Character.isWhitespace(c)) {
                pendingWhitespace = true;
            } else {
                if (pendingWhitespace && normalized.length() > 0) {
                    normalized.append(' ');
                }
                pendingWhitespace = false;
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    static String normalizeMvel(String mvel) {
        List<String> statements = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        boolean insideLiteral = false;
        for (int i = 0; i < mvel.length(); i++) {
            char c = mvel.charAt(i);
            if (insideLiteral) {
                statement.append(c);
                if (c == '\\' && i + 1 < mvel.length()) {
                    statement.append(mvel.charAt(++i));
                } else if (c == '"') {
                    insideLiteral = false;
                }
            } else if (c == '"') {
                insideLiteral = true;
                statement.append(c);
            } else if (c == ';' || c == '\n') {
                addStatement(statements, statement);
            } else {
                statement.append(c);
            }
        }
        addStatement(statements, statement);
        return statements.stream().map(s -> s + ";").collect(Collectors.joining("\n"));
    }

    private static RuleDescr singleRule(PackageDescr packageDescr) {
        assertEquals(1, packageDescr.getRules().size(),
                () -> "Expected exactly one rule but found " + packageDescr.getRules().size());
        return packageDescr.getRules().get(0);
    }

    private static String canonicalRules(PackageDescr packageDescr) {
        return packageDescr.getRules().stream()
                .map(DrlAssertions::canonicalRule)
                .collect(Collectors.joining("\n"));
    }

    private static String canonicalRule(RuleDescr rule) {
        StringBuilder canonical = new StringBuilder("rule ").append(rule.getName());
        if (rule.hasParent()) {
            canonical.append(" extends ").append(rule.getParentName());
        }
        rule.getAttributes().entrySet().stream()
                .filter(entry -> !"no-loop".equals(entry.getKey()) && !"dialect".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> canonical.append(' ').append(entry.getKey())
                        .append(' ').append(entry.getValue().getValue()));
        Object consequence = rule.getConsequence();
        return canonical.append(" when ").append(serializeLhs(rule.getLhs()))
                .append(" then ").append(normalizeMvel(consequence == null ? "" : consequence.toString()))
                .toString();
    }

    private static String serializeLhs(BaseDescr descr) {
        if (descr instanceof AndDescr andDescr) {
            return "and(" + andDescr.getDescrs().stream()
                    .map(DrlAssertions::serializeLhs).collect(Collectors.joining(", ")) + ")";
        } else if (descr instanceof OrDescr orDescr) {
            return "or(" + orDescr.getDescrs().stream()
                    .map(DrlAssertions::serializeLhs).collect(Collectors.joining(", ")) + ")";
        } else if (descr instanceof ExistsDescr existsDescr) {
            return "exists(" + existsDescr.getDescrs().stream()
                    .map(child -> serializeLhs((BaseDescr) child)).collect(Collectors.joining(", ")) + ")";
        } else if (descr instanceof NotDescr notDescr) {
            return "not(" + notDescr.getDescrs().stream()
                    .map(DrlAssertions::serializeLhs).collect(Collectors.joining(", ")) + ")";
        } else if (descr instanceof PatternDescr patternDescr) {
            StringBuilder pattern = new StringBuilder("pattern[").append(patternDescr.getObjectType());
            String identifier = patternDescr.getIdentifier();
            if (identifier != null) {
                pattern.append(" := ").append(identifier.startsWith("$") ? identifier : "$" + identifier);
            }
            pattern.append(']');
            if (patternDescr.getConstraint() != null && !patternDescr.getConstraint().getDescrs().isEmpty()) {
                pattern.append('(').append(serializeLhs((BaseDescr) patternDescr.getConstraint())).append(')');
            }
            if (patternDescr.getSource() instanceof CollectDescr collectDescr) {
                pattern.append(" from collect(").append(serializeLhs(collectDescr.getInputPattern())).append(')');
            }
            return pattern.toString();
        } else if (descr instanceof ExprConstraintDescr exprConstraintDescr) {
            return "expr[" + normalizeDrl(exprConstraintDescr.getExpression()) + "]";
        }
        return fail("Unhandled descr type: " + descr.getClass().getName());
    }

    private static void addStatement(List<String> statements, StringBuilder statement) {
        String normalized = normalizeDrl(statement.toString());
        if (!normalized.isEmpty()) {
            statements.add(normalized);
        }
        statement.setLength(0);
    }
}
