package uk.ac.ebi.uniprot.urml.engine.drools.compiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.compiler.DrlAssertions.*;

/**
 * Unit tests for {@link DrlAssertions} DRL/MVEL normalization.
 *
 * @author Muhammad Aditya Hilmy
 */
class DrlAssertionsTest {

    /**
     * DRL normalization collapses whitespace outside string literals but preserves whitespace
     * inside double-quoted literals verbatim.
     */
    @Test
    void shouldPreserveLiteralWhitespaceWhenNormalizingDrl() {
        // Given two DRL snippets differing only in whitespace OUTSIDE string literals

        // When both are normalized, Then they must be considered equal
        assertEquals(normalizeDrl("id == \"a  b\"    and    id != \"c\""),
                normalizeDrl("  id   ==   \"a  b\"\n  and\n  id != \"c\"  "));

        // Given two DRL snippets differing in whitespace INSIDE a string literal

        // When both are normalized, Then the difference must be preserved (literals are verbatim)
        assertNotEquals(normalizeDrl("id == \"a b\""), normalizeDrl("id == \"a  b\""));
    }

    /**
     * MVEL normalization splits statements on ';' and newlines, terminates every statement
     * with ';' (one per line), drops stray semicolons and collapses whitespace inside
     * statements, without splitting inside string literals.
     */
    @Test
    void shouldNormalizeMvelConsequences() {
        /*
         * Given a consequence with a newline-terminated statement (no ';'), a stray ';' and
         * irregular whitespace
         */

        /*
         * When it is normalized, Then every statement ends with ';', one per line, the stray
         * ';' is dropped and interior whitespace is collapsed
         */
        assertEquals("update($a);\n$a.setX(\"k\");\nb.setY(1);",
                normalizeMvel("  update($a)\n$a.setX(\"k\") ;\n  b.setY(1); ;\n"));

        // Given a statement whose string literal contains a ';', and no terminating ';'

        // When it is normalized, Then the literal must not be split and the final ';' is added
        assertEquals("insertLogical(A.builder().withValue(\"a;b\").build());",
                normalizeMvel("insertLogical(A.builder().withValue(\"a;b\").build())"));
    }
}
