package annvisitor.util;

import ann.type.Raw;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AnnotationValueSubtypes {
    private AnnotationValueSubtypes() {

    }

    public static boolean findPath(String leaf, String root,
                                   LinkedList<String> path,
                                   ProcessingEnvironment processingEnv) {
        if (leaf == null || root == null) {
            return false;
        }

        TypeElement curNode;
        while (!leaf.equals(root)) {
            if (leaf.equals(Raw.class.getName())) {
                return false;
            }

            path.addFirst(leaf);

            curNode = processingEnv.getElementUtils().getTypeElement(leaf);
            TypeMirror superclass = curNode.getSuperclass();

            if (superclass.toString().equals(Object.class.getName())) {
                leaf = Raw.class.getName();
            } else {
                leaf = superclass.toString();
            }
        }

        path.addFirst(leaf);

        return true;
    }

    public static String generalizeTypes(String type1, String type2, ProcessingEnvironment processingEnv) {
        if (type1.equals(type2)) {
            return type1;
        }

        LinkedList<String> path1 = new LinkedList<>();
        LinkedList<String> path2 = new LinkedList<>();

        if (findPath(type1, Raw.class.getName(), path1, processingEnv) &&
                findPath(type2, Raw.class.getName(), path2, processingEnv)) {
            return findMostCommonType(path1, path2);
        }

        return Raw.class.getName();
    }
    //TODO: add unit test
    public static String findMostCommonType(List<String> path1, List<String> path2) {
        Iterator<String> it1 = path1.listIterator();
        Iterator<String> it2 = path2.listIterator();
        String commonSuperType = Raw.class.getName();
        String lastCommon;

        while (it1.hasNext() && it2.hasNext()) {
            lastCommon = it1.next();
            if (lastCommon.equals(it2.next())) {
                commonSuperType = lastCommon;
            } else {
                break;
            }
        }

        return commonSuperType;
    }

    public static boolean isSubtype(String subt, String supt, ProcessingEnvironment processingEnv) {
        if (subt != null && supt != null &&
                supt.equals(Raw.class.getName())) {
            return true;
        }
        LinkedList<String> buf = new LinkedList<>();
        return findPath(subt, supt, buf, processingEnv);
    }
}
