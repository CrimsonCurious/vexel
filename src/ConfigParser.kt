// ============================================================
//                         ConfigParser.kt
// ============================================================
package app.cc.vexel

/**
 * VCL - Vexel Config Language
 *
 * Supported values:
 * - String
 * - Int / Long
 * - Double
 * - Boolean
 * - Map<String, Any>
 * - List<Any>
 *
 * Commas are not required between entries or array values.
 */
object ConfigParser {

    // ========================================================
    // Public API
    // ========================================================

    fun parse(text: String): Map<String, Any> {
        return Parser(text).parseDocument()
    }

    // ========================================================
    // Tokens
    // ========================================================

    private enum class TokenType {
        IDENT,
        STRING,
        NUMBER,

        LBRACE,
        RBRACE,
        LBRACKET,
        RBRACKET,
        COLON,

        EOF
    }

    private data class Token(
        val type: TokenType,
        val value: String,
        val line: Int,
        val column: Int
    )

    // ========================================================
    // Lexer
    // ========================================================

    private class Lexer(private val text: String) {

        private var index = 0
        private var line = 1
        private var column = 1

        private fun atEnd(): Boolean =
            index >= text.length

        private fun peek(offset: Int = 0): Char? =
            text.getOrNull(index + offset)

        private fun advance(): Char {
            val char = text[index++]

            if (char == '\n') {
                line++
                column = 1
            } else {
                column++
            }

            return char
        }

        private fun error(message: String): Nothing {
            vexelThrow(ErrorCode.INVALID_CONFIG,
                "VCL error at line ${textFormat(line.toString(), Vexel.Color.Blue)}, column ${textFormat(column.toString(), Vexel.Color.Blue)}: $message"
            )
        }

        private fun skipIgnored() {
            while (!atEnd()) {
                when {
                    peek()!!.isWhitespace() -> advance()

                    peek() == '#' -> {
                        while (!atEnd() && peek() != '\n') {
                            advance()
                        }
                    }

                    else -> return
                }
            }
        }

        fun nextToken(): Token {
            skipIgnored()

            val startLine = line
            val startColumn = column

            if (atEnd()) {
                return Token(
                    TokenType.EOF, "", startLine, startColumn
                )
            }

            val char = peek()!!

            fun token(type: TokenType, value: String) =
                Token(type, value, startLine, startColumn)

            when (char) {
                '{' -> {
                    advance()
                    return token(TokenType.LBRACE, "{")
                }

                '}' -> {
                    advance()
                    return token(TokenType.RBRACE, "}")
                }

                '[' -> {
                    advance()
                    return token(TokenType.LBRACKET, "[")
                }

                ']' -> {
                    advance()
                    return token(TokenType.RBRACKET, "]")
                }

                ':' -> {
                    advance()
                    return token(TokenType.COLON, ":")
                }

                '"' -> return readString(
                    startLine, startColumn
                )
            }

            if (char.isLetter() || char == '_') {
                return readIdentifier(
                    startLine, startColumn
                )
            }

            if (char.isDigit() ||
                (char == '-' && peek(1)?.isDigit() == true)
            ) {
                return readNumber(
                    startLine, startColumn
                )
            }

            error("Unexpected character '$char'")
        }

        private fun readIdentifier(
            startLine: Int,
            startColumn: Int
        ): Token {
            val start = index

            while (!atEnd()) {
                val char = peek()!!

                if (char.isLetterOrDigit() ||
                    char == '_' ||
                    char == '-' ||
                    char == '.'
                ) {
                    advance()
                } else {
                    break
                }
            }

            return Token(
                TokenType.IDENT,
                text.substring(start, index),
                startLine,
                startColumn
            )
        }

        private fun readNumber(
            startLine: Int,
            startColumn: Int
        ): Token {
            val start = index

            if (peek() == '-') advance()

            while (peek()?.isDigit() == true) {
                advance()
            }

            if (peek() == '.') {
                advance()

                if (peek()?.isDigit() != true) {
                    error("Expected digits after decimal point")
                }

                while (peek()?.isDigit() == true) {
                    advance()
                }
            }

            if (peek() == 'e' || peek() == 'E') {
                advance()

                if (peek() == '+' || peek() == '-') {
                    advance()
                }

                if (peek()?.isDigit() != true) {
                    error("Expected exponent digits")
                }

                while (peek()?.isDigit() == true) {
                    advance()
                }
            }

            return Token(
                TokenType.NUMBER,
                text.substring(start, index),
                startLine,
                startColumn
            )
        }

        private fun readString(
            startLine: Int,
            startColumn: Int
        ): Token {
            advance() // Opening quote

            val result = StringBuilder()

            while (!atEnd()) {
                val char = advance()

                when (char) {
                    '"' -> return Token(
                        TokenType.STRING,
                        result.toString(),
                        startLine,
                        startColumn
                    )

                    '\\' -> {
                        if (atEnd()) {
                            error("Unterminated escape sequence")
                        }

                        when (val escaped = advance()) {
                            '"' -> result.append('"')
                            '\\' -> result.append('\\')
                            'n' -> result.append('\n')
                            'r' -> result.append('\r')
                            't' -> result.append('\t')
                            'b' -> result.append('\b')
                            'f' -> result.append('\u000C')

                            'u' -> {
                                val hex = StringBuilder()

                                repeat(4) {
                                    if (atEnd()) {
                                        error(
                                            "Incomplete Unicode escape"
                                        )
                                    }

                                    val digit = advance()

                                    if (digit.digitToIntOrNull(16) == null) {
                                        error(
                                            "Invalid Unicode escape"
                                        )
                                    }

                                    hex.append(digit)
                                }

                                result.append(
                                    hex.toString().toInt(16).toChar()
                                )
                            }

                            else -> error(
                                "Unknown escape sequence '\\$escaped'"
                            )
                        }
                    }

                    '\n', '\r' -> error(
                        "Unescaped newline inside string"
                    )

                    else -> result.append(char)
                }
            }

            error("Unterminated string")
        }
    }

    // ========================================================
    // Parser
    // ========================================================

    private class Parser(text: String) {

        private val lexer = Lexer(text)
        private var current = lexer.nextToken()

        private fun advance(): Token {
            val previous = current
            current = lexer.nextToken()
            return previous
        }

        private fun check(type: TokenType): Boolean =
            current.type == type

        private fun expect(type: TokenType): Token {
            if (!check(type)) {
                error(
                    "Expected ${type.name}, got ${current.type.name}"
                )
            }

            return advance()
        }

        private fun error(message: String): Nothing {
            throw IllegalArgumentException(
                "VCL error at line ${current.line}, " +
                    "column ${current.column}: $message"
            )
        }

        // Root document: entries without outer braces.
        fun parseDocument(): Map<String, Any> {
            val result = linkedMapOf<String, Any>()

            while (!check(TokenType.EOF)) {
                parseEntry(result)
            }

            return result
        }

        private fun parseEntry(
            target: MutableMap<String, Any>
        ) {
            val key = expect(TokenType.IDENT).value

            expect(TokenType.COLON)

            if (target.containsKey(key)) {
                error("Duplicate key '$key'")
            }

            target[key] = parseValue()
        }

        private fun parseObject(): Map<String, Any> {
            expect(TokenType.LBRACE)

            val result = linkedMapOf<String, Any>()

            while (!check(TokenType.RBRACE)) {
                if (check(TokenType.EOF)) {
                    error("Unterminated object; expected '}'")
                }

                parseEntry(result)
            }

            expect(TokenType.RBRACE)

            return result
        }

        private fun parseArray(): List<Any> {
            expect(TokenType.LBRACKET)

            val result = mutableListOf<Any>()

            while (!check(TokenType.RBRACKET)) {
                if (check(TokenType.EOF)) {
                    error("Unterminated array; expected ']'")
                }

                if (check(TokenType.COLON)) {
                    error("Unexpected ':' inside array")
                }

                result.add(parseValue())
            }

            expect(TokenType.RBRACKET)

            return result
        }

        private fun parseValue(): Any {
            return when (current.type) {
                TokenType.STRING ->
                    advance().value

                TokenType.NUMBER ->
                    parseNumber(advance().value)

                TokenType.IDENT -> {
                    when (current.value) {
                        "true" -> {
                            advance()
                            true
                        }

                        "false" -> {
                            advance()
                            false
                        }

                        else -> error(
                            "Expected a value, got '${current.value}'"
                        )
                    }
                }

                TokenType.LBRACE ->
                    parseObject()

                TokenType.LBRACKET ->
                    parseArray()

                else -> error(
                    "Expected a value, got ${current.type.name}"
                )
            }
        }

        private fun parseNumber(value: String): Number {
            if ('.' in value || 'e' in value || 'E' in value) {
                return value.toDoubleOrNull()
                    ?: error("Invalid number '$value'")
            }

            val number = value.toLongOrNull()
                ?: error("Integer is out of range: '$value'")

            return if (number in Int.MIN_VALUE..Int.MAX_VALUE) {
                number.toInt()
            } else {
                number
            }
        }
    }
}