package com.example.chatgptapp

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatgptapp.adapter.ChatIndexAdapter
import com.example.chatgptapp.utils.SqlOprate
import com.lwh.comm.utils.OkhttpUtil
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.wait
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var msg: Message? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var list = mutableListOf<Msg>()

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        var chatIndexAdapter = ChatIndexAdapter(list)
        recyclerView.adapter = chatIndexAdapter
        recyclerView.scrollToPosition(list.size - 1)

//        var db = SqlOprate(this)
//        db.insert()
//        db.query()

        val handler = Handler(Handler.Callback {
            chatIndexAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(list.size - 1)
            false
        })

        bt_send.setOnClickListener {
            if (ed_content.text.isNotEmpty()) {
                val content = ed_content.text.toString()
                val okhttpUtil = OkhttpUtil()
                var flag = true
                list.add(Msg(content, Msg.TYPE_SENT))

                var reply: String? = ""
                for (s in list.reversed()) {
                    if (s.type == Msg.TYPE_RECEIVED) {
                        reply = s.content
                        break
                    }
                }

                if (list.size >= 3) {
                    okhttpUtil.firstContent = list.get(list.size - 3).content.toString()
                    okhttpUtil.newContent = content
                } else {
                    okhttpUtil.firstContent = content
                    okhttpUtil.newContent = ""
                }
                okhttpUtil.received = reply.toString()
                ed_content.setText("")

                chatIndexAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(list.size - 1)


                list.add(Msg("正在加载，请等待......", Msg.TYPE_RECEIVED))
                chatIndexAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(list.size - 1)

                okhttpUtil.doPost(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        flag = false
                        var str = response.body?.string()

                        str = str?.replace(Regex("^(\n)+"), "")
                            ?.replace("```", "")
                            ?.replace(Regex("^(\\{AI\\}\n)+"), "")
                            ?.replace("{AI}", "")
                            ?.replace("{/AI}", "")
                        list.set(list.size - 1, Msg(str, Msg.TYPE_RECEIVED))
                        Log.i("ResponeContent", str.toString())

                        msg = Message()
                        msg!!.obj = str
                        handler.sendMessage(msg!!)
                    }
                })
            }
        }
    }
}