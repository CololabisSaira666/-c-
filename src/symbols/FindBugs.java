package symbols;

import lexer.Tag;
import lexer.Token;
import tree.Node;
import tree.NodeType;
import error.Errors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static lexer.Tag.*;

public class FindBugs {
    private Node tree;
    public List<Errors> bugs = new ArrayList<>();
    private SymbolTable symTable = new SymbolTable();
    private SymbolTable funcTable = new SymbolTable();
    private boolean haveReturn = false; // 判断是否有 return 语句
    private boolean haverealRe = false; // 修改后确定数据流？
    private boolean lvalInStmt = false; // 常量是否被赋值
    private int inFor = 0;
    private String thisFuncName; // 当前正在处理的函数名称
    private Stack <Token> inFindName = new Stack<>(); // 目前读到的调用函数名,这里暂时用token？
    private Stack <Token> thisFuncToken = new Stack<>();
    private boolean inFunc = false; // 判断是否在处理funcdef
    private boolean inFuncFConstExp = false; // 是否处于寻找 BType Ident ['[' ']' { '[' ConstExp ']' }]阶段
    private  Stack<String> inFindParType = new Stack<>(); // 判断是否在查找函数实参类型
    private Stack<SymType> parType = new Stack<>(); //函数实参类型
    private SymType realparType; //实际函数实参类型
    private Integer returnLine;
    private SymType thisFuncType;
    public FindBugs(Node t) {
        this.tree = t;
    }

    public void analyse() { // CompUnit 层面
        symTable.setPre(null);
        funcTable.setPre(null);
        List<Node> n = tree.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Decl) {
                symTable = Decl(i, symTable);
            } else if (type == NodeType.FuncDef) {
                funcTable = FuncDef(i, funcTable);
            } else if (type == NodeType.MainFuncDef) {
                symTable = MainFuncDef(i, symTable);
            }
        }
    }

    private SymbolTable MainFuncDef(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        Token token;
        for (Node i : n){
            type = i.getType();
            if (type == NodeType.Block) {
                t = Block(i, t);
            }
        }
        if (haverealRe == false) {
            Node i = n.get(n.size()-1);
            List<Node> temp = i.getchildNodes();
            token = temp.get(temp.size()-1).getToken();
            Errors e = new Errors('g', token.getLineNum()); // 需要返回值但缺少return语句
            bugs.add(e);
        }
        haverealRe = false;
        return t;
    }

    private SymbolTable FuncDef(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        Token token = n.get(1).getToken();
        String name = token.getStr();
        thisFuncName = name;
        if (t.findSameSymbol(name) || symTable.findSameSymbol(name)) {
            Errors e = new Errors('b',token.getLineNum());
            bugs.add(e);
        } else {
            inFunc = true;
            Symbol sym = new Symbol(name);
            sym.setType(SymType.Func);
            token = n.get(0).getchildNodes().get(0).getToken();
            if (token.type == Tag.INTTK) {
                sym.setReturnType(SymType.FuncINT);
            } else if (token.type == Tag.VOIDTK) {
                sym.setReturnType(SymType.VOID);
            }
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.FuncFParams) {
                    sym = FuncFParams(i, sym);
                }
            }
            t.addSym(name, sym); // 将这一函数名（symbol内包含形参）都放入符号表中，注意，一个函数占一项
            //System.out.println(funcTable.symTable.size());
            thisFuncType = sym.getReturnType();
            t = Block(n.get(n.size() - 1), t);
            inFunc = false;
            if (haverealRe == false &&  thisFuncType == SymType.FuncINT) {
                Node i = n.get(n.size()-1);
                List<Node> temp = i.getchildNodes();
                token = temp.get(temp.size()-1).getToken();
                Errors e = new Errors('g', token.getLineNum()); // 需要返回值但缺少return语句
                bugs.add(e);
            }
            haverealRe = false;
        }

        return t;
    }

    private SymbolTable Block(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        SymbolTable newTable = new SymbolTable();
        newTable.setPre(t);
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.BlockItem) {
                newTable = BlockItem(i, newTable);
            }
        }
        t.createNext(newTable);
        return t;
    }

    private SymbolTable BlockItem(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Decl) {
                t = Decl(i, t);
            } else if (type == NodeType.Stmt) {
                t = Stmt(i, t);
            }
        }
        return t;
    }

    private SymbolTable ForStmt(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.LVal) {
                lvalInStmt = true;
                t = LVal(i, t);
                lvalInStmt = false;
            } else if (type == NodeType.Exp) {
                t = Exp(i, t);
            }
        }
        return t;
    }

    private SymbolTable Exp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        t = AddExp(n.get(0), t);
        return  t;
    }

    private SymbolTable AddExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.MulExp) {
                t = MulExp(i, t);
            } else if (type == NodeType.AddExp) {
                t = AddExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable MulExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.UnaryExp) {
                t = UnaryExp(i, t);
            } else if (type == NodeType.MulExp) {
                t = MulExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable Cond(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.LOrExp) {
                t = LOrExp(i, t);
            }
        }
        return  t;
    }

    private SymbolTable LOrExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.LAndExp) {
                t = LAndExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable LAndExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.EqExp) {
                t = EqExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable EqExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.RelExp) {
                t = RelExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable RelExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.AddExp) {
                t = AddExp(i, t);
            }
        }
        return t;
    }

    private SymbolTable Stmt(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        Token token = n.get(0).getToken();
        type = n.get(0).getType();
        if (token != null) {
            if (token.type == RETURNTK) { // return
                if (n.size() > 1) {
                    // System.out.println(n.get(1).getType());
                    if (n.get(1).getType() == NodeType.Exp) {
                        haveReturn = true;
                        returnLine = token.getLineNum();
                        haverealRe = true;
                        if (inFunc) {
                            if (thisFuncType == SymType.VOID) {
                                Errors e = new Errors('f', returnLine);
                                bugs.add(e);

                            }
                        }
                        t = Exp(n.get(1), t);
                    } else {
                        haverealRe = false;
                    }
                } else {
                    haverealRe = false;
                }
            } else if (token.type == FORTK) { // for
                for (Node i : n) {
                    type = i.getType();
                    if (type == NodeType.Cond) {
                        t = Cond(i, t);
                    } else if (type == NodeType.ForStmt) {
                        t = ForStmt(i, t);
                    } else if (type == NodeType.Stmt) {
                        inFor++;
                        t = Stmt(i, t);
                        inFor--;
                    }
                }
            } else if (token.type == BREAKTK || token.type == CONTINUETK) { // break & continue
                if (inFor == 0) {
                    Errors e = new Errors('m', token.getLineNum());
                    bugs.add(e);
                }
            } else if (token.type == IFTK) {
                for (Node i : n) {
                    type = i.getType();
                    if (type == NodeType.Cond) {
                        t = Cond(i, t);
                    } else if (type == NodeType.Stmt) {
                        t = Stmt(i, t);
                    }
                }
            } else if (token.type == PRINTFTK) {
                int formatNum = 0;
                int relNum = 0;
                for (Node i : n) {
                    type = i.getType();
                    //System.out.println(type);
                    if (type == NodeType.FormatString) {
                        token = i.getchildNodes().get(0).getToken();
                        int j = 0;
                        if (token.haveErrorA) {
                            Errors e = new Errors('a', token.getLineNum());
                            bugs.add(e);
                        }
                        while(token.getStr().indexOf("%d",j)>=0){
                            relNum++;
                            j = token.getStr().indexOf("%d", j) + "%d".length();
                        }
                    } else if (type == NodeType.Exp) {
                        formatNum++;
                        t = Exp(i, t);
                    }
                }
                if (formatNum != relNum) {
                    token = n.get(0).getToken();
                    Errors e = new Errors('l', token.getLineNum());
                    bugs.add(e);
                }
            }
        } else if (type == NodeType.LVal) { // LVal '=' Exp ';' 或 LVal '=' 'getint''('')'';'
                lvalInStmt = true;
                t = LVal(n.get(0), t);
                lvalInStmt = false;
                if (n.get(2).getType() == NodeType.Exp) {
                    t = Exp(n.get(2), t);
                }
        } else if (type == NodeType.Block) {
            t = Block(n.get(0), t);
        } else if (type == NodeType.Exp) {
            t = Exp(n.get(0), t);
        }
        return t;
    }

    private SymbolTable LVal(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        Token token = n.get(0).getToken();
        String name = token.getStr();
        if (inFunc) { // 定义函数阶段，这个应该是在block里面
            if (inFuncFConstExp == false) {
                Symbol s = funcTable.getFuncDef(thisFuncName);
                if (s.findSameParam(name) || t.useSym(name) || symTable.useSym(name)) {
                    SymType tempType;
                    if (t.useSym(name)) {
                        tempType = t.getSym(name).getTyoe();
                        if (t.findConstSymbol(name) && lvalInStmt == true) {
                            Errors e = new Errors('h', token.getLineNum());
                            bugs.add(e);
                        }
                    } else if (symTable.useSym(name)) {
                        tempType = symTable.getSym(name).getTyoe();
                        if (symTable.findConstSymbol(name) && lvalInStmt == true) {
                            Errors e = new Errors('h', token.getLineNum());
                            bugs.add(e);
                        }
                    } else {
                        tempType = s.getparamType(s.getparamId(name));
                    }
                    if (!inFindParType.empty()) {
                        if (inFindParType.peek() == "1") { // 寻找函数实参阶段
                            //System.out.println(name + token.getLineNum() + " " + tempType);
                            if (tempType == SymType.VAR || tempType == SymType.CONST) {
                                tempType = SymType.FuncINT;
                            } else if (tempType == SymType.ARRAY_1 || tempType == SymType.CONSTARRAY_1) {
                                tempType = SymType.FuncARRAY_1;
                            } else if (tempType == SymType.ARRAY_2 || tempType == SymType.CONSTARRAY_2) {
                                tempType = SymType.FuncARRAY_2;
                            }
                            //System.out.println(name + token.getLineNum() + " " + tempType);
                            if (n.size() == 1 || n.size() > 4) {
                                realparType = SymType.FuncINT;
                                if (n.size() == 1) {
                                    realparType = tempType;
                                }
                            } else if (n.size() <= 4) {
                                realparType = SymType.FuncARRAY_1;
                                if (tempType == SymType.FuncARRAY_1) {
                                    realparType = SymType.FuncINT;
                                }
                            }
                            if (realparType != parType.peek()) {
                                //System.out.println(name + token.getLineNum());
                                Errors e = new Errors('e', inFindName.peek().getLineNum());
                                bugs.add(e);
                            }
                        }
                    }
                } else {
                    Errors e = new Errors('c', token.getLineNum());
                    bugs.add(e);
                }
            } else {
                //System.out.println(name);
                if (symTable.useSym(name) == false) {
                    Errors e = new Errors('c', token.getLineNum());
                    bugs.add(e);
                }
            }

        } else {
            //System.out.println(name);
            if (t.useSym(name)) { // 已经被定义
                SymType tempType = t.getSym(name).getTyoe();
                if (t.findConstSymbol(name) && lvalInStmt == true) {
                    Errors e = new Errors('h', token.getLineNum());
                    bugs.add(e);
                }
                if (!inFindParType.empty()) {
                    if (inFindParType.peek() == "1") { // 寻找函数实参阶段
                        if (tempType == SymType.VAR || tempType == SymType.CONST) {
                            tempType = SymType.FuncINT;
                        } else if (tempType == SymType.ARRAY_1 || tempType == SymType.CONSTARRAY_1) {
                            tempType = SymType.FuncARRAY_1;
                        } else if (tempType == SymType.ARRAY_2 || tempType == SymType.CONSTARRAY_2) {
                            tempType = SymType.FuncARRAY_2;
                        }
                        if (n.size() == 1 || n.size() > 4) {
                            realparType = SymType.FuncINT;
                            if (n.size() == 1) {
                                realparType = tempType;
                            }
                        } else if (n.size() <= 4) {
                            realparType = SymType.FuncARRAY_1;
                            if (tempType == SymType.FuncARRAY_1) {
                                realparType = SymType.FuncINT;
                            }
                        }
                        if (realparType != parType.peek()) {
                            Errors e = new Errors('e', inFindName.peek().getLineNum());
                            bugs.add(e);
                        }
                    }
                }
            } else {
                Errors e = new Errors('c', token.getLineNum());
                bugs.add(e);
            }
        }
        boolean temp = lvalInStmt;
        lvalInStmt = false;
        if (!inFindParType.empty()) {
            if (inFindParType.peek() == "1") {
                // qqq
                for (Node i : n) {
                    type = i.getType();
                    if (type == NodeType.Exp) {
                        inFindParType.push("0");
                        t = Exp(i, t);
                        inFindParType.pop();
                    }
                }
            } else {
                for (Node i : n) {
                    type = i.getType();
                    if (type == NodeType.Exp) {
                        t = Exp(i, t);
                    }
                }
            }
        } else {
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.Exp) {
                    t = Exp(i, t);
                }
            }
        }
        lvalInStmt = temp;
        return t;
    }

    private SymbolTable PrimaryExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.Exp) {
                t = Exp(i, t);
            } else if (type == NodeType.LVal) {
                t = LVal(i, t);
            } else if (type == NodeType.Number) {
                if (!inFindParType.empty()) {
                    if (inFindParType.peek() == "1") {
                        realparType = SymType.FuncINT;
                        if (!parType.empty()) {
                            if (realparType != parType.peek()) {
                                Errors e = new Errors('e', inFindName.peek().getLineNum());
                                bugs.add(e);
                            }
                        }
                    }
                }
            }
        }
        return t;
    }

    private SymbolTable UnaryExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        type = n.get(0).getType();
        if (type == NodeType.PrimaryExp) {
            t = PrimaryExp(n.get(0), t);
        } else if (type == NodeType.UnaryOp) {
            t = UnaryExp(n.get(1), t);
        } else {
            Token token = n.get(0).getToken();
            String name = token.getStr();
            if (funcTable.findSameSymbol(name) == false) { // 查看此前是否定义过该函数
                Errors e = new Errors('c', token.getLineNum());
                bugs.add(e);
            } else  {
                Symbol s = funcTable.getSym(name);
                if (!inFindParType.empty()) {
                    if (inFindParType.peek() == "1") { // 已经在嵌套函数中了，要看返回值！
                        if (s.getReturnType() != parType.peek()) {
                            Errors e = new Errors('e', inFindName.peek().getLineNum());
                            bugs.add(e);
                        }
                    }
                }
                inFindName.push(token);
                for (Node i : n) {
                    type = i.getType();
                    if (type == NodeType.FuncRParams) {
                        t = FuncRParams(i, t, token);
                    }
                }
                if (n.size() == 2) {
                    if (0 != s.getparamSize()) { // 先判断参数个数是否正确
                        Errors e = new Errors('d', token.getLineNum());
                        bugs.add(e);
                    }
                } else if (n.size() == 3) {
                    if (n.get(2).getType() != NodeType.FuncRParams) {
                        if (0 != s.getparamSize()) { // 先判断参数个数是否正确
                            Errors e = new Errors('d', token.getLineNum());
                            bugs.add(e);
                        }
                    }
                }
                inFindName.pop();
            }
        }
        return t;
    }

    private SymbolTable FuncRParams(Node tNode, SymbolTable t, Token funcN) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        String name = funcN.getStr();
        Symbol s = funcTable.getSym(name);
        int num = 0;
        for (Node i : n){
            type = i.getType();
            if (type == NodeType.Exp) {
                num++;
            }
        }
        // System.out.println(num);
        if (num != s.getparamSize()) { // 先判断参数个数是否正确
            Errors e = new Errors('d', funcN.getLineNum());
            bugs.add(e);
        } else {
            num = 0;
            for (Node i : n){
                type = i.getType();
                if (type == NodeType.Exp) {
                    parType.push(s.getparamType(num));
                    inFindParType.push("1");
                    t = Exp(i, t);
                    inFindParType.pop();
                    parType.pop();
                    num++;
                }
            }
        }
        return t;
    }

    private Symbol FuncFParams(Node tNode, Symbol s) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.FuncFParam) {
                s = FuncFParam(i, s);
            }
        }
        return s;
    }

    private Symbol FuncFParam(Node tNode, Symbol s) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        int length = 0;
        Token token = n.get(1).getToken();
        String name = token.getStr();
        if (s.findSameParam(name)) {
            Errors e = new Errors('b', token.getLineNum());
            bugs.add(e);
        } else {
            for (Node i : n) {
                token = i.getToken();
                type = i.getType();
                if (token != null) {
                    if (token.type == LBRACK) {
                        length++;
                    }
                } else if (type == NodeType.ConstExp) {
                    inFuncFConstExp = true;
                    symTable = ConstExp(i, symTable);
                    inFuncFConstExp = false;
                }
            }
            if (length == 0) {
                s.addParam(name, SymType.FuncINT);
            } else if (length == 1) {
                s.addParam(name, SymType.FuncARRAY_1);
            } else if (length == 2) {
                s.addParam(name, SymType.FuncARRAY_2);
            }
        }
        return s;
    }

    private SymbolTable Decl(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.ConstDecl) {
                t = ConstDecl(i, t);
            } else if (type == NodeType.VarDecl) {
                t = VarDecl(i, t);
            }
        }
        return t;
    }

    private SymbolTable VarDecl(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.VarDef) {
                t = VarDef(i, t);
            }
        }
        return t;
    }

    private SymbolTable VarDef(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        int length = 0;
        Token token = n.get(0).getToken();
        String name = token.getStr();
        int flag = 0;
        if (inFunc) {
            Symbol s = funcTable.getFuncDef(thisFuncName);
            if (s.findSameParam(name) || t.findSameSymbol(name)) {
                Errors e = new Errors('b',token.getLineNum());
                bugs.add(e);
                flag = 1;
            }
        } else {
            if (t.findSameSymbol(name)) {
                Errors e = new Errors('b',token.getLineNum());
                bugs.add(e);
                flag = 1;
            }
        }
        if (flag == 0) {
            Symbol sym = new Symbol(name);
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.ConstExp) {
                    length++;
                } else if (type == NodeType.InitVal) {
                    t = InitVal(i, t);
                }
            }
            if (length == 0) {
                sym.setType(SymType.VAR);
            } else if (length == 1) {
                sym.setType(SymType.ARRAY_1);
            } else if (length == 2) {
                sym.setType(SymType.ARRAY_2);
            }
            t.addSym(name, sym);
        }
        return t;
    }

    private SymbolTable InitVal(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        if (n.size() == 1) {
            t = Exp(n.get(0), t);
        } else {
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.InitVal) {
                    t = InitVal(i, t);
                }
            }
        }
        return t;
    }

    private SymbolTable ConstDecl(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        for (Node i : n) {
            type = i.getType();
            if (type == NodeType.ConstDef) {
                t = ConstDef(i, t);
            }
        }
        return t;
    }

    private SymbolTable ConstInitVal(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        if (n.size() == 1) {
            t = ConstExp(n.get(0), t);
        } else {
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.ConstInitVal) {
                    t = ConstInitVal(i, t);
                }
            }
        }
        return t;
    }

    private SymbolTable ConstExp(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        t = AddExp(n.get(0), t);
        return t;
    }

    private SymbolTable ConstDef(Node tNode, SymbolTable t) {
        List<Node> n = tNode.getchildNodes();
        NodeType type;
        int length = 0;
        Token token = n.get(0).getToken();
        String name = token.getStr();
        int flag = 0;
        if (inFunc) {
            Symbol s = funcTable.getFuncDef(thisFuncName);
            if (s.findSameParam(name) || t.findSameSymbol(name)) {
                Errors e = new Errors('b',token.getLineNum());
                bugs.add(e);
                flag = 1;
            }
        } else {
            if (t.findSameSymbol(name)) {
                Errors e = new Errors('b',token.getLineNum());
                bugs.add(e);
                flag = 1;
            }
        }
        if (flag == 0) {
            Symbol sym = new Symbol(name);
            for (Node i : n) {
                type = i.getType();
                if (type == NodeType.ConstExp) {
                    length++;
                } else if (type == NodeType.ConstInitVal) {
                    t = ConstInitVal(i, t);
                }
            }
            if (length == 0) {
                sym.setType(SymType.CONST);
            } else if (length == 1) {
                sym.setType(SymType.CONSTARRAY_1);
            } else if (length == 2) {
                sym.setType(SymType.CONSTARRAY_2);
            }
            t.addSym(name, sym);
        }
        return t;
    }
}
