package annvisitor.util;

import com.sun.source.tree.Tree;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeOperatorPermissionCheckerTest {

    @Test
    public void treeKindToFieldName() {
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.PLUS), "PLUS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.MINUS), "MINUS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.DIVIDE), "DIVIDE");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.MULTIPLY), "MULTIPLY");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.AND), "AND");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.OR), "OR");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.XOR), "XOR");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.CONDITIONAL_AND), "CONDITIONAL_AND");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.CONDITIONAL_OR), "CONDITIONAL_OR");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.LEFT_SHIFT), "LEFT_SHIFT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.RIGHT_SHIFT), "RIGHT_SHIFT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.UNSIGNED_RIGHT_SHIFT), "UNSIGNED_RIGHT_SHIFT");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.UNARY_PLUS), "UNARY_PLUS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.UNARY_MINUS), "UNARY_MINUS");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.EQUAL_TO), "EQUALS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.NOT_EQUAL_TO), "EQUALS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.GREATER_THAN), "EQUALS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.LESS_THAN), "EQUALS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.LESS_THAN_EQUAL), "EQUALS");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.GREATER_THAN_EQUAL), "EQUALS");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.LOGICAL_COMPLEMENT), "LOGICAL_COMPLEMENT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.BITWISE_COMPLEMENT), "BITWISE_COMPLEMENT");

        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.POSTFIX_DECREMENT), "DECREMENT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.PREFIX_DECREMENT), "DECREMENT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.POSTFIX_INCREMENT), "INCREMENT");
        assertEquals(TypeOperatorPermissionChecker.treeKindToFieldName(Tree.Kind.PREFIX_INCREMENT), "INCREMENT");
    }
}