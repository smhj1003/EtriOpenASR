package etri.etriopenasr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.kakao.sdk.auth.LoginClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.util.helper.Utility
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                Toast.makeText(this, "토큰 정보 보기 실패", Toast.LENGTH_SHORT).show()
            }
            else if (tokenInfo != null) {
                Toast.makeText(this, "토큰 정보 보기 성공", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                var keyHash = Utility.getKeyHash(this)
                Log.i("hash", keyHash)

            }



        }
        //
        //        callback = SessionCallback()
        //        Session.getCurrentSession().addCallback(callback)
        //        Session.getCurrentSession().checkAndImplicitOpen()
        //
        //    }
        //
        //
        //
        //    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //
        //        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
        //            return
        //        }
        //        super.onActivityResult(requestCode, resultCode, data)
        //    }
        //
        //    override fun onDestroy() {
        //        super.onDestroy()
        //        Session.getCurrentSession().removeCallback(callback)
        //    }
        //
        ////    // 앱의 해쉬 키 얻는 함수
        ////    private fun getAppKeyHash() {
        ////        try {
        ////            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        ////            for (signature in info.signatures) {
        ////                val md: MessageDigest
        ////                md = MessageDigest.getInstance("SHA")
        ////                md.update(signature.toByteArray())
        ////                val something = String(Base64.encode(md.digest(), 0))
        ////                Log.e("Hash key", something)
        ////            }
        ////        } catch (e: Exception) {
        ////            // TODO Auto-generated catch block
        ////            Log.e("name not found", e.toString())
        ////        }
        ////
        ////    }
        //
        //    private inner class SessionCallback : ISessionCallback {
        //        override fun onSessionOpened() {
        //            // 로그인 세션이 열렸을 때
        //            UserManagement.getInstance().me( object : MeV2ResponseCallback() {
        //                override fun onSuccess(result: MeV2Response?) {
        //                    // 로그인이 성공했을 때
//                    var intent = Intent(this@LoginActivity, MainActivity::class.java)
//                    intent.putExtra("name", result!!.getNickname())
//                    intent.putExtra("profile", result!!.getProfileImagePath())
//                    startActivity(intent)
//                    finish()
//                }
//
//                override fun onSessionClosed(errorResult: ErrorResult?) {
//                    // 로그인 도중 세션이 비정상적인 이유로 닫혔을 때
//                    Toast.makeText(
//                        this@LoginActivity,
//                        "세션이 닫혔습니다. 다시 시도해주세요 : ${errorResult.toString()}",
//                        Toast.LENGTH_SHORT).show()
//                }
//
//
//            })
//        }
//        override fun onSessionOpenFailed(exception: KakaoException?) {
//            // 로그인 세션이 정상적으로 열리지 않았을 때
//            if (exception != null) {
//                com.kakao.util.helper.log.Logger.e(exception)
//                Toast.makeText(
//                    this@LoginActivity,
//                    "로그인 도중 오류가 발생했습니다. 인터넷 연결을 확인해주세요 : $exception",
//                    Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    }
//
//    private fun redirectSignupActivity() {
//        val intent = Intent(this, LoginActivity::class.java)
//        startActivity(intent)
//        finish()
//    }
//
//}

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Toast.makeText(this, "접근이 거부 됨(동의 취소)", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidClient.toString() -> {
                        Toast.makeText(this, "유효하지 않은 앱", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidGrant.toString() -> {
                        Toast.makeText(this, "인증 수단이 유효하지 않아 인증할 수 없는 상태", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidRequest.toString() -> {
                        Toast.makeText(this, "요청 파라미터 오류", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidScope.toString() -> {
                        Toast.makeText(this, "유효하지 않은 scope ID", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Misconfigured.toString() -> {
                        Toast.makeText(this, "설정이 올바르지 않음(android key hash)", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.ServerError.toString() -> {
                        Toast.makeText(this, "서버 내부 에러", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Unauthorized.toString() -> {
                        Toast.makeText(this, "앱이 요청 권한이 없음", Toast.LENGTH_SHORT).show()
                    }
                    else -> { // Unknown
                        Toast.makeText(this, "기타 에러", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (token != null) {
                Toast.makeText(this, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }

        kakao_login_button.setOnClickListener {
            if (LoginClient.instance.isKakaoTalkLoginAvailable(this)) {
                LoginClient.instance.loginWithKakaoTalk(this, callback = callback)
            } else {
                LoginClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }
    }
}