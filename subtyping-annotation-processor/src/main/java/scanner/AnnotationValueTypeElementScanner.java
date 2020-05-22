package scanner;

import annotation.Type;
import annotation.UnsafeCast;
import scanner.type.RawType;
import scanner.type.UnknownType;
import scanner.util.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementScanner7;

public class AnnotationValueTypeElementScanner extends ElementScanner7<Void, Void> {
    private final Trees mTrees;
    private final AnnotationValueTypeTreeScanner annValTypeTreeScanner;
    private final Messager messager;
    private final SubtypingChecker subtypingChecker;

    public AnnotationValueTypeElementScanner(ProcessingEnvironment processingEnv) {
        this.mTrees = Trees.instance(processingEnv);
        this.messager = new ErrorsWithTypesNamesMessager(mTrees);
        this.subtypingChecker = new SingleInheritanceSubtypingChecker(processingEnv);
        this.annValTypeTreeScanner = new AnnotationValueTypeTreeScanner(processingEnv, messager,
                subtypingChecker, new SimpleOperationPermissionChecker(processingEnv));

    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        this.annValTypeTreeScanner.init(mTrees.getPath(e).getCompilationUnit());
        this.annValTypeTreeScanner.scan(mTrees.getTree(e), null);
        return super.visitExecutable(e, aVoid);
    }

    @Override
    public Void visitVariable(VariableElement e, Void aVoid) {
        ExpressionTree initializer = ((VariableTree) mTrees.getTree(e)).getInitializer();

        if (initializer != null) {
            CompilationUnitTree cut = mTrees.getPath(e).getCompilationUnit();
            this.messager.init(cut);
            this.annValTypeTreeScanner.init(cut);
            String initType = this.annValTypeTreeScanner.scan(initializer, null);
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
                    if (!initType.contentEquals(RawType.class.getName()) && !this.subtypingChecker.isSubtype(initType, declType)) {
                        UnsafeCast unsafeCastAnn = e.getAnnotation(UnsafeCast.class);
                        if (unsafeCastAnn != null) {
                            if (unsafeCastAnn.printWarning()) {
                                messager.printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.UNSAFE_TYPE_CAST);
                            }
                        } else {
                            if (initType.contentEquals(UnknownType.class.getName())) {
                                messager.printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.UNKNOWN_TYPE_ASSIGNMENT);
                            } else {
                                messager.printErrorAndWarning(mTrees.getTree(e),
                                        "'" + initType + "'",
                                        "'" + declType + "'",
                                        ErrorAndWarningKind.TYPE_MISMATCH_OPERAND);
                            }
                        }
                    }
                }
            } else {
                if (!initType.contentEquals(RawType.class.getName()) &&
                        !initType.contentEquals(UnknownType.class.getName())) {
                    messager.printErrorAndWarning(mTrees.getTree(e),
                            "'" + initType + "'",
                            "",
                            ErrorAndWarningKind.MISSING_ANNOTATION_ON_FIELD);
                }
            }
        }

        return super.visitVariable(e, aVoid);
    }
}
