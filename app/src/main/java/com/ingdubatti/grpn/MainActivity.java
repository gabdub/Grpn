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

    private Calc calc= new Calc(this);

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
            txt1.setBackgroundColor(0xFFFFF5E9); //default
            return true;
        }
        return false;
    }

    private void showMenu() {
        PopupMenu p = new PopupMenu(MainActivity.this, findViewById(R.id.bMenu));
        p.getMenuInflater().inflate(R.menu.menu_main, p.getMenu());
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                return execMenuItem(item);
            }
        });
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
        calc.defKey(R.id.bShift, Calc.NORMAL, "SHIFT", false, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.SHIFT, true);}
        });
        calc.defKey(R.id.bShift, Calc.SHIFT, "SHIFT", true, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.SHIFT, false);}
        });

        calc.defKey(R.id.bHex, Calc.NORMAL, "HEX", false, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.HEXA, true);}
        });
        calc.defKey(R.id.bHex, Calc.HEXA, "HEX", true, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.HEXA, false);}
        });

        calc.defKey(R.id.bSwap, Calc.NORMAL, "SWAP", false, new Calc.action(){
            public void doAction(){ calc.op_swap();}
        });
        calc.defKey(R.id.bSwap, Calc.SHIFT, "TRIG", false, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.TRIG, true);}
        });
        calc.defKey(R.id.bSwap, Calc.TRIG+Calc.SHIFT, "TRIG", true, new Calc.action(){
            public void doAction(){ calc.set_mode_flag(Calc.TRIG, false);}
        });

        /////---------------------------
        calc.defKey(R.id.b0, Calc.NORMAL, "0", false, new Calc.action(){
            public void doAction(){ calc.edit_append('0');}
        });
        calc.defKey(R.id.b0, Calc.SHIFT, ".0", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(0);}
        });
        calc.defKey(R.id.b1, Calc.NORMAL, "1", false, new Calc.action(){
            public void doAction(){ calc.edit_append('1');}
        });
        calc.defKey(R.id.b1, Calc.SHIFT, ".1", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(1);}
        });
        calc.defKey(R.id.b2, Calc.NORMAL, "2", false, new Calc.action(){
            public void doAction(){ calc.edit_append('2');}
        });
        calc.defKey(R.id.b2, Calc.SHIFT, ".2", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(2);}
        });
        calc.defKey(R.id.b3, Calc.NORMAL, "3", false, new Calc.action(){
            public void doAction(){ calc.edit_append('3');}
        });
        calc.defKey(R.id.b3, Calc.SHIFT, ".3", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(3);}
        });
        calc.defKey(R.id.b4, Calc.NORMAL, "4", false, new Calc.action(){
            public void doAction(){ calc.edit_append('4');}
        });
        calc.defKey(R.id.b4, Calc.SHIFT, ".4", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(4);}
        });
        calc.defKey(R.id.b5, Calc.NORMAL, "5", false, new Calc.action(){
            public void doAction(){ calc.edit_append('5');}
        });
        calc.defKey(R.id.b5, Calc.SHIFT, ".5", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(5);}
        });
        calc.defKey(R.id.b6, Calc.NORMAL, "6", false, new Calc.action(){
            public void doAction(){ calc.edit_append('6');}
        });
        calc.defKey(R.id.b6, Calc.SHIFT, ".6", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(6);}
        });
        calc.defKey(R.id.b7, Calc.NORMAL, "7", false, new Calc.action(){
            public void doAction(){ calc.edit_append('7');}
        });
        calc.defKey(R.id.b7, Calc.SHIFT, ".7", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(7);}
        });
        calc.defKey(R.id.b8, Calc.NORMAL, "8", false, new Calc.action(){
            public void doAction(){ calc.edit_append('8');}
        });
        calc.defKey(R.id.b8, Calc.SHIFT, ".8", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(8);}
        });
        calc.defKey(R.id.b9, Calc.NORMAL, "9", false, new Calc.action(){
            public void doAction(){ calc.edit_append('9');}
        });
        calc.defKey(R.id.b9, Calc.SHIFT, ".9", false, new Calc.action(){
            public void doAction(){ calc.op_limit_dec(9);}
        });

        calc.defKey(R.id.b_A, Calc.NORMAL, "A ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(0);}
        });
        calc.defKey(R.id.b_A, Calc.SHIFT, "▶ A", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(0);}
        });
        calc.defKey(R.id.b_A, Calc.HEXA, "A", true, new Calc.action(){
            public void doAction(){ calc.edit_append('A');}
        });
        calc.defKey(R.id.b_B, Calc.NORMAL, "B ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(1);}
        });
        calc.defKey(R.id.b_B, Calc.SHIFT, "▶ B", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(1);}
        });
        calc.defKey(R.id.b_B, Calc.HEXA, "B", true, new Calc.action(){
            public void doAction(){ calc.edit_append('B');}
        });
        calc.defKey(R.id.b_C, Calc.NORMAL, "C ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(2);}
        });
        calc.defKey(R.id.b_C, Calc.SHIFT, "▶ C", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(2);}
        });
        calc.defKey(R.id.b_C, Calc.HEXA, "C", true, new Calc.action(){
            public void doAction(){ calc.edit_append('C');}
        });
        calc.defKey(R.id.b_D, Calc.NORMAL, "D ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(3);}
        });
        calc.defKey(R.id.b_D, Calc.SHIFT, "▶ D", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(3);}
        });
        calc.defKey(R.id.b_D, Calc.HEXA, "D", true, new Calc.action(){
            public void doAction(){ calc.edit_append('D');}
        });
        calc.defKey(R.id.b_E, Calc.NORMAL, "E ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(4);}
        });
        calc.defKey(R.id.b_E, Calc.SHIFT, "▶ E", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(4);}
        });
        calc.defKey(R.id.b_E, Calc.HEXA, "E", true, new Calc.action(){
            public void doAction(){ calc.edit_append('E');}
        });
        calc.defKey(R.id.b_F, Calc.NORMAL, "F ▶", false, new Calc.action(){
            public void doAction(){ calc.op_load_var(5);}
        });
        calc.defKey(R.id.b_F, Calc.SHIFT, "▶ F", false, new Calc.action(){
            public void doAction(){ calc.op_store_var(5);}
        });
        calc.defKey(R.id.b_F, Calc.HEXA, "F", true, new Calc.action(){
            public void doAction(){ calc.edit_append('F');}
        });

        calc.defKey(R.id.bParalelo, Calc.NORMAL, "x||y", false, new Calc.action(){
            public void doAction(){ calc.op_r_par();}
        });
        calc.defKey(R.id.bParalelo, Calc.SHIFT, "R ▶ P", false, new Calc.action(){
            public void doAction(){ calc.op_rect_polar();}
        });

        calc.defKey(R.id.bDivisor, Calc.NORMAL, "x/(x+y)", false, new Calc.action(){
            public void doAction(){ calc.op_r_div();}
        });
        calc.defKey(R.id.bDivisor, Calc.SHIFT, "P ▶ R", false, new Calc.action(){
            public void doAction(){ calc.op_polar_rect();}
        });

        calc.defKey(R.id.bInv, Calc.NORMAL, "1/x", false, new Calc.action(){
            public void doAction(){ calc.op_invert_x();}
        });

        calc.defKey(R.id.bEnter, Calc.NORMAL, "ENTER", false, new Calc.action(){
            public void doAction(){ calc.edit_enter();}
        });
        calc.defKey(R.id.bEnter, Calc.SHIFT, "EDIT", false, new Calc.action(){
            public void doAction(){ calc.edit_x();}
        });

        calc.defKey(R.id.bSigno, Calc.NORMAL, "+/-", false, new Calc.action(){
            public void doAction(){ calc.edit_signo();}
        });

        calc.defKey(R.id.bDelete, Calc.NORMAL, "DEL", false, new Calc.action(){
            public void doAction(){ calc.edit_delete();}
        });
        calc.defKey(R.id.bDelete, Calc.SHIFT, "UNDO", false, new Calc.action(){
            public void doAction(){ calc.edit_undo();}
        });

        calc.defKey(R.id.bDiv, Calc.NORMAL, "/", false, new Calc.action(){
            public void doAction(){ calc.op_div();}
        });
        calc.defKey(R.id.bMult, Calc.NORMAL, "*", false, new Calc.action(){
            public void doAction(){ calc.op_mult();}
        });
        calc.defKey(R.id.bMenos, Calc.NORMAL, "-", false, new Calc.action(){
            public void doAction(){ calc.op_sub();}
        });
        calc.defKey(R.id.bMas, Calc.NORMAL, "+", false, new Calc.action(){
            public void doAction(){ calc.op_add();}
        });
        calc.defKey(R.id.bPunto, Calc.NORMAL, ".", false, new Calc.action(){
            public void doAction(){ calc.edit_point();}
        });

        calc.defKey(R.id.bExp, Calc.NORMAL, "EEX", false, new Calc.action(){
            public void doAction(){ calc.edit_exp();}
        });
        calc.defKey(R.id.bExp, Calc.HEXA,   "NOT", false, new Calc.action(){
            public void doAction(){ calc.op_not();}
        });

        calc.defKey(R.id.bK_exp, Calc.NORMAL, "E+3", false, new Calc.action(){
            public void doAction(){ calc.op_kmult(1000.0);}
        });
        calc.defKey(R.id.bK_exp, Calc.HEXA,   "<<4", false, new Calc.action(){
            public void doAction(){ calc.op_kmult(16.0);}
        });

        calc.defKey(R.id.bMili_exp, Calc.NORMAL, "E-3", false, new Calc.action(){
            public void doAction(){ calc.op_kdiv(1000.0);}
        });
        calc.defKey(R.id.bMili_exp, Calc.HEXA,   ">>4", false, new Calc.action(){
            public void doAction(){ calc.op_kdiv(16.0);}
        });

        calc.defKey(R.id.bMenu, Calc.NORMAL, "...", false, new Calc.action(){
            public void doAction(){ showMenu();}
        });

        calc.defKey(R.id.bRaiz, Calc.NORMAL, "⎷x", false, new Calc.action(){
            public void doAction(){ calc.op_sqrt();}
        });
        calc.defKey(R.id.bRaiz, Calc.SHIFT, "x^2", false, new Calc.action(){
            public void doAction(){ calc.op_sq();}
        });
        calc.defKey(R.id.bRaiz, Calc.TRIG, "DEG", false, new Calc.action(){
            public void doAction(){ calc.op_to_deg();}
        });
        calc.defKey(R.id.bRaiz, Calc.TRIG+Calc.SHIFT, "RAD", false, new Calc.action(){
            public void doAction(){ calc.op_to_rad();}
        });

        calc.defKey(R.id.bPower, Calc.NORMAL, "Y^X", false, new Calc.action(){
            public void doAction(){ calc.op_pow();}
        });
        calc.defKey(R.id.bPower, Calc.SHIFT, "X⎷Y", false, new Calc.action(){
            public void doAction(){ calc.op_n_sqrt();}
        });
        calc.defKey(R.id.bPower, Calc.TRIG, "SIN", false, new Calc.action(){
            public void doAction(){ calc.op_sin();}
        });
        calc.defKey(R.id.bPower, Calc.TRIG+Calc.SHIFT, "ASIN", false, new Calc.action(){
            public void doAction(){ calc.op_asin();}
        });

        calc.defKey(R.id.bLog, Calc.NORMAL, "LOG", false, new Calc.action(){
            public void doAction(){ calc.op_log10();}
        });
        calc.defKey(R.id.bLog, Calc.SHIFT, "10^X", false, new Calc.action(){
            public void doAction(){ calc.op_pow10();}
        });
        calc.defKey(R.id.bLog, Calc.TRIG, "COS", false, new Calc.action(){
            public void doAction(){ calc.op_cos();}
        });
        calc.defKey(R.id.bLog, Calc.TRIG+Calc.SHIFT, "ACOS", false, new Calc.action(){
            public void doAction(){ calc.op_acos();}
        });

        calc.defKey(R.id.bLN, Calc.NORMAL, "LN", false, new Calc.action(){
            public void doAction(){ calc.op_ln();}
        });
        calc.defKey(R.id.bLN, Calc.SHIFT, "E^X", false, new Calc.action(){
            public void doAction(){ calc.op_exp();}
        });
        calc.defKey(R.id.bLN, Calc.TRIG, "TAN", false, new Calc.action(){
            public void doAction(){ calc.op_tan();}
        });
        calc.defKey(R.id.bLN, Calc.TRIG+Calc.SHIFT, "ATAN", false, new Calc.action(){
            public void doAction(){ calc.op_atan();}
        });

        calc.defKey(R.id.bPI, Calc.NORMAL, "PI", false, new Calc.action(){
            public void doAction(){ calc.op_pi();}
        });
        calc.defKey(R.id.bPI, Calc.SHIFT, "2πF", false, new Calc.action(){
            public void doAction(){ calc.op_2piF();}
        });

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
