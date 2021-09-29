# Grpn
This is my Android RPN calculator.

After almost 40 years of using HP's RPN calculators, I programmed one for my Android phone with the features I use most for electronics and programming:

* 6 memories: A to F
* stack of 100 levels
* **UNDO** = arguments of the last operation
* **X//Y** = (X*Y)/(X+Y) = 2 resistors in parallel
* **X/(X+Y)** = voltage divider
* **2πF** = 2 * PI * frequency (the frequency is get from the **F** memory)
* scientific notation with some tweaks...
* **E+3** / **E-3** = add or subtract 3 from the exponent
* polar to rectangular conversion
* tabular key definition:

`    \\option 1: method reference`
`    calc.defKey(R.id.bParalelo, Calc.SHIFT, "R ▶ P", false, calc::op_rect_polar);`

`    \\option 2: lambda`
`    calc.defKey(R.id.bMili_exp, Calc.HEXA,   ">>4", false, () -> calc.op_kdiv(16.0));`

`    \\option 3: new class`
`    calc.defKey(R.id.bParalelo, Calc.NORMAL, "x||y", false, new Calc.action(){   public void doAction(){ calc.op_r_par();}  });`

**Default layout:**

![Default layout](https://github.com/gabdub/Grpn/raw/master/screencapt/default.jpg "Default layout")  ![SHIFT layout](https://github.com/gabdub/Grpn/raw/master/screencapt/shift.jpg "SHIFT layout")


**Landscape layout:**

![Landscape layout](https://github.com/gabdub/Grpn/raw/master/screencapt/landscape.jpg "Landscape layout")


**Some trigonometric functions:**

![Trig layout](https://github.com/gabdub/Grpn/raw/master/screencapt/trig.jpg "TRIG layout")  ![SHIFT TRIG layout](https://github.com/gabdub/Grpn/raw/master/screencapt/trig_shift.jpg "SHIFT TRIG layout")


**HEX mode:**

* numbers are displayed in hexadecimal with up to two decimal bytes
* **<<4** / **>>4** = shift one nibble (4 bits) to the right (x/16) or left (x*16)
* **NOT** = one's complement

![Hex layout](https://github.com/gabdub/Grpn/raw/master/screencapt/hex.jpg "Hex layout")

