package com.example.socketexample;

import java.util.ArrayList;
/**
 * Mesaj bilgilerini tek bir nesnede tutmak için oluşturulmuştur.
 * Singleton design pattern uygulanmıştır.
 */
public class SingletonMessage {

    private static SingletonMessage singletonMessage = null;
    private ArrayList<String> messages;
    private RecyclerAdapter recyclerAdapter;

    /**
     * Eğer bir nesne oluşmadıysa yeni nesne oluşturulup döndürülür.
     * Eğer daha önce nesne oluştuysa var olan nesneyi döndürür
     * @return singletonMessage mevcut sınıf nesnesi
     */
    public static SingletonMessage getInstance(){
        if(singletonMessage == null){
            singletonMessage = new SingletonMessage();
        }

        return singletonMessage;
    }

    /**
     * Direkt olarak nesne oluşturulmaması için private kurucu method
     */
    private SingletonMessage() {

        // Arraylist ve RecyclerAdapter nesnesinin oluşturulması
        // RecyclerAdapter, mesaj ekranında mesajların listelendiği alanda kullanılmaktadır.
        messages = new ArrayList<>();
        recyclerAdapter = new RecyclerAdapter(getMessages());
    }

    /**
     * Mesaj listesine mesaj eklenmektedir.
     * @param msg eklenecek mesaj
     */
    public void addMessage(String msg){
        messages.add(msg);
    }

    /**
     * Mesaj listesini döndürmektedir.
     * @return messages mesaj listesi
     */
    public ArrayList<String> getMessages() {
        return messages;
    }

    /**
     * RecyclerAdapter nesnesini döndürmektedir.
     * @return recyclerAdapter
     */
    public RecyclerAdapter getAdapter(){
        return recyclerAdapter;
    }

    /**
     * SingletonMessage sınıfından önceden oluşturulmuş nesneyi silmektedir.
     */
    public static void setNull() {
        SingletonMessage.singletonMessage = null;
    }
}
