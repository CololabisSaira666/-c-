package parser;


import error.Errors;
import lexer.*;
import tree.Node;
import tree.NodeType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static lexer.Tag.*;

public class Parser {
    //private final BufferedWriter parseout;
    private final Lexer lexer;
    private Token curToken;
    private int tempNum = 1;
    private int pos = -1;
    List<Token> tokenList = new LinkedList<>();
    public List<Errors> errorList = new ArrayList<>();

    public Parser(Lexer lexer) {
        //this.parseout = parseout;
        this.lexer = lexer;
    }

    private void getToken() throws IOException {
        pos++;
        if (tokenList.size() == pos) {
            tokenList.add(lexer.getToken());
        }
        curToken = tokenList.get(pos);
    }

    private void backtrack(int step) {
        pos = pos - step;
        if (pos >= 0) {
            curToken = tokenList.get(pos);
        }
    }

    private Node addLeaf(Token thisToken) {
        Node node = new Node(NodeType.LeafNode);
        node.addToken(thisToken);
        return node;
    }
    public Node parserRun() throws IOException {
        Node CompUnit = new Node(NodeType.CompUnit);
        getToken();
        while (curToken.type == CONSTTK || curToken.type == INTTK) {
            if (curToken.type == CONSTTK) {
                CompUnit.addNode(parseDecl());
            } else {
                getToken();
                if (curToken.type == IDENFR) {
                    getToken();
                    if (curToken.type == LPARENT) {
                        backtrack(2);
                        break;
                    }
                    backtrack(2);
                    CompUnit.addNode(parseDecl());
                } else {
                    backtrack(1);
                    break;
                }
            }
        }
        while (curToken.type == INTTK || curToken.type == VOIDTK) {
            if (curToken.type == VOIDTK) {
                CompUnit.addNode(parseFuncDef());
            } else {
                getToken();
                if(curToken.type == IDENFR) {
                    backtrack(1);
                    CompUnit.addNode(parseFuncDef());
                } else {
                    backtrack(1);
                    break;
                }
            }
        }
        // //parseout.write(e(curToken.toString());
        CompUnit.addNode(parseMainFuncDef());
        //parseout.write(e("<CompUnit>\n");
        return CompUnit;
    }

    private Node parseConstDecl() throws IOException {
        Node ConstDecl = new Node(NodeType.ConstDecl);
        //parseout.write(e(curToken.toString()); // CONSTTK const
        ConstDecl.addNode(addLeaf(curToken));
        getToken();
        ConstDecl.addNode(parseBtype());
        ConstDecl.addNode(parseConstDef());
        while (curToken.type == COMMA) { // COMMA ,
            //parseout.write(e(curToken.toString()); // ,
            ConstDecl.addNode(addLeaf(curToken));
            getToken();
            ConstDecl.addNode(parseConstDef());
        }
        if (curToken.type == SEMICN) {
            ConstDecl.addNode(addLeaf(curToken));
            getToken();
        } else {
            backtrack(1);
            int q = curToken.getLineNum();
            Errors e = new Errors('i',q);
            errorList.add(e);
            getToken();
        }
        //parseout.write(e("<ConstDecl>\n");
        return ConstDecl;
    }
    private Node parseBtype() throws IOException {
        Node Btype = new Node(NodeType.Btype);
        //parseout.write(e(curToken.toString()); // int
        Btype.addNode(addLeaf(curToken));
        getToken();
        return Btype;
    }

    private Node parseDecl() throws IOException {
        Node Decl = new Node(NodeType.Decl);
        if (curToken.type == CONSTTK) {
            Decl.addNode(parseConstDecl());
        } else if (curToken.type == INTTK) {
            Decl.addNode(parseVarDecl());
        }
        return Decl;
    }

    private Node parseVarDecl() throws IOException {
        Node VarDecl = new Node(NodeType.VarDecl);
        VarDecl.addNode(parseBtype());
        VarDecl.addNode(parseVarDef());
        while (curToken.type == COMMA) {
            //parseout.write(e(curToken.toString()); // ,
            VarDecl.addNode(addLeaf(curToken));
            getToken();
            VarDecl.addNode(parseVarDef());
        }
        if (curToken.type == SEMICN) {
            //parseout.write(e(curToken.toString()); // ;
            VarDecl.addNode(addLeaf(curToken));
            getToken();
        } else {
            backtrack(1);
            Errors e = new Errors('i',curToken.getLineNum());
            errorList.add(e);
            getToken();
        }

        //parseout.write(e("<VarDecl>\n");
        return VarDecl;
    }

    private Node parseFuncDef() throws IOException {
        Node FuncDef = new Node(NodeType.FuncDef);
        FuncDef.addNode(parseFuncType());
        //parseout.write(e(curToken.toString()); // ident
        FuncDef.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e(curToken.toString()); // (
        FuncDef.addNode(addLeaf(curToken));
        getToken();
        if (curToken.type != RPARENT) {
            if (curToken.type != LBRACE) { // != {
                FuncDef.addNode(parseFuncFParams());
            }
        }
        if (curToken.type == RPARENT) {
            //parseout.write(e(curToken.toString()); // )
            FuncDef.addNode(addLeaf(curToken));
            getToken();
        } else {
            backtrack(1);
            Errors e = new Errors('j', curToken.getLineNum());
            errorList.add(e);
            getToken();
        }
        FuncDef.addNode(parseBlock());
        //parseout.write(e("<FuncDef>\n");
        return FuncDef;
    }

    private Node parseFuncFParams() throws IOException {
        Node FuncFParams = new Node(NodeType.FuncFParams);
        FuncFParams.addNode(parseFuncFParam());
        while (curToken.type == COMMA) {
            //parseout.write(e(curToken.toString());
            FuncFParams.addNode(addLeaf(curToken));
            getToken();
            FuncFParams.addNode(parseFuncFParam());
        }
        //parseout.write(e("<FuncFParams>\n");
        return FuncFParams;
    }

    private Node parseFuncFParam() throws IOException {
        Node FuncFParam = new Node(NodeType.FuncFParam);
        FuncFParam.addNode(parseBtype());
        //parseout.write(e(curToken.toString()); // ident
        FuncFParam.addNode(addLeaf(curToken));
        getToken();
        if (curToken.type == LBRACK) { // [
            //parseout.write(e(curToken.toString()); // [
            FuncFParam.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != RBRACK) {
                backtrack(1);
                Errors e = new Errors('k', curToken.getLineNum());
                errorList.add(e);
                getToken();
            } else {
                FuncFParam.addNode(addLeaf(curToken));
                getToken();
            }
            //parseout.write(e(curToken.toString()); // ]
            while (curToken.type == LBRACK) {
                //parseout.write(e(curToken.toString()); // [
                FuncFParam.addNode(addLeaf(curToken));
                getToken();
                FuncFParam.addNode(parseConstExp());
                if (curToken.type == RBRACK) {
                    //parseout.write(e(curToken.toString()); // ]
                    FuncFParam.addNode(addLeaf(curToken));
                    getToken();
                } else {
                    backtrack(1);
                    Errors e = new Errors('k', curToken.getLineNum());
                    errorList.add(e);
                    getToken();
                }
            }
        }
        //parseout.write(e("<FuncFParam>\n");
        return FuncFParam;
    }

    private Node parseMainFuncDef() throws IOException {
        Node MainFuncDef = new Node(NodeType.MainFuncDef);
        //parseout.write(e(curToken.toString()); // int
        MainFuncDef.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e(curToken.toString()); // main
        MainFuncDef.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e(curToken.toString()); // (
        MainFuncDef.addNode(addLeaf(curToken));
        getToken();
        if (curToken.type == RPARENT) {
            //parseout.write(e(curToken.toString()); // )
            MainFuncDef.addNode(addLeaf(curToken));
            getToken();
        } else {
            backtrack(1);
            Errors e = new Errors('j', curToken.getLineNum());
            errorList.add(e);
            getToken();
        }
        MainFuncDef.addNode(parseBlock());
        //parseout.write(e("<MainFuncDef>\n");
        return MainFuncDef;
    }

    private Node parseBlock() throws IOException {
        Node Block = new Node(NodeType.Block);
        //parseout.write(e(curToken.toString()); // {
        Block.addNode(addLeaf(curToken));
        getToken();
        while (curToken.type != RBRACE) {
            Block.addNode(parseBlockItem());
        }
        //parseout.write(e(curToken.toString()); // }
        Block.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e("<Block>\n");
        return Block;
    }

    private Node parseFuncType() throws IOException {
        Node FuncType = new Node(NodeType.FuncType);
        //parseout.write(e(curToken.toString()); // int or void
        FuncType.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e("<FuncType>\n");
        return FuncType;
    }

    private Node parseConstDef() throws IOException {
        Node ConstDef = new Node(NodeType.ConstDef);
        //parseout.write(e(curToken.toString()); // IDENFR ident
        ConstDef.addNode(addLeaf(curToken));
        getToken();
        while (curToken.type == LBRACK) { // [
            //parseout.write(e(curToken.toString()); // LBRACK [
            ConstDef.addNode(addLeaf(curToken));
            getToken();
            ConstDef.addNode(parseConstExp());
            if (curToken.type == RBRACK) {
                //parseout.write(e(curToken.toString()); // RBRACK ]
                ConstDef.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('k', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        }
        //parseout.write(e(curToken.toString()); // ASSIGN =
        ConstDef.addNode(addLeaf(curToken));
        getToken();
        ConstDef.addNode(parseConstInitVal());
        //parseout.write(e("<ConstDef>\n");
        return ConstDef;
    }

    private Node parseConstInitVal() throws IOException {
        Node ConstInitVal = new Node(NodeType.ConstInitVal);
        if (curToken.type != LBRACE) { // != {
            ConstInitVal.addNode(parseConstExp());
        } else {
            //parseout.write(e(curToken.toString()); // LBRACK {
            ConstInitVal.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != RBRACE) {
                ConstInitVal.addNode(parseConstInitVal());
                while (curToken.type == COMMA) {
                    //parseout.write(e(curToken.toString());
                    ConstInitVal.addNode(addLeaf(curToken));
                    getToken();
                    ConstInitVal.addNode(parseConstInitVal());
                }
            }
            //parseout.write(e(curToken.toString()); // RBRACE }
            ConstInitVal.addNode(addLeaf(curToken));
            getToken();
        }
        //parseout.write(e("<ConstInitVal>\n");
        return ConstInitVal;
    }

    private Node parseVarDef() throws IOException {
        Node VarDef = new Node(NodeType.VarDef);
        //parseout.write(e(curToken.toString()); // IDENFR ident
        VarDef.addNode(addLeaf(curToken));
        getToken();
        while (curToken.type == LBRACK) {
            //parseout.write(e(curToken.toString()); // LBRACK [
            VarDef.addNode(addLeaf(curToken));
            getToken();
            VarDef.addNode(parseConstExp());
            if (curToken.type == RBRACK) {
                //parseout.write(e(curToken.toString()); // RBRACK ]
                VarDef.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('k', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        }
        if (curToken.type == ASSIGN) { // =
            //parseout.write(e(curToken.toString()); // =
            VarDef.addNode(addLeaf(curToken));
            getToken();
            VarDef.addNode(parseInitVal());
        }
        //parseout.write(e("<VarDef>\n");
        return VarDef;
    }

    private Node parseInitVal() throws IOException {
        Node InitVal = new Node(NodeType.InitVal);
        if (curToken.type != LBRACE) { // != {
            InitVal.addNode(parseExp());
        } else {
            //parseout.write(e(curToken.toString()); // LBRACE {
            InitVal.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != RBRACE) {
                InitVal.addNode(parseInitVal());
                while (curToken.type == COMMA) {
                    //parseout.write(e(curToken.toString()); // COMMA ,
                    InitVal.addNode(addLeaf(curToken));
                    getToken();
                    InitVal.addNode(parseInitVal());
                }
            }
            //parseout.write(e(curToken.toString()); // RBRACE }
            InitVal.addNode(addLeaf(curToken));
            getToken();
        }
        //parseout.write(e("<InitVal>\n");
        return InitVal;
    }

    private Node parseConstExp() throws IOException {
        Node ConstExp = new Node(NodeType.ConstExp);
        this.tempNum = 1;
        ConstExp.addNode(parseAddExp());
        ConstExp.nodeNum = this.tempNum;

        //parseout.write(e("<ConstExp>\n");
        return ConstExp;
    }

    private Node parseAddExp() throws IOException {
        Node AddExp = new Node(NodeType.AddExp);
        AddExp.addNode(parseMulExp());
        //parseout.write(e("<AddExp>\n");
        while (curToken.type == PLUS || curToken.type == MINU) {
            //parseout.write(e(curToken.toString());
            AddExp.addNode(addLeaf(curToken));
            getToken();
            AddExp.addNode(parseMulExp());
            //parseout.write(e("<AddExp>\n");
        }
        return AddExp;
    }

    private Node parseRelExp() throws IOException {
        Node RelExp = new Node(NodeType.RelExp);
        RelExp.addNode(parseAddExp());
        //parseout.write(e("<RelExp>\n");
        while (curToken.type == LSS || curToken.type == LEQ || curToken.type == GRE || curToken.type == GEQ) {
            //parseout.write(e(curToken.toString()); // > >= < <=
            RelExp.addNode(addLeaf(curToken));
            getToken();
            RelExp.addNode(parseAddExp());
            //parseout.write(e("<RelExp>\n");
        }
        return RelExp;
    }

    private Node parseEqExp() throws IOException {
        Node EqExp = new Node(NodeType.EqExp);
        EqExp.addNode(parseRelExp());
        //parseout.write(e("<EqExp>\n");
        while (curToken.type == EQL || curToken.type == NEQ) {
            //parseout.write(e(curToken.toString()); // == !=
            EqExp.addNode(addLeaf(curToken));
            getToken();
            EqExp.addNode(parseRelExp());
            //parseout.write(e("<EqExp>\n");
        }
        return EqExp;
    }

    private Node parseLAndExp() throws IOException {
        Node LAndExp = new Node(NodeType.LAndExp);
        LAndExp.addNode(parseEqExp());
        //parseout.write(e("<LAndExp>\n");
        while (curToken.type == AND) {
            //parseout.write(e(curToken.toString()); // &&
            LAndExp.addNode(addLeaf(curToken));
            getToken();
            LAndExp.addNode(parseEqExp());
            //parseout.write(e("<LAndExp>\n");
        }
        return LAndExp;
    }

    private Node parseLOrExp() throws IOException {
        Node LOrExp = new Node(NodeType.LOrExp);
        LOrExp.addNode(parseLAndExp());
        //parseout.write(e("<LOrExp>\n");
        while (curToken.type == OR) {
            //parseout.write(e(curToken.toString()); // ||
            LOrExp.addNode(addLeaf(curToken));
            getToken();
            LOrExp.addNode(parseLAndExp());
            //parseout.write(e("<LOrExp>\n");
        }
        return LOrExp;
    }

    private Node parseMulExp() throws IOException {
        Node MulExp = new Node(NodeType.MulExp);
        MulExp.addNode(parseUnaryExp());
        //parseout.write(e("<MulExp>\n");
        while (curToken.type == MULT || curToken.type == DIV || curToken.type == MOD) {
            //parseout.write(e(curToken.toString());
            MulExp.addNode(addLeaf(curToken));
            getToken();
            MulExp.addNode(parseUnaryExp());
            //parseout.write(e("<MulExp>\n");
        }
        return MulExp;
    }

    private Node parseUnaryExp() throws IOException {
        Node UnaryExp = new Node(NodeType.UnaryExp);
        if(curToken.type == PLUS || curToken.type == MINU || curToken.type == NOT) {
            UnaryExp.addNode(parseUnaryOp());
            UnaryExp.addNode(parseUnaryExp());
        } else if(curToken.type == IDENFR) {
            getToken();
            if (curToken.type == LPARENT) {
                this.tempNum = 0;
                backtrack(1);
                //parseout.write(e(curToken.toString()); // IDENFR ident
                UnaryExp.addNode(addLeaf(curToken));
                getToken();
                //parseout.write(e(curToken.toString()); // LPARENT (
                UnaryExp.addNode(addLeaf(curToken));
                getToken();
                if(curToken.type != RPARENT) {
                    if (curToken.type == LPARENT || curToken.type == IDENFR || curToken.type == INTCON || curToken.type == PLUS || curToken.type == MINU) {
                        // ident ( + -
                        UnaryExp.addNode(parseFuncRParams());
                    }
                }
                if (curToken.type == RPARENT) {
                    //parseout.write(e((curToken.toString())); // RPARENT )
                    UnaryExp.addNode(addLeaf(curToken));
                    getToken();
                } else {
                    backtrack(1);
                    Errors e = new Errors('j', curToken.getLineNum());
                    errorList.add(e);
                    getToken();
                }
            } else {
                backtrack(1);
                UnaryExp.addNode(parsePrimaryExp());
            }
        } else {
            UnaryExp.addNode(parsePrimaryExp());
        }
        //parseout.write(e("<UnaryExp>\n");
        return UnaryExp;
    }

    private Node parseUnaryOp() throws IOException {
        Node UnaryOp = new Node(NodeType.UnaryOp);
        //parseout.write(e(curToken.toString()); // + - !
        UnaryOp.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e("<UnaryOp>\n");
        return UnaryOp;
    }

    private Node parsePrimaryExp() throws IOException {
        Node PrimaryExp = new Node(NodeType.PrimaryExp);
        if (curToken.type == LPARENT) { // (
            //parseout.write(e(curToken.toString()); // LPARENT (
            PrimaryExp.addNode(addLeaf(curToken));
            getToken();
            PrimaryExp.addNode(parseExp());
            if (curToken.type == RPARENT) {
                //parseout.write(e(curToken.toString()); // RPARENT )
                PrimaryExp.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('j', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        } else if (curToken.type == INTCON) { // 数字
            PrimaryExp.addNode(parseNumber());
        } else {
            PrimaryExp.addNode(parseLVal());
        }
        //parseout.write(e("<PrimaryExp>\n");
        return PrimaryExp;
    }

    private Node parseNumber() throws IOException {
        Node Number = new Node(NodeType.Number);
        Number.nodeNum = curToken.getNum();
        //parseout.write(e(curToken.toString()); // 数字
        Number.addNode(addLeaf(curToken));
        getToken();
        //parseout.write(e("<Number>\n");
        return Number;
    }

    private Node parseFuncRParams() throws IOException {
        Node FuncRParams = new Node(NodeType.FuncRParams);
        FuncRParams.addNode(parseExp());
        while (curToken.type == COMMA) {
            //parseout.write(e(curToken.toString());
            FuncRParams.addNode(addLeaf(curToken));
            getToken();
            FuncRParams.addNode(parseExp());
        }
        //parseout.write(e("<FuncRParams>\n");
        return FuncRParams;
    }

    private Node parseExp() throws IOException {
        Node Exp = new Node(NodeType.Exp);
        Exp.addNode(parseAddExp());
        //parseout.write(e("<Exp>\n");
        return Exp;
    }

    private Node parseFormatString() throws IOException {
        Node FormatString = new Node(NodeType.FormatString);
        FormatString.addNode(addLeaf(curToken));
        return FormatString;
    }

    private Node parseForStmt() throws IOException {
        Node ForStmt = new Node(NodeType.ForStmt);
        ForStmt.addNode(parseLVal());
        //parseout.write(e(curToken.toString()); // =
        ForStmt.addNode(addLeaf(curToken));
        getToken();
        ForStmt.addNode(parseExp());
        //parseout.write(e("<ForStmt>\n");
        return ForStmt;
    }

    private Node parseCond() throws IOException {
        Node Cond = new Node(NodeType.Cond);
        Cond.addNode(parseLOrExp());
        //parseout.write(e("<Cond>\n");
        return Cond;
    }

    private Node parseLVal() throws IOException {
        Node LVal = new Node(NodeType.LVal);
        //parseout.write(e(curToken.toString()); // ident
        LVal.addNode(addLeaf(curToken));
        getToken();
        while (curToken.type == LBRACK) {
            //parseout.write(e(curToken.toString()); // [
            LVal.addNode(addLeaf(curToken));
            getToken();
            LVal.addNode(parseExp());
            if (curToken.type == RBRACK) {
                //parseout.write(e(curToken.toString()); // ]
                LVal.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('k', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        }
        //parseout.write(e("<LVal>\n");
        return LVal;
    }

    private Node parseBlockItem() throws IOException {
        Node BlockItem = new Node(NodeType.BlockItem);
        if (curToken.type == CONSTTK || curToken.type == INTTK) {
            BlockItem.addNode(parseDecl());
        } else {
            BlockItem.addNode(parseStmt());
        }
        return BlockItem;
    }

    private Node parseStmt() throws IOException {
        Node Stmt = new Node(NodeType.Stmt);
        if (curToken.type == IFTK) {
            //parseout.write(e(curToken.toString()); // if
            Stmt.addNode(addLeaf(curToken));
            getToken();
            //parseout.write(e(curToken.toString()); // (
            Stmt.addNode(addLeaf(curToken));
            getToken();
            Stmt.addNode(parseCond());
            if (curToken.type == RPARENT) {
                //parseout.write(e(curToken.toString()); // )
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('j', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
            Stmt.addNode(parseStmt());
            if (curToken.type == ELSETK) {
                //parseout.write(e(curToken.toString()); // else
                Stmt.addNode(addLeaf(curToken));
                getToken();
                Stmt.addNode(parseStmt());
            }
        } else if (curToken.type == FORTK) {
            //parseout.write(e(curToken.toString()); // for
            Stmt.addNode(addLeaf(curToken));
            getToken();
            //parseout.write(e(curToken.toString()); // (
            Stmt.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != SEMICN) {
                Stmt.addNode(parseForStmt());
            }
            //parseout.write(e(curToken.toString()); // ;
            Stmt.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != SEMICN) {
                Stmt.addNode(parseCond());
            }
            //parseout.write(e(curToken.toString()); // ;
            Stmt.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != RPARENT) {
                Stmt.addNode(parseForStmt());
            }
            //parseout.write(e(curToken.toString()); // )
            Stmt.addNode(addLeaf(curToken));
            getToken();
            Stmt.addNode(parseStmt());
        } else if (curToken.type == BREAKTK || curToken.type == CONTINUETK) {
            //parseout.write(e(curToken.toString()); // break or continue
            Stmt.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type == SEMICN) {
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('i', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        } else if (curToken.type == RETURNTK) {
            //parseout.write(e(curToken.toString()); // return
            Stmt.addNode(addLeaf(curToken));
            getToken();
            if (curToken.type != SEMICN && curToken.type != RBRACE) {
                Stmt.addNode(parseExp());
            }
            if (curToken.type == SEMICN) {
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('i', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        } else if (curToken.type == PRINTFTK) {
            //parseout.write(e(curToken.toString()); // print
            Stmt.addNode(addLeaf(curToken));
            getToken();
            //parseout.write(e(curToken.toString()); // (
            Stmt.addNode(addLeaf(curToken));
            getToken();
            //parseout.write(e(curToken.toString()); // FormatString
            Stmt.addNode(parseFormatString());
            getToken();
            while (curToken.type == COMMA) {
                //parseout.write(e(curToken.toString()); // ,
                Node temp = new Node(NodeType.LeafNode);
                temp.addNode(addLeaf(curToken));
                Stmt.addNode(temp);
                getToken();
                Stmt.addNode(parseExp());
            }
            if (curToken.type == RPARENT) {
                //parseout.write(e(curToken.toString()); // )
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('j', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
            if (curToken.type == SEMICN) {
                //parseout.write(e(curToken.toString()); // ;
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                backtrack(1);
                Errors e = new Errors('i', curToken.getLineNum());
                errorList.add(e);
                getToken();
            }
        } else if (curToken.type == LBRACE) {
            Stmt.addNode(parseBlock());
        } else { // 左值表达式 和 [Exp]';'
            if (curToken.type == SEMICN) {
                //parseout.write(e(curToken.toString()); // ;
                Stmt.addNode(addLeaf(curToken));
                getToken();
            } else {
                int backNum = 1;
                getToken();
                while (curToken.type != ASSIGN && curToken.type != SEMICN) {
                    backNum++;
                    getToken();
                }
                if (curToken.type == ASSIGN) {
                    backtrack(backNum);
                    Stmt.addNode(parseLVal());
                    //parseout.write(e(curToken.toString()); // =
                    Stmt.addNode(addLeaf(curToken));
                    getToken();
                    if (curToken.type == GETINTTK) {
                        //parseout.write(e(curToken.toString()); // getint
                        Stmt.addNode(addLeaf(curToken));
                        getToken();
                        //parseout.write(e(curToken.toString()); // (
                        Stmt.addNode(addLeaf(curToken));
                        getToken();
                        if (curToken.type == RPARENT) {
                            //parseout.write(e(curToken.toString()); // )
                            Stmt.addNode(addLeaf(curToken));
                            getToken();
                        } else {
                            backtrack(1);
                            Errors e = new Errors('j', curToken.getLineNum());
                            errorList.add(e);
                            getToken();
                        }
                        if (curToken.type == SEMICN) {
                            //parseout.write(e(curToken.toString()); // ;
                            Stmt.addNode(addLeaf(curToken));
                            getToken();
                        } else {
                            backtrack(1);
                            Errors e = new Errors('i', curToken.getLineNum());
                            errorList.add(e);
                            getToken();
                        }
                    } else {
                        Stmt.addNode(parseExp());
                        if (curToken.type == SEMICN) {
                            //parseout.write(e(curToken.toString()); // ;
                            Stmt.addNode(addLeaf(curToken));
                            getToken();
                        } else {
                            backtrack(1);
                            Errors e = new Errors('i', curToken.getLineNum());
                            errorList.add(e);
                            getToken();
                        }
                    }
                } else {
                    backtrack(backNum);
                    Stmt.addNode(parseExp());
                    if (curToken.type == SEMICN) {
                        //parseout.write(e(curToken.toString()); // ;
                        Stmt.addNode(addLeaf(curToken));
                        getToken();
                    } else {
                        backtrack(1);
                        Errors e = new Errors('i', curToken.getLineNum());
                        errorList.add(e);
                        getToken();
                    }
                }
            }
        }
        //parseout.write(e("<Stmt>\n");
        return Stmt;
    }
}
