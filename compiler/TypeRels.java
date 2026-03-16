package compiler;

import compiler.AST.*;
import compiler.lib.*;

public class TypeRels {

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
			return ((RefTypeNode)a).id.equals(((RefTypeNode)b).id);
		}
		return a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode)) || ((a instanceof EmptyTypeNode) && (b instanceof RefTypeNode));
	}

}
