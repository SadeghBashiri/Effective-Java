<div dir="rtl">

<a id="top"></a>

# آیتم ۲۶: از Raw Typeها استفاده نکنید

## (Don't Use Raw Types)

Item 26 یکی از مهم‌ترین بخش‌های فصل **Generics** در کتاب **Effective Java** است. Joshua Bloch در این آیتم تنها یک توصیه‌ی نحوی ارائه نمی‌کند، بلکه یکی از اصول بنیادی **Type System** در جاوا را توضیح می‌دهد.

پیام اصلی این آیتم بسیار ساده است:

> **هرگز از Generic Type بدون تعیین Type Parameter استفاده نکنید.**

بنابراین در کدهای مدرن جاوا، استفاده از نوع خام (Raw Type) باید به دو استثنای مشخص محدود شود و در سایر موارد همواره از Genericهای مناسب استفاده شود.

به عنوان مثال:

❌ نادرست

<div dir="ltr">

```java
List list;
Set set;
Map map;
```
</div>

✅ صحیح

<div dir="ltr">

```java
List<String> names;
Set<User> users;
Map<String, User> userMap;
```
</div>

---

## فهرست مطالب

- [هدف اصلی این آیتم](#core-goal)
- [مفاهیم پایه](#basic-concepts)
- [Raw Type چیست؟](#what-is-raw)
- [چرا Raw Type هنوز در Java وجود دارد؟](#why-raw-exists)
- [مشکل اصلی Raw Type](#main-problem)
- [نسخه Generic](#generic-version)
- [یکی از مهم‌ترین اصول Effective Java](#key-principle)
- [تفاوت List و List\<Object> و List\<?>](#difference)
- [تفاوت Set و Set\<?>](#set-difference)
- [Type Erasure](#type-erasure)
- [مثال معروف unsafeAdd](#unsafeadd)
- [استثناهای مجاز استفاده از Raw Type](#exceptions)
- [بهترین شیوه‌ها (Best Practices)](#best-practices)
- [Anti-Patternها](#anti-patterns)
- [ارتباط با سایر Itemها](#connection)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-goal"></a>
## هدف اصلی این آیتم

پیام واقعی Joshua Bloch چیزی فراتر از «از Raw Type استفاده نکن» است.

او در واقع می‌گوید:

> **کامپایلر را به همکار خود تبدیل کنید، نه اینکه آن را دور بزنید.**

Generics بزرگ‌ترین مکانیزمی هستند که Java برای **Type Safety** در اختیار برنامه‌نویس قرار داده است.

هر بار که از Raw Type استفاده می‌کنید، عملاً این قابلیت را غیرفعال می‌کنید و مسئولیت تشخیص خطا را از کامپایلر به زمان اجرای برنامه منتقل می‌کنید.

[بازگشت به بالا](#top)

---

<a id="basic-concepts"></a>
## مفاهیم پایه

قبل از ورود به بحث، باید چند اصطلاح مهم را بشناسیم.

| اصطلاح | مثال | توضیح |
|--------|------|-------|
| Generic Type | `List<E>` | کلاس یا اینترفیس دارای پارامتر نوع |
| Type Parameter | `E` | پارامتر نوع در تعریف Generic |
| Actual Type Parameter | `String` | نوع واقعی که جایگزین پارامتر شده است |
| Parameterized Type | `List<String>` | نسخه تخصصی‌شده Generic |
| Raw Type | `List` | Generic بدون پارامتر نوع |

به عنوان مثال:

<div dir="ltr">

```java
List<E>
```
</div>

یک **Generic Type** است.

وقتی می‌نویسیم:

<div dir="ltr">

```java
List<String>
```
</div>

در واقع یک **Parameterized Type** ساخته‌ایم.

اما اگر بنویسیم:

<div dir="ltr">

```java
List
```
</div>

تمام اطلاعات Generic حذف شده و با یک **Raw Type** مواجه هستیم.

[بازگشت به بالا](#top)

---

<a id="what-is-raw"></a>
## Raw Type چیست؟

Raw Type یعنی استفاده از Generic بدون تعیین نوع عناصر.

مثلاً:

<div dir="ltr">

```java
List
```
</div>

به جای

<div dir="ltr">

```java
List<String>
```
</div>

در این حالت، کامپایلر دیگر اطلاعی از نوع عناصر ندارد و همه چیز را به صورت `Object` در نظر می‌گیرد.

[بازگشت به بالا](#top)

---

<a id="why-raw-exists"></a>
## چرا Raw Type هنوز در Java وجود دارد؟

اگر Raw Type خطرناک است، چرا Java اجازه استفاده از آن را می‌دهد؟

پاسخ تنها یک کلمه است: **Backward Compatibility**

زمانی که Java 5 معرفی شد، میلیون‌ها خط کد بدون Generics وجود داشت.

نمونه‌هایی مانند:

<div dir="ltr">

```java
Vector
Hashtable
ArrayList
```
</div>

همگی بدون Generic نوشته شده بودند. اگر Java استفاده از Raw Type را ممنوع می‌کرد، تقریباً تمام نرم‌افزارهای موجود از کار می‌افتادند.

بنابراین طراحان زبان تصمیم گرفتند:

- کدهای قدیمی همچنان معتبر باقی بمانند
- کدهای جدید بتوانند از Generics استفاده کنند
- هر دو نسل از کد با یکدیگر سازگار باشند

این تصمیم به مفهوم مهم **Migration Compatibility** منجر شد و در نهایت باعث شد Generics بر پایه **Type Erasure** پیاده‌سازی شوند.

[بازگشت به بالا](#top)

---

<a id="main-problem"></a>
## مشکل اصلی Raw Type

فرض کنید مجموعه‌ای از تمبرها داریم:

<div dir="ltr">

```java
private final Collection stamps = new ArrayList();
```
</div>

برنامه‌نویس می‌داند این Collection فقط باید شامل `Stamp` باشد، اما کامپایلر هیچ اطلاعی از این موضوع ندارد.

بنابراین هر دو دستور زیر مجاز هستند:

<div dir="ltr">

```java
stamps.add(new Stamp());
stamps.add(new Coin());
```
</div>

خطا زمانی آشکار می‌شود که عناصر را بازیابی کنیم:

<div dir="ltr">

```java
Iterator i = stamps.iterator();

while (i.hasNext()) {
    Stamp stamp = (Stamp) i.next();
}
```
</div>

اگر عنصر `Coin` داخل مجموعه باشد، هنگام Cast شدن به `Stamp` یک `ClassCastException` رخ می‌دهد.

نکته مهم این است که:

- **اشتباه هنگام درج (Insert) رخ داده است.**
- **اما خطا هنگام خواندن (Read) ظاهر می‌شود.**

این دقیقاً همان مشکلی است که Bloch سعی دارد از آن جلوگیری کند.

[بازگشت به بالا](#top)

---

<a id="generic-version"></a>
## نسخه Generic

کافی است نوع مجموعه را مشخص کنیم:

<div dir="ltr">

```java
private final Collection<Stamp> stamps = new ArrayList<>();
```
</div>

اکنون:

<div dir="ltr">

```java
stamps.add(new Coin());
```
</div>

اصلاً کامپایل نمی‌شود.

کامپایلر همان لحظه اعلام می‌کند:

```
Coin cannot be converted to Stamp
```

این دقیقاً فلسفه اصلی Generics است:

> **Move Errors from Runtime to Compile Time**

[بازگشت به بالا](#top)

---

<a id="key-principle"></a>
## یکی از مهم‌ترین اصول Effective Java

Joshua Bloch بارها در کتاب تأکید می‌کند:

> **هرچه زودتر خطا کشف شود، بهتر است.**

ترتیب مطلوب کشف خطا:
<div dir="ltr">

```
Compile Time → Unit Test → Integration Test → Production
```
</div>
Raw Type این زنجیره را می‌شکند و خطا را مستقیماً به Runtime منتقل می‌کند.

[بازگشت به بالا](#top)

---

<a id="difference"></a>
## تفاوت List و List\<Object> و List\<?>

این سه نوع، مفاهیم کاملاً متفاوتی دارند.

### ۱. Raw Type

<div dir="ltr">

```java
List
```
</div>

یعنی: «سیستم Generics را کنار گذاشته‌ام.» این نوع ناامن است.

### ۲. List\<Object>

<div dir="ltr">

```java
List<Object>
```
</div>

یعنی: «این لیست می‌تواند هر نوع شیئی را نگهداری کند، اما همچنان قوانین Generics برقرار هستند.»

مثلاً:

<div dir="ltr">

```java
List<Object> list = new ArrayList<>();

list.add("Ali");
list.add(10);
list.add(new User());
```
</div>

این کاملاً ایمن است.

اما:

<div dir="ltr">

```java
List<String> strings = new ArrayList<>();

List<Object> objects = strings;
```
</div>

غیرمجاز است، زیرا Generics در جاوا **Invariant** هستند.

### ۳. List\<?>

<div dir="ltr">

```java
List<?>
```
</div>

به معنای: «لیستی از یک نوع ناشناخته.»

در این حالت:

- می‌توان عناصر را خواند
- اما (به جز `null`) نمی‌توان چیزی به آن اضافه کرد

این ویژگی باعث حفظ **Type Invariant** مجموعه می‌شود.

[بازگشت به بالا](#top)

---

<a id="set-difference"></a>
## تفاوت Set و Set\<?>

فرض کنید نوع عناصر برای شما اهمیتی ندارد.

**اشتباه:**

<div dir="ltr">

```java
Set set;
```
</div>

**درست:**

<div dir="ltr">

```java
Set<?> set;
```
</div>

چرا؟

در Raw Type:

<div dir="ltr">

```java
set.add("Ali");
set.add(15);
set.add(new User());
```
</div>

همه چیز مجاز است.

اما در `Set<?>` کامپایلر می‌گوید: "من نوع واقعی را نمی‌دانم، پس اجازه اضافه کردن هیچ عنصری را نمی‌دهم (به جز `null`)."

این همان چیزی است که امنیت نوع مجموعه را حفظ می‌کند.

[بازگشت به بالا](#top)

---

<a id="type-erasure"></a>
## Type Erasure

یکی از مهم‌ترین مفاهیم پشت Generics، **Type Erasure** است.

در زمان کامپایل:

<div dir="ltr">

```java
List<String>
```
</div>

وجود دارد.

اما در زمان اجرا:

<div dir="ltr">

```java
List
```
</div>

اطلاعات Generic حذف شده‌اند.

به همین دلیل JVM نمی‌تواند تشخیص دهد که یک شیء از نوع `List<String>` است یا `List<Integer>`، زیرا هر دو در Runtime تنها یک `List` هستند.

[بازگشت به بالا](#top)

---

<a id="unsafeadd"></a>
## مثال معروف unsafeAdd

کتاب مثال زیر را ارائه می‌دهد:

<div dir="ltr">

```java
List<String> strings = new ArrayList<>();

unsafeAdd(strings, 42);

String s = strings.get(0);
```
</div>

و متد:

<div dir="ltr">

```java
static void unsafeAdd(List list, Object o) {
    list.add(o);
}
```
</div>

چه اتفاقی می‌افتد؟

1. لیست از نوع `List<String>` ساخته می‌شود.
2. متد `unsafeAdd` به دلیل استفاده از Raw Type اطلاعات Generic را نادیده می‌گیرد.
3. عدد `42` وارد لیست می‌شود.
4. هنگام اجرای `strings.get(0)`، کامپایلر Cast پنهان به `String` تولید می‌کند.
5. در Runtime، چون عنصر واقعاً `Integer` است، `ClassCastException` رخ می‌دهد.

[بازگشت به بالا](#top)

---

<a id="exceptions"></a>
## استثناهای مجاز استفاده از Raw Type

Joshua Bloch فقط دو استثنا را مجاز می‌داند.

### ۱. Class Literal

**درست:**

<div dir="ltr">

```java
List.class
String[].class
int.class
```
</div>

**نادرست:**

<div dir="ltr">

```java
List<String>.class
List<?>.class
```
</div>

زیرا بعد از Type Erasure چیزی به نام `List<String>` در Runtime وجود ندارد.

### ۲. instanceof

**درست:**

<div dir="ltr">

```java
if (obj instanceof List) {
    List<?> list = (List<?>) obj;
}
```
</div>

اما:

<div dir="ltr">

```java
obj instanceof List<String>
```
</div>

غیرقانونی است، زیرا JVM اطلاعات پارامتر نوع را در زمان اجرا در اختیار ندارد.

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## بهترین شیوه‌ها (Best Practices)

| وضعیت | انتخاب مناسب |
|--------|--------------|
| نوع دقیق مشخص است | `List<User>` |
| هر نوع شیء مجاز است | `List<Object>` |
| نوع مهم نیست و فقط خواندن انجام می‌شود | `List<?>` |
| سازگاری با کدهای قدیمی یا `instanceof` و `Class Literal` | استفاده محدود از Raw Type |
| توسعه کد جدید | هرگز از Raw Type استفاده نکنید |

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternها

❌ استفاده از Raw Type:

<div dir="ltr">

```java
List list = new ArrayList();
```
</div>

❌ Castهای متعدد:

<div dir="ltr">

```java
User user = (User) list.get(0);
```
</div>

❌ استفاده از `@SuppressWarnings("unchecked")` بدون اثبات Type Safety.

[بازگشت به بالا](#top)

---

<a id="connection"></a>
## ارتباط با سایر Itemها

```
Item 26 → Raw Types
    ↓
Item 27 → Unchecked Warnings
    ↓
Item 28 → Type Erasure و تفاوت Array و Generic
    ↓
Item 30 → Generic Methods
    ↓
Item 31 → Bounded Wildcards
```

درک صحیح Item 26 پایه فهم تمام مباحث پیشرفته Generics است.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

پیام اصلی **Item 26** فراتر از «از Raw Type استفاده نکن» است. این آیتم درباره **طراحی قرارداد (Design Contract) بین برنامه‌نویس و کامپایلر** است. با استفاده از Generics، اطلاعات نوع به‌صورت صریح در سیستم نوع جاوا ثبت می‌شود و کامپایلر می‌تواند ناسازگاری‌های نوع را پیش از اجرای برنامه شناسایی کند. در مقابل، Raw Type این اطلاعات را پنهان می‌کند، Type Safety را از بین می‌برد و تشخیص خطا را از زمان کامپایل به زمان اجرا منتقل می‌کند؛ نتیجه آن می‌تواند بروز `ClassCastException`، دشوار شدن Refactoring و کاهش خوانایی و قابلیت نگهداری API باشد.

### قانون طلایی

| اصل | توضیح |
|-----|-------|
| **انتخاب پیش‌فرض** | همیشه از Parameterized Types مانند `List<User>` استفاده کنید |
| **اگر نوع مشخص نیست** | از Wildcard مانند `List<?>` استفاده کنید |
| **اگر هر نوع شیء مجاز است** | از `List<Object>` استفاده کنید |
| **تنها استثناها** | `Class Literal` و `instanceof` |

این رویکرد به کامپایلر اجازه می‌دهد نقش یک لایه دفاعی قدرتمند را ایفا کند و بسیاری از خطاهای زمان اجرا را پیش از استقرار نرم‌افزار در محیط Production حذف کند.

---

[بازگشت به بالا](#top)

</div>
```