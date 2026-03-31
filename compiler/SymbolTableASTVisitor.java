package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode funNode) {
		if (print) printNode(funNode);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : funNode.parlist) parTypes.add(par.getType());
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,funNode.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(funNode.id, entry) != null) {
			System.out.println("Fun id " + funNode.id + " at line "+ funNode.getLine() +" already declared");
			stErrors++;
		}
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset;
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : funNode.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ funNode.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : funNode.declist) visit(dec);
		visit(funNode.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset;
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

    @Override
    public Void visitNode(DivNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

//    @Override
//	public Void visitNode(ClassNode classNode) {
//		final ClassTypeNode classType = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
//		this.symTable.getFirst().put(classNode.id, new STentry(nestingLevel, classType, decOffset--));
//		this.classTable.put(classNode.id, new HashMap<>());
//		this.nestingLevel++;
//		final Map<String, STentry> classMap = this.classTable.get(classNode.id);
//		if (this.symTable.getFirst().containsKey(classNode.id)) {
//			System.out.println("Class id " + classNode.id + " at line "+ classNode.getLine() +" already declared");
//			this.stErrors++;
//		}
//
//		this.symTable.add(classMap);
//		final int prevDecOffset = this.decOffset;
//		int fieldOffset = -1;
//		this.decOffset = 0;
//		for (var field : classNode.fields) {
//			if (classMap.containsKey(field.id)) {
//				System.out.println("Field id " + field.id + " at line "+ field.getLine() +" already declared");
//				this.stErrors++;
//			}
//
//			classMap.put(field.id, new STentry(this.nestingLevel, field.getType(), fieldOffset--));
//			classType.allFields.add(field.getType());
//		}
//		for (var method : classNode.methods) {
//			if (classMap.containsKey(method.id)) {
//				System.out.println("Field id " + method.id + " at line "+ method.getLine() +" already declared");
//				this.stErrors++;
//			}
//			visitNode(method);
//			classType.allMethods.add((ArrowTypeNode) method.getType());
//		}
//		this.symTable.remove(this.nestingLevel);
//		this.nestingLevel--;
//		this.decOffset = prevDecOffset;
//		return null;
//	}

    @Override
    public Void visitNode(ClassNode classNode) {
        if (print) printNode(classNode);

        final ClassTypeNode classType = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());

        if (this.symTable.getFirst().containsKey(classNode.id)) {
            System.out.println("Class id " + classNode.id + " at line " + classNode.getLine() + " already declared");
            this.stErrors++;
        } else {
            this.symTable.getFirst().put(classNode.id, new STentry(nestingLevel, classType, decOffset--));
            this.classTable.put(classNode.id, new HashMap<>());
        }

        TypeRels.superType.put(classNode.id, classNode.superId);

        this.nestingLevel++;
        final Map<String, STentry> classMap = this.classTable.get(classNode.id);
        this.symTable.add(classMap);

        final int prevDecOffset = this.decOffset;
        int fieldOffset = -1;
        this.decOffset = 0;

        if (classNode.superId != null){
            if (!classTable.containsKey(classNode.superId)) {
                System.out.println("Class id " + classNode.superId + " at line " + classNode.getLine() + " no exist");
                this.stErrors++;
            }
            STentry parentSTentry = symTable.getFirst().get(classNode.superId);
            Map<String, STentry> parentMap = this.classTable.get(classNode.superId);

            classNode.superEntry = parentSTentry;

            ClassTypeNode parentTypeNode = (ClassTypeNode) parentSTentry.type;

            classMap.putAll(parentMap);

            classType.allFields.addAll(parentTypeNode.allFields);
            classType.allMethods.addAll(parentTypeNode.allMethods);

            fieldOffset = - (parentTypeNode.allFields.size() +1);
            this.decOffset = parentTypeNode.allMethods.size();


        }

        for (var field : classNode.fields) {
            int offset;
            if (classMap.containsKey(field.id)) {
                STentry oldEntry = classMap.get(field.id);

                if (oldEntry.type instanceof ArrowTypeNode) {
                    System.out.println("Field " + field.id + " at line " + field.getLine() + " cannot override a method!");
                    this.stErrors++;
                    offset = fieldOffset--;
                } else {
                    offset = oldEntry.offset;
                }
            } else {
                offset = fieldOffset--;
            }

            classMap.put(field.id, new STentry(this.nestingLevel, field.getType(), offset));

            field.offset = offset;
                if (-offset - 1 < classType.allFields.size()) {
                classType.allFields.set(-offset - 1, field.getType());
            } else {
                classType.allFields.add(field.getType());
            }
        }

        Set<String> localMethods = new HashSet<>();

        for (var method : classNode.methods) {

            if (localMethods.contains(method.id)) {
                System.out.println("Method id " + method.id + " at line " + method.getLine() + " already declared");
                this.stErrors++;
            } else {
                localMethods.add(method.id);
            }

            visitNode(method);
            STentry methodEntry = classMap.get(method.id);

            if (methodEntry.offset < classType.allMethods.size()) {
                classType.allMethods.set(methodEntry.offset, (ArrowTypeNode) methodEntry.type);
            } else {
                classType.allMethods.add((ArrowTypeNode) methodEntry.type);
            }
        }

        this.symTable.remove(this.nestingLevel);
        this.nestingLevel--;
        this.decOffset = prevDecOffset;

        return null;
    }

//    @Override
//	public Void visitNode(MethodNode methodNode) {
//		if (print) {
//			printNode(methodNode);
//		}
//		final Map<String, STentry> virtualTable = this.symTable.get(this.nestingLevel);
//		final List<TypeNode> parameterTypes = new ArrayList<>();
//		methodNode.parlist.forEach(x -> parameterTypes.add(x.getType()));
//		final ArrowTypeNode methodType = new ArrowTypeNode(parameterTypes, methodNode.getType());
//		final STentry methodEntry = new STentry(this.nestingLevel, methodType, this.decOffset);
//		virtualTable.put(methodNode.id, methodEntry);
//		methodNode.offset = this.decOffset;
//		this.decOffset++;
//		return null;
//	}

    @Override
    public Void visitNode(MethodNode methodNode) {
        if (print) {
            printNode(methodNode);
        }

        final Map<String, STentry> virtualTable = this.symTable.get(this.nestingLevel);
        final List<TypeNode> parameterTypes = new ArrayList<>();
        for (ParNode par : methodNode.parlist) {
            parameterTypes.add(par.getType());
        }
        final ArrowTypeNode methodType = new ArrowTypeNode(parameterTypes, methodNode.getType());

        int offset;
        if (virtualTable.containsKey(methodNode.id)) {

            offset = virtualTable.get(methodNode.id).offset;
        } else {
            offset = this.decOffset;
            this.decOffset++;
        }

        final STentry methodEntry = new STentry(this.nestingLevel, methodType, offset);
        virtualTable.put(methodNode.id, methodEntry);
        methodNode.offset = offset;

        this.nestingLevel++;
        Map<String, STentry> methodMap = new HashMap<>();
        this.symTable.add(methodMap);
        int prevDecOffset = this.decOffset;
        this.decOffset = -2;
        int parOffset = 1;

        for (ParNode par : methodNode.parlist) {
            if (methodMap.put(par.id, new STentry(this.nestingLevel, par.getType(), parOffset++)) != null) {
                System.out.println("Par id " + par.id + " at line " + methodNode.getLine() + " already declared");
                this.stErrors++;
            }
        }

        for (Node dec : methodNode.declist) {
            visit(dec);
        }
        visit(methodNode.exp);
        this.symTable.remove(this.nestingLevel--);
        this.decOffset = prevDecOffset;

        return null;
    }

    @Override
    public Void visitNode(NewNode newNode) {
        if (print) {
            printNode(newNode);
        }
        STentry entry = symTable.getFirst().get(newNode.classId);
        if (entry == null) {
            System.out.println("NewNode id" + newNode.classId + " at line "+ newNode.getLine() + " not declared");
            stErrors++;
        } else {
            newNode.entry = entry;
        }
        for (var elem: newNode.argList){
            visit(elem);
        }
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode classCallNode) {
        if (print) {
            printNode(classCallNode);
        }

        STentry entryId1 = stLookup(classCallNode.id1);
        if (entryId1 == null) {
            System.out.println("ClassCallNode id1 " + classCallNode.id1 + " at line " + classCallNode.getLine() + " not declared");
            stErrors++;
        } else if (entryId1.type instanceof RefTypeNode refType) {
            classCallNode.entry = entryId1;
            classCallNode.nl = this.nestingLevel;

            Map<String, STentry> classMap = classTable.get(refType.id);
            if (classMap == null) {
                System.out.println("Class " + refType.id + " at line " + classCallNode.getLine() + " not found in class table");
                stErrors++;
            } else {
                classCallNode.methodEntry = classMap.get(classCallNode.id2);
                if (classCallNode.methodEntry == null) {
                    System.out.println("Id1 " + classCallNode.id1 + " at line " + classCallNode.getLine() + " has no method " + classCallNode.id2);
                    stErrors++;
                }
            }
        } else {
            System.out.println("Id1 " + classCallNode.id1 + " at line " + classCallNode.getLine() + " is not an object");
            stErrors++;
        }

        for (var elem : classCallNode.arglist) {
            visit(elem);
        }
        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        if (print) printNode(n);
        return null;
    }
}
