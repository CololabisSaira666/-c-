package generateMid;

import lexer.Tag;
import lexer.Token;
import symbols.SymType;
import symbols.Symbol;
import symbols.SymbolTable;
import tree.Node;
import tree.NodeType;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

import static generateMid.MidCodeOp.*;

public class GenerateMid {
    private CodeTable table = new CodeTable();
    public LinkedList<String> strings;
    public ArrayList<MidCode> codes; // 输出的中间代码
    private Node tree; // 输入的语法树
    private int pos; // 关于分配的寄存器的数量？或者说第多少个临时变量？
    private int preTableId = 0;
    private int tableId = 0; // 处在第几层符号表
    private Stack<Integer> id = new Stack<>(); // 存储符号表
    private boolean defFunc = false; // 是否在定义函数
    private String FuncName; // 当前操作函数名
    private int level = 0; // 现在处在第几个块中？
    private int jumpNum = 1; // 关于jump跳转
    private int loopNum = 1; // 关于for循环
    private Stack<Integer> breakNum = new Stack<>();
    private Stack<Integer> continueNum = new Stack<>();
    private boolean inConstExp = false; // 用来给constExp赋值的
    private boolean inFuncRParams = false;

    public GenerateMid (Node t) {
        this.tree = t;
        this.pos = 1; // 这里应该是读入的有效节点的个数？
        this.codes = new ArrayList<>();
        this.strings = new LinkedList<>();
    }

    public void GenerateRun() {
        List<Node> n = tree.getchildNodes();
        NodeType type;
        table.createVar(-1, this.tableId);
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Decl) {
                Decl(i);
            } else if (type == NodeType.FuncDef) {
                defFunc = true;
                FuncDef(i);
                defFunc = false;
            } else if (type == NodeType.MainFuncDef) {
                this.tableId++;
                this.table.createVar(0, this.tableId);
                MainFuncDef(i);
            }
        }
    }

    private void FuncDef(Node t) {
        List<Node> n = t.getchildNodes();
        String blocknum = String.valueOf(this.level);
        this.level++;
        MidCode funcS = new MidCode(BLOCK, blocknum, "start");
        codes.add(funcS);
        String funcName = n.get(1).getToken().getStr();
        FuncName = funcName;
        this.tableId++;

        FuncTable ftable = new FuncTable(funcName, this.tableId, table.vars.get(0));

        this.table.addFuncDef(this.FuncName, ftable);
        if (n.get(0).getchildNodes().get(0).getToken().getStr().equals("int")) {
            MidCode f = new MidCode(INT, funcName);
            codes.add(f);
            ftable.type = 1;
        } else {
            MidCode f = new MidCode(VOID, funcName);
            codes.add(f);
            ftable.type = 0;
        }
        if (n.size() == 6) { // 函数存在参数
            FuncFParams(n.get(3));
        }
        // 这里应该进block了，但是为了区别一下
        List<Node> temp = n.get(n.size()-1).getchildNodes();
        NodeType type;
        for (Node i : temp) {
            type = i.getType();
            if (type == NodeType.BlockItem) {
                BlockItem(i);
            }
        }
        codes.add(new MidCode(RETURN, null));

        MidCode funcE = new MidCode(BLOCK, blocknum, "end");
        codes.add(funcE);
    }

    private void BlockItem(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Decl) {
                Decl(i);
            } else if (type == NodeType.Stmt) {
                Stmt(i);
            }
        }
    }

    private void Cond(Node t) {
        LOrExp(t.getchildNodes().get(0), jumpNum-1);
    }

    private void LOrExp(Node t, int jumpPos) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.LAndExp) {
                LAndExp(i, jumpNum);
            } else { // 是 ||
                MidCode goto_3 = new MidCode(GOTO, String.valueOf(jumpPos));
                codes.add(goto_3);
                MidCode jump = new MidCode(JUMP, String.valueOf(jumpNum));
                jumpNum++;
                codes.add(jump);
            }
        }
        //if (n.size() > 1) {
        MidCode goto_3 = new MidCode(GOTO, String.valueOf(jumpPos));
        codes.add(goto_3);
        MidCode jump = new MidCode(JUMP, String.valueOf(jumpNum));
        jumpNum++;
        codes.add(jump);
        //}
    }

    private void LAndExp(Node t, int jumpPos) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.EqExp) {
                EqExp(i, jumpPos);
            }
        }
    }

    private void EqExp(Node t, int jumpPos) {
        List<Node> n = t.getchildNodes();
        if (n.size() == 1) {
            String v =  RelExp(n.get(0));
            MidCode bz = new MidCode(BZ, v, String.valueOf(jumpPos));
            codes.add(bz);
        } else {
            int len = (n.size() - 1) / 2;
            String v0 = "t&" + pos;
            pos = pos + len;
            int temp = pos-1;
            String L = "t&" + temp;
            String L1 = RelExp(n.get(0));
            String L2;
            for (int i = 1; i < n.size(); i = i+2) {
                L2 = RelExp(n.get(i+1));
                if (n.get(i).getToken().type == Tag.EQL) { // ==
                    MidCode eql = new MidCode(EQLOP, L, L1, L2);
                    codes.add(eql);
                } else {
                    MidCode neq = new MidCode(NEQOP, L, L1, L2);
                    codes.add(neq);
                }
                L1 = L;
                temp--;
                L = "t&" + temp;
            }
            MidCode bz = new MidCode(BZ, v0, String.valueOf(jumpPos));
            codes.add(bz);
        }
    }

    private String RelExp(Node t) {
        List<Node> n = t.getchildNodes();
        String v;
        if (n.size() == 1) {
            v = AddExp(n.get(0));
        } else {
            int len = (n.size() - 1) / 2;
            v = "t&" + pos;
            pos = pos + len;
            int temp = pos-1;
            String L = "t&" + temp;
            String L1 = AddExp(n.get(0));
            String L2;
            for (int i = 1; i < n.size(); i = i+2) {
                L2 = AddExp(n.get(i+1));
                if (n.get(i).getToken().type == Tag.LSS) { // <
                    MidCode lss = new MidCode(MidCodeOp.LSS, L, L1, L2);
                    codes.add(lss);
                } else if (n.get(i).getToken().type == Tag.LEQ){ // <=
                    MidCode leq = new MidCode(LEQ, L, L1, L2);
                    codes.add(leq);
                } else if (n.get(i).getToken().type == Tag.GRE) { // >
                    MidCode gre = new MidCode(GRE, L, L1, L2);
                    codes.add(gre);
                } else {
                    MidCode geq = new MidCode(GEQ, L, L1, L2);
                    codes.add(geq);
                }
                L1 = L;
                temp--;
                L = "t&" + temp;
            }
        }
        return v;
    }

    private void ForStmt(Node t) {
        List<Node> n = t.getchildNodes();
        if (n.get(0).getchildNodes().size() == 1) {
            String lval = LVal(n.get(0), true);
            String exp = Exp(n.get(2));
            // String name = n.get(0).getchildNodes().get(0).getToken().getStr();
            MidCode l = new MidCode(ASSIGNOP, lval, exp);
            codes.add(l);
        } else {
            String lval = LVal(n.get(0), false);
            String exp = Exp(n.get(2));
            String name = n.get(0).getchildNodes().get(0).getToken().getStr();
            MidCode L = new MidCode(PUTARRAY, name, lval, exp);
            codes.add(L);
        }
    }

    private void Stmt(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        Token token = n.get(0).getToken();
        type = n.get(0).getType();
        if (token != null) {
            if (token.type == Tag.RETURNTK) {
                if (n.size() == 3) { // 有EXP
                    MidCode re = new MidCode(RETURN, Exp(n.get(1)));
                    codes.add(re);
                } else {
                    MidCode re = new MidCode(RETURN, null);
                    codes.add(re);
                }
            } else if (token.type == Tag.PRINTFTK) {
                String s = n.get(2).getchildNodes().get(0).getToken().getStr();
                if (n.size() == 5) { // 后无exp
                    if (s.length() > 2) {
                        this.strings.add(s.substring(1, s.length()-1));
                        MidCode print = new MidCode(PRINT, s.substring(1, s.length()-1), "strings");
                        codes.add(print);
                    }
                } else {
                    int i = 0, j = 1;
                    while(s.indexOf("%d",j)>=0){
                        if (j != s.indexOf("%d", j)) {
                            String temp = s.substring(j, s.indexOf("%d", j));
                            this.strings.add(temp);
                            MidCode print1 = new MidCode(PRINT, temp, "strings");
                            codes.add(print1);
                        }
                        j = s.indexOf("%d", j) + "%d".length();
                        String d = Exp(n.get(4+i));
                        // String v = "t&" + pos;
                        // pos++;
                        // MidCode m = new MidCode(ASSIGNOP, v, d);
                        // codes.add(m);
                        MidCode print2 = new MidCode(PRINT, d);
                        codes.add(print2);
                        i = i+2;
                    }
                    if (j != s.length() -1) {
                        String temp = s.substring(j, s.length() -1);
                        this.strings.add(temp);
                        MidCode print1 = new MidCode(PRINT, temp,"strings");
                        codes.add(print1);
                    }
                }
            } else if (token.type == Tag.IFTK) {
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
                if (n.size() == 7) { // 有else
                    int[] arr={jumpNum, jumpNum+1, jumpNum+2};
                    jumpNum = jumpNum + 3;
                    Cond(n.get(2));
                    MidCode goto_1 = new MidCode(GOTO, String.valueOf(arr[0]));
                    codes.add(goto_1);
                    MidCode jump_3 = new MidCode(JUMP, String.valueOf(arr[2]));
                    codes.add(jump_3);
                    Stmt(n.get(4));
                    MidCode goto_2 = new MidCode(GOTO, String.valueOf(arr[1]));
                    codes.add(goto_2);
                    MidCode jump_1 = new MidCode(JUMP, String.valueOf(arr[0]));
                    codes.add(jump_1);
                    Stmt(n.get(6));
                    MidCode jump_2 = new MidCode(JUMP, String.valueOf(arr[1]));
                    codes.add(jump_2);
                } else { // 无else
                    int[] arr={jumpNum, jumpNum+1};
                    jumpNum = jumpNum + 2;
                    Cond(n.get(2));
                    MidCode goto_1 = new MidCode(GOTO, String.valueOf(arr[0]));
                    codes.add(goto_1);
                    MidCode jump_2 = new MidCode(JUMP, String.valueOf(arr[1]));
                    codes.add(jump_2);
                    Stmt(n.get(4));
                    MidCode jump_1 = new MidCode(JUMP, String.valueOf(arr[0]));
                    codes.add(jump_1);
                }
            } else if (token.type == Tag.FORTK) {
                int[] arr={jumpNum, jumpNum+1, jumpNum+2};
                jumpNum = jumpNum + 3;

                continueNum.add(arr[1]);
                int loop = loopNum;
                loopNum++;
                if (n.get(2).getType() == NodeType.ForStmt) {
                    ForStmt(n.get(2));
                }
                MidCode loop_s = new MidCode(JUMP, String.valueOf(loop), "loop", "start");
                codes.add(loop_s);
                boolean flag = false;
                for (Node i : n) {
                    if (i.getType() == NodeType.Cond) {
                        flag = true;
                        Cond(i);
                    }
                }
                if (flag == false) { // 不存在cond
                    MidCode goto_3 = new MidCode(GOTO, String.valueOf(arr[2]));
                    codes.add(goto_3);
                    MidCode jump_4 = new MidCode(JUMP, String.valueOf(jumpNum));
                    codes.add(jump_4);
                    jumpNum++;
                }
                breakNum.add(jumpNum-1);
                MidCode goto_loop_e = new MidCode(GOTO, String.valueOf(loop), "loop", "end");
                codes.add(goto_loop_e);
                MidCode jump_3 = new MidCode(JUMP, String.valueOf(arr[2]));
                codes.add(jump_3);
                Stmt(n.get(n.size()-1));
                MidCode jump_2 = new MidCode(JUMP, String.valueOf(arr[1]));
                codes.add(jump_2);
                if (n.get(n.size() - 3).getType() == NodeType.ForStmt) {
                    ForStmt(n.get(n.size() - 3));
                }
                MidCode goto_loop_s = new MidCode(GOTO, String.valueOf(loop), "loop", "start");
                codes.add(goto_loop_s);
                MidCode loop_e = new MidCode(JUMP, String.valueOf(loop), "loop", "end");
                codes.add(loop_e);
                breakNum.pop();
                continueNum.pop();
            } else if (token.type == Tag.BREAKTK) {
                MidCode jump = new MidCode(GOTO, String.valueOf(breakNum.peek()));
                codes.add(jump);
            } else if (token.type == Tag.CONTINUETK) {
                MidCode jump = new MidCode(GOTO, String.valueOf(continueNum.peek()));
                codes.add(jump);
            }
        } else if (type == NodeType.LVal) {
            if (n.size() == 4) { // lval = exp
                if (n.get(0).getchildNodes().size() == 1) {
                    String lval = LVal(n.get(0), true);
                    String exp = Exp(n.get(2));
                    // String name = n.get(0).getchildNodes().get(0).getToken().getStr();
                    MidCode l = new MidCode(ASSIGNOP, lval, exp);
                    codes.add(l);
                } else {
                    String lval = LVal(n.get(0), false);
                    String exp = Exp(n.get(2));
                    String name = n.get(0).getchildNodes().get(0).getToken().getStr();
                    MidCode L = new MidCode(PUTARRAY, name, lval, exp);
                    codes.add(L);
                }
            } else {
                if (n.get(0).getchildNodes().size() == 1) {
                    String lval = LVal(n.get(0), false);
                    MidCode l = new MidCode(SCAN, lval);
                    codes.add(l);
                } else {
                    String v = "t&" + this.pos;
                    this.pos++;
                    MidCode l = new MidCode(SCAN, v);
                    codes.add(l);
                    String lval = LVal(n.get(0), false);
                    String name = n.get(0).getchildNodes().get(0).getToken().getStr();
                    MidCode L = new MidCode(PUTARRAY, name, lval, v);
                    codes.add(L);
                }
            }
        } else if (type == NodeType.Block) {
            Block(n.get(0));
        } else if (type == NodeType.Exp) {
            Exp(n.get(0));
        }
    }

    private void Block(Node t) {
        if (this.id.empty()) {
            preTableId = this.tableId;
            this.tableId++;
            id.push(this.tableId);
        } else {
            preTableId = this.id.peek();
            this.tableId++;
            id.push(this.tableId-1);
        }
        if (defFunc == true) {
            this.table.createFunc(FuncName, preTableId, tableId);
        } else {
            this.table.createVar(preTableId, tableId);
        }
        List<Node> n = t.getchildNodes();
        NodeType type;
        String blocknum = String.valueOf(this.level);
        this.level++;
        MidCode funcS = new MidCode(BLOCK, blocknum, "start");
        codes.add(funcS);
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.BlockItem) {
                BlockItem(i);
            }
        }
        MidCode funcE = new MidCode(BLOCK, blocknum, "end");
        codes.add(funcE);
        this.id.pop();
    }

    private void FuncFParams(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.FuncFParam) {
                FuncFParam(i);
            }
        }
    }

    private void FuncFParam(Node t) {
        List<Node> n = t.getchildNodes();
        String name = n.get(1).getToken().getStr();
        int len = n.size();
        if (len == 2) { // 普通变量
            NewSym sym = new NewSym(name, 0);
            this.table.addVar(this.FuncName, tableId, name, sym);
            MidCode para = new MidCode(PARA, name, "0");
            codes.add(para);
        } else if (len == 4) {
            NewSym sym = new NewSym(name, 1);
            this.table.addVar(this.FuncName, tableId, name, sym);
            MidCode para = new MidCode(PARA, name, "1");
            codes.add(para);
        } else {
            String col = ConstExp(n.get(5));
            NewSym sym = new NewSym(name, 2, Integer.parseInt(col));
            this.table.addVar(this.FuncName, tableId, name, sym);
            MidCode para = new MidCode(PARA, name, "2", col);
            codes.add(para);
        }
    }

    private void MainFuncDef(Node t) {
        List<Node> n = t.getchildNodes();
        String blocknum = String.valueOf(this.level);
        this.level++;
        MidCode mainS = new MidCode(BLOCK, blocknum, "start");
        codes.add(mainS);
        MidCode main = new MidCode(MAIN, "main");
        codes.add(main);
        List<Node> temp = n.get(n.size()-1).getchildNodes();
        NodeType type;
        for (Node i : temp) {
            type = i.getType();
            if (type == NodeType.BlockItem) {
                BlockItem(i);
            }
        }
        codes.add(new MidCode(RETURN, null));
        MidCode mainE = new MidCode(BLOCK, blocknum, "end");
        codes.add(mainE);
        MidCode F = new MidCode(FINISH);
        codes.add(F);
    }

    private void Decl(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node  i : n) {
            type = i.getType();
            if (type == NodeType.ConstDecl) {
                ConstDecl(i);
            } else if (type == NodeType.VarDecl) {
                VarDecl(i);
            }
        }
    }

    private void VarDecl(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.VarDef) {
                VarDef(i);
            }
        }
    }

    private void ConstDecl(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.ConstDef) {
                ConstDef(i);
            }
        }
    }

    private void VarDef(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        String name = n.get(0).getToken().getStr(); // 当前的变量名称
        int len = n.size(); // 长度，之后判断是普通变量还是数组，呱
        if (len == 1) { // 普通不赋值
            NewSym sym = new NewSym(name, 0);
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            //MidCode mid = new MidCode(VAR, name);
            //codes.add(mid);
            MidCode mid = new MidCode(VAR, name, "0");
            codes.add(mid);
        } else if (len == 3) {// 普通赋值
            NewSym sym = new NewSym(name, 0);
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            ArrayList<String> value = InitVal(n.get(2));
            MidCode mid = new MidCode(VAR, name, value.get(0).toString());
            codes.add(mid);
        } else if (len == 4) { // 一维不赋值
            NewSym sym = new NewSym(name, 1);
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            MidCode mid = new MidCode(ARRAY_1, name, ConstExp(n.get(2)));
            codes.add(mid);
        } else if (len == 6) { // 一维赋值
            NewSym sym = new NewSym(name, 1);
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            MidCode mid = new MidCode(ARRAY_1, name, ConstExp(n.get(2)));
            codes.add(mid);
            ArrayList<String> value = InitVal(n.get(5));
            for (int num = 0; num < value.size(); num++) {
                MidCode temp = new MidCode(PUTARRAY, name, String.valueOf(num) ,value.get(num));
                codes.add(temp);
            }
        } else if (len == 7) { // 二维不赋值
            String lineNum = ConstExp(n.get(2));
            String col = ConstExp(n.get(5));
            NewSym sym = new NewSym(name, 2, Integer.parseInt(col));
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            MidCode mid = new MidCode(ARRAY_2, name, lineNum, col);
            codes.add(mid);
        } else { // 二维赋值
            String lineNum = ConstExp(n.get(2));
            String col = ConstExp(n.get(5));
            NewSym sym = new NewSym(name, 2, Integer.parseInt(col));
            if (this.defFunc == true) {
                table.addVar(this.FuncName, this.tableId, name, sym);
            } else {
                table.addVar(this.tableId, name, sym);
            }
            MidCode mid = new MidCode(ARRAY_2, name, lineNum, col);
            codes.add(mid);
            ArrayList<String> value = InitVal(n.get(8));
            for (int num = 0; num < value.size(); num++) {
                MidCode temp = new MidCode(PUTARRAY, name, String.valueOf(num) ,value.get(num));
                codes.add(temp);
            }
        }
    }

    private void ConstDef(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        String name = n.get(0).getToken().getStr(); // 当前的变量名称
        int len = n.size(); // 长度，之后判断是普通变量还是数组，呱
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.ConstInitVal) {
                ArrayList<String> value = ConstInitVal(i);
                if (len == 3) { // 普通变量
                    NewSym sym = new NewSym(name, 0);
                    sym.value = Integer.parseInt(value.get(0));
                    if (this.defFunc == true) {
                        table.addVar(FuncName, tableId, name, sym);
                    } else {
                        table.addVar(tableId, name, sym);
                    }
                    MidCode mid = new MidCode(CONST, name, value.get(0).toString());
                    codes.add(mid);
                } else if (len == 6) { // 一维数组
                    NewSym sym = new NewSym(name, 1);
                    if (this.defFunc == true) {
                        table.addVar(FuncName, tableId, name, sym);
                    } else {
                        table.addVar(tableId, name, sym);
                    }
                    MidCode mid = new MidCode(CONSTARRAY_1, name, ConstExp(n.get(2)));
                    codes.add(mid);
                    for (int num = 0; num < value.size(); num++) {
                        MidCode temp = new MidCode(PUTARRAY, name, String.valueOf(num) ,value.get(num));
                        codes.add(temp);
                    }
                } else if (len == 9) {
                    String lineNum = ConstExp(n.get(2));
                    String col = ConstExp(n.get(5));
                    NewSym sym = new NewSym(name, 2, Integer.parseInt(col));
                    if (this.defFunc == true) {
                        table.addVar(this.FuncName, this.tableId, name, sym);
                    } else {
                        table.addVar(this.tableId, name, sym);
                    }
                    MidCode mid = new MidCode(CONSTARRAY_2, name, lineNum, col);
                    codes.add(mid);
                    for (int num = 0; num < value.size(); num++) {
                        MidCode temp = new MidCode(PUTARRAY, name, String.valueOf(num) ,value.get(num));
                        codes.add(temp);
                    }
                }
            }
        }
    }

    private ArrayList<String> InitVal(Node t) {
        ArrayList<String> V = new ArrayList<>();
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Exp) {
                String value = Exp(i);
                V.add(value);
            } else if (type == NodeType.InitVal) {
                V.addAll(InitVal(i));
            }
        }
        return V;
    }

    private ArrayList<String> ConstInitVal(Node t) {
        ArrayList<String> V = new ArrayList<>();
        List<Node> n = t.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.ConstExp) {
                String value = ConstExp(i);
                V.add(value);
            } else if (type == NodeType.ConstInitVal) {
                V.addAll(ConstInitVal(i));
            }
        }
        return V;
    }

    private String ConstExp(Node t) {
        List<Node> n = t.getchildNodes();

        if (t.nodeNum == 1) {
            inConstExp = true;
        }
        String v = AddExp(n.get(0));
        inConstExp = false;
        return v;
    }

    private String AddExp(Node t) {
        List<Node> n = t.getchildNodes();
        String v;
        if (inConstExp == true) {
            if (n.size() == 1) {
                v = MulExp(n.get(0));
            } else {
                int a1 = Integer.parseInt(MulExp(n.get(0)));
                int a2 = 0;
                for (int i = 1; i < n.size()-1; i = i+2) {
                    if (n.get(i).getType() == NodeType.LeafNode) {
                        if (n.get(i).getToken().getStr().equals("+")) {
                            a2 = Integer.parseInt(MulExp(n.get(i+1)));
                            a1 = a1+a2;
                        } else {
                            a2 = Integer.parseInt(MulExp(n.get(i+1)));
                            a1 = a1-a2;
                        }
                    }
                }
                v = String.valueOf(a1);
            }
        } else {
            if (n.size() == 1) {
                v = MulExp(n.get(0));
            } else {
                v = "t&" + this.pos;
                this.pos++;
                String a2 = MulExp(n.get(n.size()-1));
                if (n.get(n.size()-2).getToken().getStr().equals("+")) {
                    n.remove(n.size()-1);
                    n.remove(n.size()-1);
                    MidCode mid = new MidCode(ADD, v, AddExp(t), a2);
                    codes.add(mid);
                } else {
                    n.remove(n.size()-1);
                    n.remove(n.size()-1);
                    MidCode mid = new MidCode(MINU, v, AddExp(t), a2);
                    codes.add(mid);
                }
            }
        }
        return v;
    }

    private String MulExp(Node t) {
        List<Node> n = t.getchildNodes();
        String v;
        if (inConstExp == true) {
            if (n.size() == 1) {
                v = UnaryExp(n.get(0));
            } else {
                int a1 = Integer.parseInt(UnaryExp(n.get(0)));
                int a2 = 0;
                for (int i = 1; i < n.size()-1; i = i+2) {
                    if (n.get(i).getType() == NodeType.LeafNode) {
                        if (n.get(i).getToken().getStr().equals("*")) {
                            a2 = Integer.parseInt(UnaryExp(n.get(i+1)));
                            a1 = a1 * a2;
                        } else if (n.get(i).getToken().getStr().equals("/")){
                            a2 = Integer.parseInt(UnaryExp(n.get(i+1)));
                            a1 = a1 / a2;
                        } else {
                            a2 = Integer.parseInt(UnaryExp(n.get(i+1)));
                            a1 = a1 % a2;
                        }
                    }
                }
                v = String.valueOf(a1);
            }
        } else {
            if (n.size() == 1) {
                v = UnaryExp(n.get(0));
            } else {
                v = "t&" + this.pos;
                this.pos++;
                String a2 = UnaryExp(n.get(n.size()-1));
                if (n.get(n.size()-2).getToken().getStr().equals("*")) {
                    n.remove(n.size()-1);
                    n.remove(n.size()-1);
                    MidCode mid = new MidCode(MULT, v, MulExp(t), a2);
                    codes.add(mid);
                } else if (n.get(n.size()-2).getToken().getStr().equals("/")){
                    n.remove(n.size()-1);
                    n.remove(n.size()-1);
                    MidCode mid = new MidCode(DIV, v, MulExp(t), a2);
                    codes.add(mid);
                } else {
                    n.remove(n.size()-1);
                    n.remove(n.size()-1);
                    MidCode mid = new MidCode(MOD, v, MulExp(t), a2);
                    codes.add(mid);
                }
            }
        }
        return v;
    }

    private String PrimaryExp(Node t) {
        List<Node> n = t.getchildNodes();
        String v;
        if (n.size() == 3) {
            v = Exp(n.get(1));
        } else {
            if (n.get(0).getType() == NodeType.Number) {
                int num = n.get(0).getchildNodes().get(0).getToken().getNum();
                v = String.valueOf(num);
            } else {
                String lval = LVal(n.get(0), false);
                if (n.get(0).getchildNodes().size() == 1) {
                    v = lval;
                } else {
                    String name = n.get(0).getchildNodes().get(0).getToken().getStr();
                    if (lval.contains("+")) {
                        v = lval;
                    } else {
                        v = "t&" + pos;
                        pos++;
                        MidCode l = new MidCode(GETARRAY, v, name, lval);
                        codes.add(l);
                    }
                }
            }
        }
        return v;
    }

    private String LVal(Node t, boolean op) {
        List<Node> n = t.getchildNodes();
        String v;
        String name = n.get(0).getToken().getStr();
        if (n.size() == 1) { // 普通变量
            v = name;
        } else if (n.size() == 4) { // 一维数组
            String v1 = Exp(n.get(2));
            if (this.defFunc == true) {
                if (this.table.getType(FuncName, name, this.tableId) == 2) {
                    v = name + "+" + v1 +"+" + this.table.getType(FuncName, name, this.tableId);
                } else {
                    if (op == true) {
                        v = name + "[" + v1 +"]";
                    }
                    else {
                        v = v1;
                    }
                }
            } else {
                if (this.table.getType(name, this.tableId) == 2) {
                    v = name + "+" + v1 +"+" + this.table.getCol(name, this.tableId);
                } else {
                    if (op == true) {
                        v = name + "[" + v1 +"]";
                    }
                    else {
                        v = v1;
                    }
                }
            }
        } else { // 二维数组
            if (this.defFunc == true) {
                // table.funcs.get(FuncName).globalVar.p(); // qqqqq
                String col = this.table.getCol(FuncName, name, this.tableId);
                String v0 = "t&" + this.pos;
                this.pos++;
                MidCode l0 = new MidCode(MULT, v0,Exp(n.get(2)), col);
                codes.add(l0);
                String v1 = "t&" + this.pos;
                this.pos++;
                MidCode l1 = new MidCode(ADD, v1, v0, Exp(n.get(5)));
                codes.add(l1);
                if (op == true) {
                    v = name + "[" + v1 +"]";
                }
                else {
                    v = v1;
                }
            } else {
                String col = this.table.getCol(name, this.tableId);
                String v0 = "t&" + this.pos;
                this.pos++;
                MidCode l0 = new MidCode(MULT, v0, Exp(n.get(2)), col);
                codes.add(l0);
                String v1 = "t&" + this.pos;
                this.pos++;
                MidCode l1 = new MidCode(ADD, v1, v0, Exp(n.get(5)));
                codes.add(l1);
                if (op == true) {
                    v = name + "[" + v1 +"]";
                }
                else {
                    v = v1;
                }
            }
        }
        return v;
    }

    private String UnaryExp(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type = n.get(0).getType();
        String v;
        if (inConstExp == true) {
            if (n.get(0).getType() == NodeType.PrimaryExp) {
                v = PrimaryExp(n.get(0));
            } else {
                String op = n.get(0).getchildNodes().get(0).getToken().getStr();
                if (op.equals("+")) {
                    v = UnaryExp(n.get(1));
                } else {
                    int a1 = Integer.parseInt(UnaryExp(n.get(1)));
                    v = String.valueOf(-a1);
                }
            }
        } else {
            if (type == NodeType.PrimaryExp) {
                v = PrimaryExp(n.get(0));
            } else if (type == NodeType.UnaryOp) {
                String op = n.get(0).getchildNodes().get(0).getToken().getStr();
                if (op.equals("+")) {
                    v = UnaryExp(n.get(1));
                } else if (op.equals("-")){
                    v = "t&" + this.pos;
                    this.pos++;
                    MidCode mid = new MidCode(MINU, v, "0", UnaryExp(n.get(1)));
                    codes.add(mid);
                } else { //  ！的情况
                    v = "t&" + this.pos;
                    this.pos++;
                    MidCode mid = new MidCode(EQLOP, v, "0", UnaryExp(n.get(1)));
                    codes.add(mid);
                }
            } else {
                if (n.size() > 3) {
                    FuncRParams(n.get(2));
                }
                // 调用函数没有参数
                String name_d = n.get(0).getToken().getStr();
                MidCode callFunc = new MidCode(CALL, name_d);
                codes.add(callFunc);
                if (this.table.getFuncType(name_d) == 1) {
                    v = "t&" + this.pos;
                    this.pos++;
                    MidCode result = new MidCode(ASSIGN, v);
                    codes.add(result);
                } else {
                    v = "";
                }
            }
        }
        return v;
    }

    private void FuncRParams(Node t) {
        List<Node> n = t.getchildNodes();
        NodeType type;
        ArrayList<MidCode> list = new ArrayList<>();
        inFuncRParams = true;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Exp) {
                String par = Exp(i);
                if (par.contains("+")) {
                    String[] strlist = par.split("\\+");
                    MidCode l = new MidCode(PASS, strlist[0], strlist[1], strlist[2]);
                    list.add(l);
                } else {
                    MidCode l = new MidCode(PASS, par);
                    list.add(l);
                }
            }
        }
        for (int j = 0; j<list.size(); j++) {
            codes.add(list.get(j));
        }
        inFuncRParams = false;
    }

    private String Exp(Node t) {
        return AddExp(t.getchildNodes().get(0));
    }
}
