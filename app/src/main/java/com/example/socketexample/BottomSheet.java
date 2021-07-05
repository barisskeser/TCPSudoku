package com.example.socketexample;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.IOException;

/**
 * Oyun ekranında açılan mesaj ekranı
 */
public class BottomSheet extends BottomSheetDialogFragment {

    private String status;
    private Context mContext;

    /**
     * Durum ve context bilgisini alan kurucu method
     * @param status Server yada Client bilgisini tutan değişken
     * @param context Oyun ekranının context bilgisi
     */
    public BottomSheet(String status, Context context) {
        this.status = status;
        this.mContext = context;
    }

    SingletonServer singletonServer;
    SingletonClient singletonClient;
    SingletonMessage singletonMessage;
    RecyclerView recyclerView;

    private static final String MESSAGE_CODE = "MSG";

    /**
     * Ekran ilk açıldığında çalışacak method
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return view res/layout/row_add_item.xml görünümü
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Tasarım dosyasının görünümü oluşturuluyor
        final View view = inflater.inflate(R.layout.row_add_item, container, false);

        // Tekil kalıplara nesne oluşturuluyor
        singletonServer = SingletonServer.getInstance();
        singletonClient = SingletonClient.getInstance();
        singletonMessage = SingletonMessage.getInstance();

        // Mesajın yazıldığı input alanı tanımlanıyor
        EditText edName = view.findViewById(R.id.messageText);

        // Mesajların listelendiği Recycler View nesnesi oluşturuluyor
        recyclerView = view.findViewById(R.id.chatView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(singletonMessage.getAdapter());

        // Gönder butonu tanımlanıyor
        Button btnAdd = view.findViewById(R.id.sendButton);
        btnAdd.setOnClickListener(new View.OnClickListener() {

            /**
             * Gönder butonuna tıklandığında gerçekleşecek olaylar
             * @param v butonun görünümü
             */
            @Override
            public void onClick(View v) {
                // Girdi alınıyor
                String msg = edName.getText().toString();

                // Eğer mesajı gönderen Server ise
                if (status.matches("Server")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Mesaj client'a gönderiliyor
                            for (ClientHandler handler : singletonServer.getAr()) {
                                try {
                                    handler.dos.writeUTF(MESSAGE_CODE + "_" + msg);
                                    handler.dos.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            // Mesaj listesine ekleniyor
                            singletonMessage.addMessage("Server : " + msg);
                        }
                    }).start();
                }
                // Eğer mesajı gönderen client ise
                else if (status.matches("Client")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // Mesaj server'a gönderiliyor
                            try {
                                singletonClient.getDos().writeUTF(MESSAGE_CODE + "_" + msg);
                                singletonClient.getDos().flush();
                                singletonMessage.addMessage("Client : " + msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }

                // Mesaj listesine ekleniyor
                singletonMessage.getAdapter().notifyDataSetChanged();

                // Girdi alanı boşaltılıyor
                edName.setText("");
            }
        });

        return view;
    }

}

