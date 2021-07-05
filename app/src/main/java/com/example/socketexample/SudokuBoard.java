package com.example.socketexample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;


class SudokuBoard extends View {
    /**
     * SudokuBoard Sudoku tahtasinin basitce olusturuldugu (tasarlandigi)
     * ve hucrelerdeki default degerlerin,oyun esnasinda
     * eklenen degerlerin renginin belirlendigi siniftir.
     *
     */

    private final int boardColor;
    private final int cellFillColor;
    private final int cellsHighlightColor;

    private final int letterColor;//sudokuda default tanimlanan rakamlarin rengi

    private final int letterColorRight;//oyuncunun hucreye dogru girdigi rakam rengi
    private final int letterColorError;//oyuncunun hucreye yanlis girdigi rakam rengi

    private final Paint boardColorPaint = new Paint();
    //Paint sınıfı; geometrilerin, metinlerin nasil cizilecegine dair stil ve renk bilgilerini tutar.
    private final Paint cellFillColorPaint = new Paint();
    private final Paint cellsHighlightColorPaint = new Paint();

    private final Paint letterPaint = new Paint();
    private final Rect letterPaintBounds = new Rect();//dortgen nesnesi olusturulur.
    private int cellSize;

    private final Solver solver = new Solver();

    public SudokuBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SudokuBoard,
                0, 0);//tema icin gecerli nitelikler a dizisine atilir.
        //bir özniteliğin son değerini çözümlemek için TypedArray kullanilir.

        try{
            //cesitli yazi tasarimlari attrs.xml icerisinden buradaki degsikenlere okunmaktadir
            boardColor = a.getInteger(R.styleable.SudokuBoard_boardColor, 0);
            cellFillColor = a.getInteger(R.styleable.SudokuBoard_cellFillColor, 0);
            cellsHighlightColor = a.getInteger(R.styleable.SudokuBoard_cellsHighlightColor, 0);
            letterColor = a.getInteger(R.styleable.SudokuBoard_letterColor, 0);
            letterColorError = a.getInteger(R.styleable.SudokuBoard_letterColorError, 0);
            letterColorRight = a.getInteger(R.styleable.SudokuBoard_letterColorRight, 0);
        }finally {
            a.recycle();// islem bitince ayrılan belleğin kullanılabilir havuza hemen döndürülmesini saglar

        }
    }

    /**
     * onMeasure() sudoku tahtasi ve hucrelerinin boyutunun hesaplandigi method
     * @param width genislik
     * @param height yukseklik
     */
    @Override
    protected void onMeasure(int width, int height){

        super.onMeasure(width, height);

        int dimension = Math.min(this.getMeasuredWidth(), this.getMeasuredHeight());
        //boyut genislik ve yuksekligin minimum degeri kabul edilir.
        cellSize = dimension / 9;

        setMeasuredDimension(dimension, dimension);//boyut olusturulur.
    }

    /**
     * Özel bir görünüm çizmek icin onDraw () yöntemini geçersiz kılmak gerekir.
     * OnDraw () parametresi, görünümün kendisini çizmek için kullanabileceği bir Canvas nesnesidir.
     * Canvas sınıfı, metin, çizgi, bitmap ve diğer birçok temel grafik çizme yöntemlerini tanımlar.
     * UI oluşturmak için bu yöntemleri onDraw () içinde kullaniriz.
     * @param canvas Canvas sinifi nesnesi
     */
    @Override
    protected void onDraw(Canvas canvas){



        boardColorPaint.setStyle(Paint.Style.STROKE);//kontur boya tarzi set edilmektedir.
        boardColorPaint.setStrokeWidth(16);//kalinlik boyutu 16 secilir
        boardColorPaint.setColor(boardColor);//
        boardColorPaint.setAntiAlias(true);

        cellFillColorPaint.setStyle(Paint.Style.FILL);
        boardColorPaint.setAntiAlias(true);
        cellFillColorPaint.setColor(cellFillColor);//secilen hucre daha koyu bir maviye boyanir

        cellsHighlightColorPaint.setStyle(Paint.Style.FILL);
        boardColorPaint.setAntiAlias(true);
        cellsHighlightColorPaint.setColor(cellsHighlightColor);//hizasindaki satir-sutun hucreleri daha acik mavi renge boyanir

        letterPaint.setStyle(Paint.Style.FILL);//dolgulu cizim tarzi set edilmektedir
        letterPaint.setAntiAlias(true);
        letterPaint.setColor(letterColor);

        colorCell(canvas, solver.getSelectedRow(), solver.getSelectedColumn());
        canvas.drawRect(0, 0, getWidth(), getHeight(), boardColorPaint);
        drawBoard(canvas);//tahta ve sayilar cizilir
        drawNumbers(canvas);
    }

    /**
     * onTouchEvent() ile bos hucrelere tiklandigi zaman deger eklenebilme ozelligi kazandirilmistir
     * @param event gerceklesen olay/aktivite
     * @return aktiviteden gecerli veya gecersiz donus olmasi
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event){

        boolean isValid;

        float x = event.getX();//tiklanilan hucreye ait x ve y koordinatlari alinir.
        float y = event.getY();

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN){
            //eger olay bir tiklamaya esitse asagidaki gibi secilen hucrenin koordinatlari alinmaktadir
            solver.setSelectedRow((int) Math.ceil(y/cellSize));
            solver.setSelectedColumn((int) Math.ceil(x/cellSize));
            isValid = true;//gecerlilik degiskeni true donmektedir
        }
        else{
            isValid = false;//gecerlilik degiskeni hucrelerde bir secim islemi yok ise false donmektedir
        }


        return isValid;
    }

    /**
     *drawNumbers() sayilarin sudoku uzerine yazildigi method.
     * bu sayilar default sudokuya ait olabilir yahut sonradan kullanici tarafindan eklenmistir,
     * duruma gore renk alir(siyah-mavi-kirmizi)
     * @param canvas tuval
     */
    private void drawNumbers(Canvas canvas){

        letterPaint.setTextSize(cellSize);//yazi boyutu hucre boyutuna gore set edilmektedir.

        for (int r=0; r<9; r++){
            for (int c=0; c<9; c++){
                if (solver.getBoard()[r][c] != 0){
                    //eger sudoku tahtasina bir deger yerlestiriliyorsa  bu blok calisir
                    String text;
                    if( (solver.getBoard()[r][c] ==solver.cozum[r][c])&&solver.skor[r][c]==0){
                        //eger tahtadaki deger ile cozum matrisindeki deger eslesiyorsa bu blok calisir

                        letterPaint.setColor(letterColor);
                        if (solver.soru[r][c] == 0){
                            //deger oyuncu tarafindan hucreye yerlestirilmis ve dogru ise rakam mavi renk alir
                            letterPaint.setColor(letterColorRight);

                        }
                        text = Integer.toString(solver.getBoard()[r][c]);
                    }
                    else{//eger cozum matrisi ile hucreye yerlestirilen deger uyusmuyor ise rakam kirmizi olarak ekranda gozukur
                        letterPaint.setColor(letterColorError);
                        text = Integer.toString(solver.getBoard()[r][c]);
                    }

                    float width, height;

                    letterPaint.getTextBounds(text, 0, text.length(), letterPaintBounds);
                    width = letterPaint.measureText(text);
                    height = letterPaintBounds.height();


                    canvas.drawText(text, (c*cellSize) + ((cellSize - width)/2),
                            (r*cellSize+cellSize) - ((cellSize - height)/2),
                            letterPaint);//eger default sudoku degerleri tahtaya ekleniyorsa siyah renkteki letterPaint tasarimi tercih edilir.

                }
            }

        }



        for (ArrayList<Object> letter : solver.getEmptyBoxIndex()){

            int r = (int) letter.get(0);
            int c = (int) letter.get(1);

            String text = Integer.toString(solver.getBoard()[r][c]);//tahtadaki degeri okur
            float width, height;

            letterPaint.getTextBounds(text, 0, text.length(), letterPaintBounds);
            width = letterPaint.measureText(text);//genislik ve yukseklik alinir
            height = letterPaintBounds.height();

            canvas.drawText(text, (c*cellSize) + ((cellSize - width)/2),//hucre cizilir
                    (r*cellSize+cellSize) - ((cellSize - height)/2),
                    letterPaint);
        }

    }

    /**
     * sudoku icerisinde tiklanan hucreye bagli olarak ayni hizadaki satir ve sutunlar da maviye boyanmaktadir.
     * colorCell() hucre iclerinin fosforlu mavi ile boyanmasini saglayan method
     *Canvas, Android'de farklı nesnelerin ekrana 2D çizimini gerçekleştiren bir sınıftır.
     * @param canvas Canvas sinifindan uretilmis cizimlerimizi destekleyen nesnedir.
     * @param r satir
     * @param c sutun
     */
    private void colorCell(Canvas canvas, int r, int c){
        if(solver.getSelectedColumn() != -1 && solver.getSelectedRow() != -1){
            canvas.drawRect((c-1)*cellSize, 0, c*cellSize, cellSize*9,
                    cellsHighlightColorPaint);//dortgensel bir sekil mavi renkte kanvasta cizilmektedir.

            canvas.drawRect(0, (r-1)*cellSize, cellSize*9, r*cellSize,
                    cellsHighlightColorPaint);

            canvas.drawRect((c-1)*cellSize, (r-1)*cellSize, c*cellSize, r*cellSize,
                    cellsHighlightColorPaint);
        }

        invalidate();

    }

    /**
     *drawThickLine() kalin cizgilerin tahta uzerine uygulanmasini saglayan method.
     */
    private void drawThickLine(){
        boardColorPaint.setStyle(Paint.Style.STROKE);//icerisi fill edilmeyen kontur boya tarzi kullanilmistir
        boardColorPaint.setStrokeWidth(10);//boya genisligi kalin cizgi icin 10 set edilmistir
        boardColorPaint.setColor(boardColor);//boya rengi set edilmistir
    }

    /*
     *drawThinLine() ince cizgilerin tahta uzerine uygulanmasini saglayan method.
     */
    private void drawThinLine(){
        boardColorPaint.setStyle(Paint.Style.STROKE);//icerisi fill edilmeyen kontur boya tarzi kullanilmistir
        boardColorPaint.setStrokeWidth(4);//boyanin genisligi 4 set edilmistir
        boardColorPaint.setColor(boardColor);//boya rengi set edilmistir
    }

    /**
     * drawBoard() sudoku tahtasindaki hucre cizgilerinin olusturuldugu method
     * Canvas, Android'de farklı nesnelerin ekrana 2D çizimini gerçekleştiren bir sınıftır.
     * @param canvas Canvas sinifindan uretilmis cizimlerimizi destekleyen nesnedir.
     */
    private void drawBoard(Canvas canvas){
        for (int c = 0; c < 10; c++){
            if (c%3 == 0){
                drawThickLine();
            }
            else{
                drawThinLine();
            }
            canvas.drawLine(cellSize * c, 0,
                    cellSize * c, getWidth(), boardColorPaint);
            //verilen koordinatlar arasinda canvas uzerine cizgilerin cizilmesi saglanir.

        }

        for (int r = 0; r < 10; r++){
            if (r%3 == 0){
                drawThickLine();//her 9 hucreden olusan karesel bolgenin etrafina kalin cizgi cekilmektedir.
            }
            else{
                drawThinLine();//ara satirlardaki ince hucre cizgileri cizilmektedir
            }

            canvas.drawLine(0, cellSize * r,
                    getWidth(), cellSize * r, boardColorPaint);
        }

    }

    /**
     *
     * @return Solver sinifindan uretilen solver nesnesini dondurur.
     */
    public Solver getSolver(){
        return this.solver;
    }


}