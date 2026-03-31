package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {

    public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {

        if (a instanceof EmptyTypeNode && b instanceof RefTypeNode) {
            return true;
        }

        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String aId = ((RefTypeNode) a).id;
            String bId = ((RefTypeNode) b).id;

            while (aId != null) {
                if (aId.equals(bId)) {
                    return true;
                }
                aId = superType.get(aId);
            }
            return false;
        }

        if (a instanceof ArrowTypeNode arrowA && b instanceof ArrowTypeNode arrowB) {
            if (arrowA.parlist.size() != arrowB.parlist.size()) {
                return false;
            }
            if (!isSubtype(arrowA.ret, arrowB.ret)) {
                return false;
            }
            for (int i = 0; i < arrowA.parlist.size(); i++) {
                if (!isSubtype(arrowB.parlist.get(i), arrowA.parlist.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode));	}
}
