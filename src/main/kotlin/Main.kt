package cn.edu.nju.cs

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File

object Main {

    //A hack that allows the Online Judge to recognize the program entry
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 1) {
            System.err.println("Error: Only one argument is allowed: the path of MiniJava file.")
            throw RuntimeException()
        }

        val mjFile = File(args[0])
        run(mjFile)
    }
}

fun run(mjFile: File) {
    val input = CharStreams.fromFileName(mjFile.absolutePath)
    val lexer = MiniJavaLexer(input)
    val tokenStream = CommonTokenStream(lexer)
    val parser = MiniJavaParser(tokenStream)
    val pt: ParseTree = parser.compilationUnit()

    // TODO
    // Implement Your Own Visitor by extending MiniJavaParserBaseVisitor, then replace the following 'MiniJavaParserBaseVisitor<>'
    // For example: new YourVisitor().visit(pt);
    // Docs: https://box.nju.edu.cn/f/d4346b65c98743fe8208/
    MiniJavaParserBaseVisitor<Any>().visit(pt)
}