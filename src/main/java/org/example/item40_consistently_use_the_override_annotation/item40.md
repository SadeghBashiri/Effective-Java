<div dir="rtl">

<a id="top"></a>

# آیتم ۴۰: استفادهٔ یکپارچه از Annotation مربوط به `@Override`

## (Always use the @Override annotation)

موضوع این آیتم از کتاب Effective Java ساده به نظر می‌رسد، اما در عمل یکی از راه‌های بسیار مؤثر برای جلوگیری از باگ‌های ظریف در Java است.

> **اصل اصلی:** هر وقت قصد داری متدی را از یک کلاس والد یا یک interface بازنویسی کنی، از `@Override` استفاده کن تا کامپایلر بتواند صحت این قصد را بررسی کند.

نکتهٔ مهم این است که `@Override` خودش متدی را بازنویسی نمی‌کند؛ بلکه به کامپایلر می‌گوید: «من انتظار دارم این متد واقعاً یک متد به‌ارث‌رسیده را override کند؛ اگر چنین نیست، خطا بده.»

---

## فهرست مطالب

- [۱. باگی که در نگاه اول دیده نمی‌شود](#the-bug)
- [۲. علت اصلی: به‌جای Override، یک متد جدید تعریف کرده‌ایم](#root-cause)
  - [تفاوت Overloading و Overriding](#overloading-vs-overriding)
- [۳. چگونه `@Override` این باگ را پیدا می‌کند؟](#how-override-catches)
- [۴. چرا باید `@Override` را به‌صورت یک عادت همیشگی استفاده کنیم؟](#why-habit)
  - [مزیت اول: کامپایلر خطاهای امضا را پیدا می‌کند](#benefit1)
  - [مزیت دوم: IDE می‌تواند overrideهای ناخواسته را پیدا کند](#benefit2)
- [۵. آیا `@Override` فقط برای کلاس‌های والد است؟](#not-only-parents)
  - [ارتباط این موضوع با Default Methodها](#default-methods)
- [۶. استثنای کوچک: پیاده‌سازی متدهای abstract در کلاس concrete](#exception)
- [۷. چرا در interfaceها و کلاس‌های abstract هم باید دقت کنیم؟](#interfaces-abstract)
- [۸. جمع‌بندی عملی Item 40](#summary)
- [قاعدهٔ نهایی کتاب](#final-rule)

[بازگشت به بالا](#top)

---

<a id="the-bug"></a>
## ۱. باگی که در نگاه اول دیده نمی‌شود

نویسنده کلاس زیر را برای نمایش یک Bigram تعریف می‌کند؛ یعنی یک زوج مرتب از دو حرف.

<div dir="ltr">

```java
public class Bigram {
    private final char first;
    private final char second;

    public Bigram(char first, char second) {
        this.first = first;
        this.second = second;
    }

    public boolean equals(Bigram b) {
        return b.first == first && b.second == second;
    }

    public int hashCode() {
        return 31 * first + second;
    }
}
```
</div>

<div dir="ltr">

```java
Bigram a = new Bigram('a', 'a');
Bigram b = new Bigram('a', 'a');
```
</div>

از نظر معنایی، `a` و `b` برابرند؛ چون هر دو نمایندهٔ زوج مرتب `('a', 'a')` هستند.

<div dir="ltr">

```java
System.out.println(a.equals(b));  // true
```
</div>

خروجی `true` است؛ چون کامپایلر متد `equals(Bigram)` را پیدا می‌کند و همان متد اجرا می‌شود.

پس مشکل کجاست؟

مشکل زمانی آشکار می‌شود که از یک مجموعه مثل `HashSet` استفاده کنیم:

<div dir="ltr">

```java
Set<Bigram> set = new HashSet<>();

for (int i = 0; i < 10; i++) {
    for (char ch = 'a'; ch <= 'z'; ch++) {
        set.add(new Bigram(ch, ch));
    }
}

System.out.println(set.size());
```
</div>

| انتظار | واقعیت |
|--------|--------|
| `26` (فقط ۲۶ زوج متفاوت) | `260` |

انتظار ما: ۲۶ عضو — هر زوج تکراری باید با زوج قبلی برابر تشخیص داده شود.

خروجی واقعی: ۲۶۰ عضو — هر بار که یک شیء جدید ساخته می‌شود، مجموعه آن را یک عضو متفاوت می‌بیند.

[بازگشت به بالا](#top)

---

<a id="root-cause"></a>
## ۲. علت اصلی: به‌جای Override، یک متد جدید تعریف کرده‌ایم

<div dir="ltr">

```java
public boolean equals(Bigram b)     // متد ما
public boolean equals(Object obj)   // امضای Object.equals
```
</div>

| متد | پارامتر |
|-----|---------|
| `equals` در `Object` | `Object` |
| متد تعریف‌شده در `Bigram` | `Bigram` |

بنابراین متد ما، متد `Object.equals` را override نکرده است؛ بلکه یک متد جدید با نام مشابه و پارامتر متفاوت تعریف کرده است. به این کار **overloading** می‌گوییم.

<a id="overloading-vs-overriding"></a>
### تفاوت Overloading و Overriding

| مفهوم | توضیح |
|--------|-------|
| **Overloading** | چند متد با نام یکسان، اما امضای متفاوت تعریف می‌شوند |
| **Overriding** | یک متد به‌ارث‌رسیده با امضای سازگار بازتعریف می‌شود تا رفتار آن تغییر کند |

در نتیجه، هر شیء `Bigram` همچنان پیاده‌سازی اصلی `Object.equals(Object)` را به ارث می‌برد که برابری هویتی را بررسی می‌کند.

<div dir="ltr">

```java
Bigram x = new Bigram('a', 'a');
Bigram y = new Bigram('a', 'a');

System.out.println(x == y);       // false
System.out.println(x.equals(y));  // true ← متد equals(Bigram) صدا زده می‌شود
```
</div>

> **نکته مهم:** `HashSet` برای بررسی برابری منطقی عناصر از قرارداد `equals(Object)` استفاده می‌کند، نه از overload اختصاصی ما. بنابراین اشیای جداگانه، برابر تشخیص داده نمی‌شوند.

> **نکته تکمیلی:** برای رعایت کامل قرارداد `HashSet`، باید `equals` و `hashCode` با هم سازگار باشند (Item 10 و Item 11).

[بازگشت به بالا](#top)

---

<a id="how-override-catches"></a>
## ۳. چگونه `@Override` این باگ را پیدا می‌کند؟

<div dir="ltr">

```java
@Override
public boolean equals(Bigram b) {
    return b.first == first && b.second == second;
}
```
</div>

کامپایلر خطا می‌دهد:

```
method does not override or implement a method from a supertype
```

چرا؟ چون ما اعلام کرده‌ایم قصد override کردن یک متد به‌ارث‌رسیده را داریم، ولی کامپایلر می‌بیند چنین متدی با این امضا در والد وجود ندارد.

> **ارزش `@Override`:** اشتباه در قصد ما را به یک خطای قابل‌تشخیص در زمان کامپایل تبدیل می‌کند، نه یک باگ پنهان در زمان اجرا.

### پیاده‌سازی صحیح

<div dir="ltr">

```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Bigram)) {
        return false;
    }

    Bigram b = (Bigram) o;
    return b.first == first && b.second == second;
}
```
</div>

با pattern matching در Java جدید:

<div dir="ltr">

```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Bigram b)) {
        return false;
    }
    return b.first == first && b.second == second;
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="why-habit"></a>
## ۴. چرا باید `@Override` را به‌صورت یک عادت همیشگی استفاده کنیم؟

<a id="benefit1"></a>
### مزیت اول: کامپایلر خطاهای امضا را پیدا می‌کند

<div dir="ltr">

```java
class Animal {
    public void makeSound() {
        System.out.println("Sound");
    }
}

class Dog extends Animal {
    @Override
    public void makesound() {  // ❌ حرف S کوچک است
        System.out.println("Woof");
    }
}
```
</div>

بدون `@Override`، این کد کامپایل می‌شود؛ اما `Dog` متد جدیدی تعریف کرده و رفتار `Animal.makeSound()` را تغییر نداده است.

با `@Override`، کامپایلر خطا می‌دهد و اشتباه را همان ابتدا مشخص می‌کند.

<a id="benefit2"></a>
### مزیت دوم: IDE می‌تواند overrideهای ناخواسته را پیدا کند

بسیاری از IDEها می‌توانند هشدار دهند که متدی را تعریف کرده‌ای که ظاهراً قرار بوده یک متد والد را override کند، اما `@Override` ندارد.

| ابزار | نقش |
|-------|-----|
| **کامپایلر** | اگر `@Override` نوشته‌ای ولی متدی برای override کردن وجود ندارد، خطا می‌دهد |
| **IDE** | می‌تواند متدهایی را که احتمالاً قصد override کردنشان را داشته‌ای ولی annotation را فراموش کرده‌ای، مشخص کند |

[بازگشت به بالا](#top)

---

<a id="not-only-parents"></a>
## ۵. آیا `@Override` فقط برای کلاس‌های والد است؟

**خیر.** این annotation برای متدهایی که متدهای یک interface را پیاده‌سازی می‌کنند نیز قابل استفاده است.

<div dir="ltr">

```java
interface PaymentProcessor {
    void processPayment();
}

class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public void processPayment() {
        System.out.println("Processing card payment");
    }
}
```
</div>

فرض کن اشتباهاً بنویسی:

<div dir="ltr">

```java
class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public void processPayments() {  // ❌ Compile Error
        System.out.println("Processing card payment");
    }
}
```
</div>

چون نام متد متفاوت است، کامپایلر خطا می‌دهد.

<a id="default-methods"></a>
### ارتباط این موضوع با Default Methodها

در Java 8، قابلیت default method به interfaceها اضافه شد:

<div dir="ltr">

```java
interface Logger {
    default void log(String message) {
        System.out.println(message);
    }
}

class FileLogger implements Logger {
    @Override
    public void log(String message) {
        // Write message to a file
    }
}
```
</div>

> **توصیه:** پیاده‌سازی‌های concrete متدهای interface نیز معمولاً `@Override` داشته باشند؛ به‌ویژه وقتی interface ممکن است در آینده متدهای default جدیدی دریافت کند.

[بازگشت به بالا](#top)

---

<a id="exception"></a>
## ۶. استثنای کوچک: پیاده‌سازی متدهای abstract در کلاس concrete

<div dir="ltr">

```java
abstract class Shape {
    public abstract double area();
}

class Circle extends Shape {
    private final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    public double area() {
        return Math.PI * radius * radius;
    }
}
```
</div>

در اینجا حتی اگر `@Override` را حذف کنیم، کامپایلر مطمئن می‌شود که کلاس concrete تمام متدهای abstract لازم را پیاده‌سازی کرده باشد.

اگر امضای متد اشتباه باشد:

<div dir="ltr">

```java
public double areas() {  // ❌
    return Math.PI * radius * radius;
}
```
</div>

کلاس `Circle` همچنان متد abstract یعنی `area()` را پیاده‌سازی نکرده است؛ بنابراین کامپایل نمی‌شود.

> **نتیجه:** از نظر فنی، در این حالت `@Override` ضروری نیست. بااین‌حال، استفاده از آن همچنان توصیه می‌شود؛ چون قصد برنامه‌نویس را روشن‌تر می‌کند و بررسی کد را آسان‌تر می‌سازد.

[بازگشت به بالا](#top)

---

<a id="interfaces-abstract"></a>
## ۷. چرا در interfaceها و کلاس‌های abstract هم باید دقت کنیم؟

در یک کلاس abstract یا خود interface، بهتر است متدهایی را که تصور می‌کنیم از نوع والد یا interface والد override می‌کنند، با `@Override` مشخص کنیم؛ چه متد concrete باشند و چه abstract.

<div dir="ltr">

```java
interface Parent {
    void execute();
}

interface Child extends Parent {
    @Override
    void execute();
}
```
</div>

`@Override` به کامپایلر کمک می‌کند تأیید کند این متد واقعاً با متد والد مطابقت دارد.

[بازگشت به بالا](#top)

---

<a id="summary"></a>
## ۸. جمع‌بندی عملی Item 40

| وضعیت | توصیه |
|--------|-------|
| Override کردن متد یک کلاس والد | همیشه `@Override` بگذار |
| پیاده‌سازی متد interface | معمولاً `@Override` بگذار |
| Override کردن default method | `@Override` بگذار |
| پیاده‌سازی متد abstract در کلاس concrete | الزامی نیست، اما توصیه می‌شود |
| اعلام مجدد متد والد در interface یا کلاس abstract | `@Override` به تشخیص اشتباهات کمک می‌کند |

اصل این آیتم فقط مربوط به `equals` نیست. همین مشکل می‌تواند برای `toString`، `compareTo`، متدهای interfaceها یا هر متدی که نام و امضای آن اهمیت دارد اتفاق بیفتد:

<div dir="ltr">

```java
public String tostring() {  // ❌
    return "MyObject";
}
```
</div>

اگر قصد بازنویسی `Object.toString()` را داشته باشیم، افزودن `@Override` باعث می‌شود اشتباه در حروف بزرگ و کوچک فوراً مشخص شود.

[بازگشت به بالا](#top)

---

<a id="final-rule"></a>
## قاعدهٔ نهایی کتاب

> **هر جا فکر می‌کنی متدی را override می‌کنی، `@Override` بگذار. این عادت ساده، هزینهٔ بسیار کمی دارد و می‌تواند از باگ‌های دشوار و گمراه‌کننده جلوگیری کند.**

---

[بازگشت به بالا](#top)

</div>