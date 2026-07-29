<div dir="rtl">

<a id="top"></a>

# آیتم ۲۴: کلاس‌های Member استاتیک را به کلاس‌های Member غیر استاتیک ترجیح دهید

## (Favor static member classes over nonstatic)

این Item درباره یکی از ظریف‌ترین بخش‌های Java یعنی **Nested Classes** است.

بسیاری از Developerها تفاوت بین:

<div dir="ltr">

```java
static class InnerClass
```
</div>

و:

<div dir="ltr">

```java
class InnerClass
```
</div>

را فقط یک Keyword ساده می‌بینند، اما در واقع تفاوت آن‌ها یک **تصمیم معماری و Memory Management** است.

Joshua Bloch در این Item می‌گوید:

> اگر یک Nested Class نیاز به دسترسی به Instance کلاس بیرونی ندارد، همیشه آن را static کنید.

دلیل اصلی:

- جلوگیری از Reference پنهان (Hidden Reference)
- جلوگیری از Memory Leak
- کاهش Memory Footprint
- طراحی API بهتر

---

## فهرست مطالب

- [انواع Nested Class در Java](#types)
- [Static Member Class چیست؟](#static-member)
- [کاربرد اصلی Static Member Class](#use-case-static)
- [Non-static Member Class چیست؟](#nonstatic-member)
- [تفاوت اصلی Static و Non-static](#key-difference)
- [مشکل بزرگ Non-static: Hidden Reference](#hidden-reference)
- [مثال Production: Cache Leak](#cache-leak)
- [مثال مهم کتاب: Map.Entry](#map-entry)
- [مثال از Collection Framework](#collection-example)
- [تصمیم‌گیری Static یا Non-static](#decision)
- [Anonymous Class](#anonymous)
- [Local Class](#local)
- [مقایسه چهار نوع Nested Class](#comparison)
- [مثال معماری: Builder Pattern](#builder-pattern)
- [اشتباه رایج در پروژه‌ها](#common-mistake)
- [ارتباط با JVM و Garbage Collector](#jvm-gc)
- [API Compatibility](#api-compatibility)
- [جمع‌بندی Item 24](#summary)

---

## Deep Dive: لایه‌های عمیق‌تر Item 24

- [تفاوت واقعی در Bytecode](#bytecode)
- [اثر روی Object Graph](#object-graph)
- [Memory Leak واقعی در Production](#production-leak)
- [چرا Bloch می‌گوید "Always put static"?](#why-always-static)
- [مهم‌ترین Use Caseهای Static Member Class](#static-use-cases)
- [مهم‌ترین Use Caseهای Non-static Member Class](#nonstatic-use-cases)
- [Adapter Pattern و Non-static Inner Class](#adapter-pattern)
- [اشتباه رایج: Inner Class برای Namespace](#namespace-mistake)
- [Anonymous Class در مقابل Lambda](#anonymous-vs-lambda)
- [Local Class چه زمانی مناسب است؟](#local-class)
- [ارتباط با Design Patterns](#design-patterns)
- [ارتباط با Java Collection Framework](#collection-framework)
- [ارتباط با Spring و Dependency Injection](#spring-di)
- [Decision Framework نهایی](#decision-framework)
- [خلاصه نهایی Item 24](#final-summary)

[بازگشت به بالا](#top)

---

<a id="types"></a>
## ۱. ابتدا انواع Nested Class در Java

Java چهار نوع Nested Class دارد:

```
Nested Classes
│
├── Static Member Class
│
├── Non-static Member Class (Inner Class)
│
├── Anonymous Class
│
└── Local Class
```

سه مورد آخر **Inner Classes** نامیده می‌شوند.

[بازگشت به بالا](#top)

---

<a id="static-member"></a>
## ۲. Static Member Class چیست؟

مثال:

<div dir="ltr">

```java
public class Calculator {
    public static class Operation {
        public static final String PLUS = "+";
        public static final String MINUS = "-";
    }
}
```
</div>

استفاده:

<div dir="ltr">

```java
Calculator.Operation.PLUS;
```
</div>

از دید Java: این تقریباً مثل یک کلاس معمولی است:

<div dir="ltr">

```java
public class CalculatorOperation { }
```
</div>

با این تفاوت که:

- داخل Calculator قرار گرفته
- به private memberهای Calculator دسترسی دارد

[بازگشت به بالا](#top)

---

<a id="use-case-static"></a>
## ۳. کاربرد اصلی Static Member Class

وقتی یک کلاس فقط در ارتباط با Outer Class معنا دارد.

مثال کتاب: `Calculator.Operation` معنی دارد. اما `Operation` به تنهایی شاید معنی نداشته باشد.

مثال واقعی:

<div dir="ltr">

```java
public class HttpResponse {
    private int statusCode;

    public static class Builder {
        private int statusCode;

        public Builder status(int code) {
            this.statusCode = code;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
```
</div>

استفاده:

<div dir="ltr">

```java
HttpResponse response =
    new HttpResponse.Builder()
        .status(200)
        .build();
```
</div>

اینجا `Builder` فقط برای ساخت HttpResponse است، پس Nested بودن منطقی است.

[بازگشت به بالا](#top)

---

<a id="nonstatic-member"></a>
## ۴. Non-static Member Class چیست؟

مثال:

<div dir="ltr">

```java
public class Outer {
    private int value = 100;

    public class Inner {
        public void print() {
            System.out.println(value);
        }
    }
}
```
</div>

اینجا `Inner` به Instance کلاس Outer وابسته است.

ساخت:

<div dir="ltr">

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
```
</div>

یعنی هر Inner یک Reference مخفی به Outer دارد. تقریباً Java پشت صحنه چیزی شبیه این تولید می‌کند:

<div dir="ltr">

```java
class Inner {
    private final Outer this$0;

    Inner(Outer outer) {
        this.this$0 = outer;
    }
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="key-difference"></a>
## ۵. تفاوت اصلی Static و Non-static

### Static Member Class

رابطه:
<div dir="ltr">

```
Outer Class → defines → Static Nested Class
```
</div>
اما Instance مستقل دارد.

### Non-static Member Class

رابطه:
<div dir="ltr">

```
Outer Object → owns → Inner Object
```
</div>
هر Inner متعلق به یک Outer خاص است.

[بازگشت به بالا](#top)

---

<a id="hidden-reference"></a>
## ۶. مشکل بزرگ Non-static: Hidden Reference

فرض کنید:

<div dir="ltr">

```java
public class LargeObject {
    byte[] data = new byte[100_000_000];

    class Helper { }
}
```
</div>

حالا:

<div dir="ltr">

```java
LargeObject obj = new LargeObject();
Helper helper = obj.new Helper();
```
</div>

حتی اگر:

<div dir="ltr">

```java
obj = null;
```
</div>

شود، ممکن است:
<div dir="ltr">

```
Helper → hidden reference → LargeObject
```
</div>
باعث شود Garbage Collector نتواند `LargeObject` را حذف کند.

نتیجه: **Memory Leak**

[بازگشت به بالا](#top)

---

<a id="cache-leak"></a>
## ۷. مثال Production: Cache Leak

فرض کنید:

<div dir="ltr">

```java
public class UserCache {
    private Map<String, User> users;

    class CacheIterator { }
}
```
</div>

اگر `CacheIterator` جایی ذخیره شود:

<div dir="ltr">

```java
static List<Object> holders;
```
</div>

آن وقت:
<div dir="ltr">

```
CacheIterator → UserCache → Huge User Map
```
</div>
همه در Memory باقی می‌مانند.

در حالی که:

<div dir="ltr">

```java
static class CacheIterator
```
</div>

این Reference را ندارد.

[بازگشت به بالا](#top)

---

<a id="map-entry"></a>
## ۸. مثال مهم کتاب: Map.Entry

یکی از بهترین مثال‌ها: ساختار داخلی Map:
<div dir="ltr">

```
HashMap → Entry → key, value
```
</div>
Entry به Map وابسته است. اما آیا Entry نیاز دارد Map را ببیند؟ خیر.

مثلاً:

<div dir="ltr">

```java
entry.getKey();
entry.getValue();
```
</div>

هیچ نیازی به Map ندارد.

پس طراحی درست:

<div dir="ltr">

```java
private static class Entry<K,V> {
    K key;
    V value;
}
```
</div>

نه:

<div dir="ltr">

```java
private class Entry<K,V>
```
</div>

اگر اشتباه کنید: هر Entry یک hidden reference به HashMap دارد. تصور کنید HashMap با ۱۰ میلیون Entry داشته باشد. هر Entry یک Reference اضافه دارد. Memory Waste بسیار زیاد می‌شود.

[بازگشت به بالا](#top)

---

<a id="collection-example"></a>
## ۹. مثال از Collection Framework

کتاب مثال:

<div dir="ltr">

```java
public class MySet<E> extends AbstractSet<E> {
    @Override
    public Iterator<E> iterator() {
        return new MyIterator();
    }

    private class MyIterator implements Iterator<E> {
        // ...
    }
}
```
</div>

چرا اینجا non-static است؟ چون Iterator باید Set اصلی را ببیند. مثلاً `next()` باید بداند element بعدی چیست و collection فعلی چیست.
<div dir="ltr">

```
Iterator → references → MySet
```
</div>
اینجا منطقی است.

[بازگشت به بالا](#top)

---

<a id="decision"></a>
## ۱۰. تصمیم‌گیری Static یا Non-static

قانون Item 24:

سؤال: آیا Nested Class برای کار کردن نیاز دارد Instance کلاس بیرونی را داشته باشد؟

**اگر بله:**

<div dir="ltr">

```java
class Inner
```
</div>

مثال: `List.Iterator`

**اگر خیر:**

<div dir="ltr">

```java
static class Inner
```
</div>

مثال: `Map.Entry`

[بازگشت به بالا](#top)

---

<a id="anonymous"></a>
## ۱۱. Anonymous Class

نوع سوم:

<div dir="ltr">

```java
button.addActionListener(
    new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // ...
        }
    }
);
```
</div>

ویژگی:

- نام ندارد
- همان‌جا ساخته می‌شود
- قابل reuse نیست

قبل از Java 8، Anonymous Class برای Functional Objectها زیاد استفاده می‌شد:

<div dir="ltr">

```java
Runnable task = new Runnable() {
    public void run() { }
};
```
</div>

اما امروز Lambda بهتر است:

<div dir="ltr">

```java
Runnable task = () -> doSomething();
```
</div>

**محدودیت Anonymous Class:** نمی‌توانید بعداً reference کنید. مثلاً `instanceof MyAnonymousClass` ممکن نیست. چون اسم ندارد.

[بازگشت به بالا](#top)

---

<a id="local-class"></a>
## ۱۲. Local Class

مثال:

<div dir="ltr">

```java
public void process() {
    class Validator {
        boolean valid(String s) {
            return s != null;
        }
    }

    Validator v = new Validator();
}
```
</div>

خصوصیات:

- داخل Method تعریف می‌شود
- Scope محدود دارد
- اسم دارد

کاربرد آن کم است.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۱۳. مقایسه چهار نوع Nested Class

| نوع | نام دارد؟ | Reference به Outer | کاربرد |
|-----|-----------|-------------------|--------|
| Static Member Class | بله | ❌ | Helper / Builder / Internal Component |
| Non-static Member Class | بله | ✅ | Adapter / Iterator |
| Anonymous Class | ❌ | بستگی دارد | یک بار استفاده |
| Local Class | ✅ | بستگی دارد | Logic محدود به Method |

[بازگشت به بالا](#top)

---

<a id="builder-pattern"></a>
## ۱۴. مثال معماری: Builder Pattern

یکی از مهم‌ترین کاربردهای Static Member Class:

<div dir="ltr">

```java
public class User {
    private final String name;

    private User(Builder builder) {
        this.name = builder.name;
    }

    public static class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```
</div>

چرا Builder باید static باشد؟ چون:

<div dir="ltr">

```java
new User.Builder()
```
</div>

نباید نیاز داشته باشد:

<div dir="ltr">

```java
User user = new User().new Builder();
```
</div>

Builder به User Instance نیاز ندارد.

[بازگشت به بالا](#top)

---

<a id="common-mistake"></a>
## ۱۵. اشتباه رایج در پروژه‌ها

**بد:**

<div dir="ltr">

```java
public class OrderService {
    class OrderValidator { }
}
```
</div>

اگر `OrderValidator` به `OrderService` دسترسی ندارد، اشتباه است.

**بهتر:**

<div dir="ltr">

```java
public class OrderService {
    static class OrderValidator { }
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="jvm-gc"></a>
## ۱۶. ارتباط با JVM و Garbage Collector

این Item فقط Style نیست.

در JVM: Non-static Inner Class دارای Field اضافه `this$0` است.

نتیجه:

- Object بزرگ‌تر
- Allocation بیشتر
- GC Pressure بیشتر
- احتمال Memory Leak

در سیستم‌های High Throughput مهم است.

[بازگشت به بالا](#top)

---

<a id="api-compatibility"></a>
## ۱۷. نکته مهم درباره API Compatibility

کتاب می‌گوید: اگر یک Member Class را public کنید:

<div dir="ltr">

```java
public class Outer {
    public class Inner { }
}
```
</div>

بعداً تبدیل:

<div dir="ltr">

```java
public static class Inner
```
</div>

کنید، ممکن است Binary Compatibility شکسته شود.

پس انتخاب اولیه مهم است.

[بازگشت به بالا](#top)

---

<a id="summary"></a>
## ۱۸. جمع‌بندی Item 24

قانون طلایی:
<div dir="ltr">

```
Nested Class
      │
      ▼
Does it need Outer instance?
      │
      ├── Yes → Non-static Member Class
      │
      └── No → Static Member Class
```
</div>
به زبان ساده:

❌ اشتباه:

<div dir="ltr">

```java
class Database {
    class ConnectionInfo { }
}
```
</div>

اگر ConnectionInfo به Database نیاز ندارد.

✅ درست:

<div dir="ltr">

```java
class Database {
    static class ConnectionInfo { }
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="bytecode"></a>
## Deep Dive — ۱. تفاوت واقعی در Bytecode

بسیاری تصور می‌کنند:

<div dir="ltr">

```java
class Outer {
    class Inner { }
}
```
</div>

فقط یک Syntax است.

اما Compiler آن را تقریباً به شکل زیر تبدیل می‌کند:

### Non-static Inner Class

کد شما:

<div dir="ltr">

```java
public class Order {
    private String id;

    class Validator { }
}
```
</div>

تقریباً تبدیل می‌شود به:

<div dir="ltr">

```java
public class Order$Validator {
    private final Order this$0;

    Order$Validator(Order order) {
        this.this$0 = order;
    }
}
```
</div>

یعنی Java یک Reference پنهان اضافه می‌کند:
<div dir="ltr">

```
Validator → Order instance
```
</div>
### Static Nested Class

<div dir="ltr">

```java
public class Order {
    static class Validator { }
}
```
</div>

تبدیل می‌شود به:

<div dir="ltr">

```java
public class Order$Validator { }
```
</div>

هیچ Reference اضافه‌ای وجود ندارد.

[بازگشت به بالا](#top)

---

<a id="object-graph"></a>
## Deep Dive — ۲. اثر روی Object Graph

فرض کنید:

<div dir="ltr">

```java
Order order = new Order();
Order.Validator validator = order.new Validator();
```
</div>

در Heap:
<div dir="ltr">

```
+----------------+
| Order          │
|                │
| id             │
+----------------+
        ^
        │
        │
+----------------+
| Validator      │
|                │
| this$0 --------+
+----------------+
```
</div>
Validator باعث زنده ماندن Order می‌شود.

اما:

<div dir="ltr">

```java
Order.Validator validator = new Order.Validator();
```
</div>

ساختار:
<div dir="ltr">

```
+----------------+
| Validator      |
+----------------+
```
</div>
هیچ ارتباطی ندارد.

[بازگشت به بالا](#top)

---

<a id="production-leak"></a>
## Deep Dive — ۳. Memory Leak واقعی در Production

یک مثال Enterprise:

<div dir="ltr">

```java
public class ReportService {
    private byte[] hugeCache = new byte[500_000_000];

    class Callback { }
}
```
</div>

حالا:

<div dir="ltr">

```java
ReportService service = new ReportService();
Callback callback = service.new Callback();
service = null;
```
</div>

شما انتظار دارید `ReportService` توسط GC حذف شود. ولی:
<div dir="ltr">

```
Callback → ReportService
```
</div>
هنوز Reference وجود دارد. پس ۵۰۰ MB cache در Memory باقی می‌ماند.

این نوع Leak بسیار خطرناک است چون در Code Review دیده نمی‌شود. شما فقط می‌بینید `class Callback` اما Reference واقعی `this$0` است.

[بازگشت به بالا](#top)

---

<a id="why-always-static"></a>
## Deep Dive — ۴. چرا Bloch می‌گوید "Always put static"?

چون در اکثر موارد، Nested Class فقط یک Helper است.

مثلاً:

<div dir="ltr">

```java
public class UserService {
    static class ValidationResult { }
}
```
</div>

آیا ValidationResult باید UserService را نگه دارد؟ خیر. پس `static` باید باشد.

[بازگشت به بالا](#top)

---

<a id="static-use-cases"></a>
## Deep Dive — ۵. مهم‌ترین Use Caseهای Static Member Class

### ۵.۱ Builder Pattern

<div dir="ltr">

```java
public class User {
    private final String username;

    private User(Builder builder) {
        this.username = builder.username;
    }

    public static class Builder {
        private String username;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```
</div>

چرا Static؟ چون Builder نیازی به User Instance ندارد.

اگر Builder غیر static بود:

<div dir="ltr">

```java
User user = new User().new Builder();
```
</div>

که از نظر طراحی بی‌معنی است.

[بازگشت به بالا](#top)

---

<a id="nonstatic-use-cases"></a>
## Deep Dive — ۶. مهم‌ترین Use Caseهای Non-static Member Class

قاعده: Inner Class باید بتواند از State کلاس بیرونی استفاده کند.

### Iterator

<div dir="ltr">

```java
class MyList {
    Object[] elements;

    class Iterator {
        int index;

        Object next() {
            return elements[index++];
        }
    }
}
```
</div>

Iterator بدون List معنی ندارد:
<div dir="ltr">

```
Iterator → MyList state
```
</div>
اینجا Non-static درست است.

[بازگشت به بالا](#top)

---

<a id="adapter-pattern"></a>
## Deep Dive — ۷. Adapter Pattern و Non-static Inner Class

<div dir="ltr">

```java
class AudioPlayer {
    void play() { }

    class Mp3Adapter implements MediaPlayer {
        public void play() {
            AudioPlayer.this.play();
        }
    }
}
```
</div>

Adapter باید AudioPlayer را Wrap کند. پس Reference منطقی است.

[بازگشت به بالا](#top)

---

<a id="namespace-mistake"></a>
## Deep Dive — ۸. اشتباه رایج: Inner Class برای Namespace

خیلی از Developerها می‌نویسند:

<div dir="ltr">

```java
class Customer {
    class Address { }
}
```
</div>

اما سؤال: آیا Address بدون Customer وجود دارد؟ در Domain بله. مثلاً `Address` در سیستم‌های دیگر هم کاربرد دارد.

پس بهتر:

<div dir="ltr">

```java
class Address { }
```
</div>

به عنوان Top-level Class.

[بازگشت به بالا](#top)

---

<a id="anonymous-vs-lambda"></a>
## Deep Dive — ۹. Anonymous Class در مقابل Lambda

قبل Java 8:

<div dir="ltr">

```java
executor.submit(
    new Runnable() {
        public void run() { }
    }
);
```
</div>

امروز:

<div dir="ltr">

```java
executor.submit(() -> process());
```
</div>

Lambda کوتاه‌تر، خواناتر و بدون Boilerplate است.

اما Anonymous Class هنوز کاربرد دارد. مثلاً وقتی نیاز دارید:

- State داشته باشید
- چند Method Override کنید
- یک Class خاص بسازید

مثال:

<div dir="ltr">

```java
Comparator<User> comparator =
    new Comparator<>() {
        public int compare(User a, User b) {
            return a.id().compareTo(b.id());
        }
    };
```
</div>

[بازگشت به بالا](#top)

---

<a id="local-class"></a>
## Deep Dive — ۱۰. Local Class چه زمانی مناسب است؟

مثال:

<div dir="ltr">

```java
public void process(List<User> users) {
    class UserValidator {
        boolean valid(User user) {
            return user != null;
        }
    }

    UserValidator validator = new UserValidator();
}
```
</div>

مزیت: Scope محدود. هیچ Class دیگری نمی‌تواند استفاده کند.

ولی اگر این کلاس بزرگ شود:

<div dir="ltr">

```java
public void process() {
    class HugeValidator {
        // 300 lines
    }
}
```
</div>

بهتر است Top-level class شود.

[بازگشت به بالا](#top)

---

<a id="design-patterns"></a>
## Deep Dive — ۱۱. ارتباط با Design Patterns

| Pattern | Nested Class مناسب |
|---------|-------------------|
| Builder | Static Member Class |
| Iterator | Non-static Member Class |
| Adapter | Non-static Member Class |
| Factory Helper | Static Member Class |
| Strategy | Lambda / Anonymous |

[بازگشت به بالا](#top)

---

<a id="collection-framework"></a>
## Deep Dive — ۱۲. ارتباط با Java Collection Framework

یک مثال بسیار مهم: **HashMap.Node**

در JDK تقریباً:

<div dir="ltr">

```java
static class Node<K,V> implements Entry<K,V>
```
</div>

چرا؟ چون Node نیاز ندارد HashMap را نگه دارد.

اگر اشتباه بود:
<div dir="ltr">

```
HashMap → 10 million Nodes → هر Node: reference → HashMap
```
</div>
یعنی میلیون‌ها Reference اضافی.

[بازگشت به بالا](#top)

---

<a id="spring-di"></a>
## Deep Dive — ۱۳. ارتباط با Spring و Dependency Injection

در Spring Boot زیاد می‌بینیم:

**بد:**

<div dir="ltr">

```java
@Service
public class UserService {
    class Helper { }
}
```
</div>

اگر Helper Bean نیست و به UserService نیاز ندارد:

**بهتر:**

<div dir="ltr">

```java
@Service
public class UserService {
    static class Helper { }
}
```
</div>

یا حتی:

<div dir="ltr">

```java
@Component
public class Helper { }
```
</div>

بسته به Responsibility.

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## Deep Dive — ۱۴. Decision Framework نهایی

قبل از ساخت Nested Class:

**سؤال اول:** آیا این کلاس خارج از Outer هم معنا دارد؟
<div dir="ltr">

```
Yes → Top-level Class
```
</div>
**سؤال دوم:** اگر Nested است، آیا به Instance Outer نیاز دارد؟
<div dir="ltr">

```
Yes → Non-static Inner Class
No  → Static Member Class
```
</div>
**سؤال سوم:** داخل یک Method است؟
<div dir="ltr">

```
One usage → Anonymous Class
Multiple usage → Local Class
```
</div>
[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## خلاصه نهایی Item 24

قانون عملی برای Production:

| نیاز | انتخاب |
|------|--------|
| Helper داخلی | `static nested class` |
| Builder | `static nested class` |
| DTO کوچک وابسته به کلاس | `static nested class` |
| Iterator | `non-static inner class` |
| Adapter که Outer را Wrap می‌کند | `non-static inner class` |
| Callback یک‌بار مصرف | Lambda |
| Logic محدود داخل Method | Local Class |

### مهم‌ترین جمله Item 24:

> اگر یک Nested Class به Instance کلاس بیرونی نیاز ندارد، static نبودن آن یک **Bug طراحی** است.

و دلیل این سخت‌گیری:

- کاهش Memory Usage
- جلوگیری از Memory Leak
- حفظ API Stability
- طراحی صحیح Object Ownership

این Item در کنار **Item 7 (Eliminate obsolete object references)** یکی از مهم‌ترین موارد Effective Java برای جلوگیری از **Memory Leak** در سیستم‌های بزرگ JVM است.

---

[بازگشت به بالا](#top)

</div>
```