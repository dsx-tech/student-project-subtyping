package annvisitor.util;

import ann.operation.arithmetic.*;
import ann.operation.logical.*;
import ann.operation.equal.*;
import ann.operation.bitwise.*;
import com.sun.source.tree.Tree;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeOperatorPermissionCheckerTest {

    @Test
    public void treeKindToFieldName() {
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.PLUS), Plus.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.MINUS), Minus.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.DIVIDE), Divide.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.MULTIPLY), Multiply.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.AND), And.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.OR), Or.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.XOR), Xor.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.CONDITIONAL_AND), ConditionalAnd.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.CONDITIONAL_OR), ConditionalOr.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.LEFT_SHIFT), LeftShift.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.RIGHT_SHIFT), RightShift.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.UNSIGNED_RIGHT_SHIFT), UnsignedRightShift.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.UNARY_PLUS), UnaryPlus.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.UNARY_MINUS), UnaryMinus.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.EQUAL_TO), Equal.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.NOT_EQUAL_TO), Equal.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.GREATER_THAN), Equal.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.LESS_THAN), Equal.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.LESS_THAN_EQUAL), Equal.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.GREATER_THAN_EQUAL), Equal.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.LOGICAL_COMPLEMENT), LogicalComplement.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.BITWISE_COMPLEMENT), BitwiseComplement.class);

        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.POSTFIX_DECREMENT), Decrement.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.PREFIX_DECREMENT), Decrement.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.POSTFIX_INCREMENT), Increment.class);
        assertEquals(TypeOperatorPermissionChecker.treeKindToAnnotation(Tree.Kind.PREFIX_INCREMENT), Increment.class);
    }
}