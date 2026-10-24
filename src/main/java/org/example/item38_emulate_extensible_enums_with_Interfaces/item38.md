<div dir="rtl">

<a id="top"></a>

# آیتم ۳۸: با Interfaceها، Enumهای قابل توسعه را شبیه‌سازی کنید

## (Emulate extensible enums with interfaces)

موضوع این بخش از کتاب Effective Java, 3rd Edition، یکی از تصمیم‌های مهم در طراحی APIهای قابل توسعه در جاوا است:

چگونه اجازه دهیم مصرف‌کنندگان یک API عملیات جدیدی به سیستم اضافه کنند، بدون اینکه خود `enum` را قابل ارث‌بری کنیم؟

این مسئله مستقیماً به طراحی قراردادها، اصل Open/Closed، قابلیت توسعه‌پذیری و محدودیت‌های ارث‌بری در Java مربوط می‌شود.

---

## فهرست مطالب

- [۱. چرا `enum` به‌صورت مستقیم قابل توسعه نیست؟](#why-not-extensible)
  - [دلایل طراحی زبان](#language-design-reasons)
- [۲. راه‌حل اصلی: جداکردن قرارداد از پیاده‌سازی](#core-solution)
  - [گام اول: تعریف قرارداد `Operation`](#step1)
  - [گام دوم: تعریف عملیات استاندارد](#step2)
- [۳. افزودن عملیات جدید بدون تغییر `BasicOperation`](#extended-operation)
  - [این طراحی چه چیزی را حل می‌کند؟](#what-it-solves)
- [۴. شرط حیاتی: API باید `Operation` بپذیرد، نه `BasicOperation`](#api-contract)
- [۵. چگونه تمام مقادیر یک `enum` توسعه‌یافته را پیمایش کنیم؟](#iterate-values)
  - [راه‌حل اول: استفاده از `Class<T>`](#class-solution)
  - [راه‌حل دوم: دریافت `Collection<? extends Operation>`](#collection-solution)
  - [مقایسه دو راه‌حل](#comparison)
- [۶. محدودیت مهم: امکان به‌اشتراک‌گذاری پیاده‌سازی میان `enum`ها](#sharing-limitation)
  - [گزینه اول: تکرار کد محدود](#option1)
  - [گزینه دوم: استفاده از `default` در interface](#option2)
  - [گزینه سوم: استفاده از Helper](#option3)
- [۷. نمونه‌ای از همین الگو در کتابخانه استاندارد جاوا](#jdk-example)
- [۸. نگاه معماری: این الگو چگونه به طراحی سیستم‌های بزرگ کمک می‌کند؟](#architectural-view)
  - [مقایسه گزینه‌های معماری](#architectural-comparison)
  - [چند ملاحظه Production-Grade](#production-notes)
- [۹. جمع‌بندی نهایی Item 38](#final-summary)

[بازگشت به بالا](#top)

---

<a id="why-not-extensible"></a>
## ۱. چرا `enum` به‌صورت مستقیم قابل توسعه نیست؟

فرض کن در Java چنین `enum`ای تعریف کرده‌ایم:

<div dir="ltr">

```java
public enum BasicOperation {
    PLUS,
    MINUS,
    TIMES,
    DIVIDE
}
```
</div>

اکنون می‌خواهیم نوع جدیدی از عملیات بسازیم که عملیات قبلی را گسترش دهد:

<div dir="ltr">

```java
// Invalid Java
public enum ExtendedOperation extends BasicOperation {
    POWER,
    REMAINDER
}
```
</div>

این کد در جاوا معتبر نیست.

دلیل این محدودیت آن است که هر `enum` در جاوا به‌طور ضمنی از `java.lang.Enum` ارث‌بری می‌کند و نمی‌تواند از یک کلاس دیگر نیز ارث‌بری کند. علاوه بر این، زبان جاوا اجازه نمی‌دهد یک `enum` از `enum` دیگری ارث‌بری کند.

<a id="language-design-reasons"></a>
### دلایل طراحی زبان

**الف) ابهام در رابطه نوع پایه و نوع توسعه‌یافته**

اگر `ExtendedOperation` زیرنوع `BasicOperation` بود، چه رابطه‌ای میان مقادیر دو نوع برقرار می‌شد؟ آیا هر عملیات توسعه‌یافته باید یک نمونه از نوع پایه نیز محسوب می‌شد؟ چگونه باید مجموعه عملیات پایه را تعریف می‌کردیم؟

**ب) دشواری پیمایش تمام مقادیر**

<div dir="ltr">

```java
BasicOperation.values()
```
</div>

این متد فقط مقادیر همان `enum` را برمی‌گرداند. اگر ارث‌بری از `enum`ها مجاز بود، مشخص‌کردن اینکه این متد باید مقادیر نوع پایه را برگرداند یا تمام مقادیر تمام انواع توسعه‌یافته را، به مسئله‌ای پیچیده تبدیل می‌شد.

**ج) پیچیدگی در مدل نوعی**

رفتارهایی مانند `EnumSet`، `EnumMap`، `ordinal()` و `getDeclaringClass()` بر ماهیت مشخص و محدود هر `enum` متکی هستند. ارث‌بری میان `enum`ها می‌توانست معنای این رفتارها را پیچیده‌تر کند.

> **نتیجه:** محدودیت جاوا تصادفی نیست. برای بسیاری از کاربردها، مجموعه مقادیر محدود و مشخص، یکی از مهم‌ترین ویژگی‌های `enum` است.

بااین‌حال، کتاب یک استثنای مهم معرفی می‌کند: **Operation Codes یا Opcodes**؛ یعنی مقادیری که نماینده عملیات قابل اجرا هستند و ممکن است لازم باشد توسعه‌دهندگان بیرونی عملیات جدیدی به آن‌ها اضافه کنند.

[بازگشت به بالا](#top)

---

<a id="core-solution"></a>
## ۲. راه‌حل اصلی: جداکردن قرارداد از پیاده‌سازی

راه‌حل پیشنهادی کتاب بر یک اصل معماری استوار است:

> به‌جای اینکه تمام عملیات را در یک `enum` قرار دهیم، رفتار مشترک آن‌ها را در یک interface تعریف می‌کنیم.

```
Operation — Interface
قرارداد مشترک تمام عملیات

BasicOperation
پیاده‌سازی استاندارد کتابخانه
PLUS, MINUS, TIMES, DIVIDE

ExtendedOperation
پیاده‌سازی قابل افزودن توسط مصرف‌کننده
EXP, REMAINDER
```

هر دو enum مستقل هستند، اما از طریق یک قرارداد مشترک در API قابل استفاده‌اند. این معماری به‌جای ارث‌بری از یک نوع مشخص، بر Polymorphism مبتنی بر Interface تکیه دارد.

<a id="step1"></a>
### گام اول: تعریف قرارداد `Operation`

<div dir="ltr">

```java
public interface Operation {
    double apply(double x, double y);
}
```
</div>

این interface مشخص می‌کند که هر عملیات باید بتواند دو عدد را دریافت کند و نتیجه محاسبه را برگرداند. اما تعیین نمی‌کند که محاسبه چگونه انجام شود.

<a id="step2"></a>
### گام دوم: تعریف عملیات استاندارد

<div dir="ltr">

```java
public enum BasicOperation implements Operation {

    PLUS("+") {
        @Override
        public double apply(double x, double y) {
            return x + y;
        }
    },

    MINUS("-") {
        @Override
        public double apply(double x, double y) {
            return x - y;
        }
    },

    TIMES("*") {
        @Override
        public double apply(double x, double y) {
            return x * y;
        }
    },

    DIVIDE("/") {
        @Override
        public double apply(double x, double y) {
            return x / y;
        }
    };

    private final String symbol;

    BasicOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
```
</div>

**چند نکته در این کد:**

1. **هر ثابت رفتار مستقل خود را دارد:** برای مثال، `PLUS` پیاده‌سازی جمع را ارائه می‌کند و `TIMES` پیاده‌سازی ضرب را (constant-specific class body).
2. **همه ثابت‌ها از قرارداد مشترک پیروی می‌کنند:** چون `BasicOperation` رابط `Operation` را پیاده‌سازی کرده است، هر مقدار آن را می‌توان در جایی استفاده کرد که یک `Operation` انتظار می‌رود.
3. **نماد عملیات بخشی از وضعیت هر ثابت است:** `PLUS` نماد `+` را نگه می‌دارد و متد `toString()` آن را نمایش می‌دهد.

> **نکته مهم:** interface فقط قرارداد رفتار را مشخص کرده است؛ ساختار ذخیره نماد و نحوه نمایش، جزئیات پیاده‌سازی `enum` هستند.

[بازگشت به بالا](#top)

---

<a id="extended-operation"></a>
## ۳. افزودن عملیات جدید بدون تغییر `BasicOperation`

حالا فرض کنیم مصرف‌کننده کتابخانه به دو عملیات جدید نیاز دارد:

- توان: $x^y$
- باقی‌مانده تقسیم: $x \bmod y$

<div dir="ltr">

```java
public enum ExtendedOperation implements Operation {

    EXP("^") {
        @Override
        public double apply(double x, double y) {
            return Math.pow(x, y);
        }
    },

    REMAINDER("%") {
        @Override
        public double apply(double x, double y) {
            return x % y;
        }
    };

    private final String symbol;

    ExtendedOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
```
</div>

`ExtendedOperation` از `BasicOperation` ارث‌بری نکرده است. هر دو مستقیماً قرارداد `Operation` را پیاده‌سازی می‌کنند.

<div dir="ltr">

```java
Operation first = BasicOperation.PLUS;
Operation second = ExtendedOperation.EXP;
```
</div>

هر دو عبارت معتبرند؛ زیرا رابطه موردنیاز کامپایلر، رابطه پیاده‌سازی interface است، نه رابطه ارث‌بری میان دو `enum`.

<a id="what-it-solves"></a>
### این طراحی چه چیزی را حل می‌کند؟

فرض کن کتابخانه ماشین‌حساب را یک تیم نوشته و عملیات جدید را تیم دیگری توسعه داده است.

تیم دوم می‌تواند `ExtendedOperation` را در ماژولی مستقل تعریف کند، بدون اینکه:

- سورس `BasicOperation` را تغییر دهد
- کتابخانه اصلی را بازنویسی کند
- برای اضافه‌کردن یک عملیات، یک نسخه جدید از `BasicOperation` بسازد

این همان ایده‌ای است که در **اصل Open/Closed** مطرح می‌شود: سیستم باید تا حد امکان برای توسعه باز و برای تغییر در کد پایدار بسته باشد.

> **نکته:** صرف استفاده از interface به‌تنهایی تضمین نمی‌کند که تمام اصول Open/Closed رعایت شده‌اند؛ APIهای مصرف‌کننده نیز باید بر اساس قرارداد مشترک طراحی شوند.

[بازگشت به بالا](#top)

---

<a id="api-contract"></a>
## ۴. شرط حیاتی: API باید `Operation` بپذیرد، نه `BasicOperation`

این نکته، مهم‌ترین بخش معماری Item 38 است.

**فرض کن API زیر را داریم:**

<div dir="ltr">

```java
public static double calculate(
        BasicOperation operation,
        double x,
        double y) {

    return operation.apply(x, y);
}
```
</div>

این متد فقط `BasicOperation` را می‌پذیرد. بنابراین:

<div dir="ltr">

```java
calculate(BasicOperation.PLUS, 4, 2);   // ✅
calculate(ExtendedOperation.EXP, 4, 2); // ❌ Compile Error
```
</div>

**راه‌حل:** نوع پارامتر را به قرارداد مشترک تغییر دهیم:

<div dir="ltr">

```java
public static double calculate(
        Operation operation,
        double x,
        double y) {

    return operation.apply(x, y);
}
```
</div>

اکنون هر دو فراخوانی معتبر هستند:

<div dir="ltr">

```java
calculate(BasicOperation.PLUS, 4, 2);   // ✅
calculate(ExtendedOperation.EXP, 4, 2); // ✅
```
</div>

> **قاعده طراحی API:** اگر می‌خواهی مصرف‌کنندگان بتوانند پیاده‌سازی‌های جدیدی ارائه دهند، پارامترها و قراردادهای API را بر اساس interface طراحی کن، نه یک پیاده‌سازی مشخص.

[بازگشت به بالا](#top)

---

<a id="iterate-values"></a>
## ۵. چگونه تمام مقادیر یک `enum` توسعه‌یافته را پیمایش کنیم؟

برای مثال، می‌خواهیم تمام عملیات `ExtendedOperation` را اجرا کنیم.

<div dir="ltr">

```java
ExtendedOperation.values()
```
</div>

این متد فقط مقادیر همان `enum` را برمی‌گرداند. از آنجا که `values()` متدی مخصوص هر `enum` است، در interface مشترک `Operation` تعریف نشده است.

<a id="class-solution"></a>
### راه‌حل اول: استفاده از `Class<T>` و محدودیت‌های جنریک

<div dir="ltr">

```java
private static <T extends Enum<T> & Operation> void test(
        Class<T> opEnumType,
        double x,
        double y) {

    for (Operation op : opEnumType.getEnumConstants()) {
        System.out.printf(
            "%f %s %f = %f%n",
            x, op, y, op.apply(x, y)
        );
    }
}
```
</div>

و نحوه فراخوانی:

<div dir="ltr">

```java
public static void main(String[] args) {
    double x = Double.parseDouble(args[0]);
    double y = Double.parseDouble(args[1]);

    test(ExtendedOperation.class, x, y);
}
```
</div>

**الف) چرا `Class<T>`؟**

`Class<T> opEnumType` شیء `Class` مربوط به نوع عملیات را دریافت می‌کند. برای مثال `ExtendedOperation.class`.

سپس:

<div dir="ltr">

```java
opEnumType.getEnumConstants()
```
</div>

این متد ثابت‌های `enum` مربوط به همان کلاس را برمی‌گرداند. این موضوع با Item 33 کتاب ارتباط دارد: استفاده از `Class` به‌عنوان یک Type Token برای انتقال اطلاعات نوع به زمان اجرا.

**ب) چرا محدودیت جنریک این‌قدر پیچیده است؟**

<div dir="ltr">

```java
<T extends Enum<T> & Operation>
```
</div>

این عبارت یک **Intersection Type Bound** است؛ یعنی `T` باید هم‌زمان هر دو شرط را داشته باشد:

1. از نوع `Enum<T>` باشد
2. رابط `Operation` را پیاده‌سازی کرده باشد

به زبان ساده:

> فقط اجازه بده نوعی به این متد ارسال شود که هم یک enum واقعی باشد و هم یک Operation معتبر.

**چرا هر دو شرط لازم‌اند؟**

- اگر فقط بنویسیم `<T extends Operation>`، ممکن است یک کلاس معمولی که `Operation` را پیاده‌سازی کرده است به متد داده شود. اما در آن صورت تضمینی نداریم که `getEnumConstants()` مجموعه‌ای از ثابت‌های `enum` را برگرداند.
- اگر فقط محدودیت `Enum<T>` را داشته باشیم، تضمینی نداریم که ثابت‌های آن نوع، رفتار `Operation` را ارائه دهند.

**ج) چرا ترتیب محدودیت‌ها مهم است؟**

در `<T extends Enum<T> & Operation>`، محدودیت کلاس باید پیش از محدودیت‌های interface قرار گیرد. این قاعده نحوی جاوا است.

<a id="collection-solution"></a>
### راه‌حل دوم: دریافت `Collection<? extends Operation>`

<div dir="ltr">

```java
private static void test(
        Collection<? extends Operation> opSet,
        double x,
        double y) {

    for (Operation op : opSet) {
        System.out.printf(
            "%f %s %f = %f%n",
            x, op, y, op.apply(x, y)
        );
    }
}
```
</div>

فراخوانی:

<div dir="ltr">

```java
public static void main(String[] args) {
    double x = Double.parseDouble(args[0]);
    double y = Double.parseDouble(args[1]);

    test(
        Arrays.asList(ExtendedOperation.values()),
        x,
        y
    );
}
```
</div>

**`Collection<? extends Operation>` دقیقاً چه معنایی دارد؟**

> مجموعه‌ای از عناصر را بپذیر که نوع آن‌ها `Operation` یا یکی از زیرنوع‌های آن باشد.

هر دو نوع زیر قابل استفاده‌اند:

<div dir="ltr">

```java
Collection<BasicOperation>
Collection<ExtendedOperation>
```
</div>

این همان کاربردی است که در Item 31، با عنوان استفاده از bounded wildcards برای افزایش انعطاف‌پذیری API، بررسی می‌شود.

**مزیت این روش: ترکیب چند نوع عملیات**

<div dir="ltr">

```java
List<Operation> operations = List.of(
    BasicOperation.PLUS,
    BasicOperation.TIMES,
    ExtendedOperation.EXP
);

test(operations, 4, 2);
```
</div>

در روش مبتنی بر `Class<T>`، ورودی باید نماینده یک `enum` واحد باشد. اما در روش مبتنی بر `Collection`، می‌توانیم مجموعه‌ای ترکیبی از عملیات متعلق به انواع مختلف داشته باشیم.

<a id="comparison"></a>
### مقایسه دو راه‌حل

| معیار | `Class<T>` | `Collection<? extends Operation>` |
|--------|------------|-----------------------------------|
| پیچیدگی جنریک | بیشتر | کمتر |
| دریافت خودکار تمام ثابت‌های یک enum | بله | خیر؛ مجموعه باید ساخته شود |
| ترکیب چند نوع عملیات | به‌صورت مستقیم خیر | بله |
| حفظ هویت نوع `enum` | بله | لزوماً خیر |
| استفاده مستقیم از `EnumSet` | مناسب | محدودتر |

> **اصلاح ظریف در تفسیر جدول:** در روش `Collection`، اگر مجموعه واقعاً شامل ثابت‌های یک `enum` مشخص باشد، همچنان می‌توان آن را به‌صورت یک `EnumSet` ساخت. اما متد عمومی مبتنی بر `Collection<? extends Operation>` دیگر نوع دقیق `enum` را نمی‌داند و نمی‌تواند به‌طور عمومی از `EnumSet` یا `EnumMap` برای هر مجموعه ترکیبی استفاده کند.

**انتخاب معماری:**

- اگر هدف، اجرای تمام ثابت‌های یک نوع `enum` است، روش `Class<T>` مناسب است.
- اگر هدف، اجرای مجموعه‌ای دلخواه و قابل ترکیب از عملیات است، روش `Collection` انعطاف بیشتری دارد.

[بازگشت به بالا](#top)

---

<a id="sharing-limitation"></a>
## ۶. محدودیت مهم: امکان به‌اشتراک‌گذاری پیاده‌سازی میان `enum`ها

فرض کن `BasicOperation` و `ExtendedOperation` هر دو این کد را دارند:

<div dir="ltr">

```java
private final String symbol;

BasicOperation(String symbol) {
    this.symbol = symbol;
}

@Override
public String toString() {
    return symbol;
}
```
</div>

و همین منطق در `ExtendedOperation` نیز تکرار شده است.

علت این است که یک `enum` نمی‌تواند از `enum` دیگری ارث‌بری کند؛ بنابراین امکان به‌اشتراک‌گذاری مستقیم فیلدها، سازنده و پیاده‌سازی‌های نمونه‌ای از طریق ارث‌بری وجود ندارد.

<a id="option1"></a>
### گزینه اول: تکرار کد محدود

اگر کد مشترک بسیار کوچک باشد، مانند ذخیره و نمایش یک نماد، تکرار آن ممکن است ساده‌ترین و قابل‌نگهداری‌ترین انتخاب باشد.

> نباید صرفاً برای حذف چند خط کد، یک لایه انتزاعی غیرضروری ایجاد کنیم.

<a id="option2"></a>
### گزینه دوم: استفاده از `default` در interface

اگر منطق مشترک به وضعیت داخلی نیاز نداشته باشد، می‌توان آن را به‌صورت متد `default` در interface قرار داد:

<div dir="ltr">

```java
public interface Operation {

    double apply(double x, double y);

    default String description() {
        return "A mathematical operation";
    }
}
```
</div>

تمام پیاده‌سازی‌ها به‌طور پیش‌فرض این رفتار را دریافت می‌کنند.

> **البته:** اگر رفتار مشترک به فیلدی مانند `symbol` نیاز داشته باشد، متد `default` به‌تنهایی نمی‌تواند آن فیلد نمونه‌ای را در اختیار همه پیاده‌سازی‌ها قرار دهد.

<a id="option3"></a>
### گزینه سوم: استفاده از Helper

اگر منطق مشترک بزرگ‌تر باشد، می‌توان آن را در یک کلاس کمکی یا متد استاتیک قرار داد.

اگر اعتبارسنجی ورودی‌ها، قالب‌بندی خروجی و تبدیل داده‌ها میان چند پیاده‌سازی مشترک باشند، می‌توان این مسئولیت‌ها را از خود `enum` جدا کرد.

> **این تصمیم به حجم منطق مشترک، تعداد پیاده‌سازی‌ها و نیازهای نگهداری بستگی دارد.**

[بازگشت به بالا](#top)

---

<a id="jdk-example"></a>
## ۷. نمونه‌ای از همین الگو در کتابخانه استاندارد جاوا

کتاب به `java.nio.file.LinkOption` اشاره می‌کند.

این `enum` رابط‌های `CopyOption` و `OpenOption` را پیاده‌سازی می‌کند:

<div dir="ltr">

```java
LinkOption.NOFOLLOW_LINKS
```
</div>

یک مقدار از نوع `LinkOption` است که در APIهایی استفاده می‌شود که یک `CopyOption` یا `OpenOption` می‌پذیرند.

> **مزیت:** API می‌تواند به یک قرارداد مشترک وابسته باشد، بدون اینکه مجبور شود فقط یک نوع مشخص از گزینه‌ها را بپذیرد. این الگو در طراحی کتابخانه‌های جاوا اهمیت دارد؛ زیرا به چند نوع مستقل اجازه می‌دهد در یک API مشترک قابل استفاده باشند.

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## ۸. نگاه معماری: این الگو چگونه به طراحی سیستم‌های بزرگ کمک می‌کند؟

فرض کن در یک سامانه پردازش پرداخت، می‌خواهیم چند نوع عملیات داشته باشیم:

- محاسبه کارمزد استاندارد
- محاسبه کارمزد بین‌المللی
- محاسبه کارمزد ویژه مشتریان سازمانی
- عملیات سفارشی ارائه‌شده توسط یک ماژول دیگر

اگر همه عملیات در یک `enum` ثابت قرار گیرند، هر توسعه جدید مستلزم تغییر همان تعریف مرکزی خواهد بود.

اما اگر قرارداد مشترکی داشته باشیم:

<div dir="ltr">

```java
public interface FeeOperation {
    Money calculate(Transaction transaction);
}
```
</div>

می‌توانیم چند `enum` یا کلاس مستقل ایجاد کنیم که این قرارداد را پیاده‌سازی کنند.

### نکته معماری مهم

> **الگوی Item 38 زمانی مناسب است که اعضای مجموعه باید مقادیر ثابت و نوع‌دار باقی بمانند، اما مجموعه انواع پیاده‌سازی باید قابل توسعه باشد.**

اگر عملیات‌ها به‌صورت پویا از دیتابیس، پیکربندی یا افزونه‌های بارگذاری‌شده در زمان اجرا اضافه می‌شوند، یک `enum` توسعه‌یافته شبیه‌سازی‌شده ممکن است محدودکننده باشد. در چنین شرایطی، interface همراه با کلاس‌های معمولی و یک مکانیزم ثبت افزونه‌ها می‌تواند انتخاب مناسب‌تری باشد.

<a id="architectural-comparison"></a>
### مقایسه گزینه‌های معماری

| معیار | یک `enum` ثابت | الگوی Item 38 | Interface با کلاس‌های قابل ثبت |
|--------|----------------|---------------|-------------------------------|
| سادگی اولیه | بسیار بالا | متوسط | متوسط تا بالا |
| افزودن پیاده‌سازی جدید | نیازمند تغییر `enum` | بدون تغییر `enum` پایه | بدون تغییر قرارداد |
| مجموعه ثابت از مقادیر | عالی | عالی در هر `enum` | نیازمند طراحی جداگانه |
| ترکیب پیاده‌سازی‌ها | محدود | ممکن | ممکن |
| افزونه‌های پویا | نامناسب | محدود | مناسب‌تر |
| پیچیدگی عملیاتی | پایین | پایین تا متوسط | بسته به سازوکار ثبت، بالاتر |

<a id="production-notes"></a>
### چند ملاحظه Production-Grade

| ملاحظه | توضیح |
|--------|-------|
| **Type Safety** | پارامترهای API را تا حد امکان از نوع interface تعریف کن تا مصرف‌کنندگان به پیاده‌سازی مشخص وابسته نشوند |
| **Validation** | قرارداد `Operation` باید رفتار ورودی‌های نامعتبر را مشخص کند. برای مثال، تقسیم بر صفر در عملیات `double` معمولاً مانند تقسیم صحیح باعث `ArithmeticException` نمی‌شود؛ ممکن است `Infinity` یا `NaN` تولید شود. این رفتار باید با نیاز دامنه سازگار باشد |
| **Observability** | نام و نماد عملیات را از هم تفکیک کن. نماد `+` برای نمایش مناسب است، اما برای لاگ‌های ساختاریافته، گزارش‌گیری یا ذخیره‌سازی، یک شناسه پایدار مانند `PLUS` می‌تواند مناسب‌تر باشد |
| **Extensibility** | اگر عملیات‌ها توسط تیم‌های مستقل توسعه داده می‌شوند، باید مشخص شود چه کسی مجاز به ارائه پیاده‌سازی است، چگونه اعتبارسنجی می‌شوند و چه تضمینی برای سازگاری نسخه‌ها وجود دارد |
| **Performance** | این الگو به‌خودی‌خود تضمین نمی‌کند که اجرای عملیات سریع‌تر شود. مزیت اصلی آن توسعه‌پذیری و جداسازی قرارداد از پیاده‌سازی است. هزینه واقعی باید در سناریوی اجرایی اندازه‌گیری شود |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۹. جمع‌بندی نهایی Item 38

این Item در اصل درباره ارث‌بری از `enum` نیست؛ درباره **انتخاب مرز صحیح انتزاع در API** است.

اگر فقط یک مجموعه محدود و ثابت از عملیات داریم، یک `enum` ساده معمولاً بهترین انتخاب است.

اما اگر می‌خواهیم مصرف‌کنندگان بتوانند عملیات جدیدی ارائه دهند، بهتر است رفتار مشترک را در یک interface تعریف کنیم و یک `enum` استاندارد به‌عنوان پیاده‌سازی پیش‌فرض داشته باشیم.

### نکات کلیدی

| # | نکته |
|---|-------|
| **۱** | `enum`ها در جاوا قابل ارث‌بری نیستند؛ این محدودیت بخشی از طراحی نوعی آن‌هاست |
| **۲** | با تعریف interface می‌توان قابلیت توسعه‌پذیری را بدون ارث‌بری از `enum` به دست آورد |
| **۳** | API باید interface را بپذیرد، نه `enum` پایه را؛ در غیر این صورت، توسعه‌دهنده نمی‌تواند پیاده‌سازی جدید را جایگزین کند |
| **۴** | اگر لازم است تمام مقادیر یک `enum` پیمایش شوند، ترکیب `Class<T>` با محدودیت `<T extends Enum<T> & Operation>` امکان دسترسی نوع‌امن به ثابت‌ها را فراهم می‌کند |
| **۵** | اگر انعطاف در انتخاب و ترکیب عملیات مهم‌تر است، `Collection<? extends Operation>` گزینه مناسب‌تری است |
| **۶** | اگر منطق مشترک میان پیاده‌سازی‌ها زیاد باشد، از `default` method، کلاس کمکی یا متد استاتیک استفاده کن |

### قاعده‌ای که باید از این Item به خاطر بسپاری

> **به‌جای تلاش برای توسعه یک نوع بسته، قرارداد مشترک آن را توسعه‌پذیر کن.**

این الگو هم قدرت `enum` را برای تعریف مقادیر ثابت حفظ می‌کند و هم انعطاف‌پذیری interfaceها را برای توسعه API در اختیار برنامه‌نویس قرار می‌دهد.

---

[بازگشت به بالا](#top)

</div>