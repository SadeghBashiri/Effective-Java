<div dir="rtl">

<a id="top"></a>

# آیتم ۲۸: Arrayها را بر List ترجیح دهید

## (Prefer lists to arrays)

این آیتم یکی از بنیادی‌ترین مباحث Generics در جاوا را توضیح می‌دهد. اگر Item 26 درباره **Raw Type** بود و Item 27 درباره **Unchecked Warning**، در Item 28 Joshua Bloch دلیل بسیاری از این محدودیت‌ها را توضیح می‌دهد:

> **چرا Genericها و Arrayها با هم سازگار نیستند؟**

درک عمیق این آیتم برای هر توسعه‌دهنده Java، مخصوصاً در طراحی Frameworkها، Collectionها، کتابخانه‌ها و APIهای عمومی ضروری است.

---

## فهرست مطالب

- [ایده اصلی (Core Idea)](#core-idea)
- [دیدگاه معماری (Architectural View)](#architectural-view)
- [تفاوت اول: Covariance vs Invariance](#covariance-vs-invariance)
  - [Arrayها Covariant هستند](#arrays-covariant)
  - [Runtime Type Checking](#runtime-checking)
  - [Genericها Invariant هستند](#generics-invariant)
  - [مقایسه Covariant و Invariant](#comparison-invariance)
  - [چرا Genericها ایمن‌تر هستند؟](#why-generics-safer)
- [تفاوت دوم: Reified vs Erasure](#reified-vs-erasure)
  - [Array = Reified](#array-reified)
  - [Generic = Erasure](#generic-erasure)
  - [چرا Erasure انتخاب شد؟](#why-erasure)
- [چرا Generic Array ممنوع است؟](#why-generic-array)
- [Non-Reifiable Types](#non-reifiable)
- [مثال Chooser](#chooser-example)
- [راه‌حل Bloch: استفاده از List به جای Array](#solution)
- [Trade-off استفاده از List به جای Array](#tradeoff)
- [Production Code](#production-code)
- [چه زمانی هنوز Array انتخاب مناسبی است؟](#when-array)
- [ارتباط با Itemهای دیگر](#connection)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی (Core Idea)

پیام اصلی این آیتم بسیار ساده است:

> **اگر بین Array و List حق انتخاب داری، تقریباً همیشه List را انتخاب کن.**

دلیل این توصیه فقط زیبایی کد نیست؛ بلکه تفاوت عمیق در **سیستم نوع (Type System)** جاوا است.

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## دیدگاه معماری (Architectural View)

در جاوا دو مکانیزم برای نگهداری مجموعه‌ای از داده‌ها وجود دارد:
<div dir="ltr">

```
               Collection of Objects
                     │
         ┌───────────┴────────────┐
         │                        │
      Array                    Generic List
      (T[])                    (List<T>)
```
</div>
این دو از بیرون شبیه هم هستند، اما در داخل JVM کاملاً متفاوت پیاده‌سازی شده‌اند.

[بازگشت به بالا](#top)

---

<a id="covariance-vs-invariance"></a>
## تفاوت اول: Covariance vs Invariance

این مهم‌ترین قسمت Item 28 است.

<a id="arrays-covariant"></a>
### Arrayها Covariant هستند

فرض کنید:
<div dir="ltr">

```
Animal
▲
│
Dog
```
</div>
از آنجا که `Dog extends Animal`، در Array نیز داریم:

<div dir="ltr">

```java
Dog[]   IS-A   Animal[]
```
</div>

یعنی:

<div dir="ltr">

```java
Dog[] dogs = new Dog[10];
Animal[] animals = dogs;  // ✅ قانونی است
```
</div>

#### چرا؟

طراحان اولیه جاوا (قبل از Generics) می‌خواستند بتوانید آرایه‌های فرزند را به متدهایی بدهید که آرایه والد را می‌پذیرند.

**مثال:**

<div dir="ltr">

```java
Animal[] animals = new Dog[5];  // ✅ قانونی
```
</div>

تا اینجا مشکلی نیست. اما:

<div dir="ltr">

```java
animals[0] = new Cat();  // ✅ کامپایل می‌شود
```
</div>

ولی هنگام اجرا: `ArrayStoreException`

چرا؟ زیرا JVM می‌داند این آرایه در واقع `Dog[]` است.

<a id="runtime-checking"></a>
### Runtime Type Checking

این همان چیزی است که Bloch می‌گوید:
<div dir="ltr">

> Arrays are reified.
</div>
یعنی آرایه‌ها در Runtime نوع واقعی عنصر را حفظ می‌کنند.

مثلاً:

<div dir="ltr">

```java
Dog[] dogs = new Dog[5];
```
</div>

در Runtime نیز JVM دقیقاً می‌داند `Dog[]`، نه فقط `Object[]`.

به همین دلیل هنگام قرار دادن `new Cat()` خطا تولید می‌شود.

<a id="generics-invariant"></a>
### Genericها Invariant هستند

Genericها کاملاً متفاوت هستند.

`List<Dog>` آیا زیرنوع `List<Animal>` است؟ **خیر.**

یعنی این کد غیرقانونی است:

<div dir="ltr">

```java
List<Animal> animals = new ArrayList<Dog>();  // ❌ Compile Error
```
</div>

#### چرا؟

فرض کنید مجاز بود:

<div dir="ltr">

```java
animals.add(new Cat());  // حالا داخل ArrayList<Dog> یک Cat قرار گرفته است
```
</div>

بنابراین جاوا اصلاً اجازه چنین چیزی را نمی‌دهد. به این ویژگی می‌گویند **Invariance**.

<a id="comparison-invariance"></a>
### مقایسه Covariant و Invariant

| ویژگی | Array | Generic |
|--------|-------|---------|
| Covariant | ✅ | ❌ |
| Invariant | ❌ | ✅ |
| خطا | Runtime | Compile Time |
| ایمنی | کمتر | بیشتر |

<a id="why-generics-safer"></a>
### چرا Genericها ایمن‌تر هستند؟

Bloch دقیقاً همین مثال را می‌زند.

**Array:**

<div dir="ltr">

```java
Object[] objects = new Long[1];
objects[0] = "Hello";  // ✅ Compile, ❌ ArrayStoreException
```
</div>

**Generic:**

<div dir="ltr">

```java
List<Object> list = new ArrayList<Long>();  // ❌ Compile Error
```
</div>

بنابراین خطا بسیار زودتر کشف می‌شود.

[بازگشت به بالا](#top)

---

<a id="reified-vs-erasure"></a>
## تفاوت دوم: Reified vs Erasure

این مهم‌ترین مفهوم کل فصل Generics است.

<a id="array-reified"></a>
### Array = Reified

Runtime دقیقاً نوع عنصر را می‌داند.
<div dir="ltr">

```
Dog[] → همچنان Dog[]
```
</div>
<a id="generic-erasure"></a>
### Generic = Erasure

در Runtime:
<div dir="ltr">

```
List<String> و List<Integer>
```
</div>
هر دو تبدیل می‌شوند به:
<div dir="ltr">

```
List
```
</div>
اطلاعات Generic حذف می‌شود. به این فرآیند می‌گویند **Type Erasure**.

**مثال:** کد:

<div dir="ltr">

```java
List<String> list = new ArrayList<>();
```
</div>

در Bytecode تقریباً تبدیل می‌شود به:

<div dir="ltr">

```java
List list = new ArrayList();
```
</div>

کامپایلر Castهای لازم را خودش اضافه می‌کند.

<a id="why-erasure"></a>
### چرا Erasure انتخاب شد؟

به خاطر سازگاری با کدهای قدیمی (Backward Compatibility). اگر Erasure وجود نداشت، تقریباً تمام کتابخانه‌های جاوا قبل از Java 5 از کار می‌افتادند.

[بازگشت به بالا](#top)

---

<a id="why-generic-array"></a>
## چرا Generic Array ممنوع است؟

این سؤال تقریباً در تمام مصاحبه‌های Senior Java مطرح می‌شود.

### چرا این کد غیرقانونی است؟

<div dir="ltr">

```java
new List<String>[10]
```
</div>

یا

<div dir="ltr">

```java
new T[10]
```
</div>

### دلیل

فرض کنید مجاز بود:

<div dir="ltr">

```java
List<String>[] array = new List<String>[1];
```
</div>

حالا:

<div dir="ltr">

```java
Object[] objects = array;  // ✅ به دلیل Covariance آرایه‌ها مجاز است
```
</div>

سپس:

<div dir="ltr">

```java
objects[0] = List.of(123);  // ✅ Runtime مشکلی نمی‌بیند (فقط می‌داند List)
```
</div>

اما بعداً:

<div dir="ltr">

```java
String s = array[0].get(0);  // ❌ ClassCastException
```
</div>

کامپایلر فرض می‌کند `String` ولی داخل `Integer` قرار دارد.

بنابراین `new List<String>[]` به طور کامل ممنوع شده است.

[بازگشت به بالا](#top)

---

<a id="non-reifiable"></a>
## Non-Reifiable Types

Bloch اصطلاح جدیدی معرفی می‌کند.

`List<String>` یک **Non-Reifiable Type** است. چرا؟ زیرا اطلاعات Runtime آن ناقص است. Runtime فقط می‌بیند `List`.

اما `String[]` کاملاً Reifiable است.

[بازگشت به بالا](#top)

---

<a id="chooser-example"></a>
## مثال Chooser

این قسمت یکی از معروف‌ترین مثال‌های Effective Java است.

نسخه اولیه:

<div dir="ltr">

```java
Object[] choices;
```
</div>

مشکل: `return (T) choices[index];` همیشه Cast لازم دارد.

نسخه Generic:

<div dir="ltr">

```java
T[] choices;
```
</div>

اما `choices.toArray()` برمی‌گرداند `Object[]`، نه `T[]`.

در نتیجه `(T[]) choices.toArray()` یک Unchecked Cast تولید می‌کند.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## راه‌حل Bloch

به جای Array، از `List<T>` استفاده کن:

<div dir="ltr">

```java
private final List<T> choices;
```
</div>

دیگر:

- Cast نداریم
- Warning نداریم
- Runtime Exception نداریم

[بازگشت به بالا](#top)

---

<a id="tradeoff"></a>
## Trade-off استفاده از List به جای Array

| معیار | Array | List |
|-------|-------|------|
| Compile-time Type Safety | ❌ | ✅ |
| Runtime Type Safety | ✅ | ❌ (به Erasure وابسته است، اما Type Safety در کامپایل تضمین می‌شود) |
| Performance | کمی بهتر | کمی سربار بیشتر |
| انعطاف | کمتر | بیشتر |
| Generic Compatibility | ضعیف | عالی |
| API Design | ضعیف‌تر | بهتر |
| Maintainability | پایین‌تر | بالاتر |

> نکته: عبارت «Runtime Type Safety» برای Genericها به این معناست که نوع پارامتر Generic در Runtime وجود ندارد (به دلیل Erasure)، اما اگر کد بدون Warning کامپایل شده باشد، کامپایلر تضمین می‌کند که Castهای تولیدشده در حالت عادی شکست نخورند.

[بازگشت به بالا](#top)

---

<a id="production-code"></a>
## Production Code

امروزه تقریباً تمام Frameworkهای مدرن جاوا همین اصل را رعایت می‌کنند.

مثلاً در:

- Spring Framework
- Hibernate
- Quarkus
- Micronaut
- Jackson
- Guava
- Apache Commons

تقریباً همه APIها به جای `T[]` از `List<T>` یا `Collection<T>` استفاده می‌کنند؛ زیرا کار با Genericها ساده‌تر، ایمن‌تر و توسعه‌پذیرتر است.

[بازگشت به بالا](#top)

---

<a id="when-array"></a>
## چه زمانی هنوز Array انتخاب مناسبی است؟

با وجود توصیه Bloch، آرایه‌ها همچنان در برخی سناریوها بهترین انتخاب هستند:

- **تعامل با APIهای سطح پایین JVM یا JNI**
- **محاسبات عددی و پردازش‌های بسیار حساس به کارایی** که هزینه Boxing یا سربار Collection مهم است
- **آرایه‌های Primitive** مانند `int[]` یا `byte[]` که معادل مستقیمی در Genericها ندارند
- **بافرهای ثابت** با اندازه مشخص، مانند `byte[]` برای I/O یا شبکه

در سایر موارد، به‌ویژه در طراحی APIهای عمومی و کدهای سازمانی، `List<T>` معمولاً انتخاب مناسب‌تری است.

[بازگشت به بالا](#top)

---

<a id="connection"></a>
## ارتباط با Itemهای دیگر

```
Item 26 → Raw Type خطرناک است
    ↓
Item 27 → Unchecked Warningها را حذف کن
    ↓
Item 28 → یکی از مهم‌ترین دلایل Warningها، ترکیب Array و Generic است
    ↓
Item 29 → از Generic Typeها به‌درستی استفاده کن
    ↓
Item 31 → Wildcardها را برای APIهای انعطاف‌پذیر به کار ببر
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

Joshua Bloch در این آیتم سه اصل کلیدی را آموزش می‌دهد:

| اصل | توضیح |
|-----|-------|
| **۱** | آرایه‌ها و Genericها مدل‌های نوع متفاوتی دارند؛ آرایه‌ها **Covariant و Reified** هستند، در حالی که Genericها **Invariant و مبتنی بر Type Erasure** هستند |
| **۲** | به دلیل ناسازگاری این دو مدل، ساخت آرایه از نوع Generic (مانند `new List<String>[]` یا `new T[]`) ممنوع است تا از بروز خطاهای نوع در Runtime جلوگیری شود |
| **۳** | در طراحی APIها و کدهای جدید، اگر با خطاها یا هشدارهای ناشی از ترکیب Array و Generic مواجه شدید، اولین گزینه بررسی باید جایگزین کردن آرایه با `List<T>` باشد. این کار Type Safety، خوانایی و قابلیت نگهداری کد را به شکل محسوسی افزایش می‌دهد |

### قانون طلایی

> **اگر بین Array و List حق انتخاب داری، List را انتخاب کن.**

---

[بازگشت به بالا](#top)

</div>
```