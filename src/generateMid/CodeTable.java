package generateMid;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeTable {
    HashMap<Integer, VarTable> vars;
    HashMap<String, FuncTable> funcs;

    public CodeTable() {
        this.vars = new HashMap<>();
        this.funcs = new HashMap<>();
    }

    public int getFuncType(String n) {
        FuncTable f = this.funcs.get(n);
        return f.type;
    }

    public int getType(String n, int curId) {
        //System.out.println(this.vars.get(0).table.keySet());
        if (this.vars.get(curId).haveSym(n)) {
            NewSym s = this.vars.get(curId).getSym(n);
            return s.type;
        } else {
            int pre = this.vars.get(curId).preId;
            return getType(n, pre);
        }
    }

    public int getType(String fname, String n, int curId) {
        NewSym s = this.funcs.get(fname).getS(n, curId);
        return s.type;
    }

    public String getCol(String n, int curId) {
        if (this.vars.get(curId).haveSym(n)) {
            NewSym s = this.vars.get(curId).getSym(n);
            return String.valueOf(s.col);
        } else {
            int pre = this.vars.get(curId).preId;
            return getCol(n, pre);
        }
    }

    public String getCol(String fname, String n, int curId) {
        NewSym s = this.funcs.get(fname).getS(n, curId);
        return String.valueOf(s.col);
    }

    public void createVar(int preid, int id) {
        VarTable temp = new VarTable(preid, id);
        this.vars.put(id, temp);
    }

    public void createFunc(String n, int preid, int id) { // 定义函数中新增，block
        FuncTable f = this.funcs.get(n);
        f.addTable(preid, id);
        this.funcs.put(n, f);
    }

    public void addFuncDef(String n, FuncTable f) { // 加入新定义的函数
        this.funcs.put(n, f);
    }

    public void addVar(int id, String n, NewSym sym) {
        VarTable temp = vars.get(id);
        temp.table.put(n, sym);
        vars.put(id, temp);
        //vars.put(id, vars.get(id));
        //VarTable temp = this.vars.get(id);
        //temp.addVarTable(n, sym);
        //this.vars.put(id, temp);
    }

    public void addVar(String funncName, int id, String n, NewSym sym) {
        FuncTable temp = this.funcs.get(funncName);
        temp.addFuncV(n, id, sym);
        funcs.replace(funncName, temp);
        //this.funcs.put(funncName, temp);
    }
}
