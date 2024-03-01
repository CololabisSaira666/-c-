package symbols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    private HashMap<String, Symbol> symTable;
    private SymbolTable pre;
    private ArrayList<SymbolTable> nexts;
    private int nextNum; // 初始化，该符号表中nexts的值

    public SymbolTable() {
        this.symTable = new HashMap<>();
        this.nexts = new ArrayList<>();
        this.nextNum = 0;
        this.pre = null;
    }

    public void setPre(SymbolTable p) {
        this.pre = p;
    }

    public void addSym(String n, Symbol s) {
        symTable.put(n, s);
    }

    public Symbol getFuncDef(String n) {
        return symTable.get(n);
    }


    public Symbol getSym (String n) { //这是确保一定能找到的时候查找
        if (symTable.containsKey(n)){
            return symTable.get(n);
        } else {
            return pre.getSym(n);
        }
    }

    public SymbolTable getPreTable() {
        return pre;
    }

    public boolean useSym(String n) { // 在使用符号时，查看是否被定义
        boolean ans;
        if (this.symTable.isEmpty() && pre == null) {
            ans =  false;
        } else {
            if (this.symTable.containsKey(n)) {
                ans = true;
            } else if (pre != null) {
                ans = pre.useSym(n);
            } else {
                ans = false;
            }
        }
        return ans;
    }

    public SymbolTable createNext(SymbolTable table) {
        this.nextNum++;
        nexts.add(table);
        return nexts.get(nextNum-1);
    }

    public boolean findConstSymbol(String n) {
        boolean ans = false;
        if (this.symTable.containsKey(n)) {
            Symbol s = symTable.get(n);
            SymType t = s.getTyoe();
            if (t == SymType.CONST || t == SymType.CONSTARRAY_1 || t == SymType.CONSTARRAY_2) {
                ans = true;
            }
            else {
                ans = false;
            }
        } else {
            ans = pre.findConstSymbol(n);
        }
        return ans;
    }

    public boolean findSameSymbol(String name) { // 仅查找本层的符号表
        if (this.symTable.isEmpty()) {
            return false;
        } else {
            if (this.symTable.containsKey(name)) {
                return true;
            }
        }
        return false;
    }
}
