package cn.edu.nju.cs


sealed class MiniJavaObject(var value: Any):Nameable {

    open fun valueToString() = value.toString()

    class MJInt(value: Int = 0):MiniJavaObject(value), Numerical {
        override fun getValue(): Int {
            return value as Int
        }

        override fun setValue(value: Int) {
            this.value=value
        }

        override fun getName() = "int"
    }

    class MJBoolean(value: Boolean = false):MiniJavaObject(value){
        override fun getName() = "boolean"

        var booleanValue: Boolean
            get() = value as Boolean
            set(value) {
                this.value = value
            }
    }

    class MJChar(value: Byte = 0):MiniJavaObject(value), Numerical {
        override fun getValue(): Int {
            return (value as Byte).toInt()
        }

        override fun setValue(value: Int) {
            this.value=value
        }

        override fun getName() = "char"


        override fun valueToString() = (value as Byte).toInt().toChar().toString()
    }

    class MJString(value: String = ""):MiniJavaObject(value){
        override fun getName() = "string"
        var stringValue: String
            get() = value as String
            set(value) {
                this.value = value
            }
    }

    data object MJNull : MiniJavaObject(0){
        override fun getName() = "null"
    }

    class MJTypeName(value: String) : MiniJavaObject(value){
        override fun getName() = value as String
    }
}