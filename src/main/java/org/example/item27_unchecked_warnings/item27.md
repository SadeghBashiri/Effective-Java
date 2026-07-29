<div dir="rtl">

<a id="top"></a>

# آیتم ۲۷: Warningهای Unchecked را حذف کنید

## (Eliminate Unchecked Warnings)

اگر Item 26 یک قانون بود که می‌گفت:

> **از Raw Type استفاده نکنید.**

Item 27 یک گام جلوتر می‌رود و می‌گوید:

> **هیچ Unchecked Warningای را نادیده نگیرید.**

این یکی از مهم‌ترین توصیه‌های Joshua Bloch برای نوشتن کد Production-Grade است.

---

## فهرست مطالب

- [دیدگاه معماری (Architectural View)](#architectural-view)
- [چرا Warningهای Unchecked ایجاد می‌شوند؟](#why-unchecked)
- [انواع Warningهای Unchecked](#types)
- [مثال اول کتاب](#example1)
- [Diamond Operator چیست؟](#diamond-operator)
- [اصل طلایی این Item](#golden-rule)
- [چرا نباید Warningها را نادیده گرفت؟](#why-not-ignore)
- [اگر نتوانیم Warning را حذف کنیم چه؟](#cannot-remove)
- [اصل مهم](#important-principle)
- [چرا Suppress کردن خطرناک است؟](#danger-suppress)
- [کوچک‌ترین Scope ممکن](#smallest-scope)
- [مثال ArrayList](#arraylist-example)
- [همیشه Comment بنویسید](#always-comment)
- [مثال Production](#production-example)
- [ارتباط Item 27 با Item 26](#connection-item26)
- [ارتباط با CI/CD](#ci-cd)
- [ارتباط با Type Erasure](#type-erasure)
- [Decision Framework](#decision-framework)
- [مقایسه گزینه‌ها](#comparison)
- [نکات مهم برای پروژه‌های Enterprise](#enterprise-tips)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## دیدگاه معماری (Architectural View)

در Java، کامپایلر اولین لایه دفاعی در برابر خطاهاست.
<div dir="ltr">

```
Source Code
│
▼
Java Compiler
│
▼
Unchecked Warnings
│
▼
Potential Runtime Failure
```
</div>
پیام اصلی این Item این است:

> **هر Warning یک قرارداد شکسته بین شما و سیستم نوع (Type System) است.**

تا زمانی که Warning وجود دارد، کامپایلر نمی‌تواند تضمین کند که برنامه از نظر Type ایمن (Type Safe) است.

[بازگشت به بالا](#top)

---

<a id="why-unchecked"></a>
## چرا Warningهای Unchecked ایجاد می‌شوند؟

بیشتر Warningهای این Item به دلیل **Type Erasure** ایجاد می‌شوند.

به عنوان مثال:

<div dir="ltr">

```java
List<String> names = (List<String>) object;
```
</div>

کامپایلر نمی‌تواند در زمان اجرا ثابت کند که `object` واقعاً `List<String>` است، زیرا در Runtime، اطلاعات Generic حذف شده است.

[بازگشت به بالا](#top)

---

<a id="types"></a>
## انواع Warningهای Unchecked

Bloch چند نوع Warning را نام می‌برد:

| Warning | علت |
|---------|-----|
| `unchecked cast` | تبدیل نوع Generic که قابل اثبات نیست |
| `unchecked conversion` | تبدیل Raw Type به Generic |
| `unchecked method invocation` | فراخوانی متد Generic با Raw Type |
| `unchecked varargs` | استفاده از Generic همراه Varargs |

همه این‌ها یک معنی دارند:

> **کامپایلر دیگر نمی‌تواند Type Safety را تضمین کند.**

[بازگشت به بالا](#top)

---

<a id="example1"></a>
## مثال اول کتاب

اشتباه رایج:

<div dir="ltr">

```java
Set<Lark> exaltation = new HashSet();
```
</div>

Warning:
<div dir="ltr">

```
unchecked conversion
```
</div>
چرا؟ سمت راست `new HashSet()` یک Raw Type است. کامپایلر می‌گوید: `HashSet of what?` مشخص نیست.

نسخه صحیح:

<div dir="ltr">

```java
Set<Lark> exaltation = new HashSet<>();
```
</div>

Diamond Operator (`<>`) باعث می‌شود Compiler از طریق Type Inference متوجه شود که `HashSet<Lark>` است. بدون Warning.

[بازگشت به بالا](#top)

---

<a id="diamond-operator"></a>
## Diamond Operator چیست؟

از Java 7 به بعد، نیازی نیست بنویسید:

<div dir="ltr">

```java
new HashSet<Lark>();
```
</div>

کافی است:

<div dir="ltr">

```java
new HashSet<>();
```
</div>

کامپایلر خودش نتیجه می‌گیرد:

<div dir="ltr">

```
Left Side: Set<Lark> → Right Side: HashSet<Lark>
```
</div>
[بازگشت به بالا](#top)

---

<a id="golden-rule"></a>
## اصل طلایی این Item

Joshua Bloch می‌گوید:

> **تمام Warningهایی را که می‌توانید حذف کنید.**

نه "بیشتر Warningها"، بلکه **همه Warningها**.

چرا؟ زیرا اگر پروژه بدون هیچ Warningی کامپایل شود:
<div dir="ltr">

```
Compiler → Type Safety Guaranteed
```
</div>
(البته تا زمانی که Warningها را به‌طور نادرست Suppress نکرده باشید.)

[بازگشت به بالا](#top)

---

<a id="why-not-ignore"></a>
## چرا نباید Warningها را نادیده گرفت؟

فرض کنید پروژه‌ای دارید با `132 Warnings`. توسعه‌دهنده‌ها به Warningها عادت می‌کنند. حالا یک Warning جدید ایجاد می‌شود: `Unchecked cast`، اما میان 132 Warning دیگر گم می‌شود.

نتیجه:
<div dir="ltr">

```
Bug → Production → ClassCastException
```
</div>
بنابراین:

> **Warningهای قدیمی باعث پنهان شدن Warningهای واقعی می‌شوند.**

[بازگشت به بالا](#top)

---

<a id="cannot-remove"></a>
## اگر نتوانیم Warning را حذف کنیم چه؟

اینجا Bloch یک استثناء معرفی می‌کند.

اگر:

- حذف Warning ممکن نیست
- و شما **به‌صورت منطقی ثابت کنید** که کد Type Safe است

آنگاه از `@SuppressWarnings("unchecked")` استفاده کنید. اما فقط در این حالت.

[بازگشت به بالا](#top)

---

<a id="important-principle"></a>
## اصل مهم

Bloch می‌گوید:
<div dir="ltr">

```
Cannot remove warning
          │
          ▼
Can prove type safety?
          │
     ┌────┴────┐
     │         │
    Yes       No
     │         │
     ▼         ▼
Suppress   Redesign
```
</div>
اگر نتوانید Type Safety را اثبات کنید، Suppress کردن یعنی:

> **خاموش کردن آژیر آتش بدون خاموش کردن آتش**

[بازگشت به بالا](#top)

---

<a id="danger-suppress"></a>
## چرا Suppress کردن خطرناک است؟

مثال:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
public void process() { }
```
</div>

اگر این Annotation بی‌دلیل باشد، کامپایلر دیگر Warning نمی‌دهد. اما Runtime `ClassCastException` هنوز رخ می‌دهد. شما فقط هشدار را حذف کرده‌اید، نه مشکل را.

[بازگشت به بالا](#top)

---

<a id="smallest-scope"></a>
## کوچک‌ترین Scope ممکن

این یکی از مهم‌ترین توصیه‌های Item 27 است.

Bloch می‌گوید:

> همیشه Annotation را روی کوچک‌ترین Scope ممکن قرار دهید.

❌ **بد:**

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
public class UserService { }
```
</div>

اینجا تمام کلاس Warningها را مخفی می‌کند.

⚠️ **کمتر بد:**

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
public void save() { }
```
</div>

اما هنوز Scope بزرگ است.

✅ **بهترین حالت:**

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
List<String> list = ...;
```
</div>

یا:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
T[] result = ...;
```
</div>

[بازگشت به بالا](#top)

---

<a id="arraylist-example"></a>
## مثال ArrayList

در JDK:

<div dir="ltr">

```java
return (T[]) Arrays.copyOf(...);
```
</div>

Warning: `Unchecked Cast`

نمی‌توان Annotation را روی `return` قرار داد چون `return` Declaration نیست.

راه‌حل:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
T[] result = (T[]) Arrays.copyOf(...);
return result;
```
</div>

Scope فقط یک Variable است.

### چرا این Cast امن است؟

در مثال کتاب:

<div dir="ltr">

```java
Arrays.copyOf(elements, size, a.getClass())
```
</div>

آرایه جدید از همان نوع آرایه ورودی ساخته می‌شود. اگر `a` → `String[]` باشد، نتیجه نیز `String[]` خواهد بود. پس Cast واقعاً امن است.

[بازگشت به بالا](#top)

---

<a id="always-comment"></a>
## همیشه Comment بنویسید

Bloch توصیه می‌کند:

هر بار که می‌نویسید `@SuppressWarnings("unchecked")`، حتماً دلیلش را بنویسید.

مثلاً:

<div dir="ltr">

```java
// Safe because Arrays.copyOf returns
// an array with the same runtime type
// as the input array.

@SuppressWarnings("unchecked")
T[] result = (T[]) Arrays.copyOf(...);
```
</div>

چرا؟ فرض کنید دو سال بعد یک توسعه‌دهنده این کد را تغییر دهد. Comment توضیح می‌دهد: "این Warning عمداً Suppress شده زیرا..." اگر نتوانید چنین Commentی بنویسید، احتمالاً Cast واقعاً امن نیست.

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## مثال Production

فرض کنید:

<div dir="ltr">

```java
public class JsonDeserializer<T> {
    public T deserialize(Object value) {
        return (T) value;
    }
}
```
</div>

کامپایلر `Unchecked Cast` می‌دهد.

آیا Suppress کنیم؟ خیر. ابتدا باید ثابت کنیم `value` واقعاً از نوع `T` است. اگر اثبات نشود، Suppress کردن اشتباه است.

[بازگشت به بالا](#top)

---

<a id="connection-item26"></a>
## ارتباط Item 27 با Item 26

- **Item 26** گفت: `Raw Types → Unchecked Warning`
- **Item 27** می‌گوید: `Unchecked Warning → حذفش کن`

بنابراین این دو Item مکمل هم هستند.

[بازگشت به بالا](#top)

---

<a id="ci-cd"></a>
## ارتباط با CI/CD

در پروژه‌های Enterprise معمولاً Build به گونه‌ای تنظیم می‌شود که Warningها را خطا در نظر بگیرد.

مثلاً در Maven:
<div dir="ltr">

```xml
-Xlint:unchecked
-Werror
```
</div>
نتیجه: `Unchecked Warning → Build Failed`

مزایا:

- جلوگیری از ورود کد ناامن
- حفظ کیفیت کد
- جلوگیری از افزایش Technical Debt

[بازگشت به بالا](#top)

---

<a id="type-erasure"></a>
## ارتباط با Type Erasure

تقریباً تمام Warningهای این فصل از یک علت مشترک ناشی می‌شوند:

<div dir="ltr">

```
Compile Time → List<String> → Type Erasure → Runtime → List
```
</div>
کامپایلر هر جا نتواند این فاصله را با اطمینان پر کند، Warning می‌دهد.

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## Decision Framework

هر بار که Warning دیدید، این مسیر را طی کنید:

```
Unchecked Warning
        │
        ▼
آیا قابل حذف است؟
        │
   ┌────┴────┐
   │         │
  بله       خیر
   │         │
   ▼         ▼
حذف کن   آیا Type Safety قابل اثبات است؟
              │
         ┌────┴────┐
         │         │
        بله       خیر
         │         │
         ▼         ▼
@SuppressWarnings   بازطراحی کد
(کوچک‌ترین Scope)
+ Comment
```

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## مقایسه گزینه‌ها

| رویکرد | Type Safety | Maintainability | Risk |
|--------|-------------|-----------------|------|
| حذف Warning | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | بسیار کم |
| `@SuppressWarnings` با اثبات و Comment | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | کم |
| `@SuppressWarnings` بدون اثبات | ⭐⭐☆☆☆ | ⭐☆☆☆☆ | زیاد |
| نادیده گرفتن Warning | ⭐☆☆☆☆ | ⭐☆☆☆☆ | بسیار زیاد |

[بازگشت به بالا](#top)

---

<a id="enterprise-tips"></a>
## نکات مهم برای پروژه‌های Enterprise

- Build را طوری تنظیم کنید که Warningهای جدید قابل مشاهده باشند.
- از `-Xlint:unchecked` در فرآیند Build استفاده کنید تا همه Warningهای مرتبط با Generics گزارش شوند.
- هر `@SuppressWarnings("unchecked")` را به‌عنوان یک **استثناء مستندشده** در نظر بگیرید، نه یک راه‌حل عادی.
- در Code Review، هر Annotation از این نوع باید همراه با توضیح فنی بررسی شود.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

قانون اصلی این Item:

> **هر Warning مربوط به Generics را جدی بگیرید.**

ترتیب اولویت‌ها:

| اولویت | اقدام |
|--------|-------|
| ۱ | Warning را حذف کنید |
| ۲ | اگر حذف ممکن نیست، Type Safety را اثبات کنید |
| ۳ | فقط در این صورت، با `@SuppressWarnings("unchecked")` و در کوچک‌ترین Scope ممکن Warning را Suppress کنید |
| ۴ | همیشه دلیل Suppress کردن را در یک Comment ثبت کنید |

### نکته معماری

از دید یک Software Architect، این Item فقط درباره یک Annotation نیست؛ بلکه درباره **اعتماد به سیستم نوع جاوا (Type System)** است. هر Warning حذف‌نشده نشان می‌دهد بخشی از این قرارداد به‌طور کامل توسط کامپایلر قابل اثبات نیست. هدف، این است که تا حد امکان تمام این قراردادها در زمان کامپایل بررسی شوند، نه اینکه مشکلات در زمان اجرای سیستم آشکار شوند.

---

[بازگشت به بالا](#top)

</div>
```