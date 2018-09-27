package com.nfs.mobility.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.intentservice.chatui.models.ChatMessage;

class MsgManager {
    private static final MsgManager ourInstance = new MsgManager();

    static MsgManager getInstance() {
        if (ourInstance == null)
            return new MsgManager();
        else
            return ourInstance;
    }

    private MsgManager() {
    }

    Map<String, List<ChatMessage>> chatData = new HashMap<>();

    public Map<String, List<ChatMessage>> getChatData() {
        return chatData;
    }

    public void setChatData(Map<String, List<ChatMessage>> chatData) {
        this.chatData = chatData;
    }

    public void messageRecievedForJID(String jid, String messageBody){
        if(chatData.containsKey(jid)){
            ChatMessage chatMessage = new ChatMessage(messageBody, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
            List<ChatMessage> messagesList = chatData.get(jid);
            messagesList.add(chatMessage);
            chatData.put(jid, messagesList);
        } else {
            ChatMessage chatMessage = new ChatMessage(messageBody, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
            List<ChatMessage> messagesList = new ArrayList<>();
            messagesList.add(chatMessage);
            chatData.put(jid, messagesList);
        }
    }
}
