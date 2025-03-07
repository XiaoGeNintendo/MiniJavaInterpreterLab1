package cn.edu.nju.cs

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor

class MyVisitor: AbstractParseTreeVisitor<MiniJavaObject>(), MiniJavaParserVisitor<MiniJavaObject> {

    private val symbolTable = HashMap<String, ArrayList<MiniJavaObject>>()

    /**
     * List of variables in this scope
     */
    private val scopeList = ArrayList<ArrayList<String>>()


    override fun visitCompilationUnit(ctx: MiniJavaParser.CompilationUnitContext): MiniJavaObject {
        return visit(ctx.block())
    }

    override fun visitBlock(ctx: MiniJavaParser.BlockContext): MiniJavaObject {
        //add the scope
        scopeList.add(ArrayList())

        for(i in ctx.blockStatement()){
            visit(i)
        }

        //debug print the scope
        for(i in scopeList.last.sorted()){
            val v=symbolTable[i]!!.last()
            if(v is MiniJavaObject.MJChar){
                println("$i : (${v.getName()}) ${v.getValue().toChar()}")
            }else{
                println("$i : (${v.getName()}) ${v.value}")
            }
        }

        //pop the scope
        for(i in scopeList.last){
            symbolTable[i]!!.removeLast()
        }
        scopeList.removeLast()

        return MiniJavaObject.MJNull
    }

    override fun visitBlockStatement(ctx: MiniJavaParser.BlockStatementContext): MiniJavaObject {
        ctx.localVariableDeclaration()?.let {
            return visit(ctx.localVariableDeclaration())
        }
        ctx.statement()?.let {
            return visit(ctx.statement())
        }

        throw IllegalStateException("Unknown block statement")
    }

    fun registerVariable(name: String, type: String){
        when (type) {
            "int" -> {
                registerVariable(name, MiniJavaObject.MJInt())
            }
            "boolean" -> {
                registerVariable(name, MiniJavaObject.MJBoolean())
            }
            "string" -> {
                registerVariable(name, MiniJavaObject.MJString())
            }
            "char" -> {
                registerVariable(name, MiniJavaObject.MJChar())
            }
            else -> {
                throw MiniJavaSyntaxErrorException("Unknown type: $type")
            }
        }
    }

    fun registerVariable(name: String, value: MiniJavaObject, typeRef:String=""){
        val scope=scopeList.last
        if(scope.contains(name)){
            throw MiniJavaSyntaxErrorException("Duplicate variable name: $name")
        }

        scope.add(name)
        if(name !in symbolTable){
            symbolTable[name]=ArrayList()
        }

        if(typeRef!="" && typeRef!=value.getName()){
            if(value.getName()=="int" && typeRef=="char"){
                symbolTable[name]!!.add(MiniJavaObject.MJChar((value.value as Int).toByte()))
                return
            }
            if(value.getName()=="char" && typeRef=="int"){
                symbolTable[name]!!.add(MiniJavaObject.MJInt((value.value as Byte).toInt()))
                return
            }
            throw MiniJavaSyntaxErrorException("Type mismatch: $typeRef != ${value.getName()}")
        }

        symbolTable[name]!!.add(value)
    }

    override fun visitLocalVariableDeclaration(ctx: MiniJavaParser.LocalVariableDeclarationContext): MiniJavaObject {
        val name=ctx.identifier().text

        ctx.expression()?.let {
            val value=visit(ctx.expression())
            registerVariable(name,value,ctx.primitiveType().text)
            return MiniJavaObject.MJNull
        }

        registerVariable(name,ctx.primitiveType().text)
        return MiniJavaObject.MJNull
    }

    override fun visitStatement(ctx: MiniJavaParser.StatementContext): MiniJavaObject {
        ctx.block()?.let{
            return visit(ctx.block())
        }
        ctx.expression()?.let{
            return visit(ctx.expression())
        }

        return MiniJavaObject.MJNull
    }

    override fun visitExpression(ctx: MiniJavaParser.ExpressionContext): MiniJavaObject {
        ctx.primary()?.let{
            return visit(ctx.primary())
        }
        ctx.postfix?.let{
            val exp=visit(ctx.expression(0))

            when(exp){
                is Numerical -> {
                    when(it.text){
                        "++" -> {
                            exp.setValue(exp.getValue()+1)
                            //We can do this because x++ returns value instead of variable
                            return MiniJavaObject.MJInt(exp.getValue()-1)
                        }
                        "--" -> {
                            exp.setValue(exp.getValue()-1)
                            return MiniJavaObject.MJInt(exp.getValue()+1)
                        }
                        else -> {
                            throw MiniJavaSyntaxErrorException("Unknown postfix operator: ${it.text}")
                        }
                    }
                }
                else -> {
                    throw MiniJavaSyntaxErrorException("Cannot apply ${it.text} to ${exp::class.simpleName}")
                }
            }
        }

        ctx.prefix?.let {
            val exp=visit(ctx.expression(0))

            when(exp){
                is Numerical -> {
                    when(it.text){
                        "+" -> {
                            return MiniJavaObject.MJInt(exp.getValue())
                        }
                        "-" -> {
                            return MiniJavaObject.MJInt(-exp.getValue())
                        }
                        "++" -> {
                            exp.setValue(exp.getValue()+1)
                            return MiniJavaObject.MJInt(exp.getValue())
                        }
                        "--" -> {
                            exp.setValue(exp.getValue()-1)
                            return MiniJavaObject.MJInt(exp.getValue())
                        }
                        "~" -> {
                            return MiniJavaObject.MJInt(exp.getValue().inv())
                        }
                        else -> {
                            throw MiniJavaSyntaxErrorException("Unknown postfix operator: ${it.text}")
                        }
                    }
                }
                is MiniJavaObject.MJBoolean -> {
                    when(it.text){
                        "not" -> {
                            return MiniJavaObject.MJBoolean(!exp.booleanValue)
                        }
                        else -> {
                            throw MiniJavaSyntaxErrorException("Unknown postfix operator: ${it.text}")
                        }
                    }
                }
                else -> {
                    throw MiniJavaSyntaxErrorException("Cannot apply ${it.text} to ${exp::class.simpleName}")
                }
            }
        }

        ctx.primitiveType()?.let{
            val exp=visit(ctx.expression(0))

            if(exp !is Numerical){
                throw MiniJavaSyntaxErrorException("Cannot cast ${exp::class.simpleName} to ${it.text} (NON NUM)")
            }

            when(it.text){
                "int" -> {
                    return MiniJavaObject.MJInt(exp.getValue())
                }
                "char" -> {
                    return MiniJavaObject.MJChar(exp.getValue().toByte())
                }
                else -> {
                    throw MiniJavaSyntaxErrorException("Cannot cast ${exp::class.simpleName} to ${it.text} (BAD TYPE)")
                }
            }
        }

        ctx.bop?.let {
            ctx.expression(2)?.let{
                //It's a ?:
                val cond=visit(ctx.expression(0)) as MiniJavaObject.MJBoolean
                if(cond.booleanValue){
                    return visit(ctx.expression(1))
                }else{
                    return visit(ctx.expression(2))
                }
            }

            var rawBop=it.text
            var assign = false

            if(rawBop=="and"){
                val a=visit(ctx.expression(0)) as MiniJavaObject.MJBoolean

                if(!a.booleanValue){
                    return MiniJavaObject.MJBoolean(false)
                }

                val b=visit(ctx.expression(1)) as MiniJavaObject.MJBoolean
                return MiniJavaObject.MJBoolean(a.booleanValue && b.booleanValue)
            }
            if(rawBop=="or"){
                val a=visit(ctx.expression(0)) as MiniJavaObject.MJBoolean

                if(a.booleanValue){
                    return MiniJavaObject.MJBoolean(true)
                }

                val b=visit(ctx.expression(1)) as MiniJavaObject.MJBoolean
                return MiniJavaObject.MJBoolean(a.booleanValue || b.booleanValue)
            }

            val a=visit(ctx.expression(0))
            val b=visit(ctx.expression(1))


            if(rawBop=="="){
                //real assignment
                a.value=b.value
                return a
            }

            if(rawBop.endsWith("=") && rawBop !in arrayOf("==","!=",">=","<=")){
                //assignment
                assign=true
                rawBop=rawBop.dropLast(1)
            }

            val judgeEq=fun(a: MiniJavaObject, b: MiniJavaObject):MiniJavaObject.MJBoolean{
                if(a is Numerical && b is Numerical){
                    return MiniJavaObject.MJBoolean(a.getValue()==b.getValue())
                }
                if(a is MiniJavaObject.MJBoolean && b is MiniJavaObject.MJBoolean){
                    return MiniJavaObject.MJBoolean(a.booleanValue==b.booleanValue)
                }
                if(a is MiniJavaObject.MJString && b is MiniJavaObject.MJString){
                    return MiniJavaObject.MJBoolean(a.stringValue==b.stringValue)
                }
                return MiniJavaObject.MJBoolean(false)
            }
            if(rawBop=="=="){
                return judgeEq(a,b)
            }
            if(rawBop=="!="){
                return MiniJavaObject.MJBoolean(!judgeEq(a,b).booleanValue)
            }

            if(a is MiniJavaObject.MJString || b is MiniJavaObject.MJString){
                assert(rawBop=="+")

                if(assign){
                    a.value=a.valueToString()+b.valueToString()
                    return a
                }
                return MiniJavaObject.MJString((a.valueToString()+b.valueToString()))
            }
            //should only have binary arithmetic operators
            val x=(a as Numerical).getValue()
            val y=(b as Numerical).getValue()

            val bres=when(rawBop){
                "<" -> x<y
                "<=" -> x<=y
                ">" -> x>y
                ">=" -> x>=y
                else -> null
            }

            if(bres!=null){
                return MiniJavaObject.MJBoolean(bres)
            }

            val result=when(rawBop){
                "+" -> x+y
                "-" -> x-y
                "*" -> x*y
                "/" -> x/y
                "%" -> x%y
                "&" -> x and y
                "|" -> x or y
                "^" -> x xor y
                "<<" -> x shl y
                ">>" -> x shr y
                ">>>" -> x ushr y
                else -> throw MiniJavaSyntaxErrorException("Unknown binary operator: $rawBop")
            }

            //char+char --> int
            if(assign){
                a.value=result
                return a
            }
            return MiniJavaObject.MJInt(result)

        }

        throw IllegalStateException("Unknown expression: ${ctx.text}")
    }

    override fun visitPrimary(ctx: MiniJavaParser.PrimaryContext): MiniJavaObject {
        ctx.expression()?.let{
            return visit(ctx.expression())
        }
        ctx.literal()?.let{
            return visit(ctx.literal())
        }
        ctx.identifier()?.let{
            return visit(ctx.identifier())
        }
        throw IllegalStateException("Unknown primary")
    }

    override fun visitLiteral(ctx: MiniJavaParser.LiteralContext): MiniJavaObject {
        ctx.STRING_LITERAL()?.let{
            return MiniJavaObject.MJString(ctx.STRING_LITERAL().text.substring(1).dropLast(1))
        }
        ctx.CHAR_LITERAL()?.let{
            return MiniJavaObject.MJChar(ctx.CHAR_LITERAL().text[1].code.toByte())
        }
        ctx.DECIMAL_LITERAL()?.let{
            return MiniJavaObject.MJInt(ctx.DECIMAL_LITERAL().text.toInt())
        }
        ctx.BOOL_LITERAL()?.let{
            return MiniJavaObject.MJBoolean(ctx.BOOL_LITERAL().text.toBoolean())
        }
        throw IllegalStateException("Unknown literal: ${ctx.text}")
    }


    override fun visitIdentifier(ctx: MiniJavaParser.IdentifierContext): MiniJavaObject {
        val name=ctx.text
        if(name !in symbolTable){
            throw MiniJavaSyntaxErrorException("Unknown variable: $name")
        }

        return symbolTable[name]!!.last()
    }

    override fun visitPrimitiveType(ctx: MiniJavaParser.PrimitiveTypeContext): MiniJavaObject {
        return MiniJavaObject.MJTypeName(ctx.text)
    }

}