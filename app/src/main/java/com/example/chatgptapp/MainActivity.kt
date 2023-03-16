package com.example.chatgptapp

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatgptapp.adapter.ChatIndexAdapter
import com.example.chatgptapp.sql.MyDatabaseHelper
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
    var sqlOprate: SqlOprate? = null
    var list: MutableList<Msg> = mutableListOf()

    var handler: Handler? = null
    var chatIndexAdapter: ChatIndexAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

        bt_send.setOnClickListener {
            if (ed_content.text.isNotEmpty()) {
                val content = ed_content.text.toString()
                val okhttpUtil = OkhttpUtil()
                var flag = true
                dataInsert(Msg(content, Msg.TYPE_SENT))


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

                chatIndexAdapter!!.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(chatIndexAdapter!!.itemCount - 1)

                list.add(Msg("正在加载，请等待......", Msg.TYPE_RECEIVED))

                chatIndexAdapter!!.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(chatIndexAdapter!!.itemCount - 1)

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
                        list.removeAt(list.size - 1)
                        dataInsert(Msg(str, Msg.TYPE_RECEIVED))
                        Log.i("ResponeContent", str.toString())

                        msg = Message()
                        msg!!.obj = str
                        handler!!.sendMessage(msg!!)
                    }
                })
            }
        }
    }

    private fun init() {
        sqlOprate = SqlOprate(this)
        dataQuery()

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        chatIndexAdapter = ChatIndexAdapter(list)
        recyclerView.adapter = chatIndexAdapter
        recyclerView.smoothScrollToPosition(chatIndexAdapter!!.itemCount - 1)

        this.handler = Handler(Handler.Callback {
            chatIndexAdapter!!.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(chatIndexAdapter!!.itemCount - 1)
            false
        })

    }

    private fun dataInsert(msg: Msg) {
        list.add(msg)
        sqlOprate!!.insert(msg.type, msg.content.toString())
    }

    private fun dataQuery() {
        var cursor = sqlOprate!!.query()
        if (cursor!!.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex("id"))
                val type = cursor.getInt(cursor.getColumnIndex("type"))
                val content = cursor.getString(cursor.getColumnIndex("content"))
                list.add(Msg(content, type))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
}