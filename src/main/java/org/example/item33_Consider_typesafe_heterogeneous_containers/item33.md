<div dir="rtl">

<a id="top"></a>

# آیتم ۳۳: از Containerهای ناهمگنِ Type-Safe استفاده کنید

## (Consider typesafe heterogeneous containers)

این Item یکی از مهم‌ترین مباحث Generics در Effective Java است، چون یک قدم فراتر از `List<T>` و `Map<K,V>` می‌رود و نشان می‌دهد:

> **اگر تعداد Typeهایی که Container باید نگهداری کند از قبل مشخص نباشد، به‌جای Parameterize کردن خود Container، می‌توانیم Key را Parameterize کنیم.**

این ایده در طراحی APIهای حرفه‌ای، Reflection، Annotationها، Metadata، Configuration و حتی بعضی الگوهای معماری بسیار مهم است.

---

## فهرست مطالب

- [۱. مسئله‌ای که Item 33 می‌خواهد حل کند](#problem)
- [۲. ایده اصلی: Type را روی Key قرار بده](#core-idea)
- [۳. مثال اصلی کتاب: Favorites](#favorites)
- [۴. چرا `Class<T>` کلید مناسبی است؟](#class-as-key)
- [۵. Type Token چیست؟](#type-token)
- [۶. API نهایی Favorites](#favorites-api)
- [۷. پیاده‌سازی](#implementation)
- [۸. چرا Map نمی‌تواند Relationship بین Key و Value را Express کند؟](#map-limitation)
- [۹. `Class.cast()` قلب Item 33 است](#class-cast)
- [۱۰. چرا از `(T)` استفاده نکردیم؟](#why-not-cast)
- [۱۱. تفاوت این دو رویکرد](#comparison-cast)
- [۱۲. چرا `Class<?>` در Map مشکل ایجاد نمی‌کند؟](#class-wildcard)
- [۱۳. جریان کامل Type Safety](#flow)
- [۱۴. یک نکته ظریف: `putFavorite` فعلی Runtime-Safe نیست](#runtime-unsafe)
- [۱۵. چطور Runtime Type Safety را هم تضمین کنیم؟](#runtime-safe)
- [۱۶. ارتباط با `Collections.checkedList`](#checkedlist)
- [۱۷. محدودیت مهم: Non-Reifiable Types](#non-reifiable-limitation)
- [۱۸. چرا اگر `List<String>.class` وجود داشت خطرناک بود؟](#why-list-class)
- [۱۹. اینجا Type Erasure را دوباره ببین](#erasure-again)
- [۲۰. Bounded Type Token](#bounded-token)
- [۲۱. چرا این API بسیار قدرتمند است؟](#why-powerful)
- [۲۲. `asSubclass()` چیست؟](#assubclass)
- [۲۳. `asSubclass()` از نظر مفهومی](#assubclass-concept)
- [۲۴. چرا Item 33 برای معماری مهم است؟](#architectural-importance)
- [۲۵. مشکل DI با Generic Types](#di-problem)
- [۲۶. نسخه Production-Oriented برای Type Token](#production-token)
- [۲۷. اما یک نکته معماری مهم](#architectural-note)
- [۲۸. تفاوت این Pattern با `Map<Class<?>, Object>` ساده](#pattern-difference)
- [۲۹. مقایسه با Containerهای معمولی](#comparison-containers)
- [۳۰. Custom Type Token](#custom-token)
- [۳۱. این ایده را در یک Database Row تصور کن](#database-row)
- [۳۲. چرا این برای طراحی API بسیار مهم است؟](#api-design-importance)
- [۳۳. ارتباط Item 33 با Type-Safe API Design](#type-safe-api)
- [۳۴. Anti-Pattern رایج](#anti-pattern)
- [۳۵. Better API](#better-api)
- [۳۶. جمع‌بندی معماری Item 33](#architectural-summary)
- [۳۷. مهم‌ترین نکات Item 33](#key-takeaways)
- [ارتباط با Itemهای قبلی](#connection-other)

[بازگشت به بالا](#top)

---

<a id="problem"></a>
## ۱. مسئله‌ای که Item 33 می‌خواهد حل کند

معمولاً Generics را روی خود Container قرار می‌دهیم:

<div dir="ltr">

```java
Set<String>
```
</div>

یعنی این Container فقط `String` نگه می‌دارد.

یا:

<div dir="ltr">

```java
Map<String, Integer>
```
</div>

یعنی:

<div dir="ltr">

```text
Key   → String
Value → Integer
```
</div>

در این مدل، تعداد Type Parameterها از قبل مشخص است.

مثلاً:

<div dir="ltr">

```java
class Container<T> { ... }
```
</div>

فقط یک Type دارد.

یا:

<div dir="ltr">

```java
class Container<K, V> { ... }
```
</div>

دو Type دارد.

اما فرض کنید چنین چیزی می‌خواهیم:

<div dir="ltr">

```text
Container
 ├── String    → "Java"
 ├── Integer   → 123
 ├── Boolean   → true
 ├── Class     → SomeClass.class
 └── BigDecimal → 12.5
```
</div>

یعنی Container باید بتواند **تعداد نامحدودی Type مختلف** را نگه دارد.

اینجا `Map<K,V>` معمولی جواب خوبی نمی‌دهد.

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ۲. ایده اصلی: Type را روی Key قرار بده

راه‌حل Item 33 بسیار زیباست:

به‌جای:

<div dir="ltr">

```java
Container<T>
```
</div>

از:

<div dir="ltr">

```java
Container<Key<T>>
```
</div>

استفاده کن.

یعنی Type Parameter را از Container بردار و روی Key قرار بده.

به زبان ساده:

<div dir="ltr">

```
Traditional Generic Container: Container<T> → T = String

Typesafe Heterogeneous Container:
Container
   ├── Key<String>  → String
   ├── Key<Integer> → Integer
   ├── Key<Boolean> → Boolean
   └── Key<Class>   → Class
```
</div>
پس هر Key می‌تواند Type خودش را داشته باشد.

این دقیقاً دلیل نام **Typesafe Heterogeneous Container** است:

- **Typesafe** → Type safety حفظ می‌شود.
- **Heterogeneous** → Container می‌تواند Objectهایی با Typeهای مختلف داشته باشد.

[بازگشت به بالا](#top)

---

<a id="favorites"></a>
## ۳. مثال اصلی کتاب: Favorites

کتاب یک Container به نام `Favorites` می‌سازد.

هدف:

<div dir="ltr">

```java
Favorites favorites = new Favorites();
```
</div>

و بتوانیم:

<div dir="ltr">

```java
favorites.putFavorite(String.class, "Java");
favorites.putFavorite(Integer.class, 42);
favorites.putFavorite(Class.class, Favorites.class);
```
</div>

بعد:

<div dir="ltr">

```java
String s = favorites.getFavorite(String.class);
Integer i = favorites.getFavorite(Integer.class);
Class<?> c = favorites.getFavorite(Class.class);
```
</div>

دقت کن که هیچ Cast دستی نداریم.

[بازگشت به بالا](#top)

---

<a id="class-as-key"></a>
## ۴. چرا `Class<T>` کلید مناسبی است؟

نکته بسیار مهم اینجاست.

احتمالاً می‌دانی:

<div dir="ltr">

```java
Class
```
</div>

یک Generic Class است:

<div dir="ltr">

```java
public final class Class<T>
```
</div>

بنابراین:

<div dir="ltr">

```java
String.class
```
</div>

از نظر Generic Type این است: `Class<String>`

و:

<div dir="ltr">

```java
Integer.class
```
</div>

است: `Class<Integer>`

و:

<div dir="ltr">

```java
Class.class
```
</div>

عملاً یک Type Token برای `Class` است.

بنابراین `String.class` فقط یک Object معمولی نیست. این Object همزمان Type Information را نیز منتقل می‌کند.

به چنین چیزی می‌گوییم **Type Token**.

[بازگشت به بالا](#top)

---

<a id="type-token"></a>
## ۵. Type Token چیست؟

فرض کن:

<div dir="ltr">

```java
Class<String> token = String.class;
```
</div>

این `token` دو نوع اطلاعات دارد:

**Compile-time:** کامپایلر می‌داند `token represents String`

**Runtime:** خود JVM نیز می‌تواند بفهمد `token represents java.lang.String`

بنابراین `Class<T>` هم Type information را در زمان Compile منتقل می‌کند و هم Runtime information در اختیار ما قرار می‌دهد.

این ویژگی برای Pattern کتاب بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="favorites-api"></a>
## ۶. API نهایی Favorites

کتاب:

<div dir="ltr">

```java
public class Favorites {
    public <T> void putFavorite(Class<T> type, T instance);
    public <T> T getFavorite(Class<T> type);
}
```
</div>

### `putFavorite`

<div dir="ltr">

```java
<T> void putFavorite(Class<T> type, T instance)
```
</div>

یعنی: "هر Type `T` که Key نماینده آن است، Value هم باید همان Type باشد."

مثلاً:

<div dir="ltr">

```java
putFavorite(String.class, "Java");
```
</div>

کامپایلر می‌گوید: `T = String` پس Signature تبدیل می‌شود به `putFavorite(Class<String>, String)` درست است.

اما:

<div dir="ltr">

```java
putFavorite(String.class, 42);
```
</div>

یعنی `Class<String>` و `Integer` و کامپایلر Reject می‌کند.

[بازگشت به بالا](#top)

---

<a id="implementation"></a>
## ۷. پیاده‌سازی

پیاده‌سازی کتاب:

<div dir="ltr">

```java
public class Favorites {
    private Map<Class<?>, Object> favorites = new HashMap<>();

    public <T> void putFavorite(Class<T> type, T instance) {
        favorites.put(Objects.requireNonNull(type), instance);
    }

    public <T> T getFavorite(Class<T> type) {
        return type.cast(favorites.get(type));
    }
}
```
</div>

در نگاه اول این قسمت ممکن است عجیب باشد:

<div dir="ltr">

```java
Map<Class<?>, Object>
```
</div>

چرا `Object`؟ چرا نه چیزی مثل `Map<Class<T>, T>`؟

[بازگشت به بالا](#top)

---

<a id="map-limitation"></a>
## ۸. چرا Map نمی‌تواند Relationship بین Key و Value را Express کند؟

ما از نظر منطقی می‌خواهیم:

<div dir="ltr">

```text
Class<String> → String
Class<Integer> → Integer
Class<Boolean> → Boolean
```
</div>

اما Java Type System نمی‌تواند چنین Relationshipای را مستقیماً در یک `Map` بیان کند. یعنی نمی‌توانیم بگوییم: "Value type = type represented by Key"

به همین دلیل داخل Map می‌شود:

<div dir="ltr">

```java
Map<Class<?>, Object>
```
</div>

یعنی:

<div dir="ltr">

```text
Class<?> → Object
```
</div>

در این نقطه Type Information را موقتاً از دست می‌دهیم.

اما اتفاق مهم در `getFavorite()` رخ می‌دهد.

[بازگشت به بالا](#top)

---

<a id="class-cast"></a>
## ۹. `Class.cast()` قلب Item 33 است

این خط را ببین:

<div dir="ltr">

```java
return type.cast(favorites.get(type));
```
</div>

فرض کن:

<div dir="ltr">

```java
String value = favorites.getFavorite(String.class);
```
</div>

در این invocation `T = String`، بنابراین `type` از نوع `Class<String>` است.

و `type.cast(...)` عملاً می‌گوید: "Object را به Typeای Cast کن که این Class Token نماینده آن است."

پس `String.class.cast(obj)` تقریباً معادل مفهومی `(String) obj` است.

اما یک تفاوت بسیار مهم وجود دارد: `Class.cast()` خودش Generic است:

<div dir="ltr">

```java
public T cast(Object obj)
```
</div>

پس اگر `Class<String>` باشد، `cast(...)` خروجی `String` خواهد بود.

[بازگشت به بالا](#top)

---

<a id="why-not-cast"></a>
## ۱۰. چرا از `(T)` استفاده نکردیم؟

ممکن بود بنویسیم:

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
public <T> T getFavorite(Class<T> type) {
    return (T) favorites.get(type);
}
```
</div>

ولی این کار یک **Unchecked Cast** است.

مشکل: `(T)` در Runtime اطلاعاتی درباره `T` ندارد. به دلیل Type Erasure، JVM نمی‌تواند واقعاً بررسی کند که `Object is T`.

اما `type.cast(object)` اطلاعات Runtime را از `Class<T>` می‌گیرد و واقعاً بررسی می‌کند.

بنابراین `type.cast(object)` راه بسیار بهتری است.

[بازگشت به بالا](#top)

---

<a id="comparison-cast"></a>
## ۱۱. تفاوت این دو رویکرد

| رویکرد | Runtime Type Check | Unchecked Warning | Type Safe |
|--------|-------------------:|------------------:|----------:|
| `(T) object` | ❌ | معمولاً بله | ❌/مشروط |
| `type.cast(object)` | ✅ | ❌ | ✅ |

این یکی از الگوهای بسیار مهم در Java Reflection است.

[بازگشت به بالا](#top)

---

<a id="class-wildcard"></a>
## ۱۲. چرا `Class<?>` در Map مشکل ایجاد نمی‌کند؟

این:

<div dir="ltr">

```java
Map<Class<?>, Object>
```
</div>

ممکن است در نگاه اول شبیه این به نظر برسد: `Map<?, ?>`

اما تفاوت مهم است. در اینجا فقط **Key** wildcard دارد:

<div dir="ltr">

```text
Map
 ├── Key   = Class<?>
 └── Value = Object
```
</div>

پس هر Key می‌تواند یک `Class<T>` متفاوت باشد: `Class<String>`، `Class<Integer>`، `Class<Boolean>`، `Class<BigDecimal>` و این دقیقاً چیزی است که Heterogeneous بودن را ممکن می‌کند.

[بازگشت به بالا](#top)

---

<a id="flow"></a>
## ۱۳. جریان کامل Type Safety

<div dir="ltr">

```java
favorites.putFavorite(String.class, "Java");
```
</div>

کامپایلر: `T = String`، پس `Class<T> → Class<String>` و `T → String`.

پس `Class<String> → String` و داخل Map: `Class<String> → Object`.

در زمان Retrieval:

<div dir="ltr">

```java
String s = favorites.getFavorite(String.class);
```
</div>

دوباره: `T = String` و `Class<String>.cast(Object)` نتیجه `String`.

بنابراین:

<div dir="ltr">

```text
Compile-time Type Safety → Class<T> → Runtime Type Token → Class.cast() → Type-safe retrieval
```
</div>

[بازگشت به بالا](#top)

---

<a id="runtime-unsafe"></a>
## ۱۴. یک نکته ظریف: `putFavorite` فعلی Runtime-Safe نیست

کتاب یک محدودیت مهم را مطرح می‌کند.

پیاده‌سازی فعلی:

<div dir="ltr">

```java
public <T> void putFavorite(Class<T> type, T instance) {
    favorites.put(type, instance);
}
```
</div>

اگر Client کد سالم Generic بنویسد، مشکلی وجود ندارد. اما با Raw Type می‌توان Type Safety را دور زد.

مثلاً:

<div dir="ltr">

```java
Favorites favorites = new Favorites();
Class raw = String.class;
favorites.putFavorite(raw, 42);
```
</div>

این کد می‌تواند با Warning کامپایل شود.

حالا Map شامل `String.class → Integer` خواهد بود.

بعد:

<div dir="ltr">

```java
String value = favorites.getFavorite(String.class);
```
</div>

ممکن است `ClassCastException` بگیریم.

[بازگشت به بالا](#top)

---

<a id="runtime-safe"></a>
## ۱۵. چطور Runtime Type Safety را هم تضمین کنیم؟

کتاب پیشنهاد می‌کند:

<div dir="ltr">

```java
public <T> void putFavorite(Class<T> type, T instance) {
    favorites.put(type, type.cast(instance));
}
```
</div>

حالا `type.cast(instance)` بررسی می‌کند `instance instanceof type`. اگر نباشد `ClassCastException` می‌گیریم.

این الگو در APIهای Production برای Boundary Validation بسیار مفید است.

[بازگشت به بالا](#top)

---

<a id="checkedlist"></a>
## ۱۶. ارتباط با `Collections.checkedList`

کتاب این ایده را در Java Library هم نشان می‌دهد.

مثلاً:

<div dir="ltr">

```java
List<String> list = new ArrayList<>();
List<String> checked = Collections.checkedList(list, String.class);
```
</div>

اگر کسی از طریق یک مسیر unsafe تلاش کند `Integer → List<String>` اضافه کند، Runtime آن را تشخیص می‌دهد.

یعنی Java Library از همان ایده استفاده می‌کند:

<div dir="ltr">

```text
Generic compile-time checking + Class<T> runtime type token = Runtime type safety
```
</div>

[بازگشت به بالا](#top)

---

<a id="non-reifiable-limitation"></a>
## ۱۷. محدودیت مهم: Non-Reifiable Types

اینجا یکی از مهم‌ترین ارتباط‌های Item 28 و Item 33 ظاهر می‌شود.

این کار ممکن نیست:

<div dir="ltr">

```java
List<String>.class
```
</div>

چون چنین Class Objectای وجود ندارد.

در Runtime: `List<String>`، `List<Integer>` و `List<Double>` همگی به `List.class` erase می‌شوند.

بنابراین نمی‌توانیم بنویسیم:

<div dir="ltr">

```java
favorites.putFavorite(List<String>.class, List.of("Java"));
```
</div>

چنین Syntaxای اصلاً وجود ندارد.

[بازگشت به بالا](#top)

---

<a id="why-list-class"></a>
## ۱۸. چرا اگر `List<String>.class` وجود داشت خطرناک بود؟

فرض کن Java اجازه می‌داد `List<String>.class` و `List<Integer>.class`، اما هر دو در Runtime به یک Object تبدیل می‌شدند: `List.class`.

آن‌گاه Container دیگر نمی‌توانست بین `List<String>` و `List<Integer>` تفاوت Runtimeای قائل شود.

بنابراین Type Token با `Class<T>` فقط برای **Reifiable Types** مناسب است.

[بازگشت به بالا](#top)

---

<a id="erasure-again"></a>
## ۱۹. اینجا Type Erasure را دوباره ببین

Item 33 در واقع یکی از بهترین مثال‌ها برای درک Type Erasure است.

مثلاً:

<div dir="ltr">

```java
Class<String>
```
</div>

در Runtime می‌توانیم اطلاعات `String` را از خود `Class` داشته باشیم.

اما:

<div dir="ltr">

```java
List<String>
```
</div>

در Runtime: `List` است.

پس:

<div dir="ltr">

```text
Class<String> → Runtime representation retains String
List<String> → Runtime representation loses String
```
</div>

به همین دلیل `String.class` ممکن است، ولی `List<String>.class` نه.

[بازگشت به بالا](#top)

---

<a id="bounded-token"></a>
## ۲۰. Bounded Type Token

تا اینجا `Class<T>` یک **Unbounded Type Token** است.

اما گاهی می‌خواهیم بگوییم: "فقط Classهایی را قبول کن که نوعشان زیرمجموعه یک Type خاص باشد."

مثلاً: `<T extends Annotation>`

نمونه واقعی Java:

<div dir="ltr">

```java
public <T extends Annotation> T getAnnotation(Class<T> annotationType);
```
</div>

مثلاً:

<div dir="ltr">

```java
Deprecated annotation = method.getAnnotation(Deprecated.class);
```
</div>

اینجا `Deprecated.class` یک `Class<Deprecated>` است و `Deprecated extends Annotation`، پس معتبر است.

[بازگشت به بالا](#top)

---

<a id="why-powerful"></a>
## ۲۱. چرا این API بسیار قدرتمند است؟

این متد:

<div dir="ltr">

```java
<T extends Annotation> T getAnnotation(Class<T> annotationType)
```
</div>

می‌گوید:

<div dir="ltr">

```text
Input: Class<T>
Constraint: T extends Annotation
Output: T
```
</div>

پس `Deprecated.class` ورودی می‌شود `Class<Deprecated>` و خروجی دقیقاً `Deprecated` است.

بدون `Annotation`، بدون `(T)`، بدون `@SuppressWarnings`.

[بازگشت به بالا](#top)

---

<a id="assubclass"></a>
## ۲۲. `asSubclass()` چیست؟

یکی از قسمت‌های بسیار ارزشمند Item همین است:

<div dir="ltr">

```java
annotationType.asSubclass(Annotation.class)
```
</div>

فرض کن:

<div dir="ltr">

```java
Class<?> annotationType;
```
</div>

ما نمی‌دانیم این Class چه Typeای است. ولی می‌دانیم باید `Annotation` باشد.

مشکل این است که `Class<?>` را نمی‌توانیم مستقیماً به `Class<? extends Annotation>` تبدیل کنیم بدون Cast.

راه unsafe:

<div dir="ltr">

```java
(Class<? extends Annotation>) annotationType
```
</div>

است. اما این Unchecked Cast است.

راه صحیح:

<div dir="ltr">

```java
annotationType.asSubclass(Annotation.class)
```
</div>

است.

[بازگشت به بالا](#top)

---

<a id="assubclass-concept"></a>
## ۲۳. `asSubclass()` از نظر مفهومی

اگر داشته باشیم `Class<?> clazz` و بگوییم `clazz.asSubclass(SomeType.class)`، Java Runtime بررسی می‌کند که `clazz represents SomeType` یا `clazz represents subclass of SomeType`.

اگر درست باشد: `Class<? extends SomeType>` برمی‌گرداند.

اگر نباشد: `ClassCastException`.

[بازگشت به بالا](#top)

---

<a id="architectural-importance"></a>
## ۲۴. چرا Item 33 برای معماری مهم است؟

این Item فقط یک تکنیک Generics نیست. این Pattern در سیستم‌های واقعی کاربرد زیادی دارد.

### Metadata Registry

<div dir="ltr">

```text
Registry
   ├── String.class → String configuration
   ├── Duration.class → Duration configuration
   ├── DataSource.class → DataSource
   └── Executor.class → Executor
```
</div>

### Dependency Injection

مفهوم ساده‌ای شبیه:

<div dir="ltr">

```java
<T> T get(Class<T> type)
```
</div>

در بسیاری از Containerهای DI دیده می‌شود.

مثلاً:

<div dir="ltr">

```java
DataSource ds = container.get(DataSource.class);
Executor executor = container.get(Executor.class);
```
</div>

در اینجا `Class<T>` عملاً نقش Type Token را بازی می‌کند.

[بازگشت به بالا](#top)

---

<a id="di-problem"></a>
## ۲۵. مشکل DI با Generic Types

مثلاً:

<div dir="ltr">

```java
Repository<User>
Repository<Order>
```
</div>

هر دو در Runtime ممکن است `Repository.class` باشند.

بنابراین `container.get(Repository.class)` نمی‌تواند بفهمد `Repository<User>` می‌خواهی یا `Repository<Order>`.

اینجا معمولاً باید از مفاهیمی مانند `ParameterizedType`، `Type`، `TypeToken`، `Key<T>` استفاده شود.

این دقیقاً همان جایی است که Pattern Item 33 را می‌توان توسعه داد.

[بازگشت به بالا](#top)

---

<a id="production-token"></a>
## ۲۶. نسخه Production-Oriented برای Type Token

می‌توانیم یک Registry ساده داشته باشیم:

<div dir="ltr">

```java
public final class TypeRegistry {
    private final Map<Class<?>, Object> values = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, T value) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        values.put(type, type.cast(value));
    }

    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object value = values.get(type);
        if (value == null) {
            throw new NoSuchElementException(
                "No value registered for " + type.getName()
            );
        }
        return type.cast(value);
    }
}
```
</div>

استفاده:

<div dir="ltr">

```java
TypeRegistry registry = new TypeRegistry();
registry.register(String.class, "Java");
registry.register(Integer.class, 42);

String language = registry.get(String.class);
Integer number = registry.get(Integer.class);
```
</div>

[بازگشت به بالا](#top)

---

<a id="architectural-note"></a>
## ۲۷. اما یک نکته معماری مهم

این Registry:

<div dir="ltr">

```java
Map<Class<?>, Object>
```
</div>

از نظر Type System یک رابطه زیر را نمی‌تواند Express کند: `Class<T> → T`

بنابراین Type Safety ما به یک **Invariant داخلی** وابسته است:

> هر Object داخل Map باید Instance همان Typeای باشد که Key نمایندگی می‌کند.

یعنی:

<div dir="ltr">

```text
Invariant: ∀ entry: entry.value instanceof entry.key
```
</div>

این خیلی مهم است.

در واقع معماری Pattern این است:

<div dir="ltr">

```text
Compile Time → Class<T> → put(type, value) → Map<Class<?>, Object> → get(type) → Class.cast() → T
```
</div>

[بازگشت به بالا](#top)

---

<a id="pattern-difference"></a>
## ۲۸. تفاوت این Pattern با `Map<Class<?>, Object>` ساده

اگر فقط بنویسی:

<div dir="ltr">

```java
Map<Class<?>, Object> map = new HashMap<>();
map.put(String.class, 42);
```
</div>

کاملاً ممکن است.

اما `Favorites` API این Contract را enforce می‌کند:

<div dir="ltr">

```java
<T> void putFavorite(Class<T>, T)
```
</div>

بنابراین API رابطه Key/Value را در Compile Time enforce می‌کند.

و `type.cast(instance)` آن را در Runtime نیز validate می‌کند.

پس تفاوت مهم:

<div dir="ltr">

```text
Raw Map → No key/value type relationship
Favorites → Compile-time relationship + Optional runtime validation
```
</div>

[بازگشت به بالا](#top)

---

<a id="comparison-containers"></a>
## ۲۹. مقایسه با Containerهای معمولی

| ویژگی | `List<T>` | `Map<K,V>` | Heterogeneous Container |
|--------|-----------|------------|-------------------------|
| Type parameters | 1 | 2 | روی Key |
| تعداد Typeهای مختلف | محدود به T | محدود به K/V | Arbitrary |
| Heterogeneous | ❌ | محدود | ✅ |
| Compile-time safety | ✅ | ✅ | ✅ |
| Runtime Type Token | ❌ | ❌ | ✅ |
| مناسب Reflection | کم | کم | بسیار مناسب |
| مناسب Registry | متوسط | خوب | عالی |

[بازگشت به بالا](#top)

---

<a id="custom-token"></a>
## ۳۰. Custom Type Token

کتاب در انتها نکته بسیار مهمی می‌گوید:

> Type Token الزاماً نباید `Class<T>` باشد.

می‌توانیم Type Token خودمان را بسازیم.

مثلاً:

<div dir="ltr">

```java
public final class Column<T> {
    private final String name;
    private final Class<T> type;

    public Column(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    public String name() { return name; }
    public Class<T> type() { return type; }
}
```
</div>

بعد:

<div dir="ltr">

```java
Column<String> NAME = new Column<>("name", String.class);
Column<Integer> AGE = new Column<>("age", Integer.class);
```
</div>

و یک Database Row:

<div dir="ltr">

```text
DatabaseRow
    ├── Column<String>  → "Sadegh"
    ├── Column<Integer> → 35
    └── Column<Boolean> → true
```
</div>

اینجا دیگر Key فقط `Class` نیست. بلکه `Column<T>` است.

این نسخه برای Domainهای واقعی بسیار قدرتمندتر است.

[بازگشت به بالا](#top)

---

<a id="database-row"></a>
## ۳۱. این ایده را در یک Database Row تصور کن

فرض کنیم:

<div dir="ltr">

```java
DatabaseRow row;
Column<String> USERNAME;
Column<Integer> AGE;
Column<Instant> CREATED_AT;
```
</div>

می‌توانیم API داشته باشیم:

<div dir="ltr">

```java
<T> void set(Column<T> column, T value);
<T> T get(Column<T> column);
```
</div>

استفاده:

<div dir="ltr">

```java
row.set(USERNAME, "sadegh");
row.set(AGE, 35);
row.set(CREATED_AT, Instant.now());

String username = row.get(USERNAME);
Integer age = row.get(AGE);
Instant createdAt = row.get(CREATED_AT);
```
</div>

این دقیقاً تعمیم Pattern کتاب است.

[بازگشت به بالا](#top)

---

<a id="api-design-importance"></a>
## ۳۲. چرا این برای طراحی API بسیار مهم است؟

به جای API ضعیف:

<div dir="ltr">

```java
Object get(String key);
```
</div>

که مجبور می‌شوی بنویسی:

<div dir="ltr">

```java
String username = (String) row.get("username");
```
</div>

می‌توانی داشته باشی:

<div dir="ltr">

```java
String username = row.get(USERNAME);
```
</div>

### قبل

<div dir="ltr">

```text
String key → Object → Cast → Potential ClassCastException
```
</div>

### بعد

<div dir="ltr">

```text
Column<String> → T → String
```
</div>

این یک API بسیار Type-Safe‌تر است.

[بازگشت به بالا](#top)

---

<a id="type-safe-api"></a>
## ۳۳. ارتباط Item 33 با Type-Safe API Design

یکی از مهم‌ترین درس‌های Item این است:

> **Generic Type را همیشه لازم نیست روی Container قرار بدهی؛ گاهی باید آن را روی Identifier/Key قرار بدهی.**

یعنی اگر این مدل داشته باشی:

<div dir="ltr">

```java
Container<T>
```
</div>

ولی Container باید همزمان انواع زیادی را نگه دارد، فکر کن آیا می‌توانی تبدیلش کنی به:

<div dir="ltr">

```java
Container<Key<T>>
```
</div>

این تغییر می‌تواند محدودیت تعداد Type Parameterهای Container را از بین ببرد.

[بازگشت به بالا](#top)

---

<a id="anti-pattern"></a>
## ۳۴. Anti-Pattern رایج

یک API ضعیف:

<div dir="ltr">

```java
public Object get(String key);
```
</div>

استفاده:

<div dir="ltr">

```java
String host = (String) config.get("host");
Integer port = (Integer) config.get("port");
Duration timeout = (Duration) config.get("timeout");
```
</div>

مشکلات:

- Runtime Cast
- امکان `ClassCastException`
- typo در String key
- نبود Compile-time checking
- Refactoring ضعیف
- IDE support ضعیف

[بازگشت به بالا](#top)

---

<a id="better-api"></a>
## ۳۵. Better API

می‌توانیم Type-Safe Keys داشته باشیم:

<div dir="ltr">

```java
final class ConfigKey<T> {
    private final String name;
    ConfigKey(String name) { this.name = name; }
}
```
</div>

بعد:

<div dir="ltr">

```java
static final ConfigKey<String> HOST = new ConfigKey<>("host");
static final ConfigKey<Integer> PORT = new ConfigKey<>("port");
static final ConfigKey<Duration> TIMEOUT = new ConfigKey<>("timeout");
```
</div>

و:

<div dir="ltr">

```java
<T> void put(ConfigKey<T> key, T value);
<T> T get(ConfigKey<T> key);
```
</div>

حالا:

<div dir="ltr">

```java
config.put(PORT, 8080);          // ✅ درست است
config.put(PORT, "8080");        // ❌ Compile Error
```
</div>

این دقیقاً روح Item 33 است.

[بازگشت به بالا](#top)

---

<a id="architectural-summary"></a>
## ۳۶. جمع‌بندی معماری Item 33

```
                 Traditional Generics
                        │
                        ▼
                  Container<T>
                        │
              fixed type parameter
                        │
                        ▼
              Limited heterogeneity


              Typesafe Heterogeneous
                        │
                        ▼
                  Container
                        │
                        ▼
                     Key<T>
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
           Key<String> Key<Integer> Key<Boolean>
              │         │         │
              ▼         ▼         ▼
           String    Integer    Boolean
```

و در Java:

<div dir="ltr">

```java
<T> void put(Key<T> key, T value);
<T> T get(Key<T> key);
```
</div>

[بازگشت به بالا](#top)

---

<a id="key-takeaways"></a>
## ۳۷. مهم‌ترین نکات Item 33

| Rule | توضیح |
|------|-------|
| **Rule 1** | اگر Container باید Objectهای Typeهای مختلف را نگه دارد، به Parameterize کردن Container اکتفا نکن. به این فکر کن: `Key<T>` |
| **Rule 2** | `Class<T>` می‌تواند یک Type Token بسیار قدرتمند باشد: `Class<String>`، `Class<Integer>`، `Class<MyService>` |
| **Rule 3** | برای تبدیل `Object` به Typeی که `Class<T>` نمایندگی می‌کند: `type.cast(object)` را به `(T) object` ترجیح بده |
| **Rule 4** | اگر Runtime Type باید زیرمجموعه Type خاصی باشد: `Class<? extends Annotation>` یا `<T extends Annotation>` |
| **Rule 5** | برای Cast کردن یک `Class<?>` به Bounded Type Token از `asSubclass()` استفاده کن، نه Unchecked Cast |
| **Rule 6** | `Class<T>` فقط برای Reifiable Types مناسب است. `String.class` ✅، `List<String>.class` ❌ |
| **Rule 7** | وقتی API تو `Object get(String key)` دارد، به این فکر کن که آیا می‌توان آن را به `<T> T get(Key<T> key)` تبدیل کرد |

[بازگشت به بالا](#top)

---

<a id="connection-other"></a>
## ارتباط با Itemهای قبلی

```
Item 26 → Raw Types
   │
   ▼
Item 28 → Erasure / Non-Reifiable Types
   │
   ▼
Item 30 → Generic Methods / Bounded Type Parameters
   │
   ▼
Item 31 → Bounded Wildcards
   │
   ▼
Item 33 → Type-Safe Heterogeneous Containers
```

به‌خصوص **Item 28 + Item 33** را باید کنار هم خیلی خوب یاد بگیری؛ چون بدون فهم `Type Erasure`، دلیل وجود `Class<T>`، `Class.cast()` و محدودیت `List<String>.class` کاملاً روشن نمی‌شود.

---

[بازگشت به بالا](#top)

</div>
