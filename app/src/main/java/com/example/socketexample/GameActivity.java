package com.example.socketexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Oyun ekranının bulunduğu sınıf
 */
public class GameActivity extends AppCompatActivity {

    private SudokuBoard gameBoard;
    private Solver gameBoardSolver;

    private String extra = null, status = null, mod = null, level = null;

    private SingletonServer singletonServer;
    private SingletonClient singletonClient;
    private SingletonMessage singletonMessage;

    private Button b1, b2, b3, b4, b5, b6, b7, b8, b9;

    private CountDownTimer countDownTimer;
    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;

    private String player;

    private TextView timeText;
    private TextView headerText;

    private Thread clientListen;

    private static final String MESSAGE_CODE = "MSG";
    private static final String GAME_CODE = "GAME";
    private static final String SIRALI_CODE = "1";
    private static final String ZAMANA_KARSI_CODE = "2";
    private static final String SERVER_CODE = "0";
    private static final String CLIENT_CODE = "1";

    private static int SERVER_PUAN = 0;
    private static int CLIENT_PUAN = 0;

    /**
     * Ekran ilk açılıdığında çalışan method
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 1-9 arası butonlar tanımlanıyor
        b1 = findViewById(R.id.button);
        b2 = findViewById(R.id.button2);
        b3 = findViewById(R.id.button3);
        b4 = findViewById(R.id.button4);
        b5 = findViewById(R.id.button5);
        b6 = findViewById(R.id.button6);
        b7 = findViewById(R.id.button7);
        b8 = findViewById(R.id.button8);
        b9 = findViewById(R.id.button9);

        // Mesaj ekranına erişimi bulunan buton tanımlanıyor
        FloatingActionButton fab = findViewById(R.id.floatMessageButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Mesaj ekranı alttan açılıyor
                BottomSheet bottomSheet = new BottomSheet(status, getApplicationContext());
                bottomSheet.show(getSupportFragmentManager(), "TAG");

            }
        });

        // Oyun tahtası ve çözücü sınıfı tanımlanıyor
        gameBoard = findViewById(R.id.SudokuBoard);
        gameBoardSolver = gameBoard.getSolver();

        // İlk kare seçiliyor
        gameBoardSolver.setSelectedRow(1);
        gameBoardSolver.setSelectedColumn(1);

        // Zaman bilgilsinin gösterileceği nesne tanımlanıyor
        timeText = findViewById(R.id.timeText);
        headerText = findViewById(R.id.headerText);

        // Önceki ekrandan buraya gönderilen bilgi okunuyor
        Intent intent = getIntent();
        extra = intent.getStringExtra("State");
        StringTokenizer st = new StringTokenizer(extra, "#");
        status = st.nextToken(); // Oyunu oynayanın kişiyiy tutar (Server yada Client)
        level = st.nextToken(); // Oyunun zorluk seviyesi
        mod = st.nextToken(); // Oyunun modu


        // Seçilen zorluk seviyesine göre oyun oluşturuluyor
        if (level.matches("1")) {
            kolaySudokuOlustur();
        } else if (level.matches("2")) {
            ortaSudokuOlustur();
        } else if (level.matches("3")) {
            zorSudokuOlustur();
        }

        // Eğer Sıralı oyun modu seçildiyse
        if (mod.matches(SIRALI_CODE)) {
            timeText.setText("Bekleniyor...");

            // Geri sayım nesnesi oluşturuluyor
            countDownTimer = new CountDownTimer(15000, 1000) {

                /**
                 * Her saniye zaman bilgisi alınıyor
                 * @param millisUntilFinished
                 */
                @Override
                public void onTick(long millisUntilFinished) {
                    timeText.setText("Time : " + millisUntilFinished / 1000);
                }

                /**
                 * Süre bittiğinde çalışacak method
                 */
                @Override
                public void onFinish() {

                    // Sıra diğer oyuncuya geçiyor
                    sendPlayer();
                }
            };
        }
        // Eğer Zamana karşı oyun modu seçildiyse
        else if (mod.matches(ZAMANA_KARSI_CODE)) {
            timeText.setText("");
            headerText.setText("Zamana Karşı");

            // Zamanı ileri doğru saymak için timer oluşturuluyor
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            time++;
                            timeText.setText(getTimerText());
                        }
                    });
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 1000);
        }

        // Tekil kalıp nesneleri oluşturuluyor
        singletonServer = SingletonServer.getInstance();
        singletonClient = SingletonClient.getInstance();
        singletonMessage = SingletonMessage.getInstance();

        // Eğer oyun ekranına gelen kişi Server ise
        if (status.matches("Server")) {

            // Client'dan gelecek bilgiyi dinlemek için yeni bir iş parçacığı başlatılıyor
            for (ClientHandler handler : singletonServer.getAr()) {
                new Thread(new TalkingToClient(handler.socket, handler.dos, handler.dis, handler.name)).start();
            }

            // Eğer sıralı oyun modu ise oyuna ilk server başlayacağı için
            // geri sayım başlıyor ve rakam butonları görünür oluyor
            if (mod.matches(SIRALI_CODE)) {
                player = "0";
                countDownTimer.start();
                setVisibleButtons(View.VISIBLE);
            }
        }
        // Eğer oyun ekranına gelen kişi Client ise
        else {
            // Eğer oyun modu sıralı ise ilk oyun Server başlayacağından client'ın butonları görünmez oluyor
            if(mod.matches(SIRALI_CODE)) {
                setVisibleButtons(View.INVISIBLE);
            }

            // Serverden gelen bilgileri dinlemek için iş parçacığı başlatılıyor
            clientListen = new Thread(new Runnable() {
                @Override
                public void run() {
                    String rec;
                    while (true) {
                        try {

                            // Serverdan gelen paket alınıyor
                            rec = singletonClient.getDis().readUTF();
                            StringTokenizer code = new StringTokenizer(rec, "_");
                            String statusCode = code.nextToken();
                            String received = code.nextToken();

                            // Eğer gelen paket oyunla ilgili ise
                            if (statusCode.matches(GAME_CODE)) {

                                // Eğer oyun modu sıralı ise
                                if (mod.matches(SIRALI_CODE)) {

                                    // Eğer sıra serverda ise butonlar görünmez oluyor ve geri sayım duruyor
                                    if (received.matches(SERVER_CODE)) {
                                        player = "0";
                                        setVisibleButtons(View.INVISIBLE);
                                        countDownTimer.cancel();
                                    }
                                    // Eğer sıra clientda ise butonlar görünür oluyor ve geri sayım başlıyor
                                    else if (received.matches(CLIENT_CODE)) {
                                        setVisibleButtons(View.VISIBLE);
                                        countDownTimer.cancel();
                                        countDownTimer.start();
                                        player = "1";

                                        // Bu durum ancak server'ın süresi bittiğinde gerçekleşeceği için server puanı azaltılıyor
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateScore("Server", false);
                                            }
                                        });
                                    }
                                    // Server oynadı ve sıra Client'a geçti ise
                                    else {
                                        // Server'ın hamlesi oyunu bitirdi ise
                                        if (received.matches("finish")) {

                                            // Oyunun bitiş mesajı gösteriliyor ve ana ekrana dönülüyor
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    updateScore("Server", true);

                                                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                                    builder.setTitle("Oyun Bitti");
                                                    if (SERVER_PUAN > CLIENT_PUAN) {
                                                        builder.setMessage("Diğer oyuncu kazandı!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                    } else if (CLIENT_PUAN > SERVER_PUAN) {
                                                        builder.setMessage("Oyunu kazandınız!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                    } else {
                                                        builder.setMessage("Beraberlik!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                    }
                                                    builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent);
                                                        }
                                                    });
                                                    builder.show();
                                                }
                                            });
                                            break;
                                        }
                                        // Oyun devam ediyor ise client butonları görünür olur ve geri sayım başlar.
                                        else {
                                            player = "1";
                                            setVisibleButtons(View.VISIBLE);
                                            countDownTimer.cancel();
                                            countDownTimer.start();

                                            // Gelen mesaj çözümleniyor
                                            StringTokenizer st = new StringTokenizer(received, "#");
                                            String row = st.nextToken(); // satır bilgisi
                                            String column = st.nextToken(); // sütun bilgisi
                                            String number = st.nextToken(); // koyulan sayı
                                            String check = st.nextToken(); // doğru yanlış bilgisi


                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // Serverdan gelen bilgi tahtaya uygulanıyor
                                                    gameBoardSolver.setSelectedColumn(Integer.parseInt(column));
                                                    gameBoardSolver.setSelectedRow(Integer.parseInt(row));
                                                    gameBoardSolver.setNumberPos(Integer.parseInt(number), getApplicationContext());

                                                    // Gelen check doğru yanlış bilgisine göre server'a puan eklenir
                                                    updateScore("Server", check.matches("1"));

                                                }
                                            });
                                        }
                                    }
                                }
                                // Eğer oyun modu zaman karşı ise ve oyun bittiyse
                                else if (mod.matches(ZAMANA_KARSI_CODE) && received.matches("finish")) {

                                    // Oyun bitti mesajı gösteriliyor ve ana ekrana dönülüyor
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                            builder.setTitle("Oyun Bitti");
                                            builder.setMessage("Diğer oyuncu kazandı!");
                                            builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                }
                                            });
                                            builder.show();
                                        }
                                    });
                                    break;

                                }
                            }
                            // Eğer gelen paket mesajla ilgili ise
                            else if (statusCode.matches(MESSAGE_CODE)) {
                                // Mesaj listesine mesaj ekleniyor
                                singletonMessage.addMessage("Server" + received);

                                // Adapterde ki liste güncelleniyor
                                singletonMessage.getAdapter().notifyDataSetChanged();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            clientListen.start();
        }
    }

    /**
     * Zamanı ilgili formatta döndürüyor
     *
     * @return
     */
    private String getTimerText() {
        int rounded = (int) Math.round(time);

        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = (rounded % 86400) / 3600;

        return formatTime(seconds, minutes, hours);
    }

    /**
     * Gelen saniye, dakika, saat bilgisini belirli bir fortta döndürüyor
     *
     * @param seconds saniye
     * @param minutes dakika
     * @param hours   saat
     * @return
     */
    private String formatTime(int seconds, int minutes, int hours) {
        return String.format("%02d", hours) + " : " + String.format("%02d", minutes) + " : " + String.format("%02d", seconds);
    }

    /**
     * Client'dan gelen mesajların dinlendiği sınıf
     */
    class TalkingToClient implements Runnable {

        Socket socket;
        DataOutputStream dos;
        DataInputStream dis;
        String name;

        /**
         * Client bilgilerinin alındığı kurucu method
         *
         * @param socket soket bilgisi
         * @param dos    output stream
         * @param dis    input stream
         * @param name   isim
         */
        public TalkingToClient(Socket socket, DataOutputStream dos, DataInputStream dis, String name) {
            this.socket = socket;
            this.dos = dos;
            this.dis = dis;
            this.name = name;
        }

        /**
         * İlk çalışacak method
         */
        @Override
        public void run() {
            String rec = "";

            // Client'dan gelen paketler dinleniyor
            while (true) {
                try {

                    // Client'dan gelen paket okunuyor
                    rec = dis.readUTF();
                    StringTokenizer code = new StringTokenizer(rec, "_");
                    String statusCode = code.nextToken();
                    String received = code.nextToken();

                    // Gelen paket oyun ile ilgili ise
                    if (statusCode.matches(GAME_CODE)) {

                        // Eğer oyun modu sıralı ise
                        if (mod.matches(SIRALI_CODE)) {

                            // Eğer oyun sırası serverda ise
                            // Bu durum yalnızca client'ın süresi bittiğinde gerçekleşir
                            if (received.matches(SERVER_CODE)) {

                                // Butonlar görünür yapılıyor ve geri sayım başlıyor
                                setVisibleButtons(View.VISIBLE);
                                player = "0";
                                countDownTimer.cancel();
                                countDownTimer.start();

                                // Client puanı 10 azaltılıyor
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateScore("Client", false);
                                    }
                                });
                            } else if (received.matches(CLIENT_CODE)) {
                                //setVisibleButtons(View.INVISIBLE);
                                //player = "1";
                                //timer.cancel();
                            }
                            // Eğer client oynamış ve sıra server'a geçmiş ise
                            else {

                                // Client'ın hamlesi oyunu bitirdi ise
                                if (received.matches("finish")) {

                                    // Oyun bitti mesajı gösteriliyor ve ana ekrana dönülüyor
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            updateScore("Client", true);

                                            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                            builder.setTitle("Oyun Bitti");
                                            if (SERVER_PUAN > CLIENT_PUAN) {
                                                builder.setMessage("Oyunu Kazandınız!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            } else if (CLIENT_PUAN > SERVER_PUAN) {
                                                builder.setMessage("Diğer oyuncu kazandı!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            } else {
                                                builder.setMessage("Beraberlik!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            }
                                            builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                }
                                            });
                                            builder.show();
                                        }
                                    });
                                    break;
                                }
                                // Eğer oyun devam ediyor ise
                                else {

                                    // Butonlar görünür yapılıyor ve geri sayım başlıyor
                                    setVisibleButtons(View.VISIBLE);
                                    countDownTimer.cancel();
                                    countDownTimer.start();

                                    // Gelen mesaj çözümleniyor
                                    StringTokenizer st = new StringTokenizer(received, "#");
                                    String row = st.nextToken(); // satır
                                    String column = st.nextToken(); // sütun
                                    String number = st.nextToken(); // koyulan sayı
                                    String check = st.nextToken(); // doğru yanlış bilgisi

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            // Yapılan hamle tahtada uygulanıyor
                                            gameBoardSolver.setSelectedColumn(Integer.parseInt(column));
                                            gameBoardSolver.setSelectedRow(Integer.parseInt(row));
                                            gameBoardSolver.setNumberPos(Integer.parseInt(number), getApplicationContext());

                                            // Client puanı artırılıyor veya azaltılıyor
                                            updateScore("Client", check.matches("1"));

                                        }
                                    });
                                }
                            }
                        }
                        // Eğer oyun modu zaman karşı ise ve oyun bitti ise
                        else if (mod.matches(ZAMANA_KARSI_CODE) && received.matches("finish")) {

                            // Oyun bitti mesajı gösteriliyor ve ana ekrana dönülüyor
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                    builder.setTitle("Oyun Bitti");
                                    builder.setMessage("Diğer oyuncu kazandı!");
                                    builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                    builder.show();
                                }
                            });
                            break;
                        }
                    }
                    // Eğer gelen paket mesajla ilgili ise
                    else if (statusCode.matches(MESSAGE_CODE)) {

                        // Mesaj listesine ekleniyor
                        singletonMessage.addMessage("Client : " + received);

                        // Adaterde ki mesaj listesi güncelleniyor
                        singletonMessage.getAdapter().notifyDataSetChanged();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 1 tıklandıysa
     *
     * @param view
     */
    public void BTNOnePress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#1";
        sendMessage(msg);


        gameBoardSolver.setNumberPos(1, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 2 tıklandıysa
     *
     * @param view
     */
    public void BTNTwoPress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#2";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(2, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 3 tıklandıysa
     *
     * @param view
     */
    public void BTNThreePress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#3";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(3, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 4 tıklandıysa
     *
     * @param view
     */
    public void BTNFourPress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#4";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(4, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 5 tıklandıysa
     *
     * @param view
     */
    public void BTNFivePress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#5";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(5, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 6 tıklandıysa
     *
     * @param view
     */
    public void BTNSixPress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#6";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(6, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 7 tıklandıysa
     *
     * @param view
     */
    public void BTNSevenPress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#7";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(7, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 8 tıklandıysa
     *
     * @param view
     */
    public void BTNEightPress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#8";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(8, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * 9 tıklandıysa
     *
     * @param view
     */
    public void BTNNinePress(View view) {

        // Koyulan pozisyon ve sayı client'a veya server'a gönderiliyor
        String msg = gameBoardSolver.getSelectedRow() + "#" + gameBoardSolver.getSelectedColumn() + "#9";
        sendMessage(msg);

        gameBoardSolver.setNumberPos(9, getApplicationContext());
        gameBoard.invalidate();
    }

    /**
     * Puan bilgisi güncellenmektedir
     *
     * @param status Server yada Client bilgisi
     * @param result true false bilgisi
     */
    public void updateScore(String status, boolean result) {
        if (status.matches("Server")) {
            if (result) {
                SERVER_PUAN += 10;
            } else {
                SERVER_PUAN -= 10;
            }
        } else {
            if (result) {
                CLIENT_PUAN += 10;
            } else {
                CLIENT_PUAN -= 10;
            }
        }

        // Skor tablosu güncelleniyor
        headerText.setText("SERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
    }

    /**
     * Orta seviye oyun oluşturur
     */
    private void ortaSudokuOlustur() {
        int[][] sudoku = {{0, 0, 0, 0, 0, 0, 6, 8, 0},
                {0, 0, 0, 0, 7, 3, 0, 0, 9},
                {3, 0, 9, 0, 0, 0, 0, 4, 5},
                {4, 9, 0, 0, 0, 0, 0, 0, 0},
                {8, 0, 3, 0, 5, 0, 9, 0, 2},
                {0, 0, 0, 0, 0, 0, 0, 3, 6},
                {9, 6, 0, 0, 0, 0, 3, 0, 8},
                {7, 0, 0, 6, 8, 0, 0, 0, 0},
                {0, 2, 8, 0, 0, 0, 0, 0, 0}};

        gameBoardSolver.bolgeyeSudokuyuEkle(sudoku);
        gameBoardSolver.solveSudoku(sudoku, sudoku.length);

    }

    /**
     * Zor seviye oyun oluşturur
     */
    private void zorSudokuOlustur() {
        int[][] sudoku = {{0, 0, 5, 3, 0, 0, 0, 0, 0},
                {8, 0, 0, 0, 0, 0, 0, 2, 0},
                {0, 7, 0, 0, 1, 0, 5, 0, 0},
                {4, 0, 0, 0, 0, 5, 3, 0, 0},
                {0, 1, 0, 0, 7, 0, 0, 0, 6},
                {0, 0, 3, 2, 0, 0, 0, 8, 0},
                {0, 6, 0, 5, 0, 0, 0, 0, 9},
                {0, 0, 4, 0, 0, 0, 0, 3, 0},
                {0, 0, 0, 0, 0, 9, 7, 0, 0}};

        gameBoardSolver.bolgeyeSudokuyuEkle(sudoku);
        gameBoardSolver.solveSudoku(sudoku, sudoku.length);
    }

    /**
     * Kolay seviye oyun oluşyutur
     */
    private void kolaySudokuOlustur() {
        int[][] sudoku = {{0, 0, 0, 3, 0, 0, 7, 1, 0},
                {8, 1, 3, 0, 6, 7, 5, 2, 0},
                {6, 0, 5, 1, 2, 9, 4, 8, 0},
                {0, 0, 4, 0, 0, 0, 0, 0, 1},
                {3, 6, 0, 5, 9, 0, 0, 0, 0},
                {0, 0, 0, 0, 4, 6, 0, 5, 0},
                {7, 0, 6, 0, 0, 0, 0, 3, 2},
                {0, 0, 0, 9, 0, 0, 0, 6, 0},
                {1, 4, 0, 0, 0, 2, 0, 0, 5}};

        gameBoardSolver.bolgeyeSudokuyuEkle(sudoku);
        gameBoardSolver.solveSudoku(sudoku, sudoku.length);
    }

    /**
     * Paketin Client veya Server'a gönderildiği method
     *
     * @param msg paket
     */
    public void sendMessage(String msg) {

        // Eğer paketi gönderen Server ise
        if (status.matches("Server")) {

            // Paket göndermek için yeni bir iş parçacığı oluşturuluyor
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (ClientHandler clientHandler : singletonServer.getAr()) {
                        try {

                            // Eğer oyun modu sıralı ise
                            if (mod.matches(SIRALI_CODE)) {
                                final String[] finalMsg = {msg};

                                // Süre bittiğinde gönderilen mesaj değil ise
                                // Süre bittiğinde 1 yada 0 bilgisi gönderilir.
                                if (!msg.matches(CLIENT_CODE) && !msg.matches(SERVER_CODE)) {

                                    // Yapılan hamleyle oyun bitmediyse
                                    if (!checkFinish()) {
                                        final boolean[] check = new boolean[1];

                                        // Hamle bilgileri client'a gönderiliyor.
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Yapılan hamlenin doğru olup olmadığı konrol ediliyor ve puan güncelleniyor
                                                check[0] = gameBoardSolver.checkNumber(gameBoardSolver.selected_row, gameBoardSolver.selected_column);
                                                updateScore(status, check[0]);

                                                finalMsg[0] += "#" + (check[0] ? "1" : "0");
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            clientHandler.dos.writeUTF(GAME_CODE + "_" + finalMsg[0]);
                                                            clientHandler.dos.flush();
                                                            countDownTimer.cancel();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }).start();
                                            }
                                        });
                                    }
                                    // Eğer oyun bittiyse
                                    else {

                                        // Puan güncelleniyor
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateScore(status, true);
                                            }
                                        });

                                        // Oyunun bittiği client'a bildiriliyor
                                        clientHandler.dos.writeUTF(GAME_CODE + "_" + "finish");
                                        clientHandler.dos.flush();

                                        // Oyun bitti mesajı gösteriliyor ve ana ekrana dönülüyor
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                                builder.setTitle("Oyun Bitti");
                                                if (SERVER_PUAN > CLIENT_PUAN) {
                                                    builder.setMessage("Oyunu Kazandınız!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                } else if (CLIENT_PUAN > SERVER_PUAN) {
                                                    builder.setMessage("Diğer oyuncu kazandı!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                } else {
                                                    builder.setMessage("Beraberlik!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                                }

                                                builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(intent);
                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                }
                                // Eğer süre bittiğinde gönderilen mesaj ise
                                else {
                                    clientHandler.dos.writeUTF(GAME_CODE + "_" + msg);
                                    clientHandler.dos.flush();
                                    countDownTimer.cancel();
                                }

                            }
                            // Eğer oyun modu zamana karşı ise ve oyun bitti ise
                            else if (mod.matches(ZAMANA_KARSI_CODE) && checkFinish()) {

                                // Oyunun bittiği Client'a bildiriliyor
                                clientHandler.dos.writeUTF(GAME_CODE + "_" + "finish");
                                clientHandler.dos.flush();

                                // Zaman durduruluyor
                                timerTask.cancel();
                                timer.cancel();

                                // Oyun bitti mesajı gösteriliyor
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                        builder.setTitle("Oyun Bitti");
                                        builder.setMessage("Oyunu Kazandınız!");
                                        builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                            }
                                        });
                                        builder.show();
                                    }
                                });
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        // Eğer paketi gönderen Client ise
        else {
            // Paket göndermek için iş parçacığı başlatılıyor
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        // Eğer oyun modu sıralı ise
                        if (mod.matches(SIRALI_CODE)) {

                            final String[] finalMsg = {msg};

                            // Eğer süre bitiminde gönderilen mesaj değil ise
                            // Süre bittiğinde 1 veya 0 gönderiliyor
                            if (!msg.matches(CLIENT_CODE) && !msg.matches(SERVER_CODE)) {

                                // Eğer yapılan hamleyle oyun bitmediyse
                                if (!checkFinish()) {
                                    final boolean[] check = new boolean[1];

                                    // Hamle bilgileri Server'a gönderiliyor
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            // Yapılan hamlenin doğru yanlışlığı kontrol ediliyor ve puan güncelleniyor
                                            check[0] = gameBoardSolver.checkNumber(gameBoardSolver.selected_row, gameBoardSolver.selected_column);
                                            updateScore(status, check[0]);

                                            finalMsg[0] += "#" + (check[0] ? "1" : "0");

                                            // Hamle bilgisi gönderiliyor
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        singletonClient.getDos().writeUTF(GAME_CODE + "_" + finalMsg[0]);
                                                        singletonClient.getDos().flush();
                                                        countDownTimer.cancel();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }).start();
                                        }
                                    });
                                }
                                // Eğer oyun bitti ise
                                else {

                                    // Puan güncelleniyor.
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateScore(status, true);
                                        }
                                    });

                                    // Oyunun bittiği Server'a bildiriliyor
                                    singletonClient.getDos().writeUTF(GAME_CODE + "_" + "finish");
                                    singletonClient.getDos().flush();

                                    // Oyun bitti mesajı gösteriliyor ve ana ekran dönülüyor
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                            builder.setTitle("Oyun Bitti");
                                            if (SERVER_PUAN > CLIENT_PUAN) {
                                                builder.setMessage("Diğer oyuncu kazandı!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            } else if (CLIENT_PUAN > SERVER_PUAN) {
                                                builder.setMessage("Oyunu Kazandınız!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            } else {
                                                builder.setMessage("Beraberlik!\nPuan Durumu ;\nSERVER " + SERVER_PUAN + " / " + CLIENT_PUAN + " CLIENT");
                                            }
                                            builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                }
                                            });
                                            builder.show();
                                        }
                                    });
                                }

                            }
                            // Eğer süre bitiminde gönderilen paket ise
                            else {
                                singletonClient.getDos().writeUTF(GAME_CODE + "_" + msg);
                                singletonClient.getDos().flush();
                                countDownTimer.cancel();
                            }
                        }
                        // Eğer oyun modu zamana karşı ise ve oyun bitti ise
                        else if (mod.matches(ZAMANA_KARSI_CODE) && checkFinish()) {

                            // Oyunun bittiği Server'a bildiriliyor
                            singletonClient.getDos().writeUTF(GAME_CODE + "_" + "finish");
                            singletonClient.getDos().flush();

                            // Zaman durduruluyor
                            timerTask.cancel();
                            timer.cancel();

                            // Oyun bitti mesajı gösteriliyor ve ana ekran dönülüyor
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                                    builder.setTitle("Oyun Bitti");
                                    builder.setMessage("Oyunu Kazandınız!");
                                    builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                    builder.show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        if (mod.matches("1") && !(status.matches("Server") && msg.matches("0"))) {
            setVisibleButtons(View.INVISIBLE);
            countDownTimer.cancel();
            timeText.setText("Bekleniyor...");
        }
    }

    /**
     * Oyunun bitip bitmediğini kontrol eder
     * Oyun bittiyse true bitmediyse false bilgisi gönderir
     * @return
     */
    public boolean checkFinish() {
        return gameBoardSolver.checkFinish(getApplicationContext());
    }

    /**
     * Sıralı oyun modunda süresi biten kullanıcının butonlarını görünmez yapar puanını azaltır
     * Diğer oyuncuya sıra sende mesajı gönderir
     */
    public void sendPlayer() {

        if (player.matches("1")) {
            sendMessage("0");
        } else {
            sendMessage("1");
        }

        setVisibleButtons(View.INVISIBLE);
        updateScore(status, false);
    }


    /**
     * Tüm butonları belirtilen duruma göre görünmez yada görünür yapar
     * @param visible
     */
    public void setVisibleButtons(int visible) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                b1.setVisibility(visible);
                b2.setVisibility(visible);
                b3.setVisibility(visible);
                b4.setVisibility(visible);
                b5.setVisibility(visible);
                b6.setVisibility(visible);
                b7.setVisibility(visible);
                b8.setVisibility(visible);
                b9.setVisibility(visible);
            }
        });
    }

    /**
     * Oyun ekranından ayrılınca tüm bağlantılar kapanır
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (status.matches("Server")) {
                singletonServer.getServerSocket().close();
            } else if (status.matches("Client")) {
                singletonClient.getDis().close();
                singletonClient.getDos().close();
                singletonClient.getSocket().close();
            }

            singletonMessage.setNull();
            SingletonClient.setNullSingletonClient();
            SingletonServer.setNullSingletonServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}