package ast;

import compile.CompileEnv;
import compile.ast.Type;

import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Assign extends Statement {

    Expression target;
    Expression value;

    public Assign(Expression target, Expression value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = target.generateConstraints(env, now);
        ScopeContext v = value.generateConstraints(env, now);
        // con: tgt should be a supertype of v
//        logger.debug(v);
//        logger.debug(env);
//        logger.debug(location);
        env.addCons(tgt.genCons(v, Relation.LEQ, env, location));
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // Context prevContext = env.prevContext;

        String ifNamePc = beginContext.pc; // Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        ExpOutcome to = null;
        String ifNameTgt = "";
        if (target instanceof Name) {
            //Assuming target is Name
            ifNameTgt = env.getVar(((Name) target).id).labelNameSLC();
        } else if (target instanceof Subscript || target instanceof Attribute) {
            // env.prevContext = valueContext;
            /*env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, valueContext.lambda), env.hypothesis, value.location, env.curContractSym.name,
                    "Lock should be maintained before execution of this operation"));*/
            to = target.genConsVisit(env, false);
            ifNameTgt = to.valueLabelName;
//            env.inContext = new Context(to.psi.getNormalPath().c.lambda, beginContext.lambda);
            env.inContext = Utils.genNewContextAndConstraints(env, false, to.psi.getNormalPath().c, beginContext.lambda, target.nextPcSHL(), location);
            // prevContext = tmp;
            // rtnLockName = tmp.lambda;
        } else {
            assert false;
            //TODO: error handling
        }
        ExpOutcome vo = value.genConsVisit(env, false);
        String ifNameValue = vo.valueLabelName;
        // prevContext = valueContext;

        env.cons.add(
                new Constraint(new Inequality(ifNameValue, ifNameTgt), env.hypothesis(), value.location,
                        env.curContractSym().getName(),
                        "Integrity of the value being assigned must be trusted to allow this assignment"));

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, ifNameTgt), env.hypothesis(), value.location,
                        env.curContractSym().getName(),
                        "Integrity of control flow must be trusted to allow this assignment"));

        typecheck.Utils.contextFlow(env, vo.psi.getNormalPath().c, endContext, value.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        if (to != null) {
            vo.psi.join(to.psi);
        }
        vo.psi.setNormalPath(endContext);
        assert vo.psi.getNormalPath().c != null;
        return vo.psi;
    }

    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        compile.ast.Expression targetExp = target.solidityCodeGen(result, code);
        compile.ast.Expression valueExp = value.solidityCodeGen(result, code);
        result.add(new compile.ast.Assign(targetExp, valueExp));
        return result;
    }

//    @Override
//    public String toSolCode() {
//        return CompileEnv.genAssign(target.toSolCode(), value.toSolCode());
//    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(value);
        return rtn;
    }

    @Override
    protected java.util.Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = target.readMap(code);
        result.putAll(value.readMap(code));
        return result;
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        return target.writeMap(code);
    }
}
