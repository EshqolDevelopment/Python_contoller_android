package com.eshqol.pythoncontoller123


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest

fun Any.toast(activity: Activity){
    Toast.makeText(activity, this.toString(), Toast.LENGTH_SHORT).show()
}

fun Any.snack(activity: Activity, duration: Int = Snackbar.LENGTH_SHORT, buttonText: String = "", func: () -> Unit = {}, top: Boolean = false){
    val parentLayout = activity.findViewById<View>(android.R.id.content)

    val snack = Snackbar.make(parentLayout, this.toString(), duration)
    if(buttonText.isNotEmpty()){
        snack.setAction(buttonText) {
            func()
            snack.dismiss()
        }
    }

    if (!top)
        snack.show()
    else {
        val view = snack.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        snack.show()
    }
}

fun ByteArray.toHex() = joinToString("") { eachByte -> "%02x".format(eachByte) }

fun sha256(message: String) = MessageDigest.getInstance("SHA-256").digest(message.toByteArray()).toHex()

@Suppress("UNCHECKED_CAST")
fun startNavigation(activity: Activity, currentID: Int) {
    val bottomNavigationView = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    bottomNavigationView.selectedItemId = currentID

    fun changePage(intent: Intent): Boolean {
        if (intent.component?.className == activity::class.java.name) return false
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.activity_show, R.anim.activity_hide)
        activity.finish()
        return false
    }

    val idToActivity = hashMapOf(
        R.id.documentation to Intent(activity, Documentation::class.java),
        R.id.home to Intent(activity, MainActivity::class.java),
        R.id.pc to {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.eshqol.com/python-controller"))
            activity.startActivity(intent)
            false
        }
    )

    bottomNavigationView.setOnItemSelectedListener {
        val intent = idToActivity[it.itemId]!!
        if (intent is Intent) changePage(intent)
        else (intent as () -> Boolean)()
        true
    }
}

class Helper {
    private val database = Firebase.database
    private val orangeWords = arrayOf(
        "False",
        "None",
        "True",
        "and",
        "as",
        "assert",
        "async",
        "await",
        "break",
        "class",
        "continue",
        "def",
        "del",
        "elif",
        "else",
        "except",
        "finally",
        "for",
        "from",
        "global",
        "if",
        "import",
        "in",
        "is",
        "lambda",
        "nonlocal",
        "not",
        "or",
        "pass",
        "raise",
        "return",
        "try",
        "while",
        "with",
        "yield"
    )
    private val purpleWords = arrayOf(
        "self",
        "__init__",
        "abs",
        "all",
        "any",
        "ascii",
        "bin",
        "bool",
        "bytearray",
        "bytes",
        "callable",
        "chr",
        "classmethod",
        "compile",
        "complex",
        "delattr",
        "dict",
        "dir",
        "divmod",
        "enumerate",
        "eval",
        "exec",
        "filter",
        "float",
        "format",
        "frozenset",
        "getattr",
        "globals",
        "hasattr",
        "hash",
        "help",
        "hex",
        "id",
        "input",
        "int",
        "isinstance",
        "issubclass",
        "iter",
        "len",
        "list",
        "locals",
        "map",
        "max",
        "memoryview",
        "min",
        "next",
        "object",
        "oct",
        "open",
        "ord",
        "pow",
        "print",
        "property",
        "range",
        "repr",
        "reversed",
        "round",
        "set",
        "setattr",
        "slice",
        "sorted",
        "staticmethod",
        "str",
        "sum",
        "super",
        "tuple",
        "type",
        "vars",
        "zip",
        "breakpoint",
        "str",
        "int",
        "float",
        "list",
        "sort"
    )

    private val orangeWordsJS = arrayOf("abstract", "arguments", "await*", "boolean" ,"break", "byte", "case", "catch", "char", "class*", "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum*", "eval", "export*", "extends*", "false", "final" ,"finally", "float", "for", "function", "goto", "if", "implements", "import*", "in", "instanceof", "int", "interface", "let*", "let", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "super*", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with", "yield")
    private val purpleWordsJS = arrayOf(
        "console",
        "length",
        "Math"
    )
    private val yellowColorsJS = arrayOf("toExponential", "toFixed", "toLocaleString", "toPrecision", "toString", "valueOf", "toSource", "toString", "valueOf", "charAt", "charCodeAt", "concat", "indexOf", "lastIndexOf", "localeCompare", "match", "replace", "replaceAll", "search", "slice", "split", "substr", "substring", "toLocaleLowerCase", "toLocaleUpperCase", "toLowerCase", "toString", "toUpperCase", "valueOf", "anchor", "big", "blink", "bold", "fixed", "fontcolor", "fontsize", "italics", "link", "small", "strike", "sub", "sup", "concat", "every", "filter", "forEach", "indexOf", "join", "lastIndexOf", "map", "pop", "push", "reduce", "reduceRight", "reverse", "shift", "slice", "some", "toSource", "sort", "splice", "toString", "unshift", "Date", "getDate", "getDay", "getFullYear", "getHours", "getMilliseconds", "getMinutes", "getMonth", "getSeconds", "getTime", "getTimezoneOffset", "getUTCDate", "getUTCDay", "getUTCFullYear", "getUTCHours", "getUTCMilliseconds", "getUTCMinutes", "getUTCMonth", "getUTCSeconds", "getYear", "setDate", "setFullYear", "setHours", "setMilliseconds", "setMinutes", "setMonth", "setSeconds", "setTime", "setUTCDate", "setUTCFullYear", "setUTCHours", "setUTCMilliseconds", "setUTCMinutes", "setUTCMonth", "setUTCSeconds", "setYear", "toDateString", "toGMTString", "toLocaleDateString", "toLocaleFormat", "toLocaleString", "toLocaleTimeString", "toSource", "toString", "toTimeString", "toUTCString", "valueOf", "Date.parse", "Date.UTC", "abs", "acos", "asin", "atan", "atan2", "ceil", "cos", "exp", "floor", "log", "max", "min", "pow", "random", "round", "sin", "sqrt", "tan", "toSource", "exec", "test", "toSource", "toString")

    private val numbers = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    private var last = 0
    private var deletion = 0
    private var stopRefresh = false

    private var currentLanguage = "Python"
    private var pythonText = ""
    private var jsText = ""

    private val orangeWords1 = {
        when (currentLanguage) {
            "Python" -> orangeWords
            else -> orangeWordsJS
        }
    }
    private val purpleWords1 = {
        when (currentLanguage) {
            "Python" -> purpleWords
            else -> purpleWordsJS
        }
    }
    private val yellowColors1 = {
        when (currentLanguage) {
            "Python" -> arrayOf()
            else -> yellowColorsJS
        }
    }

    private fun refreshColorOnce(editText: EditText) {
        last = 0
        var txt = editText.text.toString()
        if (txt.last() != ' ')
            txt += " "
        if (txt.length < deletion || (txt.last() != ' ' && txt.last() != '\n')) {
            deletion = txt.length

        } else {
            deletion = txt.length
            val spannable = SpannableString(txt)
            val mousePos = editText.selectionEnd
            val orangeWords = orangeWords1()
            val purpleWords = purpleWords1()
            val numbers = numbers
            val functionNames = arrayListOf<String>()
            var keepGreen = false
            var note = false

            try {
                val mouseLine = getCurrentCursorLine(editText)
                val lines = txt.split('\n')
                var spaceCount = 0
                for (c in lines[mouseLine - 1].toCharArray()) {
                    if (c == ' ') {
                        spaceCount++
                    } else break
                }
                val textFieldInputConnection = BaseInputConnection(editText, true)
                if (txt[mousePos - 2] == ':' && txt[mousePos - 1] == '\n') {
                    for (i in 0..spaceCount + 3) {
                        textFieldInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
                    }
                }
                else if (txt[mousePos - 1] == '\n')
                    for (i in 0 until spaceCount) {
                        textFieldInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
                    }


                val string = txt.split("\n")
                for (line in string) {
                    val strings = line.split(" ")
                    for (i in strings.indices) {
                        if (strings[i] in setOf("def") || strings[i] in setOf("function")) {
                            val stt = strings[i + 1].split("(")
                            functionNames.add(stt[0])
                        }
                    }
                }

                for (words in numbers) {
                    if (txt.contains(words)) {
                        for (i in txt.indices) {
                            if (txt[i] in numbers) {
                                var v = i
                                while (txt[v - 1] in numbers) {
                                    v--
                                }
                                if ((txt[v - 1] in numbers) || (txt[v - 1] in " .[=:(,+-*/<>%\n"))
                                    spannable.setSpan(
                                        ForegroundColorSpan(Color.parseColor("#71a6d2")),
                                        i,
                                        i + 1,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                            }
                        }
                    }
                }

                for (words in orangeWords) {
                    if (txt.contains(words)) {

                        for (i in 0 until txt.length - words.length) {

                            if (txt.substring(i, i+words.length) == words && txt[i+words.length] in "+-*/=,([: )]\n" && (i == 0 || txt[i-1] in "+-*/=,([: )]\n")){
                                spannable.setSpan(ForegroundColorSpan(Color.parseColor("#cb6b2e")), i, i + words.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }

                        }
                    }
                }

                for (word in purpleWords) {
                    if (txt.contains(word)) {
                        for (i in 0 until txt.length - word.length) {
                            if (txt.substring(
                                    i,
                                    i + word.length
                                ) == word && txt[i + word.length] in ".+-*/,([: )]=<>\n" && (i == 0 || txt[i - 1] in ".+-*/,([: )]=<>\n")
                            ) {
                                spannable.setSpan(
                                    ForegroundColorSpan(Color.parseColor("#a020f0")),
                                    i,
                                    i + word.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                    }
                }

                for (word in yellowColors1()) {
                    if (txt.contains(word)) {
                        for (i in 1..txt.length - word.length) {
                            if (txt.substring(i, i + word.length) == word && txt[i-1] == '.') {
                                spannable.setSpan(
                                    ForegroundColorSpan(Color.parseColor("#ffdd00")),
                                    i,
                                    i + word.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                spannable.setSpan(
                                    android.text.style.StyleSpan(android.graphics.Typeface.NORMAL),
                                    i,
                                    i + word.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                    }
                }


                for (word in functionNames) {
                    if (txt.contains(word)) {
                        for (i in 0..txt.length - word.length) {
                            if (txt.substring(i, i + word.length) == word && txt[i + word.length] == '(') {
                                spannable.setSpan(
                                    ForegroundColorSpan(Color.parseColor("#FFFF00")),
                                    i,
                                    i + word.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                    }
                }

                if (txt.contains('"') || txt.contains('\'')) {
                    for (i in txt.indices) {
                        if (txt[i] == '"' || txt[i] == '\'') {
                            keepGreen = !keepGreen
                        }
                        if (keepGreen) {
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#00FF00")),
                                i,
                                i + 2,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }


                for (i in txt.indices) {
                    if (txt[i] == '#' || (txt[i] == '/' && txt[i+1] == '/')) {
                        note = true
                    } else if (txt[i] == '\n') {
                        note = false
                    }
                    if (note) {
                        spannable.setSpan(
                            ForegroundColorSpan(Color.parseColor("#909090")),
                            i,
                            i + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }


            } catch (_: Exception) { }

            editText.text.clear()
            editText.append(spannable)
            stopRefresh = true

            editText.setSelection(mousePos)
        }
    }

    fun refreshColors(editText: EditText){
        editText.addTextChangedListener {
            val txt1 = editText.text.toString()

            if (!stopRefresh){
                when (currentLanguage){
                    "Python" -> pythonText = txt1
                    else -> jsText = txt1
                }

                if (currentLanguage == "Python"){
                    try{
                        val mousePos = editText.selectionEnd
                        val txt = editText.text.toString()
                        val lastLetter = txt[mousePos-1]
                        if (txt.length < deletion) {
                            deletion = txt.length
                            try {
                                val mouseLine = getCurrentCursorLine(editText)
                                var charInTheWay = false
                                val lines = txt.split('\n')

                                var spaceCount = 0
                                val currLine = lines[mouseLine]
                                if (currLine.trim().isEmpty() && txt[mousePos-1] != '\n' && mouseLine!=0){
                                    var lastL = txt[mousePos-1]
                                    while (lastL != '\n'){
                                        spaceCount++
                                        lastL = txt[mousePos-spaceCount]
                                    }
                                }else{
                                    charInTheWay = true
                                }

                                if (!charInTheWay && (spaceCount)%4==0){
                                    val textFieldInputConnection = BaseInputConnection(editText, true)
                                    for (i in 0..2) {
                                        textFieldInputConnection.sendKeyEvent(
                                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                                        )
                                    }
                                }
                            } catch (_: Exception){}
                        } else if (lastLetter !in setOf(' ', '\n', '(', ')', '"', '\'', ',')){
                            deletion = txt.length
                        }
                        else {
                            try {

                                val mouseLine = getCurrentCursorLine(editText)
                                val lines = txt.split('\n')
                                var spaceCount = 0
                                for (x in lines[mouseLine - 1].toCharArray()) {
                                    if (x == ' ') {
                                        spaceCount++
                                    } else break
                                }
                                val textFieldInputConnection = BaseInputConnection(editText, true)
                                if (txt[mousePos - 2] == ':' && txt[mousePos - 1] == '\n') {
                                    for (i in 0..spaceCount + 3) {
                                        textFieldInputConnection.sendKeyEvent(
                                            KeyEvent(
                                                KeyEvent.ACTION_DOWN,
                                                KeyEvent.KEYCODE_SPACE
                                            )
                                        )
                                    }
                                } else if (txt[mousePos - 1] == '\n') {
                                    for (i in 0 until spaceCount) {
                                        textFieldInputConnection.sendKeyEvent(
                                            KeyEvent(
                                                KeyEvent.ACTION_DOWN,
                                                KeyEvent.KEYCODE_SPACE
                                            )
                                        )
                                    }
                                }
                                else {
                                    stopRefresh = true
                                    refreshColorOnce(editText)
                                    stopRefresh = false
                                }

                            } catch (_: Exception) {}
                        }
                        editText.setSelection(mousePos)
                    } catch (_: Exception) {}
                }
                else{
                    try{
                        var brackets = 0

                        val mousePos = editText.selectionEnd
                        val txt = editText.text.toString()
                        val lastLetter = txt[mousePos-1]
                        if (txt.length < deletion) {
                            deletion = txt.length
                            try {
                                val mouseLine =  getCurrentCursorLine(editText)
                                var charInTheWay = false
                                val lines = txt.split('\n')
                                var spaceCount = 0
                                for (x in lines[mouseLine].toCharArray()) {
                                    if (x == ' ') {
                                        spaceCount++
                                    } else {
                                        charInTheWay = true
                                        break
                                    }
                                }
                                if (!charInTheWay && (spaceCount+1)%4==0){
                                    val textFieldInputConnection = BaseInputConnection(editText, true)
                                    for (i in 0..2) {
                                        textFieldInputConnection.sendKeyEvent(
                                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                                        )
                                    }
                                }
                            } catch (_: Exception){}
                        } else if (lastLetter != ' ' && lastLetter != '\n' && lastLetter != '(' && lastLetter != ')' && lastLetter != '"' && lastLetter != '\'' && lastLetter != ','){
                            deletion = txt.length
                        }
                        else {
                            try {
                                val mouseLine =  getCurrentCursorLine(editText)
                                val lines = txt.split('\n')
                                var spaceCount = 0
                                for (x in lines[mouseLine - 1].toCharArray()) {
                                    if (x == ' ') {
                                        spaceCount++
                                    } else break
                                }
                                val textFieldInputConnection = BaseInputConnection(editText, true)
                                if (txt[mousePos - 2] == '{' && txt[mousePos - 1] == '\n')  {
                                    if (txt.count{it == '{'} != txt.count{it == '}'}) {
                                        editText.setText(curlyBrackets(txt, mousePos-2, spaceCount))
                                        brackets = 4 + spaceCount
                                    }
                                    else{
                                        for (i in 0..spaceCount + 3) {
                                            textFieldInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
                                        }
                                    }

                                }
                                else if (txt[mousePos - 1] == '\n') {
                                    for (i in 0 until spaceCount) {
                                        textFieldInputConnection.sendKeyEvent(
                                            KeyEvent(
                                                KeyEvent.ACTION_DOWN,
                                                KeyEvent.KEYCODE_SPACE
                                            )
                                        )
                                    }
                                }
                                else {
                                    stopRefresh = true
                                    refreshColorOnce(editText)
                                    stopRefresh = false
                                }
                            } catch (_: Exception) {}
                        }
                        editText.setSelection(mousePos + brackets)
                        if (brackets != 0) {
                            refreshColorOnce(editText)
                            stopRefresh = false
                        }
                    } catch (_: Exception){}

                }

            }
        }
    }


    fun refreshColorOnStart(editText: EditText){
        try {
            var txt = editText.text.toString()
            if (txt.last() != ' ')
                txt += " "

            val spannable = SpannableString(txt)
            val orangeWords = orangeWords1()
            val purpleWords = purpleWords1()
            val numbers = numbers
            val functionNames = arrayListOf<String>()
            var keepGreen = false
            var note = false


            val string = txt.split("\n")
            for (line in string) {
                val strings = line.split(" ")
                for (i in strings.indices) {
                    if (strings[i] in setOf("def") || strings[i] in setOf("function")) {
                        val stt = strings[i + 1].split("(")
                        functionNames.add(stt[0])
                    }
                }
            }

            for (words in numbers) {
                if (txt.contains(words)) {
                    for (i in txt.indices) {
                        if (txt[i] in numbers) {
                            var v = i
                            while (txt[v - 1] in numbers) {
                                v--
                            }
                            if ((txt[v - 1] in numbers) || (txt[v - 1] in " .[=:(,+-*/<>%\n"))
                                spannable.setSpan(
                                    ForegroundColorSpan(Color.parseColor("#71a6d2")),
                                    i,
                                    i + 1,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                        }
                    }
                }
            }

            for (words in orangeWords) {
                if (txt.contains(words)) {

                    for (i in 0 until txt.length - words.length) {

                        if (txt.substring(i, i+words.length) == words && txt[i+words.length] in "+-*/=,([: )]\n" && (i == 0 || txt[i-1] in "+-*/=,([: )]\n")){
                            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#cb6b2e")), i, i + words.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                    }
                }
            }

            for (word in purpleWords) {
                if (txt.contains(word)) {
                    for (i in 0 until txt.length - word.length) {
                        if (txt.substring(
                                i,
                                i + word.length
                            ) == word && txt[i + word.length] in ".+-*/,([: )]=<>\n" && (i == 0 || txt[i - 1] in ".+-*/,([: )]=<>\n")
                        ) {
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#a020f0")),
                                i,
                                i + word.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
            }

            for (word in yellowColors1()) {
                if (txt.contains(word)) {
                    for (i in 1..txt.length - word.length) {
                        if (txt.substring(i, i + word.length) == word && txt[i-1] == '.') {
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#ffdd00")),
                                i,
                                i + word.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            spannable.setSpan(
                                android.text.style.StyleSpan(android.graphics.Typeface.NORMAL),
                                i,
                                i + word.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
            }

            for (word in functionNames) {
                if (txt.contains(word)) {
                    for (i in 0..txt.length - word.length) {
                        if (txt.substring(i, i + word.length) == word && txt[i + word.length] == '(') {
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#FFFF00")),
                                i,
                                i + word.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
            }

            if (txt.contains('"') || txt.contains('\'')) {
                for (i in txt.indices) {
                    if (txt[i] == '"' || txt[i] == '\'') {
                        keepGreen = !keepGreen
                    }
                    if (keepGreen) {
                        spannable.setSpan(
                            ForegroundColorSpan(Color.parseColor("#00FF00")),
                            i,
                            i + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }

            for (i in txt.indices) {
                if (txt[i] == '#' || (txt[i] == '/' && txt[i+1] == '/')) {
                    note = true
                } else if (txt[i] == '\n') {
                    note = false
                }
                if (note) {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#909090")),
                        i,
                        i + 2,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            editText.setText(spannable)

        } catch (_: Exception) { }
    }


    private fun getCurrentCursorLine(editText: EditText): Int {
        val selectionStart = Selection.getSelectionStart(editText.text)
        val layout = editText.layout
        return if (selectionStart != -1) {
            layout.getLineForOffset(selectionStart)
        } else -1
    }

    fun closeKeyboard(activity: Activity){
        activity.currentFocus?.let { view1 ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view1.windowToken, 0)
        }
    }

    private fun curlyBrackets(text: String, mousePos: Int, spaceCount: Int): String {
        val y = (1..spaceCount).joinToString("") { " " }
        return "${text.substring(0, mousePos + 1)}\n$y    \n$y}${text.substring(mousePos + 2)}"
    }


    fun writeFirebase( path: String, text: String): Task<Void> {
        val ref = database.getReference(path)
        return ref.setValue(text)
    }


    fun read(path: String): Task<DataSnapshot> {
        // read once from the firebase realtime database
        return database.getReference(path).get()
    }


    fun listenForChanges(path: String, func: (DataSnapshot) -> Unit) {
        // read once from the firebase realtime database
         database.getReference(path).addValueEventListener(object : ValueEventListener {
             override fun onDataChange(dataSnapshot: DataSnapshot) {
                 func(dataSnapshot)
             }

             override fun onCancelled(error: DatabaseError) {
                 // Failed to read value
                 Log.w("Error", "Failed to read value.", error.toException())
             }
         })
    }


    fun readLocalStorage(activity: Activity, name: String, default: String = "null"): String {
        val sharedPreference = activity.getSharedPreferences("SaveLocal", Context.MODE_PRIVATE)
        return sharedPreference.getString(name, default).toString()
    }


    fun writeLocalStorage(activity: Activity, name: String, content: String) {
        val sharedPreference = activity.getSharedPreferences("SaveLocal", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()

        editor.putString(name, content)
        editor.apply()
    }

}
