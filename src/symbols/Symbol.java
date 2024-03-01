package symbols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Symbol {
    private int col; // 如果是数组，要记录有几列
    private int paramId; // 如果是函数，记录这是第几个参数呱
    private String name; // 变量名
    private SymType symType; // 参数对应的类型 CONST, VAR 等
    private SymType returnType; // 函数为int或void，返回值类型
    private List<Integer> paramNum; // 参数的值
    private HashMap<String, SymType> param; // 函数的参数, 形参
    public HashMap<String, Integer> id; // 为了获取具体的参数类型，要把位置对应起来
    private ArrayList<SymType> parType;
    private int pos; // 记录在第几个符号表里面

    public Symbol(String n) {
        this.name = n;
        this.param = new HashMap<>();
        this.id = new HashMap<>();
        this.parType = new ArrayList<>();
        this.paramId = 0;
        this.pos = 0;
    }

    public void setCol(int c) {
        this.col = c;
    }

    public int getCol() {
        return col;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public SymType getTyoe() {
        return this.symType;
    }

    public void setReturnType(SymType t) {
        this.returnType = t;
    }

    public SymType getReturnType() {
        return this.returnType;
    }

    public void setType(SymType type) {
        this.symType = type;
    }

    public void addParam(String n, SymType t) {
        param.put(n, t);
        parType.add(t);
        id.put(n, this.paramId);
        this.paramId++;
    }

    public boolean findSameParam(String name) {
        if (this.param.isEmpty()) {
            return false;
        } else {
            if (this.param.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public Integer getparamSize() {
        return param.size();
    }

    public Integer getparamId(String n) { // 一定能找到
        return this.id.get(n);
    }

    public SymType getparamType(int i) {
        return parType.get(i);
    }
}
