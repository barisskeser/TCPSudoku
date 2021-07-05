package com.example.socketexample;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;


class Solver extends Application {


    int[][] board;//tahta matrisi
    ArrayList<ArrayList<Object>> emptyBoxIndex;
    int[][] doluPozisyonlar;//default matris konumlarini tutar
    int[][] cozum;//kod tarafindan cozulen cozum matrisi
    int[][] soru;//hazir verilen soru matrisi
    int[][] skor;


    int selected_row;//secilen satir
    int selected_column;//secilen sutun


    Solver() {
        selected_row = -1;
        selected_column = -1;
        doluPozisyonlar = new int[9][9];
        cozum = new int[9][9];
        soru = new int[9][9];
        skor = new int[9][9];

        board = new int[9][9];

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;//bu dizilere kontrol icin default 0 atamasi yapilir
                doluPozisyonlar[r][c] = 0;
                skor[r][c] = 0;
            }
        }


        emptyBoxIndex = new ArrayList<>();//bos indexler icin arraylist olusturulur
    }

    /**
     *isSafe()Sudokuya yerlestirilen sayinin bulundugu satir ve sutunda ayni deger bulunmamasi gerekir.
     * ayni zamanda sahip oldugu 3x3 luk kare alani icerisinde de essiz olmasi gerekir.
     * bu method ile onun kontrolunu saglariz.
     * @param board sudoku tahtasi matrisi
     * @param row satir
     * @param col sutun
     * @param num alinan sayi
     * @return essiz ise dogru dondur
     */
    public boolean isSafe(int[][] board, int row, int col, int num) {
        // satir benzersiz numaralara sahiptir
        for (int d = 0; d < board.length; d++) {

            // Yerleştirmeye çalıştığımız numaranın
            // o satırda zaten mevcut olup olmadığını kontrol et, var ise yanlış döndür;
            if (board[row][d] == num) {
                return false;
            }
        }

        // sutun benzersiz numaralara sahiptir
        for (int r = 0; r < board.length; r++) {

            // Yerleştirmeye çalıştığımız numaranın
            // o sutunda zaten mevcut olup olmadığını kontrol et, var ise yanlış döndür;
            if (board[r][col] == num) {
                return false;
            }
        }

        //Karşılık gelen karenin benzersiz bir numarası vardır (kutu çakışması)
        int sqrt = (int) Math.sqrt(board.length);
        int boxRowStart = row - row % sqrt;
        int boxColStart = col - col % sqrt;

        for (int r = boxRowStart;
             r < boxRowStart + sqrt; r++) {
            for (int d = boxColStart;
                 d < boxColStart + sqrt; d++) {
                if (board[r][d] == num) {
                    //eger 3x3 luk kare sekli icerisinde ayni numara bir kez daha geciyorsa
                    //cakisma olur false doner
                    return false;
                }
            }
        }

        // cakisma yok ise guvenlidir
        return true;
    }

    /**
     *solveSudoku() verilen soru sudokusunu bilgisayarin otomatik cozmesi
     * @param board sudoku tahtasi
     * @param n sudokunun boyutu
     * @return bilgisayar kod ile sudokuyu tamamiyle cozmus ise true dondurur
     */
    public boolean solveSudoku(
            int[][] board, int n) {
        int row = -1;
        int col = -1;
        boolean isEmpty = true;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 0) {//bos olan hucrenin konumu alinmaktadir.
                    row = i;
                    col = j;

                    // Sudokuda hala bazı eksik değerlerimiz var
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                break;
            }
        }

        // Hicbir eksik deger kalmadi
        if (isEmpty) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    cozum[i][j] = board[i][j];//cozum matrisinin ici bilgisayarin cozumu ile doldurulur
                }
            }
            return true;
        }


        for (int num = 1; num <= n; num++) {
            if (isSafe(board, row, col, num)) {  // Eger hucreye yerlesecek deger guvenli ise
                board[row][col] = num;//sayi tahtaya eklenir
                if (solveSudoku(board, n)) {//solveSudoku methodundan dogru deger gelmesi gerekir

                } else {//yerini degistir.
                    board[row][col] = 0;
                }
            }
        }
        return false;
    }

    /**
     *checkFinish() sudokudaki tum kontrollerin tamamlanmasi ile true donen method.Eger basariyla tahta
     * tamamlanmis ise true doner.
     * @param context Context nesnesi
     * @return tahta basarili tamamlanmis ise true doner.
     */
    public boolean checkFinish(Context context) {

        int i, j;

        System.out.println("check");

        for (i = 0; i < 9; i++) {
            for (j = 0; j < 9; j++) {
                if (this.board[i][j] == 0 || this.board[i][j] != cozum[i][j]) {
                    //eger board matrisindeki deger sifira esit veya cozum ile eslesmiyorsa
                    return false;//checkFinish false dondurur
                }
            }
        }
        return true;//tum kontroller bittiginde sudoku basariyla tamamlanmis ise true doner.
    }

    /**
     *setNumberPos() default sudokudan gelen degerlerin tahtaya yerlestirilmesini ve
     * oyuncu tarafindan degisiminin engellenmesini saglar.
     * Context Android sistemi tarafından soyut bir şekilde bize sunulan,
     * uygulamamız hakkında global bilgiye sahip arayüzdür.Sistem veya
     * yerel bazda kaynaklara erismek icin kullanilir
     * @param num hucreye eklenecek sayi
     * @param context Context nesnesi
     */
    public void setNumberPos(int num, Context context) {
        System.out.println("Satir:" + this.selected_row + "  Sutun:" + this.selected_column);
        if (doluPozisyonlar[this.selected_row - 1][this.selected_column - 1] != -5) {
            //eger default sudoku degeri olmayan bir hucrede sayi eklemek istersek basarili oluruz
            this.board[this.selected_row - 1][this.selected_column - 1] = num;

        } else {//default sudoku degerini degistirmeye calistigimizda
            // hata mesajinin ekranda gosterilmesi
            Toast.makeText(context, "Hata!!Sudokunun bu hücresi değiştirilemez.", Toast.LENGTH_SHORT).show();
        }
    }


    public int[][] getBoard() {//sudoku tahtasi icin getter
        return this.board;
    }

    /**
     *getEmptyBoxIndex()sudokudaki bos indexler dondurulmektedir
     * @return arraylist seklinde indexler dondurulur
     */
    public ArrayList<ArrayList<Object>> getEmptyBoxIndex() {
        return this.emptyBoxIndex;
    }
    //private degiskenler icin getter ve setterlar
    public int getSelectedRow() {
        return selected_row;
    }

    public int getSelectedColumn() {
        return selected_column;
    }

    public void setSelectedRow(int r) {
        selected_row = r;
    }

    public void setSelectedColumn(int c) {
        selected_column = c;
    }

    /**
     * bolgeyeSudokuyuEkle() default soru matrisinin oyun tahtasina yerlestirilmesini saglar
     * @param sudoku sudoku soru matrisi
     */
    public void bolgeyeSudokuyuEkle(int[][] sudoku) {
        int i, j;
        for (i = 0; i < 9; i++) {
            for (j = 0; j < 9; j++) {
                if (sudoku[i][j] != 0) {
                    //eger hucrede 0 yazmiyor ise sayiyi tahtaya istenen pozisyonda ekle
                    soru[i][j] = this.board[i][j] = sudoku[i][j];
                    //sudokunun bu hucresi soru matrisine atanir ve arayuzumuzdeki tahtaya yerlestirilir
                    doluPozisyonlar[i][j] = -5;
                    //dolu pozisyonlar matrisi hangi konumdaki hucrelerin default matris ile dolduruldugunu tutmaktadir
                }
            }
        }
    }

    /**
     * checkNumber() tahtadan alinan deger ile cozum matrisindeki deger uyusuyor ise true dondurmektedir
     * @param row satir
     * @param col sutun
     * @return true - false
     */
    public boolean checkNumber(int row, int col) {
        if (this.board[row - 1][col - 1] == cozum[row - 1][col - 1]) {
            return true;
        }

        return false;
    }
}