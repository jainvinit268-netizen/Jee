package com.eduniti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF080B14)
private val Panel = Color(0xFF121827)
private val Accent = Color(0xFF8B7CFF)
private val Cyan = Color(0xFF48D9D0)
private val Muted = Color(0xFF9BA6BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 20)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 20)
        val raw = runCatching { assets.open("questions.json").bufferedReader().use { it.readText() } }.getOrDefault("[]")
        setContent { EdunitiNativeApp(raw) }
    }
}

private data class Paper(val title: String, val subtitle: String, val questions: List<JSONObject>)
private fun readPapers(raw: String): List<Paper> {
    val root = JSONArray(raw)
    val out = mutableListOf<Paper>()
    for (i in 0 until root.length()) {
        val obj = root.optJSONObject(i) ?: continue
        val qs = obj.optJSONArray("questions") ?: obj.optJSONArray("question") ?: JSONArray()
        val list = (0 until qs.length()).mapNotNull { qs.optJSONObject(it) }
        val title = obj.optString("title").ifBlank { obj.optString("name").ifBlank { obj.optString("paper").ifBlank { "JEE Paper ${i + 1}" } } }
        val sub = listOf(obj.optString("year"), obj.optString("session"), obj.optString("shift")).filter { it.isNotBlank() && it != "null" }.joinToString(" • ")
        out.add(Paper(title, sub.ifBlank { "${list.size} Questions" }, list))
    }
    return out
}
private fun JSONObject.firstText(vararg keys: String): String {
    for (key in keys) {
        val v = opt(key)
        if (v != null && v != JSONObject.NULL && v !is JSONObject && v !is JSONArray) {
            val s = v.toString().trim()
            if (s.isNotEmpty() && s != "null") return s
        }
    }
    return ""
}
private fun questionText(q: JSONObject): String {
    val direct = q.firstText("question", "text", "content", "statement", "prompt", "questionText")
    if (direct.isNotBlank()) return direct
    return q.keys().asSequence().filter { it !in setOf("options", "answer", "solution", "explanation", "id") }
        .mapNotNull { key -> q.opt(key)?.takeIf { it is String }?.toString() }.firstOrNull().orEmpty()
}
private fun optionsOf(q: JSONObject): List<Pair<String, String>> {
    val a = q.optJSONArray("options") ?: q.optJSONArray("choices")
    if (a != null) return (0 until a.length()).map { i -> ('A' + i).toString() to a.opt(i).toString() }
    val keys = listOf("A", "B", "C", "D").mapNotNull { k ->
        val v = q.opt(k) ?: q.opt("option$k")
        if (v == null || v == JSONObject.NULL) null else k to v.toString()
    }
    return keys
}

@Composable
private fun EdunitiNativeApp(raw: String) {
    val papers = remember(raw) { runCatching { readPapers(raw) }.getOrDefault(emptyList()) }
    var selected by remember { mutableStateOf<Paper?>(null) }
    var qIndex by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }
    var splash by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(if (splash) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "splash")
    LaunchedEffect(Unit) { delay(1150); splash = false }
    BackHandler(enabled = selected != null) { selected = null; qIndex = 0; showAnswer = false }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Accent, secondary = Cyan,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            if (selected == null) HomeScreen(papers, search, { search = it }, { selected = it; qIndex = 0; showAnswer = false })
            else QuestionScreen(selected!!, qIndex, { qIndex = it; showAnswer = false }, showAnswer, { showAnswer = !showAnswer }, { selected = null })
            if (splash) {
                Box(Modifier.fillMaxSize().alpha(alpha).background(Brush.verticalGradient(listOf(Color(0xFF171B34), Ink))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(112.dp).background(Brush.linearGradient(listOf(Accent, Cyan)), RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                            Text("EN", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("EDUNITI", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                        Text("JEE • LEARN. PRACTICE. MASTER.", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(papers: List<Paper>, search: String, onSearch: (String) -> Unit, onOpen: (Paper) -> Unit) {
    val filtered = papers.filter { it.title.contains(search, true) || it.subtitle.contains(search, true) }
    val total = papers.sumOf { it.questions.size }
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("EDUNITI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp)
                Text("YOUR JEE PRACTICE SPACE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
            Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Accent, Cyan)), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Text("EN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF28254E), Color(0xFF12343D))), RoundedCornerShape(24.dp)).padding(20.dp)) {
            Column {
                Text("READY TO LEVEL UP?", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Text("Practice smarter.\nScore higher.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip("${papers.size}", "PAPERS")
                    StatChip(total.toString(), "QUESTIONS")
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("QUESTION BANK", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search year, paper or shift…", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF30394D), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(filtered) { index, paper ->
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable { onOpen(paper) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color(0xFF29264D), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                        Text("%02d".format(index + 1), color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(paper.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(paper.subtitle, color = Muted, fontSize = 11.sp)
                    }
                    Text("›", color = Cyan, fontSize = 28.sp)
                }
            }
            if (filtered.isEmpty()) item { Text("No matching papers found.", color = Muted, modifier = Modifier.padding(18.dp)) }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Row(Modifier.background(Color(0x33202030), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color(0xFFD4D5E7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuestionScreen(paper: Paper, index: Int, onIndex: (Int) -> Unit, showAnswer: Boolean, onToggle: () -> Unit, onBack: () -> Unit) {
    val q = paper.questions.getOrNull(index)
    val options = q?.let { optionsOf(it) }.orEmpty()
    val answer = q?.firstText("answer", "correctAnswer", "correct_option", "ans").orEmpty()
    val solution = q?.firstText("solution", "explanation", "working").orEmpty()
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(paper.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("QUESTION ${index + 1} OF ${paper.questions.size}", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { if (paper.questions.isEmpty()) 0f else (index + 1f) / paper.questions.size }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Accent, trackColor = Panel)
        if (q == null) {
            Text("No questions found in this paper.", color = Muted, modifier = Modifier.padding(20.dp))
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("QUESTION", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(questionText(q).ifBlank { "Question content is available in the source bank but its format is not recognized." }, color = Color.White, fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
                }
                if (options.isNotEmpty()) itemsIndexed(options) { i, option ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).padding(15.dp), verticalAlignment = Alignment.Top) {
                        Text(option.first, color = Accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text(option.second, color = Color.White, modifier = Modifier.weight(1f), lineHeight = 22.sp)
                    }
                }
                item {
                    OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)) {
                        Text(if (showAnswer) "HIDE ANSWER & SOLUTION" else "REVEAL ANSWER")
                    }
                    AnimatedVisibility(showAnswer) {
                        Column(Modifier.fillMaxWidth().background(Color(0xFF142C2B), RoundedCornerShape(16.dp)).padding(16.dp)) {
                            Text("ANSWER", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(answer.ifBlank { "Answer not provided for this question." }, color = Color.White, fontSize = 17.sp)
                            if (solution.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text("SOLUTION", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(solution, color = Color.White, lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onIndex((index - 1).coerceAtLeast(0)) }, enabled = index > 0, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("PREVIOUS") }
                Button(onClick = { onIndex((index + 1).coerceAtMost(paper.questions.lastIndex)) }, enabled = index < paper.questions.lastIndex, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("NEXT QUESTION") }
            }
        }
    }
}
