@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.echotest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.echotest.ui.theme.EchotestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EchotestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(modifier: Modifier = Modifier) {
    /* Mutables */
    val textInput = remember { mutableStateOf(TextFieldValue()) }
    val chatMessages = remember { mutableStateListOf<String>() }
    var horseIndex = remember { mutableStateOf(0) }
    var checked = remember { mutableStateOf(true) }
    var repeat = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var receivedImageBitmap = remember { mutableStateOf<Bitmap?>(null) }


    /* Horse image stuff */
    data class Data(
        var name: String,
        var resourceId: Int,
        var painter: Painter
    )
    val drawableHorses = listOf(
        Data("name1", R.drawable.horse_one, painterResource(R.drawable.horse_one)),
        Data("name2", R.drawable.horse_two, painterResource(R.drawable.horse_two)),
        Data("name3", R.drawable.horse_three, painterResource(R.drawable.horse_three)),
        Data("name4", R.drawable.horse_four, painterResource(R.drawable.horse_four)),
        Data("name5", R.drawable.horse_five, painterResource(R.drawable.horse_five)),
        Data("name6", R.drawable.horse_six, painterResource(R.drawable.horse_six)),
        Data("name7", R.drawable.horse_seven, painterResource(R.drawable.horse_seven)),
        Data("name8", R.drawable.horse_eight, painterResource(R.drawable.horse_eight)),
        Data("name9", R.drawable.horse_nine, painterResource(R.drawable.horse_nine)),
        Data("name10", R.drawable.horse_ten, painterResource(R.drawable.horse_ten)),
        Data("name11", R.drawable.horse_eleven, painterResource(R.drawable.horse_eleven)),
        Data("name12", R.drawable.horse_twelve, painterResource(R.drawable.horse_twelve))
    )
    /* ByteString conversion */
    val selectedHorseData = drawableHorses.get(horseIndex.value)
    val horseImageResId = selectedHorseData.resourceId
    val context = LocalContext.current

    /*Creates the bitmap*/
    val horseImage = BitmapFactory.decodeResource(context.resources, horseImageResId)

    /**/
    val outputStream = ByteArrayOutputStream()
    horseImage?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val horseBytes = outputStream.toByteArray().toByteString()


    /* Server communication */
    var client = OkHttpClient()
    val wss_url = "ws://IPGOESHERE:8765"
    fun run(url: String) {

        val wss_request: Request = Request.Builder().url(url).build()
        val webSocket = client.newWebSocket(wss_request, EchoWebSocketListener(
            textInput.value.text,
            chatMessages,
            horseBytes,
            receivedImageBitmap,
            checked
        ))
        if(textInput.value.text.isNotEmpty() && checked.value){
            chatMessages.add("You: ${textInput.value.text}")
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(color = Color.LightGray)
            ) {
                items(chatMessages) { message ->
                    Text(text = message,
                        color= Color.Black
                    )
                }
            }
            if (!repeat.value){
                receivedImageBitmap.value?.let {
                    Image(
                        modifier = Modifier
                            .width(200.dp)
                            .height(150.dp),
                        bitmap = it.asImageBitmap(),
                        contentDescription = null
                    )
                }
            }else {
                Image(
                    modifier = Modifier
                        .width(200.dp)
                        .height(150.dp),
                    painter = selectedHorseData.painter,
                    contentDescription = null)
            }
            Row {
                Text(text = checked.value.toString())
                Spacer(modifier = Modifier.width(25.dp))
                Text(text = repeat.value.toString())
            }
            Spacer(modifier = Modifier.height(15.dp))
            TextField(
                modifier = Modifier.width(180.dp),
                label = { Text(text = "User input") },
                value = textInput.value,
                onValueChange = { textInput.value = it }
            )
            Spacer(modifier = Modifier.width(15.dp))
            Row {
                ElevatedButton(
                    modifier = Modifier.width(100.dp),
                    onClick = {
                        repeat.value = !repeat.value
                        coroutineScope.launch{
                            while (repeat.value) {

                                if (checked.value) {
                                    run(wss_url)
                                } else {
                                    horseIndex.value = (horseIndex.value + 1) % drawableHorses.size
                                }
                                delay(500)
                            }
                        }
                    }
                ) {
                    if(repeat.value) {
                        Text("Stop")
                    }else{
                        Text("Repeat")
                    }
                }
                ElevatedButton(
                    modifier = Modifier.width(100.dp),
                    onClick = {
                        checked.value = !checked.value
                    }
                ) {
                    if(checked.value) {
                        Text("text")
                    }else{
                        Text("image")
                    }
                }
            }
            ElevatedButton(
                modifier = Modifier.width(200.dp),
                onClick = {
                    if(checked.value){
                        run(wss_url)
                    } else {
                        horseIndex.value = (horseIndex.value + 1) % drawableHorses.size
                        Log.v("WSS", horseIndex.value.toString())
                        run(wss_url)
                    }
                }
            ) {
                Text("Submit")
            }
        }
    }
}

private class EchoWebSocketListener(
    private val customMessage: String,
    private val chatMessages: MutableList<String>,
    private val horseBytes: ByteString,
    private var receivedImageBitmap: MutableState<Bitmap?>,
    private val checked: MutableState<Boolean>
) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        if (checked.value && customMessage.isNotEmpty()){
            webSocket.send(customMessage)
        } else {
            webSocket.send(horseBytes)
        }
    }
    override fun onMessage(webSocket: WebSocket, text: String) {
        val receivedMessage = "Server: $text"
        chatMessages.add(receivedMessage)
        output("Receiving text : $text")
    }
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val byteArray = bytes.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        receivedImageBitmap.value = bitmap
        output("Receiving bytes : " + receivedImageBitmap.value.toString() + bytes.toString())
    }
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket!!.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing : $code / $reason")
    }
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Error : " + t.message)
    }
    companion object {
        private val NORMAL_CLOSURE_STATUS = 1000
    }
    private fun output(txt: String) {
        Log.v("WSS", txt)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EchotestTheme {
        MyApp()
    }
}