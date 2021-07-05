package com.example.socketexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Oluştur butonuna basıldığında açılan ekran
 * Server'ın oluştuğu ekran
 */

public class OlusturActivity extends AppCompatActivity {

    TextView ipText, portText, infoText;
    ServerSocket serverSocket;
    Thread connectionListener;

    RadioButton kolay, orta, zor, sirali, zamanaKarsi;

    private static String SERVER_IP = "";
    private static final int PORT = 2323;

    boolean cont = true;

    /**
     * Ekran açıldığında ilk çalışacak method
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olustur);

        // Ekrandaki nesnelerin tanımlanması
        ipText = findViewById(R.id.ipText);
        portText = findViewById(R.id.portText);
        infoText = findViewById(R.id.infoText);
        kolay = findViewById(R.id.kolayRadio);
        orta = findViewById(R.id.ortaRadio);
        zor = findViewById(R.id.zorRadio);
        sirali = findViewById(R.id.siraliRadio);
        zamanaKarsi = findViewById(R.id.zamanaKarsiRadio);

        // Ip bilgisinin alınması
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Bağlantıları dinlemek için yeni iş parçacığı başlatılıyor
        connectionListener = new Thread(new ConnectionListener());
        connectionListener.start();
    }

    String mod = "", seviye = "";

    /**
     * Oyna butonuna basıldığında yapılacak işlemler
     * @param view buton görünümü
     * @throws UnknownHostException
     */
    public void onOyna(View view) throws UnknownHostException {

        // Server bilgileri tekil kalıp olarak oluşturuluyor
        SingletonServer singletonServer = SingletonServer.getInstance();
        singletonServer.setAr(ar);
        singletonServer.setServerSocket(serverSocket);


        // Seçilen zorluk seviyesi alınıyor
        if (kolay.isChecked()) {
            seviye = "1";
        } else if (orta.isChecked()) {
            seviye = "2";
        } else if (zor.isChecked()) {
            seviye = "3";
        }

        // Seçilen mod alınıyor
        if (sirali.isChecked()) {
            mod = "1";
        } else if (zamanaKarsi.isChecked()) {
            mod = "2";
        }

        // Eğer mod ve zorluk seviyesi seçildi ise
        if (!mod.equals("") && !seviye.equals("")) {
            final String finalSeviye = seviye;
            final String finalMod = mod;

            // Client'a başla bilgisi, zorluk ve mod bilgisi gönderiliyor
            Thread startGame = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (ClientHandler handler : ar) {
                        try {
                            handler.dos.writeUTF("start#" + finalSeviye + "#" + finalMod);
                            handler.dos.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            startGame.start();

            // Oyun ekranına geçiş yapılıyor
            Intent intent = new Intent(getApplicationContext(), GameActivity.class);

            // Oyun ekranına Server, zorluk seviyesi ve mod bilgisi gönderiliyor
            intent.putExtra("State", "Server#" + finalSeviye + "#" + finalMod);
            startActivity(intent);
            cont = false;
            finish();
        }
    }

    /**
     * Local IP adresini döndürmektedir
     * @return IP adresi
     * @throws UnknownHostException
     */
    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    static ArrayList<ClientHandler> ar = new ArrayList<>();

    /**
     * Clientdan gelen bağlantı isteğini dinlemektedir.
     */
    private class ConnectionListener implements Runnable {

        DataInputStream dis;
        DataOutputStream dos;

        @Override
        public void run() {
            try {

                // Server oluşturuluyor
                serverSocket = new ServerSocket(PORT);

                // IP ve PORT bilgisi ekranda gösteriliyor
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ipText.setText("IP : " + SERVER_IP);
                        portText.setText("PORT : " + PORT);
                    }
                });

                // Sürekli olarak dinlemek için sonsuz döngü başlatılyıyor
                while (cont) {
                    // Eğer bi istek olursa kabul ediliyor
                    Socket socket = serverSocket.accept();

                    // Haberleşme için input ve output streamleri soketden alınıyor
                    dis = new DataInputStream(socket.getInputStream());
                    dos = new DataOutputStream(socket.getOutputStream());

                    // Gelen isim bilgisi okunuyor
                    String name = dis.readUTF();

                    // Ekrana bağlantı bilgisi yazdırılıyor
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            infoText.append("\nConnected : " + socket);
                        }
                    });

                    // ClientHandler sınıfında client bilgileri tutuluyor
                    ClientHandler client = new ClientHandler(socket, dos, dis, name);
                    ar.add(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Eğer ekran durursa dinleme sonlandırılıyor
    @Override
    protected void onStop() {
        super.onStop();
        cont = false;
    }

    // Eğer ekran kapanırsa dinleme sonlandırılıyor
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cont = false;
    }
}

/**
 * Client bilgilerini tutmak için oluşturulmuştur.
 */
class ClientHandler {

    Socket socket;
    DataOutputStream dos;
    DataInputStream dis;
    String name;

    public ClientHandler(Socket socket, DataOutputStream dos, DataInputStream dis, String name) {
        this.socket = socket;
        this.dos = dos;
        this.dis = dis;
        this.name = name;
    }
}