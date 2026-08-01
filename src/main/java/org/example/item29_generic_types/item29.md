<div dir="rtl">

<a id="top"></a>

# آیتم ۲۹: Generic Typeها را ترجیح دهید

## (Favor Generic Types)

این آیتم یکی از مهم‌ترین آیتم‌های فصل Generics است. اگر بخواهم اهمیت آیتم‌ها را رتبه‌بندی کنم، احتمالاً **Item 26، Item 28، Item 29، Item 31 و Item 32** ستون فقرات درک Generics در جاوا هستند.

نکته جالب این است که **Item 29 ادامه‌ی مستقیم Item 28 است.**

در Item 28 یاد گرفتیم که:

- Genericها با Arrayها ناسازگارند.
- ساخت `new E[]` ممنوع است.
- علت آن Type Erasure است.

حالا در Item 29، Joshua Bloch دقیقاً به این سؤال پاسخ می‌دهد:

> **اگر قرار است Generic Type بنویسیم و در داخل آن به Array نیاز داشته باشیم، چه کار باید بکنیم؟**

در واقع این آیتم آموزش **طراحی Generic Type** است، نه صرفاً استفاده از Genericها.

---

## فهرست مطالب

- [ایده اصلی (Core Idea)](#core-idea)
- [دیدگاه معماری (Architectural View)](#architectural-view)
- [چرا اصلاً باید Generic Type بنویسیم؟](#why-generic-type)
- [راه‌حل](#solution)
- [اولین قدم در Generic کردن کلاس](#first-step)
- [مشکل Generic Array Creation](#generic-array-problem)
- [چرا؟](#why)
- [دو راه‌حل پیشنهادی Bloch](#two-solutions)
  - [راه‌حل اول (رایج‌تر)](#solution1)
  - [راه‌حل دوم](#solution2)
- [مقایسه دو راه‌حل](#comparison)
- [Heap Pollution چیست؟](#heap-pollution)
- [ارتباط با Item 27](#connection-item27)
- [ارتباط با Item 28](#connection-item28)
- [Generic Type بدون محدودیت](#unbounded)
- [Bounded Type Parameter](#bounded)
- [نمونه‌های رایج در JDK](#jdk-examples)
- [Trade-off](#tradeoff)
- [چه زمانی Generic Type بنویسیم؟](#when-to-use)
- [ارتباط با سایر Itemها](#connection-other)
- [Best Practices](#best-practices)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی (Core Idea)

پیام اصلی این آیتم بسیار ساده است:

> **اگر کلاسی با `Object` نوشته‌اید و کاربران آن مجبور به Cast هستند، احتمالاً باید آن را Generic کنید.**

یا به بیان دیگر:

> **کدی که نیاز به Cast در سمت Client دارد، معمولاً طراحی مناسبی ندارد.**

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## دیدگاه معماری (Architectural View)

در طراحی APIهای مدرن، هدف این است که **مسئولیت حفظ Type Safety بر عهده‌ی کتابخانه باشد، نه کاربر آن**.

به همین دلیل، طراحی یک کلاس Generic باعث می‌شود:

- اطلاعات نوع (Type Information) در API حفظ شود.
- Castهای صریح از کد Client حذف شوند.
- خطاهای نوع در زمان کامپایل کشف شوند.
- استفاده از API ساده‌تر و ایمن‌تر شود.

معماری کلی به صورت زیر است:
<div dir="ltr">

```
           Non Generic API
                  │
                  ▼
        Client performs Cast
                  │
        Runtime Failure Possible
                  │
                  ▼
        ClassCastException


                  │
                  ▼

           Generic API
                  │
                  ▼
        Compiler performs checks
                  │
                  ▼
         No explicit casts needed
                  │
                  ▼
         Compile-time Type Safety
```
</div>

[بازگشت به بالا](#top)

---

<a id="why-generic-type"></a>
## چرا اصلاً باید Generic Type بنویسیم؟

نسخه اولیه Stack کتاب را ببینید.

<div dir="ltr">

```java
public class Stack {
    private Object[] elements;

    public void push(Object e) { }

    public Object pop() { }
}
```
</div>

استفاده:

<div dir="ltr">

```java
Stack stack = new Stack();
stack.push("Ali");
String name = (String) stack.pop();  // ❌ نیاز به Cast
```
</div>

مشکل چیست؟ تمام مسئولیت Cast روی دوش Client است.

اگر اشتباه کند:

<div dir="ltr">

```java
Integer number = (Integer) stack.pop();  // ❌ ClassCastException
```
</div>

برنامه در Runtime با `ClassCastException` مواجه می‌شود.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## راه‌حل

کلاس را Generic کنیم.

<div dir="ltr">

```java
public class Stack<E> {
    public void push(E e) { }
    public E pop() { }
}
```
</div>

اکنون:

<div dir="ltr">

```java
Stack<String> stack = new Stack<>();
stack.push("Ali");
String name = stack.pop();  // ✅ بدون Cast
```
</div>

هیچ Castای وجود ندارد. کامپایلر خودش Cast لازم را تولید می‌کند و در صورت نبود Warning، موفقیت آن را تضمین می‌کند.

[بازگشت به بالا](#top)

---

<a id="first-step"></a>
## اولین قدم در Generic کردن کلاس

Bloch می‌گوید: ابتدا Type Parameter را اضافه کنید.

<div dir="ltr">

```java
class Stack<E>
```
</div>

سپس تمام `Object`ها را با `E` جایگزین کنید.

```
Object → E
```

تقریباً همه جا کار تمام می‌شود... اما یک مشکل بزرگ باقی می‌ماند.

[بازگشت به بالا](#top)

---

<a id="generic-array-problem"></a>
## مشکل Generic Array Creation

وقتی می‌نویسیم:

<div dir="ltr">

```java
private E[] elements;
```
</div>

در سازنده طبیعی است که بخواهیم بنویسیم:

<div dir="ltr">

```java
elements = new E[16];  // ❌ Compile Error
```
</div>

اما کامپایلر خطا می‌دهد: `generic array creation`

[بازگشت به بالا](#top)

---

<a id="why"></a>
## چرا؟

پاسخ را در Item 28 یاد گرفتیم.

`E` یک **Non-Reifiable Type** است. به دلیل **Type Erasure**، JVM در زمان اجرا نمی‌داند `E` دقیقاً چه نوعی است. در نتیجه ساخت آرایه‌ای از آن غیرممکن است.

[بازگشت به بالا](#top)

---

<a id="two-solutions"></a>
## دو راه‌حل پیشنهادی Bloch

کتاب دو راه‌حل معتبر معرفی می‌کند.

<a id="solution1"></a>
### راه‌حل اول (رایج‌تر)

ابتدا آرایه Object بسازیم.

<div dir="ltr">

```java
elements = (E[]) new Object[DEFAULT_CAPACITY];
```
</div>

در ظاهر این Cast ناامن است. به همین دلیل Warning دریافت می‌کنیم: `Unchecked Cast`

اما آیا واقعاً خطرناک است؟ خیر.

#### چرا این Cast ایمن است؟

زیرا `elements` خصوصی است:

<div dir="ltr">

```java
private E[] elements;
```
</div>

هیچ‌وقت بیرون کلاس برگردانده نمی‌شود. تمام داده‌های داخل آن فقط از طریق `push(E e)` وارد می‌شوند. پس تنها چیزی که می‌تواند وارد آرایه شود `E` است. بنابراین با وجود اینکه کامپایلر قادر به اثبات این موضوع نیست، برنامه‌نویس می‌تواند ثابت کند که Cast ایمن است.

به همین دلیل استفاده از `@SuppressWarnings("unchecked")` در این نقطه مجاز است.

<a id="solution2"></a>
### راه‌حل دوم

نوع فیلد را از ابتدا `Object[]` نگه داریم.

<div dir="ltr">

```java
private Object[] elements;
```
</div>

هنگام خواندن:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
E result = (E) elements[--size];
```
</div>

در این روش، هنگام هر بار خواندن باید Cast انجام شود.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## مقایسه دو راه‌حل

| معیار | راه‌حل اول (`E[]`) | راه‌حل دوم (`Object[]`) |
|-------|-------------------|------------------------|
| تعداد Cast | یک بار در سازنده | در هر بار خواندن |
| خوانایی | بیشتر | کمتر |
| سادگی کد | بهتر | ضعیف‌تر |
| استفاده در عمل | رایج‌تر | کمتر |
| Heap Pollution | دارد (بی‌ضرر در این سناریو) | ندارد |

به همین دلیل، Bloch روش اول را انتخاب ارجح می‌داند.

[بازگشت به بالا](#top)

---

<a id="heap-pollution"></a>
## Heap Pollution چیست؟

یکی از مفاهیم جدید این آیتم **Heap Pollution** است.

Heap Pollution زمانی رخ می‌دهد که:

> **نوعی که کامپایلر تصور می‌کند با نوع واقعی شیء در Heap یکسان نباشد.**

در مثال Stack: کامپایلر تصور می‌کند `E[]`، اما در Runtime واقعاً داریم `Object[]`. این عدم تطابق همان Heap Pollution است. در این مثال خاص، چون آرایه خصوصی است و فقط عناصر نوع `E` در آن ذخیره می‌شوند، این Heap Pollution خطری ایجاد نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="connection-item27"></a>
## ارتباط با Item 27

چرا اینجا استفاده از `@SuppressWarnings("unchecked")` مجاز است؟

زیرا دقیقاً همان شرطی برقرار است که Item 27 بیان می‌کند:

- ابتدا ایمنی Cast را اثبات کنید.
- سپس Warning را فقط در کوچک‌ترین محدوده ممکن Suppress کنید.
- دلیل این کار را در قالب Comment مستند کنید.

[بازگشت به بالا](#top)

---

<a id="connection-item28"></a>
## ارتباط با Item 28

در نگاه اول ممکن است تناقضی وجود داشته باشد.

Item 28 گفت: **List را به Array ترجیح دهید.**

اما در اینجا Stack همچنان از Array استفاده می‌کند.

تناقضی وجود ندارد. توصیه Item 28 بیشتر برای **طراحی APIها و استفاده از Genericها در کدهای معمول** است. در مقابل، Item 29 درباره **پیاده‌سازی داخلی ساختارهای داده** صحبت می‌کند. برخی کلاس‌ها ذاتاً باید بر پایه آرایه پیاده‌سازی شوند، زیرا آرایه مزایای مهمی دارد:

- دسترسی مستقیم با اندیس (`O(1)`)
- حافظه‌ی فشرده‌تر
- Cache Locality بهتر
- کارایی بالاتر

به همین دلیل کلاس‌هایی مانند `ArrayList`، `ArrayDeque`، `HashMap` و `ConcurrentHashMap` در داخل خود از آرایه استفاده می‌کنند، هرچند API آن‌ها کاملاً Generic و Type-Safe است.

[بازگشت به بالا](#top)

---

<a id="unbounded"></a>
## Generic Type بدون محدودیت

بیشتر Generic Typeها هیچ محدودیتی روی نوع پارامتر ندارند.

مثلاً:

<div dir="ltr">

```java
Stack<Object>
Stack<String>
Stack<User>
Stack<List<String>>
Stack<int[]>
```
</div>

همگی معتبر هستند.

اما:

<div dir="ltr">

```java
Stack<int>  // ❌ مجاز نیست
```
</div>

[بازگشت به بالا](#top)

---

<a id="bounded"></a>
## Bounded Type Parameter

گاهی Generic Type فقط باید انواع خاصی را بپذیرد.

نمونه کتاب:

<div dir="ltr">

```java
class DelayQueue<E extends Delayed>
```
</div>

در اینجا `E` باید زیرنوعی از `Delayed` باشد.

به این ویژگی **Bounded Type Parameter** گفته می‌شود.

مزایای آن:

- امکان فراخوانی مستقیم متدهای `Delayed`
- حذف Cast
- افزایش Type Safety
- مستندسازی قرارداد API از طریق نوع‌ها

[بازگشت به بالا](#top)

---

<a id="jdk-examples"></a>
## نمونه‌های رایج در JDK

این الگو در بسیاری از کلاس‌های استاندارد جاوا دیده می‌شود:

- `PriorityQueue<E>`
- `DelayQueue<E extends Delayed>`
- `EnumSet<E extends Enum<E>>`
- `EnumMap<K extends Enum<K>, V>`

این طراحی باعث می‌شود محدودیت‌های دامنه (Domain Constraints) در خود سیستم نوع بیان شوند.

[بازگشت به بالا](#top)

---

<a id="tradeoff"></a>
## Trade-off

| معیار | کلاس مبتنی بر Object | Generic Type |
|-------|---------------------|--------------|
| Type Safety | ❌ | ✅ |
| نیاز به Cast | زیاد | ندارد |
| کشف خطا | Runtime | Compile Time |
| خوانایی API | پایین | بالا |
| نگهداری | سخت‌تر | آسان‌تر |
| Refactoring | پرریسک | ایمن‌تر |
| تجربه توسعه‌دهنده | ضعیف | بهتر |

[بازگشت به بالا](#top)

---

<a id="when-to-use"></a>
## چه زمانی Generic Type بنویسیم؟

یک قانون عملی:

اگر پاسخ هر یک از پرسش‌های زیر مثبت است، کلاس شما احتمالاً باید Generic باشد:

- آیا کلاس روی انواع مختلف داده کار می‌کند؟
- آیا کاربران مجبور به Cast هستند؟
- آیا فیلدها یا متدها از نوع `Object` استفاده می‌کنند؟
- آیا Type Safety بخشی از قرارداد API است؟

اگر پاسخ مثبت است، Generic کردن کلاس معمولاً بهترین انتخاب است.

[بازگشت به بالا](#top)

---

<a id="connection-other"></a>
## ارتباط با سایر Itemها

```
Item 26 → از Raw Type استفاده نکن
    ↓
Item 27 → Unchecked Warningها را حذف یا اثبات کن
    ↓
Item 28 → Genericها و Arrayها قوانین متفاوتی دارند
    ↓
Item 29 → یاد بگیر چگونه Generic Type طراحی کنی
    ↓
Item 30 → Generic Methodها را طراحی کن
    ↓
Item 31 → از Wildcardها برای APIهای انعطاف‌پذیر استفاده کن
```

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## Best Practices

| قانون | توضیح |
|-------|-------|
| **کلاس‌های چندنوعی را Generic کنید** | کلاس‌هایی که روی انواع مختلف داده کار می‌کنند |
| **از Type Parameterهای معنادار استفاده کنید** | مانند `E`، `K`، `V`، `T` |
| **Cast را از Client حذف کنید** | تا حد امکان |
| **برای آرایه داخلی از الگوی Bloch استفاده کنید** | یکی از دو راه‌حل معرفی‌شده |
| **`@SuppressWarnings` را محدود کنید** | فقط پس از اثبات ایمنی و در کوچک‌ترین Scope |
| **برای محدودیت نوع از Bounded Type Parameter استفاده کنید** | برای اعمال محدودیت‌های دامنه |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

پیام اصلی **Item 29** این است که **Type Safety باید در طراحی خود کلاس تضمین شود، نه اینکه به کاربران کلاس واگذار شود**. اگر یک کلاس بر پایه‌ی `Object` نوشته شده و کاربران آن مجبور به انجام Cast هستند، آن کلاس معمولاً یک کاندید مناسب برای Generic شدن است.

### سه اصل کلیدی

| اصل | توضیح |
|-----|-------|
| **۱** | کلاس‌های مبتنی بر `Object` با Cast در سمت Client، کاندیدای مناسبی برای Generic شدن هستند |
| **۲** | بزرگ‌ترین چالش در Generic کردن، تعامل با آرایه‌هاست؛ از یکی از دو الگوی Bloch برای حل آن استفاده کنید |
| **۳** | `@SuppressWarnings("unchecked")` را فقط پس از اثبات ایمنی Cast و در کوچک‌ترین محدوده ممکن به کار ببرید |

در نهایت، Generic Typeها APIهایی ایمن‌تر، خواناتر، قابل‌نگهداری‌تر و سازگارتر با طراحی مدرن جاوا ایجاد می‌کنند و یکی از مهم‌ترین ابزارها برای انتقال خطاها از زمان اجرا به زمان کامپایل هستند.

---

[بازگشت به بالا](#top)

</div>
```