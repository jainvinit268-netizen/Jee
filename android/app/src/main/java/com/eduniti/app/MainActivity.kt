package com.eduniti.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private val Navy=Color(0xFF080D1B)
private val Panel=Color(0xFF111A2D)
private val Accent=Color(0xFF68B7FF)
private data class Q(val id:String,val n:Int,val image:String,val chapter:String,val subject:String,val answer:String,val video:String,val start:Int)
private data class Paper(val id:String,val title:String,val folder:String,val questions:List<Q>)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{App()}}
}
@Composable private fun App(){
 val ctx=LocalContext.current
 val papers=remember{load(ctx)}
 val prefs=remember{ctx.getSharedPreferences("eduniti",0)}
 var marks by remember{mutableStateOf(prefs.getStringSet("marks",emptySet())?.toSet()?:emptySet())}
 var paper by remember{mutableStateOf<Paper?>(null)}
 var index by remember{mutableIntStateOf(-1)}
 var query by remember{mutableStateOf("")}
 var folder by remember{mutableStateOf("jee_challenges")}
 var savedOnly by remember{mutableStateOf(false)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Accent,background=Navy,surface=Panel)){
  Surface(Modifier.fillMaxSize(),color=Navy){
   when{
    paper!=null&&index>=0->{val q=paper!!.questions[index];QuestionPage(q,index,paper!!.questions.size,marks.contains(q.id),{index=-1},{
     val next=marks.toMutableSet();if(!next.add(q.id))next.remove(q.id);marks=next;prefs.edit().putStringSet("marks",next).apply()
    },{if(index>0)index--},{if(index<paper!!.questions.lastIndex)index++},{
     if(q.video.isNotBlank())ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=${q.video}&t=${q.start}s")))
    })}
    paper!=null->PaperPage(paper!!,{paper=null},{index=it})
    else->Home(papers,query,{query=it},folder,{folder=it;savedOnly=false},savedOnly,{savedOnly=!savedOnly},marks,{paper=it})
   }
  }
 }
}
private fun load(ctx:android.content.Context):List<Paper> = try{
 val a=JSONArray(ctx.assets.open("questions.json").bufferedReader().use{it.readText()})
 (0 until a.length()).map{i->val p=a.getJSONObject(i);val qs=p.getJSONArray("questions")
  Paper(p.optString("id"),p.optString("title"),p.optString("folder"),(0 until qs.length()).map{j->val q=qs.getJSONObject(j)
   Q(q.optString("id"),q.optInt("qnum"),q.optString("dataUrl"),q.optString("chapter"),q.optString("subject"),q.optString("answerKey"),q.optString("videoId"),q.optInt("videoStart"))
  })
 }
}catch(_:Exception){emptyList()}
@Composable private fun Top(title:String,back:(()->Unit)?=null){
 Row(Modifier.fillMaxWidth().background(Panel).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
  if(back!=null)IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null,tint=Color.White)}
  Column{Text(title,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Text("PRACTICE • LEARN • IMPROVE",color=Accent,fontSize=9.sp)}
 }
}
@Composable private fun Home(all:List<Paper>,query:String,onQuery:(String)->Unit,folder:String,onFolder:(String)->Unit,saved:Boolean,onSaved:()->Unit,marks:Set<String>,open:(Paper)->Unit){
 val shown=all.filter{p->(!saved||p.questions.any{it.id in marks})&&(folder=="all"||p.folder==folder)&&(query.isBlank()||p.title.contains(query,true)||p.questions.any{it.chapter.contains(query,true)})}
 Column{
  Top("EDUNITI")
  Column(Modifier.padding(14.dp)){
   Text("Your JEE Physics practice",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
   Text("28 PDF sets • 2,403 questions • Offline ready",color=Color.LightGray,fontSize=12.sp)
   Spacer(Modifier.height(10.dp))
   OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search paper or chapter")},leadingIcon={Icon(Icons.Default.Search,null)})
   Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
    FilterChip(folder=="jee_challenges",{onFolder("jee_challenges")},{Text("JEE Challenges")})
    FilterChip(folder=="hcv_chapters",{onFolder("hcv_chapters")},{Text("HCV Chapters")})
    FilterChip(saved,{onSaved()},{Text("Bookmarks ${marks.size}")})
   }
  }
  LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
   items(shown,key={it.id}){p->Card(Modifier.fillMaxWidth().clickable{open(p)},colors=CardDefaults.cardColors(containerColor=Panel)){
    Column(Modifier.padding(15.dp)){Text(p.title,color=Color.White,fontWeight=FontWeight.Bold);Text("${p.questions.size} questions • ${p.questions.map{it.chapter}.distinct().size} chapters",color=Accent,fontSize=12.sp)}
   }}
  }
 }
}
@Composable private fun PaperPage(p:Paper,back:()->Unit,open:(Int)->Unit){
 var filter by remember{mutableStateOf("")}
 val list=p.questions.withIndex().filter{filter.isBlank()||it.value.chapter.contains(filter,true)||it.value.n.toString()==filter}
 Column{Top(p.title,back);OutlinedTextField(filter,{filter=it},Modifier.fillMaxWidth().padding(10.dp),singleLine=true,placeholder={Text("Filter chapter / question")})
  LazyColumn(contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){items(list){e->Card(Modifier.fillMaxWidth().clickable{open(e.index)},colors=CardDefaults.cardColors(containerColor=Panel)){
   Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text("Q${e.value.n}",color=Accent,fontWeight=FontWeight.Bold);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(e.value.chapter,color=Color.White,fontWeight=FontWeight.SemiBold);Text(e.value.subject,color=Color.LightGray,fontSize=11.sp)};Text("OPEN ›",color=Accent)}
  }}}
 }
}
@Composable private fun QuestionPage(q:Q,index:Int,total:Int,saved:Boolean,back:()->Unit,bookmark:()->Unit,prev:()->Unit,next:()->Unit,video:()->Unit){
 val ctx=LocalContext.current
 var reveal by remember(q.id){mutableStateOf(false)}
 val bitmap=remember(q.image){try{BitmapFactory.decodeStream(ctx.assets.open(q.image))?.asImageBitmap()}catch(_:Exception){null}}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().background(Panel).padding(5.dp),verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null,tint=Color.White)}
   Text("Question ${index+1} / $total",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Bold)
   IconButton(onClick=bookmark){Icon(if(saved)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null,tint=Accent)}
  }
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)){
   Text(q.chapter,color=Accent,fontWeight=FontWeight.Bold)
   Spacer(Modifier.height(10.dp))
   if(bitmap!=null)Image(bitmap,null,Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(8.dp)).padding(7.dp))else Text("Question image unavailable",color=Color.White)
   Spacer(Modifier.height(14.dp))
   Button(onClick={reveal=!reveal},Modifier.fillMaxWidth()){Text(if(reveal)"Hide official answer" else "Show official answer")}
   if(reveal)Text("Official answer: ${q.answer.ifBlank{"Not provided"}}",Modifier.padding(12.dp),color=Color(0xFF83E6A4),fontSize=20.sp,fontWeight=FontWeight.Bold)
   if(q.video.isNotBlank())OutlinedButton(onClick=video,Modifier.fillMaxWidth()){Text("Watch EDUNITI solution on YouTube")}
  }
  Row(Modifier.fillMaxWidth().background(Panel).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedButton(onClick=prev,Modifier.weight(1f),enabled=index>0){Text("Previous")}
   Button(onClick=next,Modifier.weight(1f),enabled=index<total-1){Text("Next")}
  }
 }
}