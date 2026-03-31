package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import java.util.ArrayList;
import java.util.List;

import static compiler.TypeRels.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions 
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	//checks that a type object is visitable (not incomplete) 
	private TypeNode ckvisit(TypeNode t) throws TypeException {
		visit(t);
		return t;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode n) throws TypeException {
		if (print) printNode(n);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) ) 
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !isSubtype(visit(n.exp),ckvisit(n.getType())) )
			throw new TypeException("Incompatible value for variable " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

    @Override
    public TypeNode visitNode(IfNode n) throws TypeException {
        if (print) printNode(n);

        if (!(isSubtype(visit(n.cond), new BoolTypeNode()))) {
            throw new TypeException("Non boolean condition in if", n.getLine());
        }

        TypeNode t = visit(n.th);
        TypeNode e = visit(n.el);
        TypeNode lca = TypeRels.lowestCommonAncestor(t, e);

        if (lca == null) {
            throw new TypeException("Incompatible types in then-else branches", n.getLine());
        }

        return lca;
    }

	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry); 
		if ( !(t instanceof ArrowTypeNode) )
			throw new TypeException("Invocation of a non-function "+n.id,n.getLine());
		ArrowTypeNode at = (ArrowTypeNode) t;
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of "+n.id,n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.id,n.getLine());
		return at.ret;
	}

	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry); 
		if (t instanceof ArrowTypeNode)
			throw new TypeException("Wrong usage of function identifier " + n.id,n.getLine());
		if (t instanceof ClassTypeNode) {
			throw new TypeException("Wrong usage of class identifier " + n.id, n.getLine());
		}
		return t;
	}

	@Override
	public TypeNode visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return new IntTypeNode();
	}

// gestione tipi incompleti	(se lo sono lancia eccezione)
	
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (print) printNode(n);
		for (Node par: n.parlist) visit(par);
		visit(n.ret,"->"); //marks return type
		return null;
	}

	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (print) printNode(n);
		return null;
	}

// STentry (ritorna campo type)

	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) printSTentry("type");
		return ckvisit(entry.type); 
	}

    @Override
    public TypeNode visitNode(DivNode n) throws TypeException {
        if (print) printNode(n);
        if ( !(isSubtype(visit(n.left), new IntTypeNode()) && isSubtype(visit(n.right), new IntTypeNode())) )
            throw new TypeException("Non integers in division", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(MinusNode n) throws TypeException {
        if (print) printNode(n);
        if ( !(isSubtype(visit(n.left), new IntTypeNode()) && isSubtype(visit(n.right), new IntTypeNode())) )
            throw new TypeException("Non integers in subtraction", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if ( !(isSubtype(l, r) || isSubtype(r, l)) )
            throw new TypeException("Incompatible types in >= comparison", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(LessEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if ( !(isSubtype(l, r) || isSubtype(r, l)) )
            throw new TypeException("Incompatible types in <= comparison", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(AndNode n) throws TypeException {
        if (print) printNode(n);
        if ( !(isSubtype(visit(n.left), new BoolTypeNode()) && isSubtype(visit(n.right), new BoolTypeNode())) )
            throw new TypeException("Non booleans in AND operation", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(OrNode n) throws TypeException {
        if (print) printNode(n);
        if ( !(isSubtype(visit(n.left), new BoolTypeNode()) && isSubtype(visit(n.right), new BoolTypeNode())) )
            throw new TypeException("Non booleans in OR operation", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(NotNode n) throws TypeException {
        if (print) printNode(n);
        if ( !(isSubtype(visit(n.exp), new BoolTypeNode())) )
            throw new TypeException("Non boolean in NOT operation", n.getLine());
        return new BoolTypeNode();
    }

	@Override
	public TypeNode visitNode(final ClassNode n) throws TypeException {
        if (print) {
            printNode(n);
        }
        if (n.superEntry != null) {
            ClassTypeNode parentCT = (ClassTypeNode) n.superEntry.type;

            for (var field : n.fields) {
                int position = -field.offset - 1;

                if (position < parentCT.allFields.size()) {
                    if (!isSubtype(field.getType(), parentCT.allFields.get(position))) {
                        throw new TypeException("Wrong type for overridden field " + field.id, field.getLine());
                    }
                }
            }

            for (var method : n.methods) {
                int position = method.offset;

                if (position < parentCT.allMethods.size()) {
                    List<TypeNode> parTypes = new ArrayList<>();
                    for (var par : method.parlist) {
                        parTypes.add(par.getType());
                    }
                    ArrowTypeNode childMethodType = new ArrowTypeNode(parTypes, method.getType());

                    if (!isSubtype(childMethodType, parentCT.allMethods.get(position))) {
                        throw new TypeException("Wrong type for overridden method " + method.id, method.getLine());
                    }
                }
            }
        }

        for (final var method: n.methods) {
            visit(method);
        }

        return null;
	}

	@Override
	public TypeNode visitNode(final MethodNode n) throws TypeException {
		if (print) {
			printNode(n);
		}
		n.declist.forEach(dec -> {
			try {
				// Visits declaration of each method
				visit(dec);
			} catch (final IncomplException e) {

			} catch (final TypeException e) {
				System.out.println("Type checking error in declaration: " + e.text);
			}
		});
		// Checks return type of method
		if (!isSubtype(visit(n.exp), ckvisit(n.getType()))) {
			throw new TypeException("Wrong return type for method: " + n.id, n.getLine());
		}
		return null;
	}

	@Override
	public TypeNode visitNode(final NewNode n) throws TypeException {
		if (print) {
			this.printNode(n);
		}
		final List<TypeNode> fields = ((ClassTypeNode) n.entry.type).allFields; // list of fields type
		// if number of parameters is different from expected
		if (n.argList.size() != fields.size()) {
			throw new TypeException("Wrong number of parameters for new instance of class: " + n.classId, n.getLine());
		}
		for (var i = 0; i < fields.size(); i++) {
			// checks that every param type given is subtype of expected
			if (!isSubtype(visit(n.argList.get(i)), fields.get(i))) {
				throw new TypeException("Wrong type of " + (i + 1) + " parameter in invocation of " + n.classId, n.getLine());
			}
		}
		// returns a reference type with visited class id
		return new RefTypeNode(n.classId);
	}

	@Override
	public TypeNode visitNode(final ClassCallNode n) throws TypeException {
		if (print) {
			this.printNode(n);
		}
		TypeNode t = visit(n.methodEntry);
		if ( !(t instanceof ArrowTypeNode) )
			throw new TypeException("Invocation of a non-function " + n.id2, n.getLine());
		ArrowTypeNode at = (ArrowTypeNode) t;
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of " + n.id2, n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of " + n.id2, n.getLine());
		return at.ret;
	}

	@Override
	public TypeNode visitNode(final EmptyNode n) {
		if (print) {
			this.printNode(n);
		}
		return new EmptyTypeNode();
	}

    @Override
    public TypeNode visitNode(RefTypeNode n) throws TypeException {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(ClassTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (var field : n.allFields) {
            visit(field);
        }
        for (var method : n.allMethods) {
            visit(method);
        }
        return null;
    }

    @Override
    public TypeNode visitNode(EmptyTypeNode n) {
        if (print) printNode(n);
        return null;
    }
}