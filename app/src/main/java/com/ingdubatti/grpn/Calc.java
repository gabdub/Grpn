package com.ingdubatti.grpn;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;


import java.util.FormatFlagsConversionMismatchException;

class Calc {
    static final int NORMAL= 0;
    static final int SHIFT= 1;
    static final int HEXA= 2;
    static final int TRIG= 4;
    static final int MACRO= 8;
    private static final int _KEY_MULT= 16;

    static final int hicolor= 0xff0000c0;  //dark blue
    static final int locolor= 0xff000000;  //black
    static final int edbackcolor= 0xFFFFFFFF;  //white
    static final int backcolor= 0xFFFFF5E9;  //light yellow

    private static final int MAXSTACK= 100;
    static final int MAXVARS= 6;   //A...F

    static final int MAXMACROS= 4;
    static final int MAX_MACRO_STEPS= 100;
    static final long[][] macros= new long[MAXMACROS][MAX_MACRO_STEPS];
    static final int[] macro_len= new int[MAXMACROS];

    int modo= NORMAL;
    private int next_modo= NORMAL;

    boolean editando= false;
    boolean macro_rec= false;
    boolean allow_macro_rec = false;
    int current_macro= 0;

    final double[] stack= new double[MAXSTACK];
    int stacklen= 0;
    private double stack_x; //op arguments
    private double stack_y;
    private double stack_z;

    final double[] vars= new double[MAXVARS];

    final StringBuffer editLine = new StringBuffer();

    double undoX= 0;
    double undoY= 0;
    double undoZ= 0;
    int undoN= 0; //0=no hay, 1=X, 2=XY, 3=XYZ

    int undo_stacklen= 0;
    private boolean stack_change= false;

    public interface action {
        void doAction();
    }

    private final HashMap<Long,String> hmKTxt = new HashMap<>(); //button text
    private final HashMap<Long,action>  hmKAct = new HashMap<>(); //button action
    private final HashMap<Long,Boolean> hmKHi  = new HashMap<>(); //button text highlight
    private final HashMap<Long,Long> hmKmap = new HashMap<>();    //button key mode mapping
    private final ArrayList<Integer> alModK = new ArrayList<>();   //IDs of buttons with modifiers

    private final MainActivity activity;

    Calc(MainActivity activity) {
        this.activity= activity;
    }

    @SuppressLint("DefaultLocale")
    private String convertStackPos(int pos, boolean decorate ) {   //stack pos: 1=x, 2=y, ...
        StringBuilder r= new StringBuilder();
        String sr = "";
        if( (pos > 0) && (pos <= stacklen) ) {
            double v = stack[stacklen - pos];
            try {
                if ((modo & HEXA) != 0) {
                    if (decorate) {
                        r.append("0x"); //with 0x prefix
                    }
                    double intpart= Math.floor(v);
                    long ln = Math.round( intpart );
                    r.append(Long.toHexString(ln).toUpperCase());

                    double decpart= Math.floor((v - intpart) * 65536.0 );
                    ln = Math.round( decpart );
                    if(ln != 0){
                        String s= "0000" + Long.toHexString(ln).toUpperCase();
                        int slen= s.length();
                        r.append(".").append(s.substring(slen - 4, slen));
                    }
                    sr = r.toString();
                } else {
                    if ((v < 0.1)&&(v > -0.1)) {
                        sr = String.format("%.12e", v); //|v| < 1.0 => force exponent notation
                    }else if (decorate) {
                        sr = String.format("%,.12g", v); //with 1000s separators
                    } else {
                        sr = String.format("%.12g", v);  //without 1000s separators
                    }
                    //split mantisa and exponent
                    String[] rp = sr.split("e");
                    //sr=  1.20e3  1.20   10e3  0e0
                    //rp0= 1.20    1.20   10    0
                    //rp1=     3   -        3    0
                    int n = rp[0].length();
                    if (sr.indexOf('.') >= 0) { //remove trailing decimal 0s
                        //sr=  1.20e3  1.20
                        //rp0= 1.20    1.20
                        //rp1=     3   -
                        while ((n > 0) && (rp[0].charAt(n - 1) == '0')) {
                            n--;
                        }
                        if ((n > 0) && (rp[0].charAt(n - 1) == '.')) {
                            n--;    //no decimal left, remove the dot
                        }
                        sr = rp[0].substring(0, n);
                        //sr=  1.2    1.2
                        //rp0= 1.20   1.20
                        //rp1=     3  -
                    }else{
                        //sr=  10   0e0
                        //rp0= 10   0
                        //rp1=   3   0
                        sr = rp[0];
                    }
                    //sr=  1.2     [1.2]  10   0
                    //rp1=    3    -        3   0
                    if (rp.length > 1) {    //join mantisa and exponent
                        //sr=  1.2    10   0
                        //rp1=    3     3   0
                        //force +/-3*N exponent and replace "0e0" ==> "0"
                        int ex = Integer.parseInt(rp[1]);
                        if (ex != 0) {
                            //sr=  1.2  10
                            if (ex % 3 != 0) {
                                //move decimal point to the right / reduce exponent one or two times
                                //split mantisa
                                String[] mp = sr.split("\\.");
                                //mp0= 1        10
                                //mp1=  2        -
                                r.append(mp[0]);
                                //r=   1        10
                                String d= "";
                                if (mp.length > 1) d= mp[1];
                                while (ex % 3 != 0) {
                                    if (!d.isEmpty()) {
                                        r.append(d.charAt(0));
                                        d = d.substring(1);
                                    }else{
                                        r.append('0');
                                    }
                                    ex--;
                                }
                                if (!d.isEmpty()) {
                                    r.append('.').append(d);
                                }
                                //r= 12.2 100
                            }else{
                                //use current mantisa
                                r.append(sr);
                                //r= 1.2 10
                            }
                            //r= (12.2 1.2) (100 10)
                            if (ex != 0) r.append('e').append(ex);
                            sr = r.toString();
                            //sr= r= (12.2e3 1.2e3) (100e3 10e3)
                        }
                    }
                    //sr= (12.2e2 1.2e3) 1.2 (100e3 10e3) 0
                    if (sr.equals("-0")) sr= "0";   //don't show "-0"
                }
            } catch (FormatFlagsConversionMismatchException e) {
                activity.showInfo(e.getMessage());
            }
        }
        return sr;
    }

    void updateStack() {
        TextView txt1 = activity.findViewById(R.id.val1);
        TextView txt2 = activity.findViewById(R.id.val2);
        TextView txt3 = activity.findViewById(R.id.val3);
        TextView txt4 = activity.findViewById(R.id.val4);
        if( editando ){
            txt1.setText(editLine);
            txt1.setBackgroundColor(edbackcolor); //blanco
        }else{
            txt1.setText(convertStackPos(1, true)); //show x with decorators
            txt1.setBackgroundColor(backcolor); //default
        }
        txt2.setText(convertStackPos(2, true)); //show y with decorators
        txt3.setText(convertStackPos(3, true)); //show z with decorators
        txt4.setText(convertStackPos(4, true)); //show t with decorators
    }

    private double readX() {
        if (stacklen > 0)   return stack[stacklen - 1];
        return 0;
    }

    private double popX() {
        if (stacklen > 0) {
            stacklen--;
            return stack[stacklen];
        }
        return 0;
    }

    private void replaceX( double v ) {
        if (stacklen > 0)   stack[stacklen - 1]= v;
    }

    private void pushStack( double v) {
        if( stacklen >= MAXSTACK ){
            //the stack is full: drop the oldest value
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(stack, 1, stack, 0, MAXSTACK - 1);
            stacklen= MAXSTACK-1;
        }
        stack[stacklen]= v;
        stacklen++;
        undo_stacklen= 0; //cancel CLEAR stack undo
    }

    private double parseHexa(String sh) {
        double val= 0.0;
        int p= sh.indexOf('-'); //negative?
        if (p >= 0) sh= sh.substring(p+1);
        int len = sh.length();
        if (len > 0) {
            if (len > 14) {  //too long
                sh= sh.substring(len-14, len);
                len= 14;
            }
            while (len > 0){
                if (len > 4) {
                    val += Integer.parseInt(sh.substring(0,4), 16);
                    sh= sh.substring(4);
                }else{
                    val += Integer.parseInt(sh, 16);
                }
                len -= 4;
                if (len >= 4) {
                    val *= 65536.0;
                }else if (len == 3) {
                    val *= 4096.0;
                }else if (len == 2) {
                    val *= 256.0;
                }else if (len == 1) {
                    val *= 16.0;
                }
            }
            if (p >= 0) return -val;
        }
        return val;
    }

    private boolean finEdit() {
        if( editando ){
            if(stacklen > 0) {
                if(editLine.length() == 0){
                    //linea vacia, cancela edit
                    stacklen--; //quita el 0
                }else {
                    try {
                        String in= editLine.toString();
                        double val= 0;
                        if ((modo & HEXA) != 0) {
                            int pp= in.indexOf('.');
                            if( pp >= 0){//con punto decimal
                                if( pp > 0){    //lee parte entera
                                    val = parseHexa(in.substring(0,pp));
                                }
                                String decp= in.substring(pp+1);
                                int len = decp.length();
                                if (len > 6) {  //too long
                                    decp = decp.substring(0, 6);
                                    len= 6;
                                }
                                if (len > 0) {
                                    val += parseHexa(decp) / Math.pow(16, len);
                                }
                            }else { //sin decimales
                                val = parseHexa(in);
                            }
                        }else{
                            val = Double.parseDouble(in);
                        }
                        replaceX( val );
                    } catch (NumberFormatException e) {
                        activity.showInfo(e.getMessage());
                        return false; //ERROR: no sale de edicion
                    }
                }
            }
            editando = false;
        }
        stack_change= true; //fuerza redraw del stack al terminar la accion actual
        return true;
    }

    private void startEdit() {
        if( !editando ){
            editando= true;
            pushStack(0); //0 en el stack por ahora (muestra editLine)
            editLine.setLength(0);
        }
    }

    private boolean getOpArgs_NO_UNDO(int n) {  //get stack_x/y/z
        if( finEdit() ) {       //termina edicion en curso (fuerza update del stack al terminar la accion)
            if(stacklen >= n) {
                if (n >= 1) stack_x= popX();    //pop X argument
                if (n >= 2) stack_y= popX();    //pop Y argument
                if (n >= 3) stack_z= popX();    //pop Z argument
                return true;    //OK: hay argumentos suficientes
            }
        }
        return false;           //error en input o faltan argumentos
    }

    private boolean getOpArgs(int n) {  //get stack_x/y/z (SAVE then for UNDO)
        if( getOpArgs_NO_UNDO(n) ) {
            undoN= n;           //salva undo de argumentos
            undoX = stack_x;
            undoY = stack_y;
            undoZ = stack_z;
            return true;        //OK: hay argumentos suficientes
        }
        return false;           //error en input o faltan argumentos
    }

    private long getKmode(long k0, int m) {
        Long lv= hmKmap.get(k0 + m);
        if (lv != null) {
            return lv;
        }
        return k0;
    }

    private boolean getKhi(long k) {
        Boolean bv= hmKHi.get(k);
        if (bv != null) {
            return bv;
        }
        return false;
    }

    private String getKtxt(long k) {
        String txt= "";
        try{
            txt = hmKTxt.get(k);
        }catch(NullPointerException ignored){}
        return txt;
    }

    void updateMode() {  //ajusta TODOS los botones segun el modo actual (texto y hilight)
        for (int id: alModK) {
            Button b = activity.findViewById(id);
            if (b != null) {
                long k0= (long) id * _KEY_MULT;
                long k= getKmode(k0, modo);
                b.setText(getKtxt(k));
                if (getKhi(k)) {
                    b.setTextColor(hicolor);
                }else{
                    b.setTextColor(locolor);
                }
            }
        }
    }

    private void onKeyClick(View view) {    //evento de tecla presionada
        //luego de ejecutar la accion de la tecla borra el flag de SHIFT
        next_modo= modo & ~SHIFT;

        //busca la definicion de la tecla segun el modo actual
        int id = view.getId();
        long k0= (long) id * _KEY_MULT;
        long k = getKmode(k0, modo);
        if (hmKAct.containsKey(k)) {
            action act= hmKAct.get(k);
            if (act != null){
                allow_macro_rec = true;  //allow the action to exclude itself from the macro recording
                act.doAction();     //ejecuta la accion de la tecla
                if( (macro_rec) && (allow_macro_rec) ){
                    int n= macro_len[current_macro];
                    macros[current_macro][n]= k;    //save k in macro list
                    n++;
                    macro_len[current_macro]= n;
                    if( n >= MAX_MACRO_STEPS){  //no more room, stop recording
                        op_end_macro();
                    }
                }
            }
        }

        //cuando cambio el modo: ajusta solo los botones que hagan falta (texto y hilight)
        if (modo != next_modo){
            for (int kid: alModK) {
                long ki0= (long) kid * _KEY_MULT;
                long km1= getKmode(ki0, modo);
                long km2= getKmode(ki0, next_modo);
                if (km1 != km2) {     //cambia el mapeo de este boton, actualizarlo
                    Button b = activity.findViewById(kid);
                    if (b != null) {
                        b.setText(getKtxt(km2));
                        boolean h1= getKhi(km1);
                        boolean h2= getKhi(km2);
                        if (h1 != h2) { //cambia el highlight de este boton, actualizarlo
                            if (h2) {
                                b.setTextColor(hicolor);
                            }else {
                                b.setTextColor(locolor);
                            }
                        }
                    }
                }
            }
            modo= next_modo;    //setea el nuevo modo
        }
        if (stack_change) {
            stack_change= false;
            updateStack();
        }
    }

    //Key definition
    void defKey(int keyid, int modif, String txt, boolean hilight, action act ) {
        long k0= (long) keyid * _KEY_MULT;
        long k=  k0 + modif;
        if (!hmKTxt.containsKey(k0)) {
            //conecta una sola vez cada tecla con un evento generico
            Button bKey = activity.findViewById(keyid);
            if( bKey != null ){//ignorar si el layout no usa esta tecla
                bKey.setOnClickListener(this::onKeyClick);
            }
            //la primera definicion deberia ser NORMAL
            if (modif != NORMAL) {  //si no es, inserta una normal vacia
                hmKTxt.put(k0,"");
            }
        }
        hmKTxt.put(k,txt);      //texto del boton
        hmKHi.put(k,hilight);   //hilight texto
        hmKAct.put(k,act);      //accion a ejecutar

        hmKmap.put(k,k);    //always map to itself
        if (modif == NORMAL) {  //set default (all modes --> NORMAL)
            for (int i= 1; i < _KEY_MULT; i++){
                hmKmap.put(k0+i, k);
            }
        }else if (modif == SHIFT) {  //set SHIFT default
            hmKmap.put(k+HEXA,k);
            hmKmap.put(k+TRIG,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+HEXA+TRIG,k);
            hmKmap.put(k+HEXA+MACRO,k);
            hmKmap.put(k+TRIG+MACRO,k);
            hmKmap.put(k+HEXA+TRIG+MACRO,k);
        }else if (modif == HEXA) {  //set HEXA default
            hmKmap.put(k+SHIFT,k);
            hmKmap.put(k+TRIG,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+TRIG+SHIFT,k);
            hmKmap.put(k+SHIFT+MACRO,k);
            hmKmap.put(k+TRIG+MACRO,k);
            hmKmap.put(k+TRIG+SHIFT+MACRO,k);
        }else if (modif == HEXA+SHIFT) {  //set HEXA+SHIFT default
            hmKmap.put(k+TRIG,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+TRIG+MACRO,k);
        }else if (modif == TRIG) {  //set TRIG default
            hmKmap.put(k+SHIFT,k);
            hmKmap.put(k+HEXA,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+HEXA+SHIFT,k);
            hmKmap.put(k+SHIFT+MACRO,k);
            hmKmap.put(k+HEXA+MACRO,k);
            hmKmap.put(k+HEXA+SHIFT+MACRO,k);
        }else if (modif == TRIG+SHIFT) {  //set TRIG+SHIFT default
            hmKmap.put(k+HEXA,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+HEXA+MACRO,k);
        }else if (modif == HEXA+TRIG) {  //set HEXA+TRIG default
            hmKmap.put(k+SHIFT,k);
            hmKmap.put(k+MACRO,k);
            hmKmap.put(k+SHIFT+MACRO,k);
        }else if (modif == MACRO) {  //set MACRO default
            hmKmap.put(k+SHIFT,k);
            hmKmap.put(k+HEXA,k);
            hmKmap.put(k+HEXA+SHIFT,k);
            hmKmap.put(k+TRIG,k);
            hmKmap.put(k+TRIG+SHIFT,k);
            hmKmap.put(k+HEXA+TRIG,k);
            hmKmap.put(k+HEXA+TRIG+SHIFT,k);
        }else if (modif == MACRO+SHIFT) {  //set MACRO+SHIFT default
            hmKmap.put(k+HEXA,k);
            hmKmap.put(k+TRIG,k);
            hmKmap.put(k+HEXA+TRIG,k);
        }
        if (modif != NORMAL) {
            //registra las teclas que usan modificadores
            if (!alModK.contains(keyid)) alModK.add(keyid);
        }
    }

    /* --------------- key actions ----------------- */
    void set_mode_flag(int modeflg, boolean on_off) {    //set SHIFT/HEXA/TRIG mode on/off
        allow_macro_rec= false; //don't save keys that change the mode of the keyboard

        if ((modeflg == HEXA) && (!finEdit())) return;
        if (on_off) {
            next_modo |= modeflg;       //setea flag de modo
        }else{
            next_modo &= ~modeflg;      //resetea flag de modo
        }
        if (modeflg == HEXA) stack_change= true; //fuerza update del stack al terminar la accion
    }

    void edit_x() {             //"EDIT"
        if (!editando) {
            String v= "";
            if (stacklen > 0) {
                v= convertStackPos(1, false); //remove "0x" and 1000s decorators
                stacklen--;
            }
            startEdit();
            editLine.append(v);
            stack_change= true;
        }
    }

    void edit_enter() {           //"ENTER"
        if (editando) {
            finEdit();
        } else if (stacklen > 0) {
            pushStack( readX() );   //Duplicar
            stack_change= true;
        }
    }

    void edit_append(char key){
        startEdit();
        editLine.append(key);
        stack_change= true;
    }

    void edit_point(){
        if( (!editando) ||  //agrega un solo '.' y nunca luego de una 'e'
            ((editLine.toString().indexOf('.') < 0) && (editLine.toString().indexOf('e') < 0)) ){
            edit_append('.');
        }
    }

    void edit_delete() {        //"DEL"
        if( editando ){         //delete last char
            if(editLine.length() == 0){
                //linea vacia, cancela edit
                stacklen--; //quita el 0
                editando = false;
            }else{
                editLine.setLength(editLine.length() - 1);
            }
        }else{                  //DROP
            getOpArgs(1);       //remove X (save UNDO)
        }
        stack_change= true;     //fuerza update del stack al terminar la accion
    }

    void edit_undo() {          //"UNDO"
        if( editando ) {        //cancela edit
            stacklen--;         //quita el 0
            editando = false;
        }else{
            if( (undo_stacklen > 0) && (stacklen == 0) ){
                //undo CLEAR STACK
                stacklen= undo_stacklen;
                undo_stacklen= 0;
            }else {
                //recupera argumentos de la ultima OP
                if (undoN > 2) {
                    pushStack(undoZ);
                }
                if (undoN > 1) {
                    pushStack(undoY);
                }
                if (undoN > 0) {
                    pushStack(undoX);
                }
                undoN = 0;
            }
        }
        stack_change= true;     //fuerza update del stack al terminar la accion
    }

    void edit_signo() {         //"+/-"
        if( editando ){
            //cambia signo de la edicion
            if (editLine.length() == 0) {
                //linea vacia, pone '-'
                edit_append('-');
            }else {
                //la linea ya tiene digitos
                int epos= editLine.toString().indexOf('e');
                if( epos < 0 ){
                    //no tiene exponente: cambia signo al comienzo
                    char cbeg= editLine.charAt(0);
                    if( cbeg == '+'){
                        editLine.setCharAt(0,'-');
                    }else if( cbeg == '-'){
                        editLine.setCharAt(0,'+');
                    }else{
                        //inserta '-' al principio
                        editLine.insert(0,'-');
                    }
                }else{
                    //cambia signo del exponente
                    if( epos == editLine.length()-1){
                        //nada luego de la 'e'
                        edit_append('-');
                    }else{
                        char cexp= editLine.charAt(epos+1);
                        if( cexp == '+'){
                            editLine.setCharAt(epos+1,'-');
                        }else if( cexp == '-'){
                            editLine.setCharAt(epos+1,'+');
                        }else{
                            //inserta '-' al principio
                            editLine.insert(epos+1,'-');
                        }
                    }
                }
            }
        }else if(stacklen > 0){
            replaceX( -readX() ); //cambia signo de X
        }
        stack_change= true;     //fuerza update del stack al terminar la accion
    }

    void edit_exp() {           //"EEX"
        if (!editando)  edit_append('1');
        if (editLine.toString().indexOf('e') < 0)   edit_append('e');   //permite una sola 'e'
    }

    //------------------ MENU ACTIONS -----------------
    String menu_copy_str() { //get the string to copy to the clipboard
        //format: X as a string with "0x" prefix but without 1000s decorators
        String txt = "";
        boolean hexa= ((modo & HEXA) != 0);
        if (editando) {
            txt = editLine.toString();
            if (hexa) txt = "0x" + txt;
        } else if (stacklen > 0) txt = convertStackPos(1, hexa);
        return txt;
    }

    void menu_paste_str(String txt) { //paste string from clipboard into editLine
        if (!txt.isEmpty()) {
            if (txt.startsWith("0x") || txt.startsWith("0X")) {
                //hexa
                txt= txt.substring(2).toUpperCase(); //remove 0x and force upper case
                if ((modo & HEXA) == 0) {
                    //force hexa mode without closing the current edit
                    modo |= HEXA;
                    updateMode();
                }
            }else{
                //decimal
                txt= txt.toLowerCase(); //force exponent in lower case
                //TO DO: paste +-Infinity / NaN
            }
            startEdit();
            editLine.append(txt);
            updateStack();
        }
    }

    /* --------------- stack operations ----------------- */
    void op_load_var(int nvar) { //"A ▶"
        if( finEdit() )         pushStack(vars[nvar]);
    }

    void op_store_var(int nvar) { //"▶ A"
        if( getOpArgs_NO_UNDO(1) )    vars[nvar]= stack_x;
    }

    void op_swap() {            //"SWAP" x y
        if( getOpArgs_NO_UNDO(2) ){
            pushStack(stack_x);
            pushStack(stack_y);
        }
    }

    void op_roll_yxz() {            //"ROLL" z y x => y x z "Z ▶"
        if( getOpArgs_NO_UNDO(3) ){
            pushStack(stack_y);
            pushStack(stack_x);
            pushStack(stack_z);
        }
    }

    void op_roll_xzy() {            //"ROLL" z y x => x z y "▶ Z"
        if( getOpArgs_NO_UNDO(3) ){
            pushStack(stack_x);
            pushStack(stack_z);
            pushStack(stack_y);
        }
    }

    void op_rect_polar() {      //"R ▶ P" (DEG)
        if( getOpArgs(2) ){
            pushStack( Math.sqrt( stack_x*stack_x + stack_y*stack_y) );
            pushStack( Math.atan2(stack_y, stack_x) * 180.0 / Math.PI );
        }
    }

    void op_polar_rect() {      //"P ▶ R" (DEG)
        if( getOpArgs(2) ){
            double xr= stack_x * Math.PI / 180.0;    //X-> RAD
            pushStack( Math.sin(xr) * stack_y );
            pushStack( Math.cos(xr) * stack_y );
        }
    }

    void op_r_par() {           //"x||y"
        if( getOpArgs(2) )    pushStack( (stack_y * stack_x) / (stack_y + stack_x) );
    }

    void op_r_div() {           //"x/(x+y)"
        if( getOpArgs(2) )    pushStack( stack_x / (stack_x + stack_y) );
    }

    void op_invert_x() {        //"1/x"
        if( getOpArgs(1) )    pushStack( 1.0 / stack_x );
    }

    void op_add() {             //"+"
        if( getOpArgs(2) )    pushStack( stack_y + stack_x );
    }

    void op_sub() {             //"-"
        if( getOpArgs(2) )    pushStack( stack_y - stack_x );
    }

    void op_sub_add() {             //"- & +"
        if( getOpArgs(2) ){
            pushStack( stack_y - stack_x );
            pushStack( stack_y + stack_x );
        }
    }

    void op_sub_add_porc() {             //"-/+ %"
        if( getOpArgs(2) ){
            pushStack( stack_y * (1.0 - stack_x/100.0) );
            pushStack( stack_y * (1.0 + stack_x/100.0) );
        }
    }

    void op_mult() {            //"*"
        if( getOpArgs(2) )    pushStack( stack_y * stack_x );
    }

    void op_porc() {            //"%"
        if( getOpArgs(2) )    pushStack( stack_y * stack_x / 100.0);
    }

    void op_div() {             //"/"
        if( getOpArgs(2) )    pushStack( stack_y / stack_x );
    }

    void op_div_porc() {             //"/ > %"
        if( getOpArgs(2) )    pushStack( (stack_y * 100.0) / stack_x );
    }

    void op_sqrt() {            //"\/x"
        if( getOpArgs(1) )    pushStack( Math.sqrt(stack_x) );
    }

    void op_sq() {              //"x^2"
        if( getOpArgs(1) )    pushStack( stack_x * stack_x );
    }

    void op_not() {             //"NOT"  -(x+1)
        if( getOpArgs(1) ){
            //cambia signo de X+1 (complemento a 1 desde complemento a 2)
            pushStack( stack_x );
            String v= convertStackPos(1, false); //remove "0x" and 1000s decorators
            double d= 1.0;                              //sin decimales: + "1.0"
            if (v.indexOf('.') >= 0)    d= 1.0/65536.0; //con decimales: + "0.0001" (hexa)
            replaceX( -(stack_x + d) );
        }
    }

    void op_to_deg() {          //"DEG"
        if( getOpArgs(1) )    pushStack( stack_x * 180.0 / Math.PI );
    }

    void op_to_rad() {          //"RAD"
        if( getOpArgs(1) )    pushStack( stack_x * Math.PI / 180.0 );
    }

    void op_pow() {             //"Y^X"
        if( getOpArgs(2) )    pushStack( Math.pow(stack_y, stack_x) );
    }

    void op_n_sqrt() {          //"x\/y"
        if( getOpArgs(2) )    pushStack( Math.pow(stack_y, 1.0 / stack_x) );  //Y ^ 1/X
    }

    void op_sin() {             //"SIN"
        if( getOpArgs(1) )    pushStack( Math.sin(stack_x * Math.PI / 180.0) );  //X-> RAD-> SIN
    }

    void op_asin() {            //"ASIN"
        if( getOpArgs(1) )    pushStack( Math.asin(stack_x) * 180.0 / Math.PI ); //ASIN-> DEG-> X
    }

    void op_log10() {           //"LOG"
        if( getOpArgs(1) )    pushStack( Math.log10(stack_x) );
    }

    void op_pow10() {           //"10^X"
        if( getOpArgs(1) )    pushStack( Math.pow(10.0, stack_x) );
    }

    void op_cos() {             //"COS"
        if( getOpArgs(1) )    pushStack( Math.cos(stack_x * Math.PI / 180.0) );  //X-> RAD-> COS
    }

    void op_acos() {            //"ACOS"
        if( getOpArgs(1) )    pushStack( Math.acos(stack_x) * 180.0 / Math.PI ); //ACOS-> DEG-> X
    }

    void op_ln() {              //"LN"
        if( getOpArgs(1) )    pushStack( Math.log(stack_x) );
    }

    void op_exp() {             //"E^X"
        if( getOpArgs(1) )    pushStack( Math.exp(stack_x) );
    }

    void op_tan() {             //"TAN"
        if( getOpArgs(1) )    pushStack( Math.tan(stack_x * Math.PI / 180.0) );  //X-> RAD-> TAN
    }

    void op_atan() {            //"ATAN"
        if( getOpArgs(1) )    pushStack( Math.atan(stack_x) * 180.0 / Math.PI ); //ATAN-> DEG-> X
    }

    void op_pi() {              //"PI"
        if( finEdit() )         pushStack(Math.PI);
    }

    void op_2piF() {            //"2*PI*F"
        if( finEdit() ) {
            pushStack(Math.PI * 2.0 * vars[5]);
            activity.showInfo("Frec= " + vars[5] + " Hz");
        }
    }

    void op_limit_dec(int ndec) {
        if( getOpArgs(1) ){
            pushStack(stack_x);
            String v= convertStackPos(1, false);
            if (v.indexOf('.') >= 0) {
                String[] me = v.split("e");
                String[] mp = me[0].split("\\.");
                int nd= mp[1].length();
                if (nd > ndec) {
                    StringBuilder r= new StringBuilder();
                    r.append(mp[0]);
                    if (ndec > 0) {
                        r.append('.');
                        r.append(mp[1].substring(0, ndec));
                    }
                    if (me.length > 1) {
                        r.append('e');
                        r.append(me[1]);
                    }
                    replaceX( Double.parseDouble(r.toString()) );
                }
            }
        }
    }

    void op_kmult( double k ) { //x *= k
        if( finEdit() ) {
            if (stacklen == 0)  pushStack(1.0);
            replaceX( readX() * k );
        }
    }

    void op_kdiv( double k ) {  //x /= k
        if( finEdit() ) {
            if (stacklen == 0)  pushStack(1.0);
            replaceX( readX() / k );
        }
    }

    void op_exp3_vf() { // z=initial, y= middle, x= last value (ty-tz = tx-ty) => negative exponential final value
        if( getOpArgs(3) ){
            double d1= stack_y - stack_z;
            double d2= stack_x - stack_z;
            if( (d1 > 0) && (d2 > d1) && (d1 > d2/2) ){
                double amp = d1 * d1 / (2 * stack_y - stack_z - stack_x);
                pushStack(stack_z + amp);   //final value= initial value + amplitude
            }else if( d1 < 0 ){
                d1= -d1;
                d2= -d2;
                if( (d2 > d1) && (d1 > d2/2) ){
                    double amp = d1 * d1 / (2 * stack_y - stack_z - stack_x);
                    pushStack(stack_z + amp);   //final value= initial value + amplitude
                }
            }
        }
    }

    void op_exp3_tao() {  // z=initial, y= middle, x= last value (ty-tz = tx-ty) => negative exponential tao / (ty-tz)
        if( getOpArgs(3) ){
            double d1= stack_y - stack_z;
            double d2= stack_x - stack_z;
            if( (d1 > 0) && (d2 > d1) && (d1 > d2/2) ){
                pushStack(1 / Math.log(d1/(d2-d1)) );   //tao / (ty-tz)
            }else if( d1 < 0 ){
                d1= -d1;
                d2= -d2;
                if( (d2 > d1) && (d1 > d2/2) ){
                    pushStack(1 / Math.log(d1/(d2-d1)) );   //tao / (ty-tz)
                }
            }
        }
    }

    void op_clear_stk() {  //clear stack
        finEdit();
        if( stacklen > 0 ) {
            undo_stacklen= stacklen;
            stacklen = 0;
        }
    }

    void op_save_macro(int nmac) {  //save key macro
        if( finEdit() && (nmac >= 1) && (nmac <= MAXMACROS) ) {
            current_macro= nmac-1;
            macro_len[current_macro]= 0;
            macro_rec= true;
            set_mode_flag(Calc.MACRO, true);
        }
    }

    void op_end_macro() {  //end save key macro
        set_mode_flag(Calc.MACRO, false);
        if( macro_rec ) {
            macro_rec = false;
            int n = macro_len[current_macro];
            String ms = "Macro #" + (current_macro + 1) + " : " + n + " steps saved";
            activity.showInfo(ms);
        }
    }

    void op_play_macro(int nmac) {  //play key macro
        if( finEdit() && (nmac >= 1) && (nmac <= MAXMACROS) ) {
            int nm = nmac - 1;
            int n = macro_len[nm];
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    long k = macros[nm][i];      //re-play the saved actions
                    action act = hmKAct.get(k);
                    if (act != null) {
                        act.doAction();
                    }
                }
            } else {
                activity.showInfo("Macro #" + nmac + " is empty");
            }
        }
    }

    static String get_macro_as_str(int nmac){
        StringBuilder stk= new StringBuilder();
        if( (nmac >= 1) && (nmac <= MAXMACROS) ) {
            int nm = nmac - 1;
            int n = macro_len[nm];
            stk.append(n);
            stk.append(',');
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    long k = macros[nm][i];
                    stk.append(k);
                    stk.append(',');
                }
            }
        }
        return stk.toString();
    }

    static void set_macro_from_str(int nmac, String s){
        if( (nmac >= 1) && (nmac <= MAXMACROS) ) {
            int nm = nmac - 1;
            String[] slist = s.split(",");
            if( slist.length > 1 ){
                macro_len[nm]= Integer.parseInt( slist[0] );
                for(int i= 1; i < slist.length; i++){
                    try {
                        macros[nm][i-1]=  Long.parseLong(slist[i]);
                    }catch (Exception ignored){}
                }
            }
        }
    }
}
