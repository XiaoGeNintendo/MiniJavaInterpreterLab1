package cn.edu.nju.cs

class MiniJavaSyntaxErrorException:RuntimeException {
    constructor(message: String):super(message)
    constructor(message: String, cause: Throwable):super(message, cause)
    constructor(cause: Throwable):super(cause)
    constructor():super()
}