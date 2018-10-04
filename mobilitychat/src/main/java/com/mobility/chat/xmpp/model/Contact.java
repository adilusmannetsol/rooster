package com.mobility.chat.xmpp.model;

/**
 * Created by adilusman on Oct/2016.
 */
public class Contact {
    private String jid;
    private String userName;
    private String status;
    private int countUnRead = 0;
    private int countRead = 0;

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

    public int getCountUnRead(int total) {
        return total - countRead;
    }

    public void clearCountUnRead() {
        this.countUnRead = 0;
    }

    public void setCountRead(int totalCount) {
        this.countRead = totalCount;
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
