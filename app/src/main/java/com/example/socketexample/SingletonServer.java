package com.example.socketexample;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Client bilgilerini tek bir nesnede tutmak için oluşturulmuştur.
 * Singleton design pattern uygulanmıştır.
 */

public class SingletonServer {

    private static SingletonServer singletonServer = null;

    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> ar = new ArrayList<>();

    /**
     * Eğer bir nesne oluşmadıysa yeni nesne oluşturulup döndürülür.
     * Eğer daha önce nesne oluştuysa var olan nesneyi döndürür
     * @return singletonClient mevcut sınıf nesnesi
     */
    public static SingletonServer getInstance(){
        if(singletonServer == null){
            singletonServer = new SingletonServer();
        }

        return singletonServer;
    }

    /**
     * Direkt olarak nesne oluşturulmaması için private kurucu method
     */
    private SingletonServer(){

    }
    /**
     * Daha önce oluşturulan nesneyi silmektedir.
     */
    public static void setNullSingletonServer() {
        singletonServer = null;
    }

    /**
     * Server'ın serverSocket bilgisini döndürmektedir.
     * @return serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * Server'ın serverSocket'ine atama yapmaktadır.
     * @param serverSocket
     */
    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Client listesini döndürmektedir.
     * @return ar client listesi
     */
    public ArrayList<ClientHandler> getAr() {
        return ar;
    }

    /**
     * Client listesine atama yapmaktadır.
     * @param ar client listesi
     */
    public void setAr(ArrayList<ClientHandler> ar) {
        this.ar = ar;
    }
}
