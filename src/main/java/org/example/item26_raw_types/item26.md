<div dir="rtl">

<a id="top"></a>

# آیتم ۲۶: از Raw Type ها استفاده نکنید

## (Don't use raw types)

این Item یکی از مهم‌ترین بخش‌های فصل **Generics** در کتاب Effective Java است.
Joshua Bloch در اینجا یک قانون ساده ولی بسیار مهم ارائه می‌کند:

> **هرگز از Generic Type بدون Type Parameter استفاده نکنید.**

یعنی:

❌ نادرست:

<div dir="ltr">

```java
List list;
Set set;
Map map;
```
</div>

✅ درست:

<div dir="ltr">

```java
List<String> names;
Set<User> users;
Map<String, User> userMap;
```
</div>

---

## فهرست مطالب

- [Generic Type چیست؟](#generic-type)
- [Parameterized Type چیست؟](#parameterized-type)
- [Raw Type چیست؟](#raw-type)
- [چرا Raw Type وجود دارد؟](#why-raw-exists)
- [مشکل اصلی Raw Type چیست؟](#main-problem)
- [نسخه Generic](#generic-version)
- [یکی از مهم‌ترین اصول Effective Java](#key-principle)
- [تفاوت مهم: List vs List\<Object> vs List\<?>](#difference)
- [Covariance در Generics](#covariance)
- [Unbounded Wildcard Type](#wildcard)
- [مثال واقعی Production](#production-example)
- [مثال unsafeAdd کتاب](#unsafeadd)
- [Exceptionهای مجاز استفاده از Raw Type](#exceptions)
- [Type Erasure چیست؟](#type-erasure)
- [مقایسه نهایی](#comparison)
- [Decision Framework](#decision-framework)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="generic-type"></a>
## ۱. Generic Type چیست؟

قبل از Java 5، Collectionها هیچ اطلاعی از نوع داده داخل خودشان نداشتند.

مثلاً:

<div dir="ltr">

```java
List names = new ArrayList();

names.add("Ali");
names.add(10);
names.add(new User());
```
</div>

کامپایلر هیچ اطلاعی ندارد که این List قرار است چه چیزی نگه دارد.

Java 5 قابلیت Generics را اضافه کرد:

<div dir="ltr">

```java
List<String> names = new ArrayList<>();

names.add("Ali");
names.add(10);   // Compile Error
```
</div>

حالا:

```
Compiler → این List فقط String قبول می‌کند
```

کلاسی که Type Parameter دارد:

<div dir="ltr">

```java
public interface List<E> { }
```
</div>

اینجا `E` یک Type Parameter است. بنابراین `List<E>` یک Generic Type است.

مثال‌های دیگر:

<div dir="ltr">

```java
class Box<T> { }
Map<K,V>
```
</div>

[بازگشت به بالا](#top)

---

<a id="parameterized-type"></a>
## ۲. Parameterized Type چیست؟

وقتی مقدار واقعی Type Parameter را مشخص کنیم:

<div dir="ltr">

```java
List<String>
```
</div>

اینجا:

- Generic Type: `List<E>`
- Parameter: `String`

نتیجه: `List<String>` یک Parameterized Type است.

مثال:

<div dir="ltr">

```java
Map<String, User>  // K = String, V = User
```
</div>

[بازگشت به بالا](#top)

---

<a id="raw-type"></a>
## ۳. Raw Type چیست؟

Raw Type یعنی Generic Type بدون مشخص کردن Type Parameter:

<div dir="ltr">

```java
List
```
</div>

به جای:

<div dir="ltr">

```java
List<String>
```
</div>

یعنی `List<E>` تبدیل می‌شود به `List` و اطلاعات Generic حذف می‌شود.

[بازگشت به بالا](#top)

---

<a id="why-raw-exists"></a>
## ۴. چرا Raw Type وجود دارد؟

سؤال مهم: اگر بد است، چرا Java اجازه می‌دهد؟

پاسخ: **Backward Compatibility**

قبل از Java 5:

<div dir="ltr">

```java
List list = new ArrayList();
```
</div>

کد کاملاً معتبر بود. اگر Java 5 این را ممنوع می‌کرد، میلیون‌ها خط کد قدیمی خراب می‌شد.

بنابراین Java تصمیم گرفت:
<div dir="ltr">

```
Old Code → Raw Types → New Generic Code
```
</div>
را با هم سازگار کند.

[بازگشت به بالا](#top)

---

<a id="main-problem"></a>
## ۵. مشکل اصلی Raw Type چیست؟

بیایید مثال کتاب را بررسی کنیم.

فرض:

<div dir="ltr">

```java
// Collection of stamps
private final Collection stamps = new ArrayList();
```
</div>

کامپایلر نمی‌داند `stamps contains what?`

برنامه‌نویس در ذهن خود می‌گوید: "این Collection فقط Stamp دارد" اما کامپایلر این Comment را نمی‌فهمد.

بنابراین:

<div dir="ltr">

```java
stamps.add(new Stamp());
stamps.add(new Coin());
```
</div>

هر دو قبول می‌شوند.

بعداً:

<div dir="ltr">

```java
Iterator i = stamps.iterator();
while (i.hasNext()) {
    Stamp stamp = (Stamp) i.next();
}
```
</div>

در اینجا `Coin` → Cast to Stamp → `ClassCastException` رخ می‌دهد.

**مشکل اصلی:** خطا هنگام Insert اتفاق افتاده ولی هنگام Read مشخص شده است.

```
Bug Creation → (زمان طولانی) → Bug Detection
```

این بدترین نوع Bug است.

[بازگشت به بالا](#top)

---

<a id="generic-version"></a>
## ۶. نسخه Generic

حالا:

<div dir="ltr">

```java
private final Collection<Stamp> stamps;
```
</div>

کامپایلر می‌داند: `Collection → Only Stamp allowed`

پس:

<div dir="ltr">

```java
stamps.add(new Coin());
```
</div>

خطا: `Coin cannot be converted to Stamp`

**مزیت بزرگ:** خطا در **Compile Time**، نه **Runtime**.

[بازگشت به بالا](#top)

---

<a id="key-principle"></a>
## ۷. یکی از مهم‌ترین اصول Effective Java

Bloch تقریباً در تمام کتاب تکرار می‌کند:

> هرچه زودتر خطا را پیدا کنید بهتر است.

اولویت:
<div dir="ltr">

```
Compile Time → Test Time → Production Runtime
```
</div>
Raw Type شما را از مرحله اول به مرحله سوم پرت می‌کند.

[بازگشت به بالا](#top)

---

<a id="difference"></a>
## ۸. تفاوت مهم: List vs List\<Object> vs List\<?>

### حالت اول: `List` (Raw Type)

معنی: "من Generic System را خاموش کردم" — کاملاً unsafe.

### حالت دوم: `List<Object>` (Parameterized Type)

معنی: "این List هر Object ای را قبول می‌کند"

<div dir="ltr">

```java
List<Object> list = new ArrayList<>();
list.add("Ali");
list.add(10);
list.add(new User());  // کاملاً صحیح است
```
</div>

اما:

<div dir="ltr">

```java
List<String> strings = new ArrayList<>();
List<Object> objects = strings;  // ❌ Compile Error
```
</div>

چرا؟ چون `List<String>` فقط String دارد، ولی `List<Object>` اجازه می‌دهد `objects.add(new Integer(10))` که باعث خراب شدن List اصلی می‌شود.

[بازگشت به بالا](#top)

---

<a id="covariance"></a>
## ۹. Covariance در Generics

این اشتباه رایج است:

<div dir="ltr">

```java
List<String> → List<Object>
```
</div>

در Java:
<div dir="ltr">

```
Generic Types are invariant
```
</div>

یعنی `List<String> != List<Object>`

اگرچه:

<div dir="ltr">

```java
String extends Object
```
</div>

درست است، ولی:

<div dir="ltr">

```java
List<String> → List<Object>
```
</div>

**X** (غیرمجاز)

[بازگشت به بالا](#top)

---

<a id="wildcard"></a>
## ۱۰. Unbounded Wildcard Type

اینجا Bloch راه‌حل را معرفی می‌کند:

<div dir="ltr">

```java
Set<?>
```
</div>

یعنی: Set of some unknown type

`Set<String>`، `Set<Integer>`، `Set<User>` همگی `Set<?>` هستند.

### تفاوت مهم:

**Raw Type (`Set`):** می‌گوید هر چیزی بریز داخل من.

**Wildcard (`Set<?>`):** می‌گوید من نمی‌دانم نوع چیست، ولی اجازه خراب کردن Type Safety را نمی‌دهم.

مثال:

<div dir="ltr">

```java
static void print(Set<?> set) { }
```
</div>

می‌تواند `Set<String>`، `Set<Integer>`، `Set<User>` بگیرد.

اما:

<div dir="ltr">

```java
set.add("hello");  // ❌ Compile Error
```
</div>

چون نمی‌دانیم `?` = String؟ Integer؟ User؟

تنها چیزی که اجازه دارد:

<div dir="ltr">

```java
set.add(null);
```
</div>

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## ۱۱. مثال واقعی Production

فرض کنید API دارید:

<div dir="ltr">

```java
public void process(List list)  // ❌
```
</div>

مشکل: هر چیزی می‌تواند وارد شود.

بهتر:

- اگر فقط خواندن: `public void process(List<?> list)`
- اگر فقط String: `public void process(List<String> list)`
- اگر Object لازم دارید: `public void process(List<Object> list)`

[بازگشت به بالا](#top)

---

<a id="unsafeadd"></a>
## ۱۲. مثال unsafeAdd کتاب

<div dir="ltr">

```java
public static void main(String[] args) {
    List<String> strings = new ArrayList<>();
    unsafeAdd(strings, 42);
    String s = strings.get(0);
}

static void unsafeAdd(List list, Object o) {
    list.add(o);
}
```
</div>

**چرا خراب می‌شود؟**

1. `List<String>` ساخته شد.
2. Raw Type وارد شد: `List` — Generic Information از بین رفت.
3. Integer وارد List شد.
4. Compiler: `String s = strings.get(0);` کدی شبیه `String s = (String) strings.get(0);` تولید می‌کند.
5. `Integer != String` → `ClassCastException`

[بازگشت به بالا](#top)

---

<a id="exceptions"></a>
## ۱۳. Exceptionهای مجاز استفاده از Raw Type

Bloch دو استثناء ذکر می‌کند:

### Exception 1: Class Literal

❌ غلط:

<div dir="ltr">

```java
List<String>.class
```
</div>

✅ درست:

<div dir="ltr">

```java
List.class
```
</div>

چون Runtime اصلاً Generic Information ندارد (به دلیل Type Erasure).

### Exception 2: instanceof

❌ غلط:

<div dir="ltr">

```java
if (obj instanceof List<String>)
```
</div>

چون Runtime نمی‌داند String چیست.

✅ درست:

<div dir="ltr">

```java
if (obj instanceof List) {
    List<?> list = (List<?>) obj;
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="type-erasure"></a>
## ۱۴. Type Erasure چیست؟

Java Generics در Runtime وجود ندارند.

مثلاً `List<String>` در Runtime تبدیل می‌شود به `List`.

یعنی:

- Compile Time: `List<String>`
- Runtime: `List`

به همین دلیل `obj instanceof List<String>` غیرممکن است.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۱۵. مقایسه نهایی

| Type | مثال | Type Safety | کاربرد |
|------|------|-------------|--------|
| Generic Type | `List<E>` | ندارد تا مقداردهی شود | تعریف کلاس |
| Parameterized Type | `List<String>` | ✅ کامل | استفاده معمول |
| Raw Type | `List` | ❌ خطرناک | فقط Legacy |
| Wildcard | `List<?>` | ✅ امن | وقتی Type مهم نیست |
| Object Parameter | `List<Object>` | ✅ امن | هر Object |

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## ۱۶. Decision Framework

وقتی Collection می‌نویسید:

### سؤال ۱: نوع دقیق مشخص است؟
<div dir="ltr">

```
Yes → List<User>
```
</div>
### سؤال ۲: هر نوعی قابل قبول است؟
<div dir="ltr">

```
Yes → List<Object>
```
</div>
### سؤال ۳: نوع مهم نیست و فقط خواندن داریم؟
<div dir="ltr">

```
Yes → List<?>
```
</div>
### هیچ‌وقت:

<div dir="ltr">

```java
List  // ❌
```
</div>

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

قانون اصلی:

> Raw Type ها را استفاده نکنید.

دلایل:

- ✅ از دست دادن Type Safety
- ✅ انتقال خطا از Compile Time به Runtime
- ✅ ایجاد ClassCastException
- ✅ کاهش خوانایی API
- ✅ سخت شدن Refactoring

استثناها:

- `.class`
- `instanceof`

### قانون معماری

در Java مدرن:

<div dir="ltr">

```java
List<User>
```
</div>

باید انتخاب پیش‌فرض شما باشد.

اگر نوع مشخص نیست:

<div dir="ltr">

```java
List<?>
```
</div>

نه:

<div dir="ltr">

```java
List
```
</div>

### نکته Senior

از دید یک توسعه‌دهنده Senior، Item 26 در واقع درباره **Design Contract بین Compiler و Developer** است:

با Generics شما بخشی از قرارداد سیستم را به Compiler می‌دهید تا قبل از اجرای Production جلوی خطا را بگیرد. Raw Type یعنی شما عمداً این قرارداد را خاموش کرده‌اید.

---

[بازگشت به بالا](#top)

</div>
```