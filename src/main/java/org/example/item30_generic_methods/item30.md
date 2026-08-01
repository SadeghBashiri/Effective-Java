<div dir="rtl">

<a id="top"></a>

# آیتم ۳۰: Generic Methodها را ترجیح دهید

## (Favor Generic Methods)

این آیتم نیز مانند آیتم‌های ۲۶ تا ۲۹ یکی از **مهم‌ترین آیتم‌های فصل Generics** است. اگر:

- **Item 26** یاد می‌دهد چگونه از Genericها استفاده کنیم،
- **Item 29** یاد می‌دهد چگونه Generic Type طراحی کنیم،

آنگاه **Item 30** یک گام جلوتر می‌رود و توضیح می‌دهد:

> **چگونه متدهایی طراحی کنیم که خودشان Generic باشند و بدون نیاز به Cast، Type Safety را برای کاربران فراهم کنند.**

در واقع، این آیتم آغاز طراحی **Reusable Generic APIs** است؛ الگویی که تقریباً در تمام کتابخانه‌های مدرن جاوا (مانند JDK، Spring، Guava، Hibernate و ...) به‌طور گسترده استفاده می‌شود.

---

## فهرست مطالب

- [ایده اصلی (Core Idea)](#core-idea)
- [دیدگاه معماری (Architectural View)](#architectural-view)
- [Generic Method چیست؟](#what-is-generic-method)
- [چرا Generic Method؟](#why-generic-method)
- [نسخه Generic](#generic-version)
- [تفاوت Generic Type و Generic Method](#generic-type-vs-method)
- [چه زمانی Generic Method بهتر از Generic Type است؟](#when-generic-method)
- [Type Inference](#type-inference)
- [محدودیت متد union](#union-limitation)
- [Generic Singleton Factory](#generic-singleton-factory)
- [چرا این Cast ایمن است؟](#why-cast-safe)
- [کاربردهای Generic Singleton Factory](#applications)
- [Recursive Type Bound](#recursive-type-bound)
- [چرا Recursive؟](#why-recursive)
- [مثال max](#max-example)
- [چرا از Comparable\<?> استفاده نشده است؟](#why-not-comparable-wildcard)
- [ارتباط با Item 29](#connection-item29)
- [ارتباط با Item 31](#connection-item31)
- [ارتباط با Type Erasure](#type-erasure)
- [Trade-off](#tradeoff)
- [Best Practices](#best-practices)
- [ارتباط با سایر Itemها](#connection-other)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی (Core Idea)

پیام اصلی این آیتم بسیار ساده است:

> **اگر متدی با انواع مختلف داده کار می‌کند و کاربران مجبور به Cast هستند، آن متد باید Generic باشد.**

یا به بیان دیگر:

> **همان‌طور که کلاس‌ها می‌توانند Generic باشند، متدها نیز باید در صورت نیاز Generic طراحی شوند تا مسئولیت Type Safety بر عهده‌ی کامپایلر باشد، نه کاربر.**

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## دیدگاه معماری (Architectural View)

در طراحی APIهای مدرن، سه سطح از Genericها وجود دارد:
<div dir="ltr">

```
                 Generic Programming
                        │
        ┌───────────────┼────────────────┐
        │               │                │
        ▼               ▼                ▼
Generic Type     Generic Method   Generic Interface
Stack<E>       <T> T copy(...)   Comparable<T>
```
</div>
هر سه ابزار یک هدف مشترک دارند:

- حذف Castهای دستی
- انتقال خطا از Runtime به Compile Time
- طراحی APIهای قابل استفاده مجدد (Reusable)
- افزایش Type Safety

Generic Method زمانی استفاده می‌شود که **خود کلاس Generic نیست یا نیازی به Generic بودن ندارد، اما یک متد باید روی انواع مختلف کار کند.**

[بازگشت به بالا](#top)

---

<a id="what-is-generic-method"></a>
## Generic Method چیست؟

یک Generic Method متدی است که **Type Parameter مخصوص خودش** را دارد.

نکته مهم: Type Parameter متد مستقل از Type Parameter کلاس است.

ساختار کلی:

<div dir="ltr">

```java
public static <T> T method(T value)
```
</div>

دقت کنید که `<T>` بین Modifierها و Return Type قرار می‌گیرد.

[بازگشت به بالا](#top)

---

<a id="why-generic-method"></a>
## چرا Generic Method؟

فرض کنید متدی برای Union دو مجموعه داریم.

نسخه اولیه:

<div dir="ltr">

```java
public static Set union(Set s1, Set s2)
```
</div>

مشکلات:

- استفاده از Raw Type
- از دست رفتن Type Safety
- تولید Warning
- نیاز به Cast در سمت Client
- احتمال ClassCastException

[بازگشت به بالا](#top)

---

<a id="generic-version"></a>
## نسخه Generic

<div dir="ltr">

```java
public static <E> Set<E> union(Set<E> s1, Set<E> s2)
```
</div>

مزایا:

- بدون Warning
- بدون Cast
- Type Safety کامل
- API خواناتر

استفاده:

<div dir="ltr">

```java
Set<String> result = union(names1, names2);
```
</div>

کامپایلر نوع `E` را به صورت خودکار استنتاج (Type Inference) می‌کند.

[بازگشت به بالا](#top)

---

<a id="generic-type-vs-method"></a>
## تفاوت Generic Type و Generic Method

گاهی این دو با هم اشتباه گرفته می‌شوند.

### Generic Type

<div dir="ltr">

```java
class Box<T> { }
```
</div>

تمام شیء Generic است.

نمونه: `Box<String>`

### Generic Method

<div dir="ltr">

```java
class Utils {
    static <T> T copy(T value) { }
}
```
</div>

کلاس Generic نیست. فقط همان متد Generic است.

[بازگشت به بالا](#top)

---

<a id="when-generic-method"></a>
## چه زمانی Generic Method بهتر از Generic Type است؟

اگر فقط یک یا چند متد نیاز به Generic بودن دارند و وضعیت (State) کلاس به نوع وابسته نیست، Generic Method انتخاب مناسب‌تری است.

نمونه‌های معروف در JDK:

- `Collections.sort`
- `Collections.binarySearch`
- `Collections.emptyList`
- `Collections.emptySet`
- `Collections.singleton`
- `Arrays.asList`

تقریباً تمام این متدها Generic هستند، در حالی که کلاس `Collections` خود Generic نیست.

[بازگشت به بالا](#top)

---

<a id="type-inference"></a>
## Type Inference

یکی از مهم‌ترین مزایای Generic Methodها، **استنتاج نوع** است.

مثال:

<div dir="ltr">

```java
Set<String> result = union(set1, set2);
```
</div>

در اینجا نیازی به نوشتن `Collections.<String>union(...)` نیست.

کامپایلر خودش تشخیص می‌دهد که `E` برابر `String` است. این ویژگی باعث ساده‌تر شدن API می‌شود.

[بازگشت به بالا](#top)

---

<a id="union-limitation"></a>
## محدودیت متد union

نسخه اولیه کتاب:

<div dir="ltr">

```java
<E> Set<E> union(Set<E>, Set<E>)
```
</div>

تنها زمانی کار می‌کند که هر دو مجموعه دقیقاً از یک نوع باشند.

مثلاً `Set<Integer>` و `Set<Integer>`.

اما `Set<Integer>` و `Set<Double>` یا `Set<Dog>` و `Set<Animal>` را نمی‌پذیرد.

Bloch اشاره می‌کند که این محدودیت در **Item 31** با استفاده از **Bounded Wildcard** برطرف می‌شود.

[بازگشت به بالا](#top)

---

<a id="generic-singleton-factory"></a>
## Generic Singleton Factory

این قسمت یکی از مهم‌ترین بخش‌های آیتم است.

### مسئله

فرض کنید یک شیء Stateless داریم.

مثلاً `UnaryOperator<T>` که فقط ورودی را برمی‌گرداند: `t -> t`

آیا باید برای هر نوع، یک شیء جدید بسازیم؟

`UnaryOperator<String>`، `UnaryOperator<Integer>`، `UnaryOperator<User>`

خیر. به دلیل **Type Erasure**، در Runtime تفاوتی بین این نوع‌ها وجود ندارد. همه آن‌ها یک پیاده‌سازی یکسان دارند. در نتیجه می‌توان یک Singleton ساخت.

### پیاده‌سازی

<div dir="ltr">

```java
private static final UnaryOperator<Object> IDENTITY_FN = t -> t;
```
</div>

و سپس:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
public static <T> UnaryOperator<T> identityFunction() {
    return (UnaryOperator<T>) IDENTITY_FN;
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="why-cast-safe"></a>
## چرا این Cast ایمن است؟

در نگاه اول `UnaryOperator<Object>` → `UnaryOperator<T>` یک Unchecked Cast است.

اما اینجا تابع `t -> t` هیچ تغییری روی داده انجام نمی‌دهد. هر چیزی وارد شود همان خارج می‌شود. پس برای هر نوعی ایمن است.

به همین دلیل کتاب استفاده از `@SuppressWarnings("unchecked")` را مجاز می‌داند، زیرا ایمنی Cast قابل اثبات است.

[بازگشت به بالا](#top)

---

<a id="applications"></a>
## کاربردهای Generic Singleton Factory

این الگو در JDK به‌طور گسترده استفاده می‌شود.

نمونه‌ها:

- `Collections.emptyList()`
- `Collections.emptySet()`
- `Collections.emptyMap()`
- `Collections.reverseOrder()`
- `Comparator.naturalOrder()`
- `Function.identity()`

به جای ساخت هزاران شیء یکسان، یک Singleton بین همه نوع‌ها به اشتراک گذاشته می‌شود.

[بازگشت به بالا](#top)

---

<a id="recursive-type-bound"></a>
## Recursive Type Bound

این بخش معمولاً یکی از دشوارترین مفاهیم Generics است.

ساختار:

<div dir="ltr">

```java
<E extends Comparable<E>>
```
</div>

در نگاه اول عجیب به نظر می‌رسد. اما معنای آن ساده است:

> **هر نوعی که بتواند با نمونه‌ای از همان نوع مقایسه شود.**

مثلاً `String` پیاده‌سازی کرده است `Comparable<String>`، یا `Integer` پیاده‌سازی کرده است `Comparable<Integer>`.

[بازگشت به بالا](#top)

---

<a id="why-recursive"></a>
## چرا Recursive؟

چون خود `E` در تعریف محدودیت استفاده شده است.

```
E → Comparable<E>
```

به همین دلیل به آن **Recursive Type Bound** می‌گویند.

[بازگشت به بالا](#top)

---

<a id="max-example"></a>
## مثال max

کتاب متد زیر را معرفی می‌کند:

<div dir="ltr">

```java
public static <E extends Comparable<E>> E max(Collection<E> c)
```
</div>

این قرارداد تضمین می‌کند که هر عنصر مجموعه بتواند با عنصر دیگری از همان نوع مقایسه شود. در نتیجه می‌توان بدون Cast نوشت:

<div dir="ltr">

```java
e.compareTo(result)
```
</div>

[بازگشت به بالا](#top)

---

<a id="why-not-comparable-wildcard"></a>
## چرا از Comparable\<?> استفاده نشده است؟

زیرا `Comparable<E>` تضمین می‌کند: "E compares with E". در حالی که `Comparable<Object>` چنین تضمینی ارائه نمی‌دهد. این محدودیت بخشی از قرارداد Type System است.

[بازگشت به بالا](#top)

---

<a id="connection-item29"></a>
## ارتباط با Item 29

- **Item 29:** `class Stack<E>` → Generic Type
- **Item 30:** `static <T> T max(...)` → Generic Method

در عمل، این دو مکمل یکدیگر هستند.

[بازگشت به بالا](#top)

---

<a id="connection-item31"></a>
## ارتباط با Item 31

Bloch در این آیتم چند بار اشاره می‌کند که نسخه فعلی `union` هنوز انعطاف کافی ندارد. در Item 31 خواهیم دید چگونه با استفاده از `? extends` و `? super` همان متد را بسیار انعطاف‌پذیرتر طراحی کنیم.

[بازگشت به بالا](#top)

---

<a id="type-erasure"></a>
## ارتباط با Type Erasure

تمام Generic Methodها نیز مانند Generic Typeها بر پایه‌ی **Type Erasure** پیاده‌سازی می‌شوند.

بنابراین `<T>` در زمان اجرا حذف می‌شود. اما کامپایلر پیش از حذف اطلاعات نوع:

- بررسی Type Safety
- تولید Castهای لازم
- استنتاج نوع

را انجام می‌دهد.

[بازگشت به بالا](#top)

---

<a id="tradeoff"></a>
## Trade-off

| معیار | متد معمولی | Generic Method |
|-------|------------|----------------|
| Type Safety | پایین‌تر | بالا |
| نیاز به Cast | زیاد | ندارد |
| خوانایی API | کمتر | بیشتر |
| Reusability | محدود | بسیار بالا |
| Compile-time Checking | محدود | کامل |
| انعطاف | کمتر | بیشتر |

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## Best Practices

| قانون | توضیح |
|-------|-------|
| **متدهای چندنوعی را Generic کنید** | هر متدی که روی انواع مختلف داده کار می‌کند |
| **از Type Parameterهای استاندارد استفاده کنید** | `T`، `E`، `K`، `V` |
| **اجازه دهید کامپایلر نوع را استنتاج کند** | از Type Inference استفاده کنید |
| **Cast را محدود کنید** | اگر Cast اجتناب‌ناپذیر است، ایمنی آن را اثبات کنید |
| **از Generic Singleton Factory استفاده کنید** | برای اشیای Stateless و قابل استفاده برای همه نوع‌ها |
| **از Bounded Type Parameter استفاده کنید** | برای اعمال محدودیت روی نوع پارامتر |

[بازگشت به بالا](#top)

---

<a id="connection-other"></a>
## ارتباط با سایر Itemها

```
Item 26 → از Raw Type استفاده نکن
    ↓
Item 27 → Unchecked Warningها را حذف یا اثبات کن
    ↓
Item 28 → قوانین Array و Generic را درک کن
    ↓
Item 29 → Generic Type طراحی کن
    ↓
Item 30 → Generic Method طراحی کن
    ↓
Item 31 → Wildcardها را برای APIهای انعطاف‌پذیر به کار ببر
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

پیام اصلی **Item 30** این است که **همان‌طور که کلاس‌ها باید در صورت نیاز Generic باشند، متدها نیز باید به گونه‌ای طراحی شوند که کاربران بدون نیاز به Cast از آن‌ها استفاده کنند**. Generic Methodها اطلاعات نوع را در سطح متد مدل می‌کنند و با کمک **Type Inference**، APIهایی ساده، خوانا و Type-Safe در اختیار کاربران قرار می‌دهند.

### سه اصل کلیدی

| اصل | توضیح |
|-----|-------|
| **۱** | اگر متدی نیاز به Cast در سمت Client دارد، آن متد کاندیدای مناسبی برای Generic شدن است |
| **۲** | Generic Methodها با Type Inference، APIهایی ساده و Type-Safe ارائه می‌دهند |
| **۳** | دو الگوی مهم: **Generic Singleton Factory** (استفاده مجدد از اشیای Stateless) و **Recursive Type Bound** (مقایسه‌پذیری با نوع خود) |

این مفاهیم پایه‌ی بسیاری از APIهای استاندارد JDK و کتابخانه‌های سازمانی هستند و درک آن‌ها برای طراحی Frameworkها، کتابخانه‌های عمومی و APIهای Production-Grade ضروری است.

---

[بازگشت به بالا](#top)

</div>
```