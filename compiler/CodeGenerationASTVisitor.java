package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import visualsvm.ExecuteVM;

import static compiler.lib.FOOLlib.*;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

    List<List<String>> dispatchTables = new ArrayList<>();

    CodeGenerationASTVisitor() {}
    CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

    @Override
    public String visitNode(ProgLetInNode n) {
        if (print) printNode(n);
        String declCode = null;
        for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
        return nlJoin(
                "push 0",
                declCode, // generate code for declarations (allocation)
                visit(n.exp),
                "halt",
                getCode()
        );
    }

    @Override
    public String visitNode(ProgNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.exp),
                "halt"
        );
    }

    @Override
    public String visitNode(FunNode n) {
        if (print) printNode(n,n.id);
        String declCode = null, popDecl = null, popParl = null;
        for (Node dec : n.declist) {
            declCode = nlJoin(declCode,visit(dec));
            popDecl = nlJoin(popDecl,"pop");
        }
        for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
        String funl = freshFunLabel();
        putCode(
                nlJoin(
                        funl+":",
                        "cfp", // set $fp to $sp value
                        "lra", // load $ra value
                        declCode, // generate code for local declarations (they use the new $fp!!!)
                        visit(n.exp), // generate code for function body expression
                        "stm", // set $tm to popped value (function result)
                        popDecl, // remove local declarations from stack
                        "sra", // set $ra to popped value
                        "pop", // remove Access Link from stack
                        popParl, // remove parameters from stack
                        "sfp", // set $fp to popped value (Control Link)
                        "ltm", // load $tm value (function result)
                        "lra", // load $ra value
                        "js"  // jump to to popped address
                )
        );
        return "push "+funl;
    }

    @Override
    public String visitNode(VarNode n) {
        if (print) printNode(n,n.id);
        return visit(n.exp);
    }

    @Override
    public String visitNode(PrintNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.exp),
                "print"
        );
    }

    @Override
    public String visitNode(IfNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.cond),
                "push 1",
                "beq "+l1,
                visit(n.el),
                "b "+l2,
                l1+":",
                visit(n.th),
                l2+":"
        );
    }

    @Override
    public String visitNode(EqualNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "beq "+l1,
                "push 0",
                "b "+l2,
                l1+":",
                "push 1",
                l2+":"
        );
    }

    @Override
    public String visitNode(TimesNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "mult"
        );
    }

    @Override
    public String visitNode(PlusNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "add"
        );
    }

    @Override
    public String visitNode(CallNode n) {
        String argCode = null; // arg code
        String getAR = null; // get activation record
        if (print) {
            printNode(n, n.id);
        }
        for (int i = n.arglist.size() - 1; i >= 0; i--) {
            argCode = nlJoin(argCode, visit(n.arglist.get(i)));
        }
        for (int i = 0; i < n.nl - n.entry.nl; i++) {
            getAR = nlJoin(getAR, "lw");
        }

        if (n.entry.offset >= 0) {
            return nlJoin(
                    "lfp", // load control link
                    argCode, // load args
                    "lfp",
                    getAR, // climb static chain
                    "stm", // save object pointer to tm
                    "ltm", // push access link (il "this")
                    "ltm", // duplicate access link
                    "lw", // load dispatch pointer
                    "push " + n.entry.offset,
                    "add", // add method offset
                    "lw", // load method address dalla dispatch table
                    "js" // jump
            );
        } else {
            return nlJoin(
                    "lfp", // load control link
                    argCode, // load args
                    "lfp",
                    getAR, // climb static chain
                    "stm", // save to tm
                    "ltm", // push access link
                    "ltm", // duplicate access link
                    "push " + n.entry.offset,
                    "add", // compute address
                    "lw", // load address
                    "js"  // jump
            );
        }
    }

    @Override
    public String visitNode(IdNode n) {
        if (print) printNode(n,n.id);
        String getAR = null;
        for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
        return nlJoin(
                "lfp", getAR, // retrieve address of frame containing "id" declaration
                // by following the static chain (of Access Links)
                "push "+n.entry.offset, "add", // compute address of "id" declaration
                "lw" // load value of "id" variable
        );
    }

    @Override
    public String visitNode(BoolNode n) {
        if (print) printNode(n,n.val.toString());
        return "push "+(n.val?1:0);
    }

    @Override
    public String visitNode(IntNode n) {
        if (print) printNode(n,n.val.toString());
        return "push "+n.val;
    }

    // --- NUOVI OPERATORI ---

    @Override
    public String visitNode(DivNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "div"
        );
    }

    @Override
    public String visitNode(MinusNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "sub"
        );
    }

    @Override
    public String visitNode(GreaterEqualNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.right),
                visit(n.left),
                "bleq " + l1,
                "push 0",
                "b " + l2,
                l1 + ":",
                "push 1",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(LessEqualNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "bleq " + l1,
                "push 0",
                "b " + l2,
                l1 + ":",
                "push 1",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(AndNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                "push 0",
                "beq " + l1,
                visit(n.right),
                "b " + l2,
                l1 + ":",
                "push 0",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(OrNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                "push 1",
                "beq " + l1,
                visit(n.right),
                "b " + l2,
                l1 + ":",
                "push 1",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(NotNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.exp),
                "push 1",
                "beq " + l1,
                "push 1",
                "b " + l2,
                l1 + ":",
                "push 0",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(final MethodNode n) {
        if (print) {
            printNode(n);
        }
        n.label = freshLabel();
        String declCode = null, popDecl = null, popParl = null;
        for (Node dec : n.declist) {
            declCode = nlJoin(declCode, visit(dec));
            popDecl = nlJoin(popDecl,"pop");
        }
        for (int i = 0; i < n.parlist.size(); i++) popParl = nlJoin(popParl,"pop");

        String funl = freshFunLabel();
        putCode(
                nlJoin(
                        funl+":",
                        "cfp", // set $fp to $sp value
                        "lra", // load $ra value
                        declCode, // generate code for local declarations (they use the new $fp!!!)
                        visit(n.exp), // generate code for function body expression
                        "stm", // set $tm to popped value (function result)
                        popDecl, // remove local declarations from stack
                        "sra", // set $ra to popped value
                        "pop", // remove Access Link from stack
                        popParl, // remove parameters from stack
                        "sfp", // set $fp to popped value (Control Link)
                        "ltm", // load $tm value (function result)
                        "lra", // load $ra value
                        "js"  // jump to popped address
                )
        );
        return null;
    }

    @Override
    public String visitNode(final ClassNode n) {
        final List<String> dispatchTable = new ArrayList<>();
        this.dispatchTables.add(dispatchTable);
        for (int i = 0; i < n.methods.size(); i++) {
            visit(n.methods.get(i));
            dispatchTable.add(n.methods.get(i).label);
        }
        String dispatchTableCreation = null;
        for (var label: dispatchTable) {
            dispatchTableCreation = nlJoin(
                    dispatchTableCreation,
                    "push " + label,
                    "lhp",
                    "sw", // saves label into memory address pointed to by $hp
                    "lhp",
                    "push 1",
                    "add",
                    "shp" // increments $hp by 1
            );
        }
        return nlJoin(
                "lhp",
                dispatchTableCreation
        );
    }

    @Override
    public String visitNode(final EmptyNode n) {
        if (print) {
            printNode(n);
        }
        return "push -1";
    }

    @Override
    public String visitNode(final ClassCallNode n) {
        String argCode = null; // argument code accumulate assembly code
        String getAR = null;   // get active record
        if (print) {
            printNode(n, n.id1 + "." + n.id2);
        }
        for (int i = n.arglist.size() - 1; i >= 0; i--) {
            argCode = nlJoin(argCode, visit(n.arglist.get(i)));
        }
        for (int i = 0; i < n.nl - n.entry.nl; i++) {
            getAR = nlJoin(getAR, "lw");
        }
        return nlJoin(
                "lfp",
                argCode,
                "lfp",
                getAR,
                "push " + n.entry.offset,
                "add",
                "lw",
                "stm",
                "ltm",
                "ltm",
                "lw",
                "push " + n.methodEntry.offset,
                "add",
                "lw",
                "js"
        );
    }

    @Override
    public String visitNode(final NewNode n) {
        String argCode = null; // evaluate args
        String storeCode = null; // store args to heap
        if (print) {
            printNode(n);
        }

        for (int i = 0; i < n.argList.size(); i++) {
            argCode = nlJoin(argCode, visit(n.argList.get(i)));
        }

        for (int i = 0; i < n.argList.size(); i++) {
            storeCode = nlJoin(storeCode,
                    "lhp",
                    "sw", // store arg to heap (pops value from stack)
                    "lhp",
                    "push 1",
                    "add",
                    "shp" // increment hp
            );
        }

        return nlJoin(
                argCode,
                storeCode,
                "push " + ExecuteVM.MEMSIZE,
                "push " + n.entry.offset,
                "add",
                "lw", // load dispatch pointer
                "lhp",
                "sw", // store dispatch pointer
                "lhp", // push object pointer (this remains on stack as the returned value!)
                "lhp",
                "push 1",
                "add",
                "shp" // increment hp
        );
    }
}