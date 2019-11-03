package com.materkey.codepoints

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import java.io.StringWriter

class FromCodePointsConverter : AnAction() {
    companion object {
        const val CODE_HEX_LENGTH = 4
        const val HEX_RADIX = 16
        const val BACKSLASH_CODE_POINT_VALUE = 92
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val selectedText = document.getText(TextRange.from(start, end - start))
        val result = unescape(selectedText)

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, result)
        }
    }

    private fun unescape(str: String): String {
        val size = str.length
        val out = StringWriter(size)

        val unicode = StringBuffer(CODE_HEX_LENGTH)
        var hadSlash = false
        var inUnicode = false

        for (i in 0 until size) {
            val ch = str[i]
            if (inUnicode) {
                unicode.append(ch)
                if (unicode.length == CODE_HEX_LENGTH) {
                    try {
                        val value = unicode.toString().toInt(HEX_RADIX)
                        out.write(value.toChar().toInt())
                        unicode.setLength(0)
                        inUnicode = false
                        hadSlash = false
                    } catch (nfe: NumberFormatException) {
                        throw NumberFormatException("Unable to parse unicode value: $unicode")
                    }
                }
                continue
            }
            if (hadSlash) {
                // handle an escaped value
                hadSlash = false
                when (ch) {
                    'u' -> inUnicode = true
                    else -> out.write(ch.toInt())
                }
                continue
            } else if (ch == '\\') {
                hadSlash = true
                continue
            }
            out.write(ch.toInt())
        }
        if (hadSlash) {
            // case of a \ at the end of the
            out.write(BACKSLASH_CODE_POINT_VALUE)
        }

        return out.toString()
    }
}