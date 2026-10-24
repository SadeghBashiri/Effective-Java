<div dir="rtl">

<a id="top"></a>

# آیتم ۴۲: Lambdaها را به کلاس‌های ناشناس ترجیح دهید

## (Prefer Lambdas to Anonymous Classes)

در این آیتم از کتاب Effective Java، Joshua Bloch توضیح می‌دهد که چرا از Java 8 به بعد، برای نمایش رفتارهای کوچک و قابل‌انتقال، معمولاً باید از Lambda Expression به‌جای Anonymous Class استفاده کنیم.

> **ایدهٔ اصلی:** وقتی می‌خواهیم رفتاری را به‌عنوان یک مقدار به متدی بدهیم، معمولاً لازم نیست برای آن یک کلاس کامل و پر از کدهای اضافی بنویسیم؛ یک lambda کافی است.

این موضوع فقط دربارهٔ کوتاه‌تر شدن کد نیست. Lambdaها نوشتن کدهای تابعی (Functional Programming) را در Java ساده‌تر می‌کنند و باعث می‌شوند رفتار موردنظر از جزئیات پیاده‌سازی واضح‌تر باشد.

---

## فهرست مطالب

- [۱. پیش از Java 8، رفتار را چگونه به متدها می‌دادیم؟](#before-java8)
  - [ارتباط با الگوی Strategy](#strategy-pattern)
- [۲. Lambda Expression چه تغییری ایجاد کرد؟](#lambda-change)
- [۳. Functional Interface و استنتاج نوع (Type Inference)](#functional-interface)
  - [آیا همیشه باید نوع پارامترهای lambda را بنویسیم؟](#parameter-types)
- [۴. چرا استفاده از Generics برای lambdaها اهمیت بیشتری دارد؟](#generics-importance)
- [۵. می‌توانیم کد را از این هم کوتاه‌تر کنیم!](#even-shorter)
- [۶. Lambdaها چگونه طراحی Enum را ساده‌تر می‌کنند؟](#enum-design)
  - [`DoubleBinaryOperator` چیست؟](#double-binary-operator)
  - [چرا این طراحی بهتر است؟](#why-better)
- [۷. آیا پس از Java 8، Constant-Specific Class Bodyها دیگر کاربردی ندارند؟](#constant-specific)
- [۸. آیا Anonymous Classها کاملاً منسوخ شده‌اند؟](#not-obsolete)
  - [تفاوت اول: Lambda فقط برای Functional Interface است](#diff1)
  - [تفاوت دوم: معنای `this` متفاوت است](#diff2)
  - [تفاوت سوم: Lambda نمی‌تواند مستقیماً به خودش ارجاع دهد](#diff3)
- [۹. چرا نباید Lambdaها را به‌راحتی Serialize کنیم؟](#serialization)
- [۱۰. جمع‌بندی نهایی Item 42](#final-summary)

[بازگشت به بالا](#top)

---

<a id="before-java8"></a>
## ۱. پیش از Java 8، رفتار را چگونه به متدها می‌دادیم؟

فرض کن لیستی از رشته‌ها داریم و می‌خواهیم آن‌ها را بر اساس طولشان مرتب کنیم:

<div dir="ltr">

```java
List<String> words = new ArrayList<>();
words.add("Java");
words.add("Effective");
words.add("Code");
```
</div>

پیش از Java 8، یکی از روش‌های رایج، استفاده از Anonymous Class بود:

<div dir="ltr">

```java
Collections.sort(words, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
});
```
</div>

منطق اصلی فقط این است:

<div dir="ltr">

```java
Integer.compare(s1.length(), s2.length())
```
</div>

اما برای بیان همین منطق، مجبوریم ساختار کلاس، نام متد، نوع پارامترها و بدنهٔ پیاده‌سازی را هم بنویسیم. این کد درست است، ولی برای یک رفتار کوچک، مقدار زیادی کد اضافی دارد.

<a id="strategy-pattern"></a>
### ارتباط با الگوی Strategy

در این الگو، یک رفتار یا الگوریتم را از کدی که از آن استفاده می‌کند جدا می‌کنیم تا بتوانیم پیاده‌سازی‌های مختلفی را به‌جای یکدیگر قرار دهیم.

در مثال ما:
- `Comparator` نقش قرارداد Strategy را دارد
- کلاس ناشناس، یک پیاده‌سازی مشخص از این قرارداد است
- `Collections.sort` از این رفتار برای تصمیم‌گیری دربارهٔ ترتیب عناصر استفاده می‌کند

[بازگشت به بالا](#top)

---

<a id="lambda-change"></a>
## ۲. Lambda Expression چه تغییری ایجاد کرد؟

از Java 8 به بعد، می‌توانیم همان رفتار را با lambda بنویسیم:

<div dir="ltr">

```java
Collections.sort(
    words,
    (s1, s2) -> Integer.compare(s1.length(), s2.length())
);
```
</div>

| Anonymous Class | Lambda Expression |
|-----------------|-------------------|
| `new Comparator<String>() { @Override public int compare(String s1, String s2) { return Integer.compare(s1.length(), s2.length()); } }` | `(s1, s2) -> Integer.compare(s1.length(), s2.length())` |

لامبدا مستقیماً رفتار موردنظر را بیان می‌کند، بدون اینکه مجبور باشیم ساختار کلاس را بنویسیم.

سمت چپ `->` پارامترهای ورودی را مشخص می‌کند و سمت راست آن عبارت یا بلوکی است که اجرا می‌شود.

> **نکتهٔ کلیدی:** Lambdaها جایگزین همهٔ کلاس‌ها نیستند؛ آن‌ها برای پیاده‌سازی interfaceهای تابعی طراحی شده‌اند.

[بازگشت به بالا](#top)

---

<a id="functional-interface"></a>
## ۳. Functional Interface و استنتاج نوع (Type Inference)

چرا Java اجازه می‌دهد چنین کدی بنویسیم:

<div dir="ltr">

```java
(s1, s2) -> Integer.compare(s1.length(), s2.length())
```
</div>

درحالی‌که نوع پارامترها را مشخص نکرده‌ایم؟

چون `Comparator<String>` یک **Functional Interface** است؛ یعنی interfaceای که دقیقاً یک متد abstract دارد:

<div dir="ltr">

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```
</div>

وقتی می‌نویسیم:

<div dir="ltr">

```java
Collections.sort(words, (s1, s2) ->
    Integer.compare(s1.length(), s2.length())
);
```
</div>

کامپایلر از امضای متد `sort` و نوع `words` نتیجه می‌گیرد که آرگومان دوم باید یک `Comparator<String>` باشد. در نتیجه:
- `s1` از نوع `String` است
- `s2` از نوع `String` است
- نتیجه باید با نوع بازگشتی `int` در متد `compare` سازگار باشد

به این فرایند **Type Inference** می‌گوییم؛ یعنی کامپایلر نوع‌ها را از بافت کد استنتاج می‌کند.

<a id="parameter-types"></a>
### آیا همیشه باید نوع پارامترهای lambda را بنویسیم؟

**خیر.** معمولاً بهتر است اجازه دهیم کامپایلر نوع‌ها را استنتاج کند:

<div dir="ltr">

```java
Comparator<String> byLength =
    (s1, s2) -> Integer.compare(s1.length(), s2.length());
```
</div>

در بیشتر موارد نیازی نیست این‌طور بنویسیم:

<div dir="ltr">

```java
Comparator<String> byLength =
    (String s1, String s2) ->
        Integer.compare(s1.length(), s2.length());
```
</div>

بااین‌حال، اگر کامپایلر نتواند نوع پارامتر را استنتاج کند یا مشخص‌کردن نوع باعث خوانایی بهتر شود، می‌توانیم نوع را صریح بنویسیم.

> **قاعدهٔ عملی کتاب:** نوع پارامترهای lambda را حذف کن، مگر اینکه مشخص‌کردن آن‌ها برای کامپایل یا خوانایی لازم باشد.

[بازگشت به بالا](#top)

---

<a id="generics-importance"></a>
## ۴. چرا استفاده از Generics برای lambdaها اهمیت بیشتری دارد؟

نویسنده به آیتم‌های قبلی کتاب ارجاع می‌دهد:

| Item | موضوع |
|------|-------|
| Item 26 | از raw typeها استفاده نکنید |
| Item 29 | نوع‌های generic را ترجیح دهید |
| Item 30 | متدهای generic را ترجیح دهید |

این توصیه‌ها در استفاده از lambda اهمیت ویژه‌ای دارند؛ چون کامپایلر برای استنتاج نوع‌ها به اطلاعات نوعی موجود در کد نیاز دارد.

<div dir="ltr">

```java
List<String> words = new ArrayList<>();
```
</div>

کامپایلر می‌داند عناصر لیست از نوع `String` هستند. پس در کد زیر می‌تواند نوع پارامترهای lambda را استنتاج کند:

<div dir="ltr">

```java
words.sort((s1, s2) ->
    Integer.compare(s1.length(), s2.length())
);
```
</div>

اما اگر از raw type استفاده کنیم:

<div dir="ltr">

```java
List words = new ArrayList();  // ❌
```
</div>

اطلاعات مربوط به نوع عناصر از دست می‌رود. در چنین شرایطی، کامپایلر ممکن است نتواند نوع پارامترهای lambda را به شکل مورد انتظار استنتاج کند.

> **نتیجه:** استفادهٔ صحیح از Generics فقط به امنیت نوع‌ها مربوط نیست؛ بلکه کدهای lambda را نیز ساده‌تر و خواناتر می‌کند.

[بازگشت به بالا](#top)

---

<a id="even-shorter"></a>
## ۵. می‌توانیم کد را از این هم کوتاه‌تر کنیم!

تا اینجا از lambda استفاده کردیم، اما نویسنده نشان می‌دهد که حتی ممکن است اصلاً به نوشتن lambda هم نیاز نداشته باشیم.

**نسخهٔ قبلی:**

<div dir="ltr">

```java
words.sort((s1, s2) ->
    Integer.compare(s1.length(), s2.length())
);
```
</div>

**نسخهٔ ساده‌تر با `comparingInt` و Method Reference:**

<div dir="ltr">

```java
import java.util.Comparator;

words.sort(Comparator.comparingInt(String::length));
```
</div>

| قابلیت | نقش |
|--------|-----|
| `comparingInt` | یک Comparator می‌سازد که مقایسه را بر اساس یک مقدار `int` انجام می‌دهد |
| `String::length` | یک Method Reference است که به کامپایلر می‌گوید از متد `length` رشته‌ها برای استخراج کلید مرتب‌سازی استفاده کند |

به‌جای اینکه خودمان دو رشته را مقایسه کنیم، می‌گوییم:

> «رشته‌ها را بر اساس مقدار `length` آن‌ها مرتب کن.»

همچنین `List` از Java 8 به بعد متد `sort` دارد؛ بنابراین دیگر نیازی به فراخوانی `Collections.sort` هم نیست.

### سه مرحله تکامل

| روش | کد |
|------|-----|
| **روش قدیمی** | `Collections.sort(words, new Comparator<String>() { ... });` |
| **با lambda** | `words.sort((a, b) -> Integer.compare(a.length(), b.length()));` |
| **روش پیشنهادی** | `words.sort(Comparator.comparingInt(String::length));` |

> **نکتهٔ مهم:** Lambda همیشه بهترین شکل ممکن نیست. گاهی Method Reference یا یک متد کمکی استاندارد، از lambda هم واضح‌تر است.

[بازگشت به بالا](#top)

---

<a id="enum-design"></a>
## ۶. Lambdaها چگونه طراحی Enum را ساده‌تر می‌کنند؟

یکی از مثال‌های مهم کتاب، ارتباط این آیتم با Item 34 است.

**نسخهٔ Constant-Specific Class Body:**

<div dir="ltr">

```java
public enum Operation {
    PLUS("+") {
        @Override
        public double apply(double x, double y) { return x + y; }
    },
    MINUS("-") {
        @Override
        public double apply(double x, double y) { return x - y; }
    },
    TIMES("*") {
        @Override
        public double apply(double x, double y) { return x * y; }
    },
    DIVIDE("/") {
        @Override
        public double apply(double x, double y) { return x / y; }
    };

    private final String symbol;

    Operation(String symbol) { this.symbol = symbol; }

    @Override
    public String toString() { return symbol; }

    public abstract double apply(double x, double y);
}
```
</div>

**نسخهٔ مبتنی بر lambda:**

<div dir="ltr">

```java
import java.util.function.DoubleBinaryOperator;

public enum Operation {
    PLUS("+", (x, y) -> x + y),
    MINUS("-", (x, y) -> x - y),
    TIMES("*", (x, y) -> x * y),
    DIVIDE("/", (x, y) -> x / y);

    private final String symbol;
    private final DoubleBinaryOperator op;

    Operation(String symbol, DoubleBinaryOperator op) {
        this.symbol = symbol;
        this.op = op;
    }

    @Override
    public String toString() { return symbol; }

    public double apply(double x, double y) {
        return op.applyAsDouble(x, y);
    }
}
```
</div>

حالا رفتار هر ثابت در همان خط تعریف آن مشخص شده است:

<div dir="ltr">

```java
PLUS("+", (x, y) -> x + y)
```
</div>

> این الگو را **ترکیب رفتار با داده (Behavior as Data)** می‌توان نامید: رفتار دیگر فقط در بدنهٔ متدها پنهان نشده، بلکه به‌صورت یک مقدار به شیء داده می‌شود.

<a id="double-binary-operator"></a>
### `DoubleBinaryOperator` چیست؟

این interface یکی از Functional Interfaceهای آماده در `java.util.function` است:

<div dir="ltr">

```java
double applyAsDouble(double left, double right);
```
</div>

پس تابعی با دو ورودی از نوع `double` و خروجی `double` را نمایش می‌دهد. به همین دلیل، برای عملگرهای ریاضی مثال مناسبی است؛ نیازی به تعریف interface اختصاصی نداریم.

<a id="why-better"></a>
### چرا این طراحی بهتر است؟

- تعریف ثابت‌ها ساده‌تر شده است
- رفتار هر عملگر در همان محل تعریف آن دیده می‌شود
- نیاز به override جداگانه برای هر ثابت از بین می‌رود
- اضافه‌کردن عملیات ساده‌تر می‌شود

> **البته:** این نتیجه به ساده‌بودن رفتارها بستگی دارد؛ نویسنده تأکید می‌کند که Constant-Specific Class Bodyها هنوز هم کاربرد دارند.

[بازگشت به بالا](#top)

---

<a id="constant-specific"></a>
## ۷. آیا پس از Java 8، Constant-Specific Class Bodyها دیگر کاربردی ندارند؟

**خیر.** این یکی از نکات ظریف این آیتم است.

در مثال قبلی، هر lambda یک خط بود. اما اگر رفتار یک ثابت enum پیچیده شود، lambda طولانی می‌تواند خوانایی کد را کاهش دهد:

<div dir="ltr">

```java
PROCESS("+", (x, y) -> {
    // Several validation steps
    // Multiple conditional branches
    // Complex calculations
    return x + y;
})
```
</div>

اگر پیاده‌سازی واقعاً پیچیده باشد، بهتر است آن را به یک متد نام‌دار منتقل کنیم یا از Constant-Specific Class Body استفاده کنیم.

> **توصیه:** Lambdaها را کوچک نگه داریم؛ یک خط ایده‌آل است و سه خط را می‌توان حد معقولی برای راهنمایی اولیه در نظر گرفت، نه یک قانون مطلق زبان Java.

### محدودیت مهم: دسترسی به فیلدهای نمونه

آرگومان‌های سازندهٔ enum در یک زمینهٔ ایستا ارزیابی می‌شوند. بنابراین lambdaای که به سازندهٔ enum داده می‌شود، نمی‌تواند مستقیماً به فیلدها یا متدهای نمونهٔ همان enum دسترسی داشته باشد:

<div dir="ltr">

```java
enum Operation {
    PLUS("+", (x, y) -> x + y);  // lambda به symbol دسترسی ندارد

    private final String symbol;

    Operation(String symbol, DoubleBinaryOperator op) {
        this.symbol = symbol;
    }
}
```
</div>

### جمع‌بندی این بخش

| وضعیت | انتخاب |
|--------|--------|
| رفتار ساده و کوتاه | Lambda |
| رفتار پیچیده | متد نام‌دار یا Constant-Specific Class Body |
| وابسته به فیلدهای نمونهٔ اختصاصی | Constant-Specific Class Body |

> **اصل مهم:** کوتاه‌تر بودن کد همیشه به معنای بهتر بودن آن نیست؛ خوانایی و قابلیت نگهداری اهمیت بیشتری دارند.

[بازگشت به بالا](#top)

---

<a id="not-obsolete"></a>
## ۸. آیا Anonymous Classها کاملاً منسوخ شده‌اند؟

**خیر.** Lambdaها بسیاری از کاربردهای کلاس‌های ناشناس را ساده کرده‌اند، اما جایگزین کامل آن‌ها نیستند.

<a id="diff1"></a>
### تفاوت اول: Lambda فقط برای Functional Interface است

<div dir="ltr">

```java
interface Task {
    void execute();
}

Task task = () -> System.out.println("Executing");  // ✅
```
</div>

اما اگر interface دو متد abstract داشته باشد:

<div dir="ltr">

```java
interface Task {
    void execute();
    void cancel();
}
```
</div>

یک lambda نمی‌تواند به‌تنهایی پیاده‌سازی هر دو متد را ارائه دهد. در این حالت باید از Anonymous Class استفاده کنیم:

<div dir="ltr">

```java
Task task = new Task() {
    @Override
    public void execute() {
        System.out.println("Executing");
    }

    @Override
    public void cancel() {
        System.out.println("Cancelled");
    }
};
```
</div>

همچنین برای ساخت نمونه از یک کلاس abstract، lambda جایگزین مستقیم Anonymous Class نیست.

> **نکتهٔ فنی:** Functional Interface می‌تواند متدهای دیگری هم داشته باشد، مشروط بر اینکه فقط یک متد abstract مرتبط داشته باشد. وجود متدهای `default` یا `static` مانع functional بودن آن نمی‌شود.

<a id="diff2"></a>
### تفاوت دوم: معنای `this` متفاوت است

این مورد در کدنویسی حرفه‌ای اهمیت زیادی دارد.

| سازوکار | معنای `this` |
|----------|--------------|
| **Lambda** | به نمونهٔ کلاس بیرونی اشاره می‌کند (همان نمونه‌ای که lambda در آن تعریف شده است) |
| **Anonymous Class** | به خود نمونهٔ کلاس ناشناس اشاره می‌کند |

<div dir="ltr">

```java
class Example {
    private String name = "Outer";

    Runnable lambda = () -> {
        System.out.println(this.name);  // "Outer"
    };

    Runnable anonymous = new Runnable() {
        private String name = "Anonymous";

        @Override
        public void run() {
            System.out.println(this.name);  // "Anonymous"
        }
    };
}
```
</div>

<a id="diff3"></a>
### تفاوت سوم: Lambda نمی‌تواند مستقیماً به خودش ارجاع دهد

فرض کن یک function object لازم دارد داخل پیاده‌سازی خودش به همان نمونه اشاره کند. در Anonymous Class می‌توان از `this` برای ارجاع به خود نمونه استفاده کرد. در lambda، `this` معنای دیگری دارد و به نمونهٔ بیرونی اشاره می‌کند.

> **نتیجه:** اگر واقعاً به ارجاع مستقیم به خود function object نیاز داشته باشیم، باید طراحی را بازبینی کنیم یا از Anonymous Class استفاده کنیم.

[بازگشت به بالا](#top)

---

<a id="serialization"></a>
## ۹. چرا نباید Lambdaها را به‌راحتی Serialize کنیم؟

نویسنده در پایان یک هشدار مهم مطرح می‌کند: **نباید روی serialize و deserialize کردن lambdaها یا نمونه‌های Anonymous Class بین پیاده‌سازی‌ها یا نسخه‌های متفاوت برنامه حساب باز کنیم.**

ممکن است lambda در بعضی شرایط قابلیت serialization داشته باشد، اما شکل داخلی آن و سازوکار بازسازی آن قرارداد مناسبی برای ذخیره‌سازی بلندمدت یا انتقال قابل‌اتکا بین محیط‌های متفاوت نیست.

**راه‌حل: استفاده از کلاس static nested خصوصی نام‌دار:**

```java
private static class LengthComparator
        implements Comparator<String>, Serializable {

    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
}
```

این کلاس نام‌دار است و می‌توانیم ساختار و رفتار serialization آن را بهتر مدیریت کنیم.

> **نکته:** حتی در این حالت نیز سازگاری بلندمدت به نسخه‌بندی کلاس و فیلدهای آن وابسته است. این موضوع برای سیستم‌هایی که از Java Serialization استفاده می‌کنند، به‌خصوص در ذخیره‌سازی طولانی‌مدت داده‌ها یا ارتباط بین سرویس‌ها، اهمیت دارد.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۱۰. جمع‌بندی نهایی Item 42

| موقعیت | انتخاب پیشنهادی |
|---------|-----------------|
| رفتار کوتاه برای یک Functional Interface | Lambda |
| استخراج یک متد موجود | Method Reference |
| مرتب‌سازی بر اساس یک کلید ساده | متدهایی مانند `Comparator.comparingInt` |
| ساخت نمونه از interface با چند متد abstract | Anonymous Class یا کلاس نام‌دار |
| ساخت نمونه از کلاس abstract | Anonymous Class یا کلاس concrete |
| نیاز به `this` برای ارجاع به خود function object | Anonymous Class |
| رفتار پیچیده و چندمرحله‌ای | متد نام‌دار یا کلاس مناسب، نه lambda طولانی |
| نیاز به serialization قابل‌کنترل | کلاس static nested نام‌دار |

### سه اصل مهم

| # | اصل |
|---|-----|
| **۱** | برای رفتارهای کوچک، lambda را به Anonymous Class ترجیح بده |
| **۲** | اگر Method Reference یا متد کمکی استاندارد قصد کد را واضح‌تر بیان می‌کند، از آن استفاده کن |
| **۳** | برای پیچیدگی زیاد، نیازهای خاص کلاس یا serialization، صرفاً به‌خاطر مختصر بودن از lambda استفاده نکن |

### نکتهٔ پایانی

> **مهم‌ترین دستاورد Java 8 فقط حذف چند خط کد اضافی نیست؛ Lambdaها باعث شدند انتقال رفتار به‌عنوان یک مقدار در Java طبیعی‌تر شود. این قابلیت، نوشتن کدهای تابعی، استفاده از APIهای مبتنی بر رفتار و ساختارهایی مانند `Comparator` و `java.util.function` را بسیار ساده‌تر کرده است.**

---

[بازگشت به بالا](#top)

</div>
