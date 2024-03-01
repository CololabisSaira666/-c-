package lexer;

import java.util.HashMap;
import static lexer.Tag.*;

public class Token {
    public Tag type;
    String str;
    int num;
    int lineNum;
    char ch;
    public boolean haveErrorA;
    private static final HashMap<Tag, String> code = new HashMap<>();

    static {
        code.put(MAINTK, "main");
        code.put(CONSTTK, "const");
        code.put(INTTK, "int");
        code.put(BREAKTK, "break");
        code.put(CONTINUETK, "continue");
        code.put(IFTK, "if");
        code.put(ELSETK, "else");
        code.put(AND, "&&");
        code.put(OR, "||");
        code.put(FORTK, "for");
        code.put(GETINTTK, "getint");
        code.put(PRINTFTK, "printf");
        code.put(RETURNTK, "return");
        code.put(VOIDTK, "void");
        code.put(LEQ, "<=");
        code.put(GEQ, ">=");
        code.put(EQL, "==");
        code.put(NEQ, "!=");
        code.put(NOT, "!");
        code.put(PLUS, "+");
        code.put(MINU, "-");
        code.put(MULT, "*");
        code.put(DIV, "/");
        code.put(MOD, "%");
        code.put(LSS, "<");
        code.put(GRE, ">");
        code.put(ASSIGN, "=");
        code.put(SEMICN, ";");
        code.put(COMMA, ",");
        code.put(LPARENT, "(");
        code.put(RPARENT, ")");
        code.put(LBRACK, "[");
        code.put(RBRACK, "]");
        code.put(LBRACE, "{");
        code.put(RBRACE, "}");
    }

    public Token(String str, Tag t, int lineNum) {
        this.str = str;
        this.type = t;
        this.lineNum = lineNum;
    }

    public Token(String str, boolean err, int lineNum) {
        this.str = str;
        this.type = STRCON;
        this.haveErrorA = err;
        this.lineNum = lineNum;
    }

    public Token (int num, int lineNum) {
        this.num = num;
        this.type = INTCON;
        this.lineNum = lineNum;
    }

    public String getStr() {
        return str;
    }

    public int getNum() {
        return this.num;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String toString() {
        if (type == IDENFR || type == STRCON) {
            return type + " " + str + "\n";
        } else if (type == INTCON) {
            return type + " " + num + "\n";
        } else {
            return type + " " + code.get(type) + "\n";
        }
    }
}
