package mips;

import Symbol_table.IntergerTable;
import Symbol_table.Symbols.ArraySymbol;
import Symbol_table.Symbols.NorSymbol;
import Symbol_table.Symbols.VarSymbol;
import generateMid.MidCode;
import generateMid.MidCodeOp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Mips {
    // 最终输出的mips代码
    private ArrayList<MipsCode> finalCodes = new ArrayList<>();
    private ArrayList<MidCode> midCodes;
    private LinkedList<String> strings;

    private int funcpointer = 0;
    private boolean inMain = false;
    private boolean inFunc = false;
    ArrayList<MidCode> pushOpstcak = new ArrayList<>(); // 调用函数时传入的实参
    private IntergerTable table = new IntergerTable();
    HashMap<String, Integer> funclength = new HashMap<>();
    HashMap<String, String> stringHashMap = new HashMap<>();

    public Mips(ArrayList<MidCode> midCodes, LinkedList<String> strings) {
        this.midCodes = midCodes;
        this.strings = strings;
        getLength();
        generatMips();
    }

    private void getLength() { // 获取全局变量的
        String name = null;
        int count = 0;
        int i;
        MidCode m;
        for (i = 0; i < midCodes.size(); i++) {
            m = midCodes.get(i);
            if (m.op == MidCodeOp.MAIN || m.op == MidCodeOp.INT ||m.op == MidCodeOp.VOID) {
                break;
            }
        }
        while (i < midCodes.size()) {
            m = midCodes.get(i);
            if (m.op == MidCodeOp.MAIN || m.op == MidCodeOp.INT ||m.op == MidCodeOp.VOID) {
                if (name != null) {
                    funclength.put(name, count);
                }
                name = m.result;
                count = 0;
            }
            if (m.op == MidCodeOp.ARRAY_1 || m.op == MidCodeOp.ARRAY_2 || m.op == MidCodeOp.CONSTARRAY_1 || m.op == MidCodeOp.CONSTARRAY_2) {
                int line = Integer.parseInt(m.arg1);
                if (m.op == MidCodeOp.ARRAY_2 || m.op == MidCodeOp.CONSTARRAY_2) {
                    int col = Integer.parseInt(m.arg2);
                    line = line * col;
                }
                count = count + line;
            }
            count = count + 2;
            i++;
        }
        funclength.put(name, count);
    }

    private boolean inGlobal(String n) {
        IntergerTable t = table;
        while (t != null) {
            if (t.maps.containsKey(n)) {
                if (t.out == null) {
                    return true;
                } else {
                    return false;
                }
            }
            t = t.out;
        }
        return false;
    }

    private int findOffset(String name) {
        IntergerTable t = table;

        while (t != null) {
            if (t.maps.containsKey(name)) {
                //System.out.println(name + " " + t.maps.get(name).offset);
                return t.maps.get(name).offset;
            }
            t = t.out;
        }
        return -1;
    }

    private void loadValue(String name, String reg, boolean newsymbol) {
        char temp = name.charAt(0);
        if (Character.isDigit(temp) || temp == '-') {
            MipsCode mips = new MipsCode(MipsOp.li, reg, "", "", Integer.parseInt(name));
            finalCodes.add(mips);
        } else {
            if (newsymbol == true) { // 是否为新加入符号表的参数
                loadSym(name);
            }
            // qqqqq
            boolean global = inGlobal(name);
            int offset = findOffset(name);
            if (global == true) { // 属于全局变量
                MipsCode mips = new MipsCode(MipsOp.lw, reg, "$gp", "", 4*offset);
                finalCodes.add(mips);
            } else {
                MipsCode mips = new MipsCode(MipsOp.lw, reg, "$fp", "", -4*offset);
                finalCodes.add(mips);
            }
        }
    }

    private void loadSym(String name) {  // 加入符号表
        if (table.maps.containsKey(name) == false) {
            VarSymbol var = new VarSymbol(name, funcpointer);
            table.maps.put(name, var);
            funcpointer++; // 记录当前符号表里参数个数？
        }
    }

    private void loadSym_a(String name) {  // 函数的形参
        if (table.maps.containsKey(name) == false) {
            ArraySymbol var = new ArraySymbol(name, funcpointer, true);
            table.maps.put(name, var);
            funcpointer++; // 记录当前符号表里参数个数,标记
        }
    }

    private void loadSym_a_n(String name, int l) {  // 需要获取数组
        if (table.out == null) {
            ArraySymbol var = new ArraySymbol(name, funcpointer);
            table.maps.put(name, var);
            funcpointer = funcpointer + l;
        } else {
            funcpointer = funcpointer + l-1;
            ArraySymbol var = new ArraySymbol(name, funcpointer);
            table.maps.put(name, var);
            funcpointer = funcpointer + 1;
        }

    }

    private void loadSym_v(String name) {
        if (table.maps.containsKey(name) == false) {
            VarSymbol var = new VarSymbol(name, funcpointer);
            table.maps.put(name, var);
            funcpointer += 1;
        }
    }

    private void storeValue(String name, String reg, boolean newsymbol) {
        if (newsymbol) {
            loadSym(name);
        }
        boolean global = inGlobal(name);
        int offset = findOffset(name);
        if (global) {
            MipsCode mips = new MipsCode(MipsOp.sw, reg, "$gp", "", 4*offset);
            finalCodes.add(mips);
        } else {
            MipsCode mips = new MipsCode(MipsOp.sw, reg, "$fp", "", -4*offset);
            finalCodes.add(mips);
        }
    }

    private boolean isT(String s) {
        if (s.length() >= 2) {
            if (s.charAt(1) == '&') {
                return true;
            }
        }
        return false;
    }

    private boolean isPointer(String name) {
        IntergerTable t = table;
        while (t != null) {
            if (t.maps.containsKey(name)) {
                return t.maps.get(name).ispointer;
            }
            t = t.out;
        }
        return false;
    }

    void loadAddress(String name, String reg) {
        IntergerTable t = table;
        char temp = name.charAt(0);
        if (Character.isDigit(temp) || temp == '-') {
            MipsCode mips = new MipsCode(MipsOp.li, reg, "", "", Integer.parseInt(name));
            finalCodes.add(mips);
            return;
        }
        while (t != null) {
            if (t.maps.containsKey(name)) {
                NorSymbol sym = t.maps.get(name);
                if (sym instanceof ArraySymbol) {
                    if (isPointer(name)) {
                        loadValue(name, reg, false);
                    } else {
                        if (inGlobal(name)) {
                            MipsCode mips = new MipsCode(MipsOp.addi, reg, "$gp", "", 4*sym.offset);
                            finalCodes.add(mips);
                        } else {
                            MipsCode mips = new MipsCode(MipsOp.addi, reg, "$fp", "", -4*sym.offset);
                            finalCodes.add(mips);
                        }
                    }
                } else {
                    loadValue(name, reg, false);
                }
                break;
            }
            t = t.out;
        }
    }

    private void generatMips() {
        finalCodes.add(new MipsCode(MipsOp.data, ""));
        for (int i = 0; i < strings.size(); i++) {
            finalCodes.add(new MipsCode(MipsOp.asciiz, "s_" +i, strings.get(i)));
            stringHashMap.put(strings.get(i), "s_" + i);
        }
        finalCodes.add(new MipsCode(MipsOp.text, ""));
        MidCode m;
        MipsCode mips;
        for (int i = 0; i < midCodes.size(); i++) {
            m = midCodes.get(i);
            if (m.op == MidCodeOp.ADD) { // +
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.add, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.MINU) {
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.sub, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.MULT) {
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.mult, "$t0", "$t1", "");
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.mflo, "$t2");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.DIV) {
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.div, "$t0", "$t1", "");
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.mflo, "$t2");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.MOD) {
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.div, "$t0", "$t1", "");
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.mfhi, "$t2");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.ASSIGNOP) {
                loadValue(m.arg1, "$t0", false);
                storeValue(m.result, "$t0", isT(m.result));
            } else if (m.op == MidCodeOp.PASS) { // 传递函数实参
                pushOpstcak.add(m);
            } else if (m.op == MidCodeOp.CALL) {
                for (int j = 0; j < pushOpstcak.size(); j++) {
                    MidCode mTemp = pushOpstcak.get(j);
                    if (mTemp.arg1 != null) { // 即传入函数参数为a[1]，但实际有a[][]
                        loadAddress(mTemp.result, "$t0");
                        loadValue(mTemp.arg1, "$t1", false);
                        mips = new MipsCode(MipsOp.li, "$t2", "", "", Integer.parseInt(mTemp.arg2) * 4);
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.mult, "$t2", "$t1", "");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.mflo, "$t2");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.add, "$t0", "$t0", "$t2");
                        finalCodes.add(mips);
                    } else {
                        loadAddress(mTemp.result, "$t0");
                    }
                    mips = new MipsCode(MipsOp.sw, "$t0", "$sp", "", -4*j);
                    finalCodes.add(mips);
                }
                pushOpstcak.clear();
                mips = new MipsCode(MipsOp.addi, "$sp", "$sp", "", -4*funclength.get(m.result)-8);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.sw, "$ra", "$sp", "", 4);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.sw, "$fp", "$sp", "", 8);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.addi, "$fp", "$sp", "", 4 * funclength.get(m.result) + 8);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.jal, m.result);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.lw, "$fp", "$sp", "", 8);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.lw, "$ra", "$sp", "", 4);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.addi, "$sp", "$sp", "", 4 * funclength.get(m.result) + 8);
                finalCodes.add(mips);
            } else if (m.op == MidCodeOp.RETURN) {
                if (inMain) {
                    mips = new MipsCode(MipsOp.li, "$v0", "", "", 10);
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.syscall, "");
                    finalCodes.add(mips);
                } else {
                    if (m.result != null) {
                        loadValue(m.result, "$v0", false);
                    }
                    mips = new MipsCode(MipsOp.jr, "$ra");
                    finalCodes.add(mips);
                }
            } else if (m.op == MidCodeOp.ASSIGN) { // 有返回值的函数赋值给&t
                storeValue(m.result, "$v0", isT(m.result));
            } else if (m.op == MidCodeOp.PRINT) {
                if (m.arg1 != null) {
                    String addr = stringHashMap.get(m.result);
                    mips = new MipsCode(MipsOp.la, "$a0", addr);
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.li, "$v0", "", "", 4);
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.syscall, "");
                    finalCodes.add(mips);
                } else {
                    loadValue(m.result, "$a0", false);
                    mips = new MipsCode(MipsOp.li, "$v0", "", "", 1);
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.syscall, "");
                    finalCodes.add(mips);
                }
            } else if (m.op == MidCodeOp.SCAN) {
                mips = new MipsCode(MipsOp.li, "$v0", "", "", 5);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.syscall, "");
                finalCodes.add(mips);
                storeValue(m.result, "$v0", isT(m.result));
            } else if (m.op == MidCodeOp.BLOCK) { // 进入不同的块
                if (m.arg1.equals("start")) {
                    table = new IntergerTable(table);
                } else if (m.arg1.equals("end")) {
                    funcpointer = funcpointer - table.contentlength;
                    table = table.out;
                }
            } else if (m.op == MidCodeOp.INT || m.op == MidCodeOp.VOID) {
                if (inFunc == false) {
                    mips = new MipsCode(MipsOp.j, "main");
                    finalCodes.add(mips);
                    inFunc = true;
                }
                mips = new MipsCode(MipsOp.label, m.result);
                finalCodes.add(mips);
                funcpointer = 0;
            } else if (m.op == MidCodeOp.PARA) { // 函数参数
                if (m.arg1.equals("0")) { // 普通变量
                    loadSym(m.result);
                } else {
                    loadSym_a(m.result);
                }
            } else if (m.op == MidCodeOp.GETARRAY) {
                loadValue(m.arg2, "$t0", false);
                mips = new MipsCode(MipsOp.sll, "$t0", "$t0", "", 2);
                finalCodes.add(mips);
                if (isPointer(m.arg1)) {
                    loadValue(m.arg1, "$t1", false);
                    mips = new MipsCode(MipsOp.add, "$t1", "$t1", "$t0");
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.lw, "$t2", "$t1", "", 0);
                    finalCodes.add(mips);
                } else {
                    if (inGlobal(m.arg1)) {
                        mips = new MipsCode(MipsOp.add, "$t1", "$t0", "$gp");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.lw, "$t2", "$t1", "", 4 * findOffset(m.arg1));
                        finalCodes.add(mips);
                    } else {
                        mips = new MipsCode(MipsOp.add, "$t1", "$t0", "$fp");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.lw, "$t2", "$t1", "", -4 * findOffset(m.arg1));
                        finalCodes.add(mips);
                    }
                }
                storeValue(m.result, "$t2", isT(m.result));
            } else if (m.op == MidCodeOp.PUTARRAY) { // 定义数组时赋值
                loadValue(m.arg2, "$t0", false); // 要赋的值
                loadValue(m.arg1, "$t1", false);
                mips = new MipsCode(MipsOp.sll, "$t1", "$t1", "", 2);
                finalCodes.add(mips);
                if (isPointer(m.result)) {
                    loadValue(m.result, "$t2", false);
                    mips = new MipsCode(MipsOp.add, "$t2", "$t2", "$t1");
                    finalCodes.add(mips);
                    mips = new MipsCode(MipsOp.sw, "$t0", "$t2", "", 0);
                    finalCodes.add(mips);
                } else {
                    if (inGlobal(m.result)) {
                        mips = new MipsCode(MipsOp.add, "$t1", "$t1", "$gp");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.sw, "$t0", "$t1", "", 4*findOffset(m.result));
                        finalCodes.add(mips);
                    } else {
                        mips = new MipsCode(MipsOp.addu, "$t1", "$t1", "$fp");
                        finalCodes.add(mips);
                        mips = new MipsCode(MipsOp.sw, "$t0", "$t1", "", -4*findOffset(m.result));      // qqqqq
                        finalCodes.add(mips);
                    }
                }
            } else if (m.op == MidCodeOp.CONST) { // const 常量
                loadValue(m.arg1, "$t0", false);
                storeValue(m.result, "$t0", true);
            } else if (m.op == MidCodeOp.VAR) { // var 赋值与不赋值
                if (m.arg1 != null) { // 赋值
                    loadValue(m.arg1, "$t0", false);
                    storeValue(m.result, "$t0", true);
                } else {
                    loadSym_v(m.result);
                }
            } else if (m.op == MidCodeOp.ARRAY_1 || m.op == MidCodeOp.ARRAY_2 || m.op == MidCodeOp.CONSTARRAY_1 || m.op == MidCodeOp.CONSTARRAY_2) {
                int k = Integer.parseInt(m.arg1);
                if (m.op == MidCodeOp.ARRAY_2 || m.op == MidCodeOp.CONSTARRAY_2) {
                    int l = Integer.parseInt(m.arg2);
                    k = k * l;
                }
                loadSym_a_n(m.result, k);
            } else if (m.op == MidCodeOp.MAIN) {
                if (inFunc == false) {
                    mips = new MipsCode(MipsOp.j, "main");
                    finalCodes.add(mips);
                    inFunc = true;
                }
                inMain = true;
                mips = new MipsCode(MipsOp.label, m.result);
                finalCodes.add(mips);
                funcpointer = 0;
                int len = funclength.get("main");
                mips = new MipsCode(MipsOp.moveop, "$fp", "$sp");
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.addi, "$sp", "$sp", "", -4*len - 8);
                finalCodes.add(mips);
            } else if (m.op == MidCodeOp.LSS) { // <
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.slt, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.GRE) { // >
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.sgt, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.LEQ) { // <=
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.sle, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.GEQ) { // >=
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.sge, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.EQLOP) { // ==
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.seq, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.NEQOP) {
                loadValue(m.arg1, "$t0", false);
                loadValue(m.arg2, "$t1", false);
                mips = new MipsCode(MipsOp.sne, "$t2", "$t0", "$t1");
                finalCodes.add(mips);
                storeValue(m.result, "$t2", true);
            } else if (m.op == MidCodeOp.BZ) {
                loadValue(m.result, "$t0", false);
                mips = new MipsCode(MipsOp.li, "$t1", "", "", 0);
                finalCodes.add(mips);
                mips = new MipsCode(MipsOp.beq, "Jump"+m.arg1, "$t0", "$t1");
                finalCodes.add(mips);
            } else if (m.op == MidCodeOp.JUMP) {
                if (m.arg1 == null) {
                    mips = new MipsCode(MipsOp.label, "Jump"+m.result);
                    finalCodes.add(mips);
                } else {
                    mips = new MipsCode(MipsOp.label, "Loop"+m.result+m.arg2);
                    finalCodes.add(mips);
                }
            } else if (m.op == MidCodeOp.GOTO) {
                if (m.arg1 == null) {
                    mips = new MipsCode(MipsOp.j, "Jump"+m.result);
                    finalCodes.add(mips);
                } else {
                    mips = new MipsCode(MipsOp.j, "Loop"+m.result+m.arg2);
                    finalCodes.add(mips);
                }
            }
        }
    }

    public void printCodes(BufferedWriter w) throws IOException {
        for (int i = 0; i < finalCodes.size(); i++) {
            MipsCode m = finalCodes.get(i);
            switch (m.op) {
                case sne:
                    w.write("sne " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case sgt:
                    w.write("sgt " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case seq:
                    w.write("seq " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case sge:
                    w.write("sge " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case sle:
                    w.write("sle " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case slt:
                    w.write("slt " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case beq:
                    w.write("beq " + m.a1 + ", " + m.a2 + ", " + m.r + "\n");
                    break;
                case add:
                    w.write("add " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case addu:
                    w.write("addu " + m.r + ", " + m.a1 + ", " + m.a2 +  "\n");
                    break;
                case sub:
                    w.write("sub " + m.r + ", " + m.a1 + ", " + m.a2 + "\n");
                    break;
                case mult:
                    w.write("mult " + m.r + ", " + m.a1 + "\n");
                    break;
                case div:
                    w.write("div " + m.r + ", " + m.a1 + "\n");
                    break;
                case addi:
                    w.write("addi " + m.r + ", " + m.a1 + ", " + m.num +  "\n");
                    break;
                case mflo:
                    w.write("mflo " + m.r + "\n");
                    break;
                case mfhi:
                    w.write("mfhi " + m.r + "\n");
                    break;
                case j:
                    w.write("j " + m.r + "\n");
                    break;
                case jr:
                    w.write("jr " + m.r + "\n");
                    break;
                case jal:
                    w.write("jal " + m.r + "\n");
                    break;
                case lw:
                    w.write("lw " + m.r + ", " + m.num + "(" + m.a1 + ")\n");
                    break;
                case sw:
                    w.write("sw " + m.r + ", " + m.num + "(" + m.a1 + ")\n");
                    break;
                case syscall:
                    w.write("syscall\n");
                    break;
                case moveop:
                    w.write("move " + m.r + ", " + m.a1 + "\n");
                    break;
                case li:
                    w.write("li " + m.r + ", " + m.num + "\n");
                    break;
                case la:
                    w.write("la " + m.r + ", " + m.a1 + "\n");
                    break;
                case data:
                    w.write(".data\n");
                    break;
                case text:
                    w.write("\n.text\n");
                    break;
                case asciiz:
                    w.write(m.r + ": .asciiz \"" + m.a1 + "\"\n");
                    break;
                case label:
                    w.write("\n" + m.r + ":\n");
                    break;
                case sll:
                    w.write("sll " + m.r + ", " + m.a1 + ", " + m.num + "\n");
                default:
                    break;
            }
        }
    }
}
