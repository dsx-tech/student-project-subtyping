package scanner.type;

import annotation.MetaType;
import annotation.operation.arithmetic.*;
import annotation.operation.bitwise.BitwiseComplement;
import annotation.operation.bitwise.LeftShift;
import annotation.operation.bitwise.RightShift;
import annotation.operation.bitwise.UnsignedRightShift;
import annotation.operation.equal.Equal;
import annotation.operation.logical.*;
// Unknown type is implicit supertype all of types
@Plus
@Minus
@Divide
@Multiply
@Remainder
@UnaryPlus
@UnaryMinus
@Increment
@Decrement
@BitwiseComplement
@LeftShift
@RightShift
@UnsignedRightShift
@Equal
@And
@Or
@Xor
@ConditionalAnd
@ConditionalOr
@LogicalComplement
@MetaType
public class UnknownType {
}
