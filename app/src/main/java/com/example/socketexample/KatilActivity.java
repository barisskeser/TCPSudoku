package com.example.socketexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringTokenizer;

/**
 * IP ve port bilgileri girilerek sunucuya bağlanılan ekran
 */

public class KatilActivity extends AppCompatActivity {

    TextView connectionInfo;

    EditText ipAddress, portNo;
    private int serverPORT;

    private String serverIP = null;

    /**
     * Ekran oluştuğunda yapılacak işlemler
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_katil);

        // Ekrandaki nesnelere referans oluşturuluyor
        connectionInfo = findViewById(R.id.connectionInfo);
        ipAddress = findViewById(R.id.ipAddress);
        portNo = findViewById(R.id.portNo);

        // IP adresi alanını dinliyor
        ipAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                serverIP = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // PORT NO alanını dinliyor
        portNo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                serverPORT = Integer.parseInt(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    /**
     * Bağlan butonuna basıldığında yapılacak işlemler
     * @param view buton görünümü
     */
    public void onConnect(View view) {
        Connection connection = new Connection();
        Thread connectionThread = new Thread(connection);

        connectionThread.start();
    }

    /**
     * WIFI ağına bağlandığımızda verilen IP adresini döndürmektedir
     * @return cihazın local IP adresi
     * @throws UnknownHostException
     */
    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    /**
     * Bağlan butonuna tıklandığında server'a bağlanmak için kullanılmaktadır.
     * Ayrı bir sınıfta yapılmasının nedeni, android iş parçacığının dışında farklı bir iş parçacığında olması gerektiğinden.
     */
    class Connection implements Runnable {

        /**
         * Sınıf thread ile birlikte çağrıldığında ilk çalışacak method
         */
        @Override
        public void run() {
            try {
                // IP ve PORT bilgisi ile servera bağlantı gerçekleştiriliyor.
                Socket socket = new Socket(serverIP, serverPORT);

                // Soket bilgisinden input ve output stream bilgileri alınıyor
                // input stream serverdan gelen paketi okumaktadır.
                // output stream servara paketi göndermektedir.
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // Clitent adı olarak IP adresi ayarlanıyor
                String name = getLocalIpAddress();

                // Servera isim bilgisi gönderiliyor
                dos.writeUTF(name);
                dos.flush();

                // Serverdan gelecek olarak oyuna başlama mesajını dinlemek için ayrı bir thread oluşturuluyor.
                Thread readMessage = new Thread(new Runnable() {
                    /**
                     * İlk çalışacak method
                     */
                    @Override
                    public void run() {

                        // Sürekli olarak dinlemek için sonsuz döngü oluşturuluyor
                        while (true) {
                            try {
                                // Serverdan gelen mesaj okunuyor
                                String msg = dis.readUTF();
                                String start, seviye, mod;

                                // Okunan mesaj parçalara ayrılıyor
                                StringTokenizer st = new StringTokenizer(msg, "#");
                                start = st.nextToken();
                                seviye = st.nextToken();
                                mod = st.nextToken();

                                // Eğer start mesajı geldiyse
                                if (start.matches("start")) {

                                    // Client bilgilerinden tekil bir kalıp oluşturuluyor
                                    SingletonClient singletonClient = SingletonClient.getInstance();
                                    singletonClient.setSocket(socket);
                                    singletonClient.setName(name);
                                    singletonClient.setDis(dis);
                                    singletonClient.setDos(dos);

                                    // Oyun ekranına geçiş yapılıyor
                                    Intent intent = new Intent(getApplicationContext(), GameActivity.class);
                                    // Gerekli bilgiler oyun ekranına gönderiliyor
                                    intent.putExtra("State", "Client#" + seviye + "#" + mod);
                                    startActivity(intent);
                                    break;
                                }
                                System.out.println(msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        finish();
                    }
                });

                readMessage.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}