package com.ingdubatti.grpn;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "GRPNPrefs";

    private final Calc calc= new Calc(this);

    void showInfo(String msg){
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean execMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_copy) {   //COPY X
            String txt = calc.menu_copy_str();
            if (!txt.isEmpty()){
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("x-value", txt);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showInfo("Copiado: " + txt );
                }
            }
            return true;
        }
        if (id == R.id.action_paste) {  //PASTE
            ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    String txt = clip.getItemAt(0).coerceToText(MainActivity.this).toString();
                    calc.menu_paste_str(txt);
                }
            }
            return true;
        }
        if (id == R.id.action_about) {  //ABOUT
            TextView txt1 = findViewById(R.id.val1);
            TextView txt2 = findViewById(R.id.val2);
            TextView txt3 = findViewById(R.id.val3);
            TextView txt4 = findViewById(R.id.val4);
            txt4.setText(R.string.grpn_tit);
            txt3.setText(R.string.grpn_github);
            txt2.setText("");
            txt1.setText(R.string.grpn_version);
            txt1.setBackgroundColor(Calc.backcolor); //force default back color
            return true;
        }
        return false;
    }

    private void showMenu() {
        PopupMenu p = new PopupMenu(MainActivity.this, findViewById(R.id.bMenu));
        p.getMenuInflater().inflate(R.menu.menu_main, p.getMenu());
        p.setOnMenuItemClickListener(this::execMenuItem);
        p.show();
    }

    private double loadCfgDbl(SharedPreferences settings, String name, double defval) {
        double rv= defval;
        try {
            String svar= settings.getString(name,"");
            if( !svar.isEmpty() ) {
                rv = Double.parseDouble(svar);
            }
        }catch (Exception ignored){}
        return rv;
    }

    private void loadVar(SharedPreferences settings, int n, double defval) {
        calc.vars[n]= loadCfgDbl(settings, "var"+n, defval );
    }

    @Override
    @SuppressLint("DefaultLocale")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        calc.modo= settings.getInt("modo", 0);
        calc.editando = settings.getBoolean("editando", false);
        calc.editLine.setLength(0);
        calc.editLine.append( settings.getString("editLine",""));

        String stk= settings.getString("stack","");
        String[] slist = stk.split(",");
        if( slist.length > 1 ){
            calc.stacklen= Integer.parseInt( slist[0] );
            for(int i= 1; i < slist.length; i++){
                try {
                    calc.stack[i-1]=  Double.parseDouble(slist[i]);
                }catch (Exception ignored){}
            }
        }
        loadVar( settings, 0, 130.0 / 47.0 ); //A= shunt de 13 resistencias de 4,7 ohm en paralelo (INVERSA)
        loadVar( settings, 1, Math.sqrt(2.0));//B= raiz(2)
        loadVar( settings, 2, 0.0);           //C= 0
        loadVar( settings, 3, 0.0);           //D= 0
        loadVar( settings, 4, Math.E);        //E= e
        loadVar( settings, 5, 50.0);          //F=50 (frecuencia default)
        calc.undoN= settings.getInt("undoN", 0);
        calc.undoX= loadCfgDbl(settings, "undoX", 0.0 );
        calc.undoY= loadCfgDbl(settings, "undoY", 0.0 );

        ////////////////// BUTTONS //////////////////
        /////---------modificadores----------
        calc.defKey(R.id.bShift, Calc.NORMAL, "SHIFT", false, () -> calc.set_mode_flag(Calc.SHIFT, true));
        calc.defKey(R.id.bShift, Calc.SHIFT, "SHIFT", true, () -> calc.set_mode_flag(Calc.SHIFT, false));

        calc.defKey(R.id.bHex, Calc.NORMAL, "HEX", false, () -> calc.set_mode_flag(Calc.HEXA, true));
        calc.defKey(R.id.bHex, Calc.HEXA, "HEX", true, () -> calc.set_mode_flag(Calc.HEXA, false));

        //---------- with TRIG mode (1 row) -------------
        calc.defKey(R.id.bSwap_trig, Calc.NORMAL, "SWAP", false, calc::op_swap);
        calc.defKey(R.id.bSwap_trig, Calc.SHIFT, "TRIG", false, () -> calc.set_mode_flag(Calc.TRIG, true));
        calc.defKey(R.id.bSwap_trig, Calc.TRIG+Calc.SHIFT, "TRIG", true, () -> calc.set_mode_flag(Calc.TRIG, false));
        //---------- without TRIG mode (2 rows) -------------
        calc.defKey(R.id.bSwap_roll, Calc.NORMAL, "SWAP", false, calc::op_swap);
        calc.defKey(R.id.bSwap_roll, Calc.SHIFT, "ROLL", false, calc::op_roll);
        //----------------------------------
        

        /////---------------------------
        calc.defKey(R.id.b0, Calc.NORMAL, "0", false, () -> calc.edit_append('0'));
        calc.defKey(R.id.b0, Calc.SHIFT, ".0", false, () -> calc.op_limit_dec(0));
        calc.defKey(R.id.b1, Calc.NORMAL, "1", false, () -> calc.edit_append('1'));
        calc.defKey(R.id.b1, Calc.SHIFT, ".1", false, () -> calc.op_limit_dec(1));
        calc.defKey(R.id.b2, Calc.NORMAL, "2", false, () -> calc.edit_append('2'));
        calc.defKey(R.id.b2, Calc.SHIFT, ".2", false, () -> calc.op_limit_dec(2));
        calc.defKey(R.id.b3, Calc.NORMAL, "3", false, () -> calc.edit_append('3'));
        calc.defKey(R.id.b3, Calc.SHIFT, ".3", false, () -> calc.op_limit_dec(3));
        calc.defKey(R.id.b4, Calc.NORMAL, "4", false, () -> calc.edit_append('4'));
        calc.defKey(R.id.b4, Calc.SHIFT, ".4", false, () -> calc.op_limit_dec(4));
        calc.defKey(R.id.b5, Calc.NORMAL, "5", false, () -> calc.edit_append('5'));
        calc.defKey(R.id.b5, Calc.SHIFT, ".5", false, () -> calc.op_limit_dec(5));
        calc.defKey(R.id.b6, Calc.NORMAL, "6", false, () -> calc.edit_append('6'));
        calc.defKey(R.id.b6, Calc.SHIFT, ".6", false, () -> calc.op_limit_dec(6));
        calc.defKey(R.id.b7, Calc.NORMAL, "7", false, () -> calc.edit_append('7'));
        calc.defKey(R.id.b7, Calc.SHIFT, ".7", false, () -> calc.op_limit_dec(7));
        calc.defKey(R.id.b8, Calc.NORMAL, "8", false, () -> calc.edit_append('8'));
        calc.defKey(R.id.b8, Calc.SHIFT, ".8", false, () -> calc.op_limit_dec(8));
        calc.defKey(R.id.b9, Calc.NORMAL, "9", false, () -> calc.edit_append('9'));
        calc.defKey(R.id.b9, Calc.SHIFT, ".9", false, () -> calc.op_limit_dec(9));

        calc.defKey(R.id.b_A, Calc.NORMAL, "    A ▶    ", false, () -> calc.op_load_var(0));
        calc.defKey(R.id.b_A, Calc.SHIFT, "    ▶ A    ", false, () -> calc.op_store_var(0));
        calc.defKey(R.id.b_A, Calc.HEXA, "       A      ", true, () -> calc.edit_append('A'));
        calc.defKey(R.id.b_B, Calc.NORMAL, "    B ▶    ", false, () -> calc.op_load_var(1));
        calc.defKey(R.id.b_B, Calc.SHIFT, "    ▶ B    ", false, () -> calc.op_store_var(1));
        calc.defKey(R.id.b_B, Calc.HEXA, "       B      ", true, () -> calc.edit_append('B'));
        calc.defKey(R.id.b_C, Calc.NORMAL, "    C ▶    ", false, () -> calc.op_load_var(2));
        calc.defKey(R.id.b_C, Calc.SHIFT, "    ▶ C    ", false, () -> calc.op_store_var(2));
        calc.defKey(R.id.b_C, Calc.HEXA, "       C      ", true, () -> calc.edit_append('C'));

        calc.defKey(R.id.bMenu, Calc.NORMAL, "     ...     ", false, this::showMenu);

        calc.defKey(R.id.b_D, Calc.NORMAL, "D ▶", false, () -> calc.op_load_var(3));
        calc.defKey(R.id.b_D, Calc.SHIFT, "▶ D", false, () -> calc.op_store_var(3));
        calc.defKey(R.id.b_D, Calc.HEXA, "D", true, () -> calc.edit_append('D'));
        calc.defKey(R.id.b_E, Calc.NORMAL, "E ▶", false, () -> calc.op_load_var(4));
        calc.defKey(R.id.b_E, Calc.SHIFT, "▶ E", false, () -> calc.op_store_var(4));
        calc.defKey(R.id.b_E, Calc.HEXA, "E", true, () -> calc.edit_append('E'));
        calc.defKey(R.id.b_F, Calc.NORMAL, "F ▶", false, () -> calc.op_load_var(5));
        calc.defKey(R.id.b_F, Calc.SHIFT, "▶ F", false, () -> calc.op_store_var(5));
        calc.defKey(R.id.b_F, Calc.HEXA, "F", true, () -> calc.edit_append('F'));

        calc.defKey(R.id.bParalelo, Calc.NORMAL, "x||y", false, calc::op_r_par);
        calc.defKey(R.id.bParalelo, Calc.SHIFT, "R ▶ P", false, calc::op_rect_polar);

        calc.defKey(R.id.bDivisor, Calc.NORMAL, "x/(x+y)", false, calc::op_r_div);
        calc.defKey(R.id.bDivisor, Calc.SHIFT, "P ▶ R", false, calc::op_polar_rect);

        calc.defKey(R.id.bInv, Calc.NORMAL, "1/x", false, calc::op_invert_x);

        calc.defKey(R.id.bEnter, Calc.NORMAL, "ENTER", false, calc::edit_enter);
        calc.defKey(R.id.bEnter, Calc.SHIFT, "EDIT", false, calc::edit_x);

        calc.defKey(R.id.bSigno, Calc.NORMAL, "+/-", false, calc::edit_signo);

        calc.defKey(R.id.bDelete, Calc.NORMAL, "DEL", false, calc::edit_delete);
        calc.defKey(R.id.bDelete, Calc.SHIFT, "UNDO", false, calc::edit_undo);

        calc.defKey(R.id.bDiv, Calc.NORMAL, "/", false, calc::op_div);
        calc.defKey(R.id.bDiv, Calc.SHIFT, "/ ▶ %", false, calc::op_div_porc);

        calc.defKey(R.id.bMult, Calc.NORMAL, "*", false, calc::op_mult);
        calc.defKey(R.id.bMult, Calc.SHIFT, "%", false, calc::op_porc);

        calc.defKey(R.id.bMenos, Calc.NORMAL, "-", false, calc::op_sub);
        calc.defKey(R.id.bMenos, Calc.SHIFT, "- & +", false, calc::op_sub_add);

        calc.defKey(R.id.bMas, Calc.NORMAL, "+", false, calc::op_add);
        calc.defKey(R.id.bMas, Calc.SHIFT, "-/+ %", false, calc::op_sub_add_porc);

        calc.defKey(R.id.bPunto, Calc.NORMAL, ".", false, calc::edit_point);

        calc.defKey(R.id.bExp, Calc.NORMAL, "EEX", false, calc::edit_exp);
        calc.defKey(R.id.bExp, Calc.HEXA,   "NOT", false, calc::op_not);

        calc.defKey(R.id.bK_exp, Calc.NORMAL, "E+3", false, () -> calc.op_kmult(1000.0));
        calc.defKey(R.id.bK_exp, Calc.HEXA,   "<<4", false, () -> calc.op_kmult(16.0));

        calc.defKey(R.id.bMili_exp, Calc.NORMAL, "E-3", false, () -> calc.op_kdiv(1000.0));
        calc.defKey(R.id.bMili_exp, Calc.HEXA,   ">>4", false, () -> calc.op_kdiv(16.0));

        //---------- with TRIG mode (1 row) -------------
        calc.defKey(R.id.bSquare_deg, Calc.NORMAL, "⎷x", false, calc::op_sqrt);
        calc.defKey(R.id.bSquare_deg, Calc.SHIFT, "x^2", false, calc::op_sq);
        calc.defKey(R.id.bSquare_deg, Calc.TRIG, "DEG", false, calc::op_to_deg);
        calc.defKey(R.id.bSquare_deg, Calc.TRIG+Calc.SHIFT, "RAD", false, calc::op_to_rad);

        calc.defKey(R.id.bPower_sin, Calc.NORMAL, "Y^X", false, calc::op_pow);
        calc.defKey(R.id.bPower_sin, Calc.SHIFT, "X⎷Y", false, calc::op_n_sqrt);
        calc.defKey(R.id.bPower_sin, Calc.TRIG, "SIN", false, calc::op_sin);
        calc.defKey(R.id.bPower_sin, Calc.TRIG+Calc.SHIFT, "ASIN", false, calc::op_asin);

        calc.defKey(R.id.bLog_cos, Calc.NORMAL, "LOG", false, calc::op_log10);
        calc.defKey(R.id.bLog_cos, Calc.SHIFT, "10^X", false, calc::op_pow10);
        calc.defKey(R.id.bLog_cos, Calc.TRIG, "COS", false, calc::op_cos);
        calc.defKey(R.id.bLog_cos, Calc.TRIG+Calc.SHIFT, "ACOS", false, calc::op_acos);

        calc.defKey(R.id.bLn_tan, Calc.NORMAL, "LN", false, calc::op_ln);
        calc.defKey(R.id.bLn_tan, Calc.SHIFT, "E^X", false, calc::op_exp);
        calc.defKey(R.id.bLn_tan, Calc.TRIG, "TAN", false, calc::op_tan);
        calc.defKey(R.id.bLn_tan, Calc.TRIG+Calc.SHIFT, "ATAN", false, calc::op_atan);

        //---------- without TRIG mode (2 rows) -------------
        calc.defKey(R.id.bSquare, Calc.NORMAL, "⎷x",  false, calc::op_sqrt);
        calc.defKey(R.id.bSquare, Calc.SHIFT,  "x^2",  false, calc::op_sq);
        calc.defKey(R.id.bPower,  Calc.NORMAL, "Y^X",  false, calc::op_pow);
        calc.defKey(R.id.bPower,  Calc.SHIFT,  "X⎷Y", false, calc::op_n_sqrt);
        calc.defKey(R.id.bLog,    Calc.NORMAL, "LOG",  false, calc::op_log10);
        calc.defKey(R.id.bLog,    Calc.SHIFT,  "10^X", false, calc::op_pow10);
        calc.defKey(R.id.bLn,     Calc.NORMAL, "LN",   false, calc::op_ln);
        calc.defKey(R.id.bLn,     Calc.SHIFT,  "E^X",  false, calc::op_exp);
        
        calc.defKey(R.id.bDeg, Calc.NORMAL, "DEG",  false, calc::op_to_deg);
        calc.defKey(R.id.bDeg, Calc.SHIFT,  "RAD",  false, calc::op_to_rad);
        calc.defKey(R.id.bSin, Calc.NORMAL, "SIN",  false, calc::op_sin);
        calc.defKey(R.id.bSin, Calc.SHIFT,  "ASIN", false, calc::op_asin);
        calc.defKey(R.id.bCos, Calc.NORMAL, "COS",  false, calc::op_cos);
        calc.defKey(R.id.bCos, Calc.SHIFT,  "ACOS", false, calc::op_acos);
        calc.defKey(R.id.bTan, Calc.NORMAL, "TAN",  false, calc::op_tan);
        calc.defKey(R.id.bTan, Calc.SHIFT,  "ATAN", false, calc::op_atan);
        //----------------------------------

        calc.defKey(R.id.bPI, Calc.NORMAL, "PI", false, calc::op_pi);
        calc.defKey(R.id.bPI, Calc.SHIFT, "2πF", false, calc::op_2piF);

        //update interface
        calc.updateMode();
        calc.updateStack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //menu del sistema
        if( execMenuItem(item) ){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //save preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("modo", calc.modo);
        editor.putBoolean("editando", calc.editando);
        editor.putString("editLine", calc.editLine.toString());

        StringBuilder stk= new StringBuilder();
        stk.append(calc.stacklen);
        stk.append(',');
        for(int i=0; i< calc.stacklen; i++){
            stk.append(calc.stack[i]);
            stk.append(',');
        }
        editor.putString("stack", stk.toString());

        for(int i=0; i< Calc.MAXVARS; i++){
            editor.putString("var"+i, Double.toString(calc.vars[i]));
        }

        editor.putInt("undoN", calc.undoN);
        editor.putString("undoX", Double.toString(calc.undoX));
        editor.putString("undoY", Double.toString(calc.undoY));
        editor.apply();
    }
}
