package scanner;

import annotation.Type;
import annotation.UnsafeCast;
import scanner.type.RawType;
import scanner.type.UnknownType;
import scanner.util.ErrorAndWarningKind;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner7;

import static scanner.util.SubtypingChecker.isSubtype;
import static scanner.util.Messager.printErrorAndWarning;

public class AnnotationValueTypeElementScanner extends ElementScanner7<Void, Void> {
    private final ProcessingEnvironment processingEnv;
    private final Trees mTrees;

    public AnnotationValueTypeElementScanner(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.mTrees = Trees.instance(processingEnv);
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        AnnotationValueTypeTreeScanner
                .getInstance(processingEnv, mTrees.getPath(e).getCompilationUnit())
                .scan(mTrees.getTree(e), null);
        return super.visitExecutable(e, aVoid);
    }

    @Override
    public Void visitVariable(VariableElement e, Void aVoid) {
        ExpressionTree init = ((VariableTree) mTrees.getTree(e)).getInitializer();

        if (init != null) {
            CompilationUnitTree cut = mTrees.getPath(e).getCompilationUnit();
            String initType = AnnotationValueTypeTreeScanner.getInstance(processingEnv, cut).scan(init, null);
            String declType = RawType.class.getName();
            Type ann = e.getAnnotation(Type.class);

            if (ann != null) {
                try {
                    ann.value();
                } catch (MirroredTypeException mte) {
                    if (mte.getTypeMirror() != null) {
                        declType = mte.getTypeMirror().toString();
                    }
                }

                if (!declType.contentEquals(RawType.class.getName()) && !declType.contentEquals(UnknownType.class.getName())) {
                    if (!initType.contentEquals(RawType.class.getName()) && !isSubtype(initType, declType, processingEnv)) {
                        UnsafeCast unsafeCastAnn = e.getAnnotation(UnsafeCast.class);
                        if (unsafeCastAnn != null) {
                            if (unsafeCastAnn.printWarning()) {
                                printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.UNSAFE_TYPE_CAST,
                                        mTrees, cut);
                            }
                        } else {
                            if (initType.contentEquals(UnknownType.class.getName())) {
                                printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.UNKNOWN_TYPE_ASSIGNMENT,
                                        mTrees, cut);
                            } else {
                                printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.TYPE_MISMATCH_OPERAND,
                                        mTrees, cut);
                            }
                        }
                    }
                }
            } else {
                if (!initType.contentEquals(RawType.class.getName()) &&
                        !initType.contentEquals(UnknownType.class.getName())) {
                    printErrorAndWarning(mTrees.getTree(e),
                            "'" + initType + "'",
                            "",
                            ErrorAndWarningKind.MISSING_ANNOTATION_ON_FIELD,
                            mTrees, cut);
                }
            }
        }

        return super.visitVariable(e, aVoid);
    }
}
