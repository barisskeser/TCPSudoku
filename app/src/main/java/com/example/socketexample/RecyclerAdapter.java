package com.example.socketexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Mesaj listesini res/layout/recycler_row.xml dosyasındaki görünüme göre oluşturmaktadır.
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RowHolder> {

    private ArrayList<String> messages;

    /**
     * Mesaj listesini parametre alan kurucu method
     * @param messages mesaj listesi
     */
    public RecyclerAdapter(ArrayList<String> messages) {
        this.messages = messages;
    }

    /**
     * XML dosyasına referans verilecek bir görüntü oluşturulmaktadır
     * @param parent
     * @param viewType
     * @return new RowHolder(view) xml dosyasının görünümü
     */
    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.recycler_row, parent, false);

        return new RowHolder(view);
    }

    /**
     * XML dosyasındaki nesnelere satır satır müdahale edildiği method
     * @param holder satırların görünümünü tutar
     * @param position satır numarasını tutar
     */
    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {

        // Her satıra ilgili mesajı yerleştirir
        holder.message.setText(messages.get(position));

    }

    /**
     * Toplam satır sayısını döndürür
     * Mesaj sayısı = satır sayısı
     * @return message.size() satır sayısı
     */
    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * XML dosyasındaki nesnelerin tanımlandığı sınıf
     */
    class RowHolder extends RecyclerView.ViewHolder{

        TextView message;

        public RowHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.message);

        }
    }

}
