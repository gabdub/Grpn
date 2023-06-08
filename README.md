# Grpn
This is my Android RPN calculator.

After almost 40 years of using HP's RPN calculators, I programmed one for my Android phone with the features I use most for electronics and programming:

* 6 memories: A to F
* stack of 100 levels
* **UNDO** = arguments of the last operation
* **X//Y** = (X*Y)/(X+Y) = 2 resistors in parallel
* **X/(X+Y)** = voltage divider
* **2πF** = 2 * PI * frequency (the frequency is get from the **F** memory)
* **/ ▶ %** = Y / X * 100
* **%** = Y * X / 100
* **- & +** = substract and add: y=(Y - X) | x=(Y + X)
* **-/+ %** = substract and add a percent: y=(Y * (1 - X/100)) | x=(Y * (1 + X/100))
* scientific notation with some tweaks...
* **E+3** / **E-3** = add or subtract 3 from the exponent
* **.0 to .9** = truncate X to N decimal places
* polar to rectangular conversion
* **version 1.3**: bigger screen resolution **2400 x 1080** : more buttons in portrait mode
* **4 macros**: up to 100 keys each
* **3 point negative exponential estimation**: final value and time constant τ (tao) estimation
* **CLEAR stack**: clear all the element in the stack (can be UNDOed)
* tabular key definition:

` \\option 1: method reference`
` calc.defKey(R.id.bParalelo, Calc.SHIFT, "R ▶ P", false, calc::op_rect_polar);`

` \\option 2: lambda`
` calc.defKey(R.id.bMili_exp, Calc.HEXA,   ">>4", false, () -> calc.op_kdiv(16.0));`

` \\option 3: new class`
` calc.defKey(R.id.bParalelo, Calc.NORMAL, "x||y", false, new Calc.action(){   public void doAction(){ calc.op_r_par();}  });`

**Default layout: Ver 1.3 (screen: 2400 x 1080)**

![Default layout 1.3](https://github.com/gabdub/Grpn/blob/master/screencapt/default1_3.jpg "Default layout 1.3")  ![SHIFT layout 1.3](https://github.com/gabdub/Grpn/blob/master/screencapt/shift1_3.jpg "SHIFT layout 1.3")


**Default layout: Ver 1.2 (HD screen: 1920 x 1080)**

![Default layout 1.2](https://github.com/gabdub/Grpn/blob/master/screencapt/default.jpg "Default layout 1.2")  ![SHIFT layout 1.2](https://github.com/gabdub/Grpn/blob/master/screencapt/shift.jpg "SHIFT layout 1.2")


**Landscape layout: Ver 1.2 / 1.3**

![Landscape layout](https://github.com/gabdub/Grpn/blob/master/screencapt/landscape.jpg "Landscape layout 1.2 / 1.3")


**Some trigonometric functions:**

* Angles in degrees
* **DEG** = x * 180 / PI
* **RAD** = x * PI / 180
* **R ▶ P** = rectangular to polar (degrees)
* **P ▶ R** = polar (degrees) to rectangular

![Trig layout](https://github.com/gabdub/Grpn/blob/master/screencapt/trig.jpg "TRIG layout")  ![SHIFT TRIG layout](https://github.com/gabdub/Grpn/blob/master/screencapt/trig_shift.jpg "SHIFT TRIG layout")


**HEX mode:**

* numbers are displayed in hexadecimal with up to two decimal bytes
* **<<4** / **>>4** = shift one nibble (4 bits) to the right (x/16) or left (x*16)
* **NOT** = one's complement

![Hex layout](https://github.com/gabdub/Grpn/blob/master/screencapt/hex.jpg "Hex layout")

