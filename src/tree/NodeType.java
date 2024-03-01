package tree;

public enum NodeType {
    LeafNode,
    CompUnit, Decl, ConstDecl, Btype, ConstDef, ConstInitVal,
    VarDecl, VarDef, InitVal, FuncDef, MainFuncDef,
    FuncType, FuncFParams, FuncFParam, Block, BlockItem,
    Stmt,
    ForStmt, Exp, Cond, LVal, PrimaryExp, Number,
    UnaryExp, UnaryOp, FuncRParams, MulExp, AddExp,
    RelExp, EqExp, LAndExp, LOrExp, ConstExp,
    FormatString, COMMA
}
