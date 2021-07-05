package com.example.socketexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Uygulamada açılan ilk ekrandaki olayların gerçekleştiği sınıftır.
 */

public class MainActivity extends AppCompatActivity {

    /**
     * Ekran oluştuğunda yapılacak işlemler
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tekil kalıp olarak kullanılan nesneleri daha sonrasında yeni nesne oluşturmak için null yapıyorum
        SingletonClient.setNullSingletonClient();
        SingletonServer.setNullSingletonServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SingletonClient.setNullSingletonClient();
        SingletonServer.setNullSingletonServer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SingletonClient.setNullSingletonClient();
        SingletonServer.setNullSingletonServer();
    }

    /**
     * Oluştur butonuna tıklandığında yapılacak işlemler
     * @param view butonun görünüm nesnesi
     */
    public void onOlustur(View view) {
        go(OlusturActivity.class);
    }

    /**
     * Katıl butonuna tıklandığında yapılacak işlemler
     * @param view butonun görünüm nesnesi
     */
    public void onKatil(View view) {
        go(KatilActivity.class);
    }

    /**
     * Belirliten sayfaya ekran geçişi sağlanmaktadır
     * @param c Gidilecek class
     */
    public void go(Class c) {

        // Yeni bir ekrana geçiş yapılması
        Intent intent = new Intent(getApplicationContext(), c);
        startActivity(intent);
    }
}