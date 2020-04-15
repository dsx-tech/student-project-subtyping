package annvisitor.util;

import ann.type.Top;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Iterator;
import java.util.LinkedList;

public class AnnotationValueSubtypes {
    private AnnotationValueSubtypes() {

    }
    // TODO: unit tests
    public static boolean findPath(String leaf, String root,
                                   LinkedList<String> path,
                                   ProcessingEnvironment processingEnv) {
        if (leaf == null || root == null) {
            return false;
        }

        TypeElement curNode;
        while (!leaf.equals(root)) {
            if (leaf.equals(Top.class.getName())) {
                return false;
            }

            path.addFirst(leaf);

            curNode = processingEnv.getElementUtils().getTypeElement(leaf);
            TypeMirror superclass = curNode.getSuperclass();

            if (superclass.toString().equals(Object.class.getName())) {
                leaf = Top.class.getName();
            } else {
                leaf = superclass.toString();
            }
        }

        path.addFirst(leaf);

        return true;
    }

    // TODO: unit tests
    public static String generalizeTypes(String r, String l, ProcessingEnvironment processingEnv) {
        if (r.equals(l)) {
            return r;
        }

        LinkedList<String> anc1 = new LinkedList<>();
        LinkedList<String> anc2 = new LinkedList<>();
        String commonAnc = Top.class.getName();

        if (findPath(r, commonAnc, anc1, processingEnv) && findPath(l, commonAnc, anc2, processingEnv)) {
            Iterator<String> it1 = anc1.listIterator();
            Iterator<String> it2 = anc2.listIterator();
            String lastCommon;

            while (it1.hasNext() && it2.hasNext()) {
                lastCommon = it1.next();
                if (lastCommon.equals(it2.next())) {
                    commonAnc = lastCommon;
                } else {
                    break;
                }
            }
        }

        return commonAnc;
    }

    public static boolean isSubtype(String subt, String supt, ProcessingEnvironment processingEnv) {
        if (subt != null && supt != null &&
                supt.equals(Top.class.getName())) {
            return true;
        }
        LinkedList<String> buf = new LinkedList<>();
        return findPath(subt, supt, buf, processingEnv);
    }
}
