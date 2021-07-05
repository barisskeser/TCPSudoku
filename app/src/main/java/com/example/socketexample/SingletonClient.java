package com.example.socketexample;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Client bilgilerini tek bir nesnede tutmak için oluşturulmuştur.
 * Singleton design pattern uygulanmıştır.
 */

public class SingletonClient {

    private static SingletonClient singletonClient = null;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private String name;

    /**
     * Eğer bir nesne oluşmadıysa yeni nesne oluşturulup döndürülür.
     * Eğer daha önce nesne oluştuysa var olan nesneyi döndürür
     * @return singletonClient mevcut sınıf nesnesi
     */
    public static SingletonClient getInstance(){
        if(singletonClient == null){
            singletonClient = new SingletonClient();
        }
        return singletonClient;
    }

    /**
     * Direkt olarak nesne oluşturulmaması için private kurucu method
     */
    private SingletonClient(){

    }

    /**
     * Daha önce oluşturulan nesneyi silmektedir.
     */
    public static void setNullSingletonClient() {
        singletonClient = null;
    }

    /**
     * Client'ın input stream nesnesini döndürmektedir
     * @return dis data input stream nesnesi
     */
    public DataInputStream getDis() {
        return dis;
    }

    /**
     * Client'ın input stream nesnesine değer vermektedir
     */
    public void setDis(DataInputStream dis) {
        this.dis = dis;
    }

    /**
     * Client'ın output stream nesnesini döndürmektedir
     * @return dos data output stream nesnesi
     */
    public DataOutputStream getDos() {
        return dos;
    }

    /**
     * Client'ın output stream nesnesine değer vermektedir
     */
    public void setDos(DataOutputStream dos) {
        this.dos = dos;
    }

    /**
     * Client'ın soket bilgisini döndürmektedir.
     * @return socket soket bilgisi
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Client'ın soketine değer vermektedir.
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Client'ın adını döndürmektedir.
     * @return name client adı (IP adresi olacaktır.)
     */
    public String getName() {
        return name;
    }

    /**
     * Client'ın adına değer vermektedir.
     */
    public void setName(String name) {
        this.name = name;
    }
}
