package ru.sberdevices.pub.demoapp.ui.smartapp.network

import android.util.Log
import com.google.gson.Gson
import ru.sberdevices.pub.demoapp.ui.smartapp.model.Invoice
import ru.sberdevices.pub.demoapp.ui.smartapp.model.InvoiceResponse
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NetworkClient {

    /**
     * API SmartPay
     */
    private val PAYLIB_BASE_URL = "https://smartmarket.online.sberbank.ru/smartpay/v1/invoices"

    /**
     * Временный токен для тестирования платежей
     * @see <a href="https://developers.sber.ru/docs/ru/va/how-to/monetization/payments/access">Документация по получению и использованию</a>
     */
    private val PAYLIB_TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjRmNWI4NGUxLTU2Y2ItNDViZi05OTRkLTBmZDc2YTEzMzVkNyJ9.eyJzdWIiOiIxMTYiLCJleHAiOjE4MzAyOTc2MDAsImlhdCI6MTUxNjIzOTAyMiwiaXNzIjoicnUuc2JlcmRldmljZXMuaW5hcHAiLCJhdWQiOiJzbWFydG1hcmtldCIsInByb2R1Y3QiOiJpbmFwcCIsImp0aSI6IkU2Qjg2MDE3LUU1OTgtNEFERi1CQkJDLTQ0QTU4ODA4RDBDNiJ9.tGm2AfutcbqfPLJZYBdBHxdQ5oLIh1VNK3mYY7U8T5WJifM8ylg_5mzejS5ljLUqbrewUk9pwZR0jJ6-lDuE4fvUy17e21S_cfGYW8I_gpPIBeaNrIA6U877oS_bjrr1OpNrq1cG2G3jiSqDWF6ZY7vsLpr4bee1ILU9dI8BoaQlW9ItlxoV9Y593ZmvalNO3x7lOIzPRw_ciR3zn7BJSUcOFBJCQxPFobVL_asOoTiaiSxYv_ZI1wN9xif-UYOz3_LuhuSju6wMSxkXLpLCB2QHvPfe-a2gQq6JHejD57TVSSZKPZB2nNHzi8EJuq_tlI0Qkn02FBik7uMSBLB8jOSBLHlQFD_Kx0_4bSX7j6u43C4FxtDL_kDMfCYT5sc0ebiLen2oan1SiW_tPi12FsFq3JC3V6ugmyVKQSYhNqYwAjEE15fig555Xem32jnGDRCKyzXsippNGzS_IsBBYaWtOnHP_NU2Jbo0Eas1H5rhjWCY16Z4BKuXu7a0GmtCWqFragAFjU9GeTkv2raAXTqiytpHMuUEL7a1LWIecWN990iWYbFB-woQkMq6VNgErrpFt_BdJq89fnPKodYTCtt3BSMCd1cToCag05BJV0Mw_0Lff7rOU1iGDhDKTkIwxT6mn4ef8KcBCCiouv7sPfktbIYu2RIH9_DigOF7zOM"

    /**
     * Регистрация счета в SmartPay с тестовыми реквизитами
     * @param invoice структура для заказа с корзиной
     */
    fun createInvoice(invoice: Invoice): InvoiceResponse? {
        val postData = Gson().toJson(invoice)
        val url = URL(PAYLIB_BASE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $PAYLIB_TEST_TOKEN")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.useCaches = false
        val outputStreamWriter = OutputStreamWriter(conn.outputStream)
        outputStreamWriter.write(postData)
        outputStreamWriter.flush()

        return if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Gson().fromJson(response, InvoiceResponse::class.java)
        } else {
            Log.e("NetworkClient", conn.responseMessage)
            null
        }
    }
}