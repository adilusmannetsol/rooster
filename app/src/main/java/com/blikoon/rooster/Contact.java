package com.blikoon.rooster;

/**
 * Created by gakwaya on 4/16/2016.
 */
public class Contact {
    private String jid;
    private String userName;
    private String status;

    public Contact(String userName , String status , String contactJid)
    {
        this.jid = contactJid;
        this.userName = userName;
        this.status = status;
    }

    public String getJid()
    {
        return jid;
    }

    public String getUserName() {
        return userName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "jid='" + jid + '\'' +
                ", userName='" + userName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
