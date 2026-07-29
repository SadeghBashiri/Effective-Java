<div dir="rtl">

<a id="top"></a>

# آیتم ۱۴: پیاده‌سازی Comparable را در نظر بگیرید (Consider implementing Comparable)

برخلاف آیتم قبلی (clone) که Joshua Bloch ما را از آن می‌ترساند، در اینجا به شدت ما را تشویق می‌کند که از `Comparable` استفاده کنیم. این آیتم یکی از کاربردی‌ترین آیتم‌های کتاب است و تقریباً تمام کلاس‌های Value Object در پروژه‌های واقعی از آن بهره می‌برند.

---

## فهرست مطالب

- [Comparable چیست و چرا با equals متفاوت است؟](#what-is-comparable)
- [۱. قدرت "ترتیب طبیعی" (Natural Ordering)](#natural-ordering)
- [۲. قدرت کم در برابر سود زیاد (Power for Effort)](#power-for-effort)
- [۳. ساختار متد compareTo](#structure)
- [قانون اول: Anti-Symmetry (تقارن معکوس)](#rule1)
- [قانون دوم: Transitivity (تعدی‌پذیری)](#rule2)
- [قانون سوم: Consistency with other comparisons](#rule3)
- [سازگاری با equals (Consistency with equals)](#consistency-with-equals)
- [مشکل ارث‌بری و فیلدهای جدید](#inheritance-problem)
- [تفاوت‌های فنی با equals](#technical-differences)
- [چرا جاوا این الزام را نکرده است؟](#why-not-required)
- [مسئولیت توسعه‌دهنده دقیقاً چیست؟](#developer-responsibility)
- [چه زمانی نقض قابل قبول است؟](#when-violation-acceptable)
- [Ruleهای طلایی تصمیم‌گیری](#golden-rules)
- [استانداردهای طلایی پیاده‌سازی compareTo](#implementation-standards)
  - [۱. اولویت‌بندی فیلدها (Significance Order)](#significance-order)
  - [۲. خداحافظی با اپراتورهای < و >](#no-relational-operators)
  - [۳. روش مدرن: Comparator Construction Methods](#modern-comparator)
  - [۴. تله‌ی تفریق (The Difference-Based Trap)](#subtraction-trap)
  - [۵. کمک به استنتاج تایپ (Type Inference)](#type-inference)
- [Production-Grade Example](#production-example)
- [جمع‌بندی نهایی آیتم ۱۴](#final-summary)

[بازگشت به بالا](#top)

---

<a id="what-is-comparable"></a>
## Comparable چیست و چرا با equals متفاوت است؟

Bloch با یک نکته‌ی مهم شروع می‌کند:

> `compareTo` برخلاف `equals` در `Object` تعریف نشده است.

### پیام طراحی

| متد | هدف |
|-----|-----|
| `equals` | هویت/برابری منطقی (Logical Equality) |
| `compareTo` | ترتیب طبیعی (Natural Ordering) |

این یعنی:

- `equals` فقط جواب بله/خیر می‌دهد
- `compareTo` مکان یک شیء را نسبت به شیء دیگر مشخص می‌کند

[بازگشت به بالا](#top)

---

<a id="natural-ordering"></a>
## ۱. قدرت "ترتیب طبیعی" (Natural Ordering)

Bloch اشاره می‌کند که `compareTo` در کلاس `Object` نیست، بلکه تنها متد اینترفیس `Comparable` است.

- **تفاوت با equals:** متد `equals` فقط می‌گوید دو شیء برابر هستند یا نه. اما `compareTo` یک قدم فراتر می‌رود و می‌گوید کدام یک «بزرگتر» یا «کوچکتر» است.

- **مزیت بزرگ:** وقتی کلاسی `Comparable` می‌شود، بلافاصله با تمام الگوریتم‌های آماده جاوا (مثل مرتب‌سازی، جستجوی دودویی و مجموعه‌های مرتب) سازگار می‌شود. یک `TreeSet` بدون هیچ کد اضافه‌ای، کلمات را هم مرتب می‌کند و هم تکراری‌هایشان را حذف می‌کند.

[بازگشت به بالا](#top)

---

<a id="power-for-effort"></a>
## ۲. قدرت کم در برابر سود زیاد (Power for Effort)

جمله طلایی Bloch این است:

> «شما با مقدار کمی تلاش، قدرت عظیمی به دست می‌آورید.»

تقریباً تمام کلاس‌های مقداری (Value Classes) در جاوا مثل `String`، `Integer`، `Date` و همچنین `Enum`ها این اینترفیس را پیاده‌سازی کرده‌اند.

پیام Bloch روشن است: اگر کلاس شما نشان‌دهنده داده‌ای است که ترتیب در آن معنا دارد (مثل الفبا، اعداد یا زمان)، **حتماً باید Comparable باشد.**

[بازگشت به بالا](#top)

---

<a id="structure"></a>
## ۳. ساختار متد compareTo

<div dir="ltr">

```java
public interface Comparable<T> {
    int compareTo(T t);
}
```
</div>

- این متد **Generic** است. یعنی برخلاف `equals` که ورودی آن `Object` بود و نیاز به `instanceof` و cast داشت، در اینجا نوعِ ورودی دقیقاً همان کلاسی است که دارید با آن مقایسه می‌کنید. این یعنی **امنیت در زمان کامپایل (Compile-time safety)**.

### خروجی‌های متد compareTo (یک قرارداد نانوشته)

طبق استاندارد جاوا، این متد باید یک عدد صحیح برگرداند:

| مقدار بازگشتی | معنی |
|---------------|-------|
| عدد منفی | شیء فعلی کوچک‌تر از ورودی است |
| عدد صفر | هر دو برابر هستند |
| عدد مثبت | شیء فعلی بزرگ‌تر از ورودی است |

> **نکته تحلیلی:** Bloch در اینجا اشاره می‌کند که `Comparable` اجازه می‌دهد کلاس شما با "Generic Algorithms" همکاری کند. این یکی از ستون‌های اصلی **Reusable Code** (کد با قابلیت استفاده مجدد) در جاواست. شما یک بار قانون مقایسه را می‌نویسید و `Collections.sort()` برای همیشه می‌داند چطور با اشیاء شما رفتار کند.

[بازگشت به بالا](#top)

---

<a id="rule1"></a>
## قانون اول: Anti-Symmetry (تقارن معکوس)

### 📜 قرارداد
<div dir="ltr">

```
sgn(x.compareTo(y)) == -sgn(y.compareTo(x))
```
</div>
### 🔍 تفسیر ساده

اگر:

- `x < y` ⇒ `y > x`
- `x == y` ⇒ `y == x`

### ❌ Bad Practice (نقض تقارن)

<div dir="ltr">

```java
class BadVersion implements Comparable<BadVersion> {
    int v;

    @Override
    public int compareTo(BadVersion o) {
        return this.v > o.v ? 1 : -1; // صفر را در نظر نگرفته!
    }
}
```
</div>

### 📌 مشکل

<div dir="ltr">

```java
BadVersion a = new BadVersion(5);
BadVersion b = new BadVersion(5);

a.compareTo(b) == -1  // ❌
b.compareTo(a) == -1  // ❌
```
</div>

- `a` می‌گوید من کوچک‌ترم
- `b` هم می‌گوید من کوچک‌ترم

پس کدام کوچک‌تر است؟ `TreeSet` باید کدام را جلوتر بگذارد؟

### نتیجه

- رفتار غیرقابل‌پیش‌بینی
- حذف نشدن duplicate
- loop در sort
- یا حتی `IllegalArgumentException` در TimSort

### ✅ Better

<div dir="ltr">

```java
@Override
public int compareTo(Version o) {
    return Integer.compare(this.v, o.v);
}
```
</div>

### چرا این نقض قرارداد خیلی خطرناک است؟

چون:

- `TreeSet`
- `TreeMap`
- `Arrays.sort`
- `Collections.sort`

همه فرض می‌کنند `compareTo` یک ترتیب منطقی و پایدار می‌سازد. وقتی این فرض شکسته شود، الگوریتم‌ها گیج می‌شوند و رفتار تصادفی می‌شود.

[بازگشت به بالا](#top)

---

<a id="rule2"></a>
## قانون دوم: Transitivity (تعدی‌پذیری)

### 📜 قرارداد
<div dir="ltr">

```
(x > y && y > z) ⇒ x > z
```
</div>
### ❌ Bad Practice — ordering وابسته به شرط

<div dir="ltr">

```java
class BadUser implements Comparable<BadUser> {
    int age;
    boolean vip;

    @Override
    public int compareTo(BadUser o) {
        if (this.vip && !o.vip) return 1;
        if (!this.vip && o.vip) return -1;
        return Integer.compare(this.age, o.age);
    }
}
```
</div>

### 📌 سناریوی شکست

تعریف سه کاربر:

| کاربر | vip | age |
|-------|-----|-----|
| A | true | 30 |
| B | false | 40 |
| C | true | 20 |

مقایسه‌ها:

- **A vs B:** A vip است، B نیست ➡️ `A > B`
- **B vs C:** B vip نیست، C vip است ➡️ `B < C`
- **A vs C:** هر دو vip هستند ➡️ مقایسه بر اساس سن: 30 > 20 ➡️ `A > C`

📌 اینجا ترتیب به شکل زیر می‌شود:
<div dir="ltr">

```
C < A > B
```
</div>
آیا این یک خط صاف است؟ ❌ نه، یک ساختار شاخه‌ای و ناپایدار است. الگوریتم‌های sort خط صاف می‌خواهند.

### ✅ Better — ترتیب پایدار و خطی

<div dir="ltr">

```java
@Override
public int compareTo(User o) {
    int r = Boolean.compare(this.vip, o.vip);
    if (r != 0) return r;
    return Integer.compare(this.age, o.age);
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="rule3"></a>
## قانون سوم: Consistency with other comparisons

### 📜 قرارداد
<div dir="ltr">

```
x.compareTo(y) == 0 ⇒ sgn(x.compareTo(z)) == sgn(y.compareTo(z))
```
</div>
### 🔍 معنی عملی

اگر `x` و `y` از نظر ordering برابرند، نباید در مقایسه با یک `z` نتایج متفاوت بدهند.

### ❌ Bad Practice

<div dir="ltr">

```java
class Point implements Comparable<Point> {
    int x;
    int y;

    @Override
    public int compareTo(Point o) {
        return Integer.compare(this.x, o.x);  // ❌ فقط x را مقایسه می‌کند
    }
}
```
</div>

### 📌 مشکل

<div dir="ltr">

```java
Point p1 = new Point(1, 100);
Point p2 = new Point(1, 0);

p1.compareTo(p2) == 0  // چون x برابر است
```
</div>

اما:

<div dir="ltr">

```java
p1.compareTo(z) != p2.compareTo(z)  // y متفاوت است
```
</div>

### فاجعه در TreeSet

<div dir="ltr">

```java
TreeSet<Point> set = new TreeSet<>();
set.add(new Point(1, 100));
set.add(new Point(1, 0));

System.out.println(set.size()); // 1 ❌
```
</div>

چرا؟ چون `TreeSet` می‌گوید: `compareTo == 0` ⇒ duplicate. ولی این دو نقطه واقعاً متفاوت‌اند.

### ✅ Better

<div dir="ltr">

```java
@Override
public int compareTo(Point o) {
    int r = Integer.compare(this.x, o.x);
    if (r != 0) return r;
    return Integer.compare(this.y, o.y);
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="consistency-with-equals"></a>
## سازگاری با equals (Consistency with equals)

این یکی از مهم‌ترین توصیه‌های Bloch است. اگرچه اجباری نیست، اما شدیداً توصیه می‌شود که اگر `x.equals(y)` درست است، `x.compareTo(y)` هم حتماً صفر شود.

### چرا این موضوع حیاتی است؟

اینترفیس‌هایی مثل `Set` بر اساس `equals` تعریف شده‌اند، اما کلاس‌هایی مثل `TreeSet` برای تشخیص تکراری بودن، به جای `equals` از `compareTo` استفاده می‌کنند.

### مثال خیره‌کننده Bloch: `BigDecimal`

<div dir="ltr">

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");
```
</div>

| متد | نتیجه |
|-----|--------|
| `a.equals(b)` | `false` |
| `a.compareTo(b)` | `0` |

### نتیجه در Collectionها

<div dir="ltr">

```java
Set<BigDecimal> hash = new HashSet<>();
hash.add(a);
hash.add(b);
System.out.println(hash.size()); // 2

Set<BigDecimal> tree = new TreeSet<>();
tree.add(a);
tree.add(b);
System.out.println(tree.size()); // 1
```
</div>

📌 دو `Set`، دو نتیجه‌ی متفاوت!

### چرا این وضعیت خطرناک است؟

1. **نقض اصل Least Surprise:** کاربر انتظار دارد "Set یعنی عناصر یکتا"، ولی این یکتایی در `HashSet` یک چیز است و در `TreeSet` چیز دیگر.

2. **API غیرقابل‌پیش‌بینی:** فرض کن متدی داری:
<div dir="ltr">

```java
   Set<User> getUsers();
```
</div>
   و نمی‌دانی داخلش `HashSet` است یا `TreeSet`. رفتار برنامه بسته به پیاده‌سازی داخلی تغییر می‌کند.

3. **باگ‌های محیط Production:** این باگ‌ها تست واحد را رد می‌کنند، لاگ ندارند، فقط در بعضی مسیرها فعال می‌شوند و Debug آن‌ها بسیار سخت است.

[بازگشت به بالا](#top)

---

<a id="why-not-required"></a>
## چرا جاوا این الزام را نکرده است؟

چون مواردی وجود دارد که ترتیب طبیعی ≠ هویت منطقی.

### مثال مشروع: BigDecimal

ترتیب عددی مهم‌تر از مقیاس (scale) است.

📌 بنابراین جاوا انعطاف را حفظ کرده، اما مسئولیت را به توسعه‌دهنده داده است.

### "ترتیب طبیعی ≠ هویت منطقی" یعنی چه؟

| مفهوم | معنی |
|--------|-------|
| ترتیب طبیعی (Natural Ordering) | روشی برای مرتب‌سازی اشیاء |
| هویت منطقی (Logical Identity) | معیاری برای اینکه بگوییم دو شیء «یکی هستند یا نه» |

در بسیاری از کلاس‌ها این دو یکی هستند، اما نه همیشه.

### چرا انتخاب جاوا درست است؟

اگر `equals` فقط عدد را می‌دید:
- دقت محاسباتی از بین می‌رفت
- نمایش عددی خراب می‌شد
- کاربردهای مالی دچار مشکل می‌شدند

اگر `compareTo` scale را هم لحاظ می‌کرد:
- مرتب‌سازی عددی معنا نداشت: `1.0 < 1.00` ؟!

📌 پس:
- `equals` باید دقیق باشد
- `compareTo` باید «ریاضی‌محور» باشد

[بازگشت به بالا](#top)

---

<a id="developer-responsibility"></a>
## مسئولیت توسعه‌دهنده دقیقاً چیست؟

### ۱. آگاهی

بدانی:
- کدام Collection از `compareTo` استفاده می‌کند
- کدام از `equals`

### ۲. مستندسازی (خیلی مهم)

اگر نقض وجود دارد:
<div dir="ltr">

```java
/**
 * Note: This class has a natural ordering that is inconsistent with equals.
 */
```
</div>
### ۳. طراحی آگاهانه API

❌ برگرداندن `Set` بدون دانستن نوع آن
✅ مشخص کردن رفتار یا استفاده از `Comparator` صریح

[بازگشت به بالا](#top)

---

<a id="when-violation-acceptable"></a>
## چه زمانی نقض قابل قبول است؟

| شرایط | مجاز؟ |
|--------|-------|
| کلاس Value Object عمومی | ❌ |
| کلاس با مصرف خاص | ⚠️ فقط با مستندات |
| مشابه BigDecimal | ✅ با آگاهی کامل |

📌 اگر نقض می‌کنی، باید صراحتاً بگویی: "Note: This class has a natural ordering that is inconsistent with equals."

### مثال واقعی بد (Bug مخفی)
<div dir="ltr">

```java
class User implements Comparable<User> {
    String nationalId;  // معیار equals
    int age;            // معیار compareTo

    @Override
    public boolean equals(Object o) {
        return o instanceof User u &&
               this.nationalId.equals(u.nationalId);
    }

    @Override
    public int compareTo(User o) {
        return Integer.compare(this.age, o.age);  // ❌ ناهماهنگ
    }
}
```
</div>
نتیجه:
- دو کاربر با age برابر → در `TreeSet` یکی حذف می‌شود
- ولی در `HashSet` هر دو باقی می‌مانند
  🔥 فاجعه‌ی منطقی

### نسخه‌ی صحیح (هماهنگ)
<div dir="ltr">

```java
@Override
public int compareTo(User o) {
    int r = this.nationalId.compareTo(o.nationalId);
    if (r != 0) return r;
    return Integer.compare(this.age, o.age);
}
```
</div>
📌 حالا: `compareTo == 0 ⇔ equals == true`

[بازگشت به بالا](#top)

---

<a id="golden-rules"></a>
## Ruleهای طلایی تصمیم‌گیری

### ✅ Rule 1

اگر کلاس Comparable است، اول همان فیلدی را مقایسه کن که `equals` بر اساس آن است.

### ✅ Rule 2

اگر چند معیار داری، معیار `equals` باید اولین معیار `compareTo` باشد.

### ✅ Rule 3

اگر نمی‌توانی این هماهنگی را حفظ کنی، از `Comparable` استفاده نکن، فقط `Comparator` بده.

[بازگشت به بالا](#top)

---

<a id="implementation-standards"></a>
## استانداردهای طلایی پیاده‌سازی compareTo

Bloch در اینجا استانداردهای طلایی پیاده‌سازی `compareTo` را معرفی می‌کند.

<a id="significance-order"></a>
### ۱. اولویت‌بندی فیلدها (Significance Order)

Bloch تأکید می‌کند که اگر کلاسی چندین فیلد دارد (مثل شماره تلفن که کد شهر، پیش‌شماره و شماره خط دارد)، باید از مهم‌ترین فیلد شروع کنید:

- اگر مقایسه فیلد اول صفر نشد، بلافاصله نتیجه را برگردانید.
- فقط اگر فیلد اول برابر بود، به سراغ فیلد دوم بروید.

این دقیقاً مثل ترتیب کلمات در لغت‌نامه است (Lexicographical Order).

<a id="no-relational-operators"></a>
### ۲. خداحافظی با اپراتورهای < و >

این یک تغییر مهم نسبت به ویرایش‌های قبلی کتاب است.

- **توصیه قدیمی:** استفاده از `if (x < y) return -1;`
- **توصیه جدید:** استفاده از متد استاتیک `compare` در کلاس‌های Wrapper (مثل `Integer.compare` یا `Double.compare`)

چرا؟ چون استفاده از اپراتورهای رابطه‌ای برای اعداد اعشاری (`Float`/`Double`) یا در محاسبات پیچیده، مستعد خطا و بسیار پرحجم (Verbose) است.

<a id="modern-comparator"></a>
### ۳. روش مدرن: Comparator Construction Methods

در جاوا ۸، اینترفیس `Comparator` مجهز به متدهایی شد که اجازه می‌دهد مقایسه‌گر را به صورت زنجیره‌ای (Fluent) بسازید.
<div dir="ltr">

```java
private static final Comparator<PhoneNumber> COMPARATOR =
    comparingInt((PhoneNumber pn) -> pn.areaCode)
    .thenComparingInt(pn -> pn.prefix)
    .thenComparingInt(pn -> pn.lineNum);
```
</div>
- **مزایا:** فوق‌العاده خوانا و تمیز است. احتمال اشتباه در منطق "اگر صفر بود بعدی را چک کن" را به صفر می‌رساند.
- **هزینه:** Bloch صادقانه می‌گوید که این روش حدود ۱۰٪ کندتر از روش دستی است (به دلیل ساخت اشیاء موقتی و فراخوانی متدها). اما برای اکثر برنامه‌ها، خوانایی کد بر این افت ناچیز عملکرد ترجیح دارد.

<a id="subtraction-trap"></a>
### ۴. تله‌ی تفریق (The Difference-Based Trap)

Bloch به شدت نسبت به یک ترفند قدیمی هشدار می‌دهد. بعضی برنامه‌نویسان برای کوتاهی کد این کار را می‌کنند:
<div dir="ltr">

```java
return o1.hashCode() - o2.hashCode();  // ❌ فاجعه!
```
</div>
#### چرا این کد خراب است؟

1. **Integer Overflow:** اگر عدد اول خیلی بزرگ و مثبت و عدد دوم خیلی بزرگ و منفی باشد، تفاضل آن‌ها از ظرفیت `int` خارج شده و عدد منفی می‌شود (در حالی که باید مثبت می‌بود).

2. **اعداد اعشاری:** در IEEE 754 (اعداد اعشاری)، این کار باعث از دست رفتن دقت و نتایج عجیب می‌شود.

**راه حل:** همیشه از `Integer.compare` یا روش‌های مشابه استفاده کنید.

<a id="type-inference"></a>
### ۵. کمک به استنتاج تایپ (Type Inference)

در مثال `comparingInt`، Bloch اشاره می‌کند که گاهی باید نوع ورودی لامبدا را صریحاً بنویسیم: `(PhoneNumber pn) -> ...`. این به این دلیل است که جاوا در زنجیره‌های پیچیده گاهی نمی‌تواند به تنهایی تشخیص دهد که ورودی چیست.

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## Production-Grade Example
<div dir="ltr">

```java
public final class PhoneNumber implements Comparable<PhoneNumber> {

    private final short areaCode;
    private final short prefix;
    private final short lineNum;

    public PhoneNumber(short areaCode, short prefix, short lineNum) {
        this.areaCode = areaCode;
        this.prefix = prefix;
        this.lineNum = lineNum;
    }

    // استفاده از Comparator مدرن (جاوا ۸+)
    private static final Comparator<PhoneNumber> COMPARATOR =
            Comparator.comparingInt((PhoneNumber pn) -> pn.areaCode)
                    .thenComparingInt(pn -> pn.prefix)
                    .thenComparingInt(pn -> pn.lineNum);

    // روش دستی (برای مسیرهای حساس به عملکرد)
    private static final Comparator<PhoneNumber> COMPARATOR_MANUAL =
            (p1, p2) -> {
                int r = Integer.compare(p1.areaCode, p2.areaCode);
                if (r != 0) return r;
                r = Integer.compare(p1.prefix, p2.prefix);
                if (r != 0) return r;
                return Integer.compare(p1.lineNum, p2.lineNum);
            };

    @Override
    public int compareTo(PhoneNumber o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PhoneNumber other)) return false;

        return areaCode == other.areaCode
                && prefix == other.prefix
                && lineNum == other.lineNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaCode, prefix, lineNum);
    }

    @Override
    public String toString() {
        return String.format("%03d-%03d-%04d",
                areaCode, prefix, lineNum);
    }
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی آیتم ۱۴

### ۵ قانون طلایی

| قانون | توضیح |
|-------|-------|
| **۱. Comparable را پیاده کنید** | برای هر کلاس Value Object که ترتیب در آن معنا دارد |
| **۲. از متدهای استاتیک استفاده کنید** | به جای `<` و `>` از `Double.compare` و غیره استفاده کنید |
| **۳. قدرت جاوا ۸** | برای کدهای تمیزتر، از زنجیره `comparing` و `thenComparing` استفاده کنید |
| **۴. هرگز تفریق نکنید** | از تفریق دو فیلد برای مقایسه خودداری کنید |
| **۵. با equals هماهنگ باشید** | مگر اینکه دلیل موجهی برای نقض داشته باشید |

### جدول جمع‌بندی قراردادها

| قانون | توضیح | عواقب نقض |
|-------|-------|-----------|
| **Anti-Symmetry** | `sgn(x.compareTo(y)) == -sgn(y.compareTo(x))` | رفتار تصادفی در sort |
| **Transitivity** | `(x > y && y > z) ⇒ x > z` | ساختار شاخه‌ای، sort خراب |
| **Consistency** | اگر `x.compareTo(y) == 0`، مقایسه با z باید یکسان باشد | `TreeSet` رفتار غیرمنتظره |
| **Consistency with equals** | `x.compareTo(y) == 0` ⇔ `x.equals(y)` (توصیه قوی) | دو تعریف متفاوت از برابری |

### نکات نهایی Production

1. **همیشه از `Comparable` برای Value Objectها استفاده کنید** اگر ترتیب معنا دارد.
2. **از `Comparator`های مدرن جاوا ۸ استفاده کنید** مگر در مسیرهای فوق‌حساس به عملکرد.
3. **هرگز از تفریق برای مقایسه استفاده نکنید** - خطر Overflow و از دست رفتن دقت.
4. **سازگاری با `equals` را تا حد امکان حفظ کنید** و در صورت نقض، مستند کنید.
5. **در صورت استفاده از `TreeSet` یا `TreeMap`، مطمئن شوید `compareTo` با `equals` هماهنگ است**، در غیر این صورت رفتار مجموعه‌ها غیرقابل‌پیش‌بینی خواهد بود.

---

[بازگشت به بالا](#top)

</div>
```