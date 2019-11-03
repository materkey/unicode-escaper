package com.materkey.codepoints

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import java.io.StringWriter

class ToCodePointsConverter : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val selectedText = document.getText(TextRange.from(start, end - start))
        val result = escape(selectedText)

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, result)
        }
    }

    private fun escape(str: String): String {
        val out = StringWriter(str.length * 2)
        val size = str.length

        for (i in 0 until size) {
            val ch = str[i]
            when {
                ch.toInt() > 0xFFF -> out.write("\\u" + ch.toHexString())
                ch.toInt() > 0xFF -> out.write("\\u0" + ch.toHexString())
                ch.toInt() > 0x7F -> out.write("\\u00" + ch.toHexString())
                else -> out.write(ch.toInt())
            }
        }
        return out.toString()
    }

    private fun Char.toHexString(): String {
        return Integer.toHexString(toInt()).toUpperCase()
    }
}