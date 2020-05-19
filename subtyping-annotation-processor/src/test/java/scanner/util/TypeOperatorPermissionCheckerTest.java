package scanner.util;

import annotation.operation.arithmetic.*;
import annotation.operation.logical.*;
import annotation.operation.equal.*;
import annotation.operation.bitwise.*;
import com.sun.source.tree.Tree;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeOperatorPermissionCheckerTest {

    @Test
    public void treeKindToFieldName() {
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.PLUS), Plus.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.MINUS), Minus.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.DIVIDE), Divide.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.MULTIPLY), Multiply.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.AND), And.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.OR), Or.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.XOR), Xor.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.CONDITIONAL_AND), ConditionalAnd.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.CONDITIONAL_OR), ConditionalOr.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.LEFT_SHIFT), LeftShift.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.RIGHT_SHIFT), RightShift.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.UNSIGNED_RIGHT_SHIFT), UnsignedRightShift.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.UNARY_PLUS), UnaryPlus.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.UNARY_MINUS), UnaryMinus.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.EQUAL_TO), Equal.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.NOT_EQUAL_TO), Equal.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.GREATER_THAN), Equal.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.LESS_THAN), Equal.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.LESS_THAN_EQUAL), Equal.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.GREATER_THAN_EQUAL), Equal.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.LOGICAL_COMPLEMENT), LogicalComplement.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.BITWISE_COMPLEMENT), BitwiseComplement.class);

        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.POSTFIX_DECREMENT), Decrement.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.PREFIX_DECREMENT), Decrement.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.POSTFIX_INCREMENT), Increment.class);
        assertEquals(OperationPermissionChecker.treeKindToAnnotation(Tree.Kind.PREFIX_INCREMENT), Increment.class);
    }
}