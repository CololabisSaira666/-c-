package generateMid;

import java.util.HashMap;

public class FuncTable {
    String funcName;
    int type; // 0 为void, i 为int
    HashMap<Integer, VarTable> funcSym;
    VarTable globalVar;

    public FuncTable(String n, int id, VarTable globalVar) {
        this.funcName = n;
        this.funcSym = new HashMap<>();
        this.globalVar = globalVar;
        VarTable vt = new VarTable(0, id);
        this.funcSym.put(id, vt);
    }

    public NewSym getS(String n, int id) {
        if (funcSym.get(id).haveSym(n)) {
            return funcSym.get(id).getSym(n);
        } else {
            int pre = funcSym.get(id).preId;
            if (pre == 0) {
                return globalVar.getSym(n);
            } else {
                return getS(n, pre);
            }
        }
    }

    public void addTable(int preid, int id) {
        VarTable temp = new VarTable(preid, id);
        this.funcSym.put(id, temp);
    }

    public void addFuncV(String n, int id, NewSym sym) {
        VarTable vTemp = this.funcSym.get(id);
        vTemp.addVarTable(n, sym);
        this.funcSym.put(id, vTemp);
    }
}
