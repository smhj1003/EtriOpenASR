package etri.etriopenasr

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils.indexOf
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.kakao.sdk.user.UserApiClient
import com.kakao.util.helper.Utility
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.lang.StringEscapeUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.experimental.and


//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
class MainActivity : AppCompatActivity() {
    var buttonStart: Button? = null
    var textResult: TextView? = null
    var spinnerMode: Spinner? = null
    var editID: EditText? = null
    var curMode: String? = null
    var result: String? = null
    var maxLenSpeech = 16000 * 45
    var speechData = ByteArray(maxLenSpeech * 2)
    var lenSpeech = 0
    var isRecording = false
    var forceStop = false

    var checkcheck: String? = null //카톡보내는 명령어 확인
    var repeatSpeak: String? = null //명령어 받은후 재녹음 여부

    var commandText: TextView? = null

    private val handler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        @Synchronized
        override fun handleMessage(msg: Message) {
            val bd = msg.data
            val v = bd.getString(MSG_KEY)
            when (msg.what) {
                1 -> {
                    textResult!!.text = v
                    buttonStart!!.text = "PUSH TO STOP"

                }
                2 -> {
                    textResult!!.text = v
                    buttonStart!!.isEnabled = false

                }
                3 -> {
                    textResult!!.text = v
                    buttonStart!!.text = "PUSH TO START"

                }
                4 -> {
                    textResult!!.text = v
                    buttonStart!!.isEnabled = true
                    buttonStart!!.text = "PUSH TO START"


                }
                5 -> {
                    textResult!!.text = StringEscapeUtils.unescapeJava(result)
                    buttonStart!!.isEnabled = true
                    buttonStart!!.text = "PUSH TO START"

                    if( repeatSpeak=="T" ) {
                        msg.what = 1
                        Log.d("메시지","도착")
                    }
                    if(commandCheck()=="T") {
                        commandText!!.text = "카톡보내기 요청"
//                        Log.d("변해5","도착")
                        repeatRecord() //재녹음작동
                        commandText!!.text = "카톡보내기 재녹음"

                        if(repeatSpeak=="R") {
                            checkcheck ="T"
                            sendMsg()
                        }
                    } else {
                        commandText!!.text = "카톡보내기 요청안함"
                        Log.d("확인용","재녹음 $repeatSpeak")
                        if(repeatSpeak=="R") { //반복녹음후 작동
                            commandText!!.text = "카톡보내기 요청"
                            checkcheck ="T"
                            sendMsg()
                        }

                    }

//                    commandText?.text = "변경"
                }
            }
            super.handleMessage(msg)
        }
    }

    fun SendMessage(str: String?, id: Int) {
        val msg = handler.obtainMessage()
        val bd = Bundle()
        bd.putString(MSG_KEY, str)
        msg.what = id
        msg.data = bd
        handler.sendMessage(msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UserApiClient.instance.me { user, error ->
//            id.text = "회원번호: ${user?.id}"
            nickname.text = "닉네임: ${user?.kakaoAccount?.profile?.nickname}"
//            profileimage_url.text = "프로필 링크: ${user?.kakaoAccount?.profile?.profileImageUrl}"
//            thumbnailimage_url.text = "썸네일 링크: ${user?.kakaoAccount?.profile?.thumbnailImageUrl}"

//            var keyHash = Utility.getKeyHash(this)//해시키 확인
//            Log.i("hash", keyHash)
        }


        //메시지보내기

        btnSendMsg.setOnClickListener {
            Log.i("result1", result.toString())
            val defaultText = TextTemplate(
                    text =
                    result.toString().substring(result.toString().indexOf("\":\""), result.toString().indexOf("\"}", 2)).replace("\":\"", "")
                            .trimIndent(),
                    link = Link(
                            webUrl = "https://daum.net",
                            mobileWebUrl = "https://daum.net"
                    )
            )
//        btn_custom_login_out.setOnClickListener {
            TalkApiClient.instance.sendDefaultMemo(defaultText) { error ->
                if (error != null) {
                    Log.e("보내기", "나에게 보내기 실패", error)
                } else {
                    Log.i("보내기", "나에게 보내기 성공")
                    Toast.makeText(this, "나에게 보내기 성공", Toast.LENGTH_SHORT).show()
                }
            }
        }


        kakao_logout_button.setOnClickListener {
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Toast.makeText(this, "로그아웃 실패 $error", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "로그아웃 성공", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }

        kakao_unlink_button.setOnClickListener {
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    Toast.makeText(this, "회원 탈퇴 실패 $error", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "회원 탈퇴 성공", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
            }
        }


        buttonStart = findViewById<View>(R.id.buttonStart) as Button
        textResult = findViewById<View>(R.id.textResult) as TextView
        spinnerMode = findViewById<View>(R.id.spinnerMode) as Spinner
        commandText = findViewById<View>(R.id.commandText) as TextView
        editID = findViewById<View>(R.id.editID) as EditText
        val settings = getSharedPreferences(PREFS_NAME, 0)
        editID!!.setText(settings.getString("client_id", getString(R.string.client_id)))
        val modeArr = ArrayList<String>()
        modeArr.add("한국어인식")
        modeArr.add("영어인식")
        modeArr.add("영어발음평가")
        val modeAdapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, modeArr)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMode!!.adapter = modeAdapter
        spinnerMode!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                curMode = parent.getItemAtPosition(pos).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                curMode = ""
            }
        }
        editID!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val settings = getSharedPreferences(PREFS_NAME, 0)
                val editor = settings.edit()
                editor.putString("client-id", v.text.toString())
                editor.apply()
            }
            false
        }
        buttonStart!!.setOnClickListener {
            if (isRecording) {
                forceStop = true
            } else {
                try {
                    Thread(Runnable {
                        SendMessage("Recording...", 1)
                        try {
                            recordSpeech()
                            SendMessage("Recognizing...", 2)
                        } catch (e: RuntimeException) {
                            SendMessage(e.message, 3)
                            return@Runnable
                        }
                        val threadRecog = Thread { result = sendDataAndGetResult() }
                        threadRecog.start()
                        try {
                            threadRecog.join(20000)
                            if (threadRecog.isAlive) {
                                threadRecog.interrupt()
                                SendMessage("No response from server for 20 secs", 4)
                            } else {
                                SendMessage("OK", 5)
                            }
                        } catch (e: InterruptedException) {
                            SendMessage("Interrupted", 4)
                        }
                    }).start()
                } catch (t: Throwable) {
                    textResult!!.text = "ERROR: $t"
                    forceStop = false
                    isRecording = false
                }
            }
        }



    }
    
    
    //카톡말로 보내기 사전확인
    fun commandCheck(): String {
        var check = result.toString().substring(result.toString().indexOf("\":\""), result.toString().indexOf("\"}",2)).replace("\":\"","").trimIndent()

        if(check.slice(IntRange(0,1))!="카톡") {
            checkcheck ="F"
//            repeatSpeak ="F"
            return "F"
        } else {
            checkcheck ="T"
            repeatSpeak="T"
            return "T"
        }
    }

    //카톡명령이 들어오면 녹음재실행
    fun repeatRecord() {
        result =""
        if(repeatSpeak=="T") {
            if (isRecording) {
                forceStop = true
            } else {
                try {
                    Thread(Runnable {
                        SendMessage("Recording...", 1)
                        try {
                            recordSpeech()
                            SendMessage("Recognizing...", 2)
                        } catch (e: RuntimeException) {
                            SendMessage(e.message, 3)
                            return@Runnable
                        }
                        val threadRecog = Thread { result = sendDataAndGetResult() }
                        Log.d("확인용","위치찾기2")
                        repeatSpeak="R"
                        threadRecog.start()
                        try {
                            threadRecog.join(20000)
                            if (threadRecog.isAlive) {
                                threadRecog.interrupt()
                                SendMessage("No response from server for 20 secs", 4)
                            } else {
                                SendMessage("OK", 5)
//                                Log.d("확인용","위치찾기1")
//                                repeatSpeak="R"
                            }
                        } catch (e: InterruptedException) {
                            SendMessage("Interrupted", 4)
                        }
                    }).start()
                } catch (t: Throwable) {
                    textResult!!.text = "ERROR: $t"
                    forceStop = false
                    isRecording = false
                }
            }

        }
    }

    fun sendMsg() {
        if(checkcheck =="T"){
            val defaultText = TextTemplate(
                    text =
                    result.toString().substring(result.toString().indexOf("\":\""), result.toString().indexOf("\"}", 2)).replace("\":\"", "")
                            .trimIndent(),
                    link = Link(
                            webUrl = "https://daum.net",
                            mobileWebUrl = "https://daum.net"
                    )
            )
//        btn_custom_login_out.setOnClickListener {
            TalkApiClient.instance.sendDefaultMemo(defaultText) { error ->
                if (error != null) {
                    Log.e("보내기", "나에게 보내기 실패", error)
                } else {
                    Log.i("보내기", "나에게 보내기 성공")
                    Toast.makeText(this, "나에게 보내기 성공", Toast.LENGTH_SHORT).show()
                }
            }
        }else if(checkcheck !="T"){
//            val defaultText = TextTemplate(
//                    text =
//                    result.toString().substring(result.toString().indexOf("\":\""), result.toString().indexOf("\"}", 2)).replace("\":\"", "")
//                            .trimIndent(),
//                    link = Link(
//                            webUrl = "https://daum.net",
//                            mobileWebUrl = "https://daum.net"
//                    )
//            )
////        btn_custom_login_out.setOnClickListener {
//            TalkApiClient.instance.sendDefaultMemo(defaultText) { error ->
//                if (error != null) {
//                    Log.e("보내기", "나에게 보내기 실패", error)
//                } else {
//                    Log.i("보내기", "나에게 보내기 성공")
//                    Toast.makeText(this, "나에게 보내기 성공", Toast.LENGTH_SHORT).show()
//                }
//            }
            Log.d("명령어","카톡보내기명령 아님")
        }
    }

    @Throws(RuntimeException::class)
    fun recordSpeech() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                    16000,  // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
            val audio = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000,  // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize)
            lenSpeech = 0
            if (audio.state != AudioRecord.STATE_INITIALIZED) {
                throw RuntimeException("ERROR: Failed to initialize audio device. Allow app to access microphone")
            } else {
                val inBuffer = ShortArray(bufferSize)  // AudioFormat.ENCODING_PCM_16BIT 이기때문레 16Bit = short Array 일수 밖에 없다. 주의!
                val inByteBuffer = ByteArray(bufferSize*2)  // AudioFormat.ENCODING_PCM_16BIT 이기때문레 16Bit = short Array 일수 밖에 없다. 주의!
                forceStop = false
                isRecording = true
                audio.startRecording()
                while (!forceStop) {
                    val ret = audio.read(inByteBuffer, 0, bufferSize*2)
                    for (i in 0 until ret) {
                        if (lenSpeech >= maxLenSpeech) {
                            forceStop = true
                            break
                        }
//                        speechData[lenSpeech * 2] = (inBuffer[i] + 0x00FF) as Byte
//                        speechData[lenSpeech * 2 + 1] = (inBuffer[i] + 0xFF00 shr 8) as Byte
//                        speechData[lenSpeech * 2] = (inBuffer[i] and 0x00FF) as Byte
//                        speechData[lenSpeech * 2 + 1] = (inBuffer[i] and (0xFF00.toBigInteger() shr 8).toByte().toShort()) as Byte

                          speechData[lenSpeech] = inByteBuffer[i]
//                        speechData[lenSpeech * 2 + 1] = (inBuffer[i] and (0xFF00 ushr 8)).toByte()
//                        speechData[lenSpeech * 2] = (inBuffer[i] and 0xFF00.shr(8)).toByte()
//                        speechData[lenSpeech * 2 + 1] = (inBuffer[i] and 0x00FF).toByte()

                        //자바일때
//                        speechData[lenSpeech*2] = (byte)(inBuffer[i] & 0x00FF);
//                        speechData[lenSpeech*2+1] = (byte)((inBuffer[i] & 0xFF00) >> 8);

                        lenSpeech++
                    }
                }
                audio.stop()
                audio.release()
                isRecording = false
            }
        } catch (t: Throwable) {
            throw RuntimeException(t.toString())
        }
    }

    fun sendDataAndGetResult(): String {
        var openApiURL = "http://aiopen.etri.re.kr:8000/WiseASR/Recognition"
        val accessKey = editID!!.text.toString().trim { it <= ' ' }
        val languageCode: String
        val gson = Gson()
        when (curMode) {
            "한국어인식" -> languageCode = "korean"
            "영어인식" -> languageCode = "english"
            "영어발음평가" -> {
                languageCode = "english"
                openApiURL = "http://aiopen.etri.re.kr:8000/WiseASR/Pronunciation"
            }
            else -> return "ERROR: invalid mode"
        }
        val request: MutableMap<String, Any> = HashMap()
        val argument: MutableMap<String, String> = HashMap()
        val audioContents: String = Base64.encodeToString(
                speechData, 0, lenSpeech * 2, Base64.NO_WRAP)
        argument["language_code"] = languageCode
        argument["audio"] = audioContents
        request["access_key"] = accessKey
        request["argument"] = argument
        val url: URL
        val responseCode: Int
        val responBody: String
        return try {
            url = URL(openApiURL)
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.doOutput = true
            val wr = DataOutputStream(con.outputStream)
            wr.write(gson.toJson(request).toByteArray(charset("UTF-8")))
            wr.flush()
            wr.close()
            responseCode = con.responseCode
            if (responseCode == 200) {
                val `is`: InputStream = BufferedInputStream(con.inputStream)
                responBody = readStream(`is`)
                responBody
            } else "ERROR: " + Integer.toString(responseCode)
        } catch (t: Throwable) {
            "ERROR: $t"
        }
    }

    companion object {
        const val PREFS_NAME = "prefs"
        private const val MSG_KEY = "status"
        @Throws(IOException::class)
        fun readStream(`in`: InputStream): String {
            val sb = StringBuilder()
            val r = BufferedReader(InputStreamReader(`in`), 1000)
            var line = r.readLine()
            while (line != null) {
                sb.append(line)
                line = r.readLine()
            }
            `in`.close()
            return sb.toString()
        }
    }
}