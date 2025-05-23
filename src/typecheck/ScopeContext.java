package typecheck;

import ast.*;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

import java.util.HashMap;
import java.util.Map;


/*
 * A context class that stores tree structure between nodes and an exception map that indicates what
 * exceptions are acceptable in the current contract
 * */
public class ScopeContext {

    private Node cur;
    private Map<ExceptionTypeSym, Boolean> funcExceptionMap;
    private ScopeContext parent;
    private final String SHErrLocName;

    public ScopeContext(Node cur, ScopeContext parent) {
        this.cur = cur;
        this.parent = parent;
        if (parent != null) {
            funcExceptionMap = new HashMap<>(parent.funcExceptionMap);
        } else {
            funcExceptionMap = new HashMap<>();
        }
        SHErrLocName = calcSHErrLocName();
    }
    
    public ScopeContext(Node cur, ScopeContext parent,
            Map<ExceptionTypeSym, Boolean> funcExceptionMap) {
        this.cur = cur;
        this.parent = parent;
        this.funcExceptionMap = new HashMap<>(funcExceptionMap);
        SHErrLocName = calcSHErrLocName();
    }

    private String calcSHErrLocName() {
        if (cur == null && parent == null) {
            return "global";
        }

        String localPostfix;
        if (cur instanceof Contract) {
            localPostfix = ((Contract) cur).getContractName();
        } else if (cur instanceof FunctionSig) {
            localPostfix = ((FunctionSig) cur).getName();
        } else if (cur instanceof If) {
            localPostfix = "if" + cur.locToString();
        } else if (cur instanceof EndorseIfStatement) {
            localPostfix = "endorseIf" + cur.locToString();
        } else if (cur instanceof While) {
            localPostfix = "while" + cur.locToString();
        } else if (cur instanceof For) {
            localPostfix = "for" + cur.locToString();
        } else if (cur instanceof Interface) {
            localPostfix = ((Interface) cur).getContractName();
        } else if (cur instanceof GuardBlock) {
            localPostfix = "guardBlock" + cur.locToString();
        } else if (cur instanceof Try) {
            localPostfix = "try" + cur.locToString();
        } else if (cur instanceof Atomic) {
            localPostfix = "atomic" + cur.locToString();
        } else if (cur instanceof SourceFile) {
            localPostfix = ((SourceFile) cur).getSourceFileId();
        } else {
            assert cur != null;
            assert !cur.toSHErrLocFmt().startsWith("null"): cur.toSHErrLocFmt();
            // localPostfix = cur.toSHErrLocFmt();
            return cur.toSHErrLocFmt();
            //throw new RuntimeException();
        }

        if (parent != null) {
            return parent.getSHErrLocName() + "." + localPostfix;
        } else {
            assert !localPostfix.equals("null");
            return localPostfix;
        }
    }

    public String getSHErrLocName() {
        return SHErrLocName;
    }

    public Constraint genCons(ScopeContext rhs, Relation op, NTCEnv env, CodeLocation location) {

        return new Constraint(new Inequality(getSHErrLocName(), op, rhs.getSHErrLocName()),
                env.globalHypothesis(), location, "");
    }

    public Constraint genCons(String rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs), env.globalHypothesis(),
                location, "");
    }

    public boolean isContractLevel() {
        if (cur instanceof Contract) {
            return true;
        }
        if ((cur instanceof FunctionDef) || (cur instanceof FunctionSig) || (parent == null)) {
            return false;
        }
        return parent.isContractLevel();
    }

    public ScopeContext getParent() {
        return parent;
    }

    public String getFuncName() {
        ScopeContext now = this;
        while (!(now.cur instanceof FunctionSig)) {
            now = now.parent;
        }
        return ((FunctionSig) now.cur).getName();
    }

    // static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return SHErrLocName;
    }

    public boolean isCheckedException(ExceptionTypeSym t, boolean extern) {
        return funcExceptionMap.containsKey(t) && (extern || funcExceptionMap.get(t));
    }

    public void addException(ExceptionTypeSym t, boolean inTx) {
        funcExceptionMap.put(t, inTx);
    }

    public void removeException(ExceptionTypeSym t) {
        funcExceptionMap.remove(t);
    }

    public void printExceptionSet() {
        System.err.println(funcExceptionMap.size());
        for (Map.Entry<ExceptionTypeSym, Boolean> t : funcExceptionMap.entrySet()) {
            System.err.println(t + t.getKey().getName());
        }
    }

    public Node cur() {
        return cur;
    }

    public ScopeContext parent() {
        return parent;
    }

    public Constraint genEqualCons(Sym sym, NTCEnv env, CodeLocation location, String explanation) {
        assert !getSHErrLocName().startsWith("null") : getSHErrLocName();
        return new Constraint(new Inequality(getSHErrLocName(), CompareOperator.Eq, sym.toSHErrLocFmt()),
                env.globalHypothesis(), location, explanation);
    }

    public void clearExceptions() {
        funcExceptionMap.clear();
    }
}
