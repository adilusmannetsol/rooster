package com.nfs.mobility.chat;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.blikoon.roster.R;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ChatContactsActivity}
 * in two-pane mode (on tablets) or a {@link ChatDetailActivity}
 * on handsets.
 */
public class ChatMessagesFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */

    private static final String TAG = ChatMessagesFragment.class.getSimpleName();

    public static final String ARG_ITEM_JID = "item_jid";

    private String contactJid;

    private ChatView mChatView;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatMessagesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_JID))
            contactJid = getArguments().getString(ARG_ITEM_JID);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_chat_view, container, false);

        mChatView = rootView.findViewById(R.id.roster_chat_view);

        //mChatView.addMessages(new ArrayList<ChatMessage>(MsgManager.getInstance().getChatData().get(contactJid)));

        mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
            @Override
            public boolean sendMessage(ChatMessage chatMessage) {
                // perform actual message sending
                if (RosterConnectionService.getState().equals(RosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sending Message");
                    //Send the message to the server

                    Intent intent = new Intent(RosterConnectionService.SEND_MESSAGE);
                    intent.putExtra(RosterConnectionService.BUNDLE_MESSAGE_BODY,
                            mChatView.getTypedMessage());
                    intent.putExtra(RosterConnectionService.BUNDLE_TO, contactJid);

                    getContext().sendBroadcast(intent);

                } else {
                    Toast.makeText(getContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }
                //message sending ends here
                return true;
            }
        });

        return rootView;
    }





}
