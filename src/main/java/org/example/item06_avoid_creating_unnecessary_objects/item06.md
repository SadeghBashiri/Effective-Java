<div dir="rtl">

<a id="top"></a>

# آیتم ۶: اجتناب از ایجاد Objectهای غیرضروری (Avoid Creating Unnecessary Objects)

این Item یکی از **مهم‌ترین Itemهای کتاب برای Performance، JVM و Memory Management** است و متأسفانه یکی از **بدفهم‌ترین** Itemها نیز هست.

خیلی‌ها بعد از خواندن عنوان آن فکر می‌کنند Joshua Bloch می‌خواهد بگوید:

> **هر جا توانستی Object نساز!**

در حالی که این برداشت **کاملاً اشتباه** است.

در انتهای Item خودش صراحتاً می‌گوید:

> **Creating additional objects to enhance the clarity, simplicity, or power of a program is generally a good thing.**

یعنی:

> اگر ساختن Object باعث تمیزتر شدن طراحی شود، **حتماً Object بساز.**

پس این Item درباره **Object Creation** نیست؛ درباره **Unnecessary Object Creation** است.

---

## فهرست مطالب

- [دسته‌بندی Objectها](#object-categories)
  - [دسته اول: Cheap Objects](#cheap-objects)
  - [دسته دوم: Reusable Objects](#reusable-objects)
  - [دسته سوم: Expensive Objects](#expensive-objects)
- [نکته‌ای که اکثر برنامه‌نویسان اشتباه متوجه می‌شوند](#common-misunderstanding)
- [۸ ایده اصلی Joshua Bloch](#eight-ideas)
- [محورهای اصلی این Item](#main-topics)
- [۱. String Pool و تفاوت `new String()` با String Literal](#string-pool)
- [۲. Static Factory Method و Reuse](#static-factory-reuse)
- [۳. Caching اشیای گران‌قیمت](#caching)
- [۴. تحلیل کامل مثال `Pattern.compile()`](#pattern-compile)
- [۵. چرا Lazy Initialization در این مثال توصیه نمی‌شود؟](#lazy-initialization)
- [۶. Adapter/View Objects و مفهوم Shared View](#adapter-view)
- [۷. Autoboxing و هزینه پنهان](#autoboxing)
- [۸. چرا Object Pool برای اشیای سبک Anti-Pattern است؟](#object-pool)
- [۹. تفاوت فلسفی Item 6 با Item 50 (Reuse vs Defensive Copy)](#vs-item50)
- [جدول جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="object-categories"></a>
## دسته‌بندی Objectها

قبل از هر مثال، باید مفهوم اصلی را درک کنیم. Joshua Bloch در واقع Objectها را به **سه دسته** تقسیم می‌کند.

<a id="cheap-objects"></a>
### دسته اول: Cheap Objects

مثل:
<div dir="ltr">

```java
new Point(...)
new UserDto(...)
new OrderResponse(...)
```
</div>
ساختن این Objectها بسیار ارزان است.

امروزه JVM دارای:

- **TLAB** (Thread-Local Allocation Buffer)
- **Escape Analysis**
- **Generational GC**

است. ساخت این Objectها تقریباً رایگان است.

> **پس: اینجا اصلاً به فکر Reuse نباش.**

<a id="reusable-objects"></a>
### دسته دوم: Reusable Objects

مثل:
<div dir="ltr">

```java
String
Pattern
BigInteger
Boolean
Enum
Collections.emptyList()
```
</div>
اینها Immutable هستند. می‌توان بارها از همان Instance استفاده کرد.

<a id="expensive-objects"></a>
### دسته سوم: Expensive Objects

مثل:

```java
Database Connection
SSLContext
Pattern
ObjectMapper
ThreadPool
HttpClient
Jackson ObjectMapper
ValidatorFactory
```

اینجا ساخت Object **گران** است. اینجا باید Reuse انجام شود.

### ساختار ذهنی Item 6

```
                Object
                    │
        ┌───────────┼────────────┐
        │           │            │
     Cheap      Immutable     Expensive
        │           │            │
    Create      Reuse       Cache
```

Joshua Bloch دقیقاً روی **دسته دوم و سوم** تمرکز دارد.

[بازگشت به بالا](#top)

---

<a id="common-misunderstanding"></a>
## نکته‌ای که اکثر برنامه‌نویسان اشتباه متوجه می‌شوند

بسیاری بعد از خواندن این Item شروع می‌کنند به نوشتن چیزهایی مانند:
<div dir="ltr">

```java
UserDto dto = pool.borrow();
PointPool
StringBuilderPool
```
</div>
این دقیقاً برخلاف چیزی است که Joshua Bloch در انتهای Item می‌گوید.

او صریحاً می‌گوید:

> **Object Pool برای Objectهای سبک معمولاً ایده بدی است.**

[بازگشت به بالا](#top)

---

<a id="eight-ideas"></a>
## ۸ ایده اصلی Joshua Bloch

Joshua Bloch در این Item ۸ ایده را مطرح می‌کند:

| # | ایده | توضیح مختصر |
|---|------|-------------|
| ۱ | String Pool | تفاوت `new String()` با String Literal |
| ۲ | Static Factory Reuse | ارتباط مستقیم با Item 1 |
| ۳ | Cache کردن Objectهای گران | Pattern، ObjectMapper، HttpClient |
| ۴ | `Pattern.compile()` | تحلیل کامل و دلیل بهبود Performance |
| ۵ | Lazy Initialization | چرا در این مثال توصیه نمی‌شود؟ |
| ۶ | Adapter/View Reuse | مانند `Map.keySet()` و Shared View |
| ۷ | Autoboxing | هزینه پنهان `Long` در مقابل `long` |
| ۸ | Object Pool | چرا برای اشیای سبک Anti-Pattern است؟ |

[بازگشت به بالا](#top)

---

<a id="main-topics"></a>
## محورهای اصلی این Item

برای اینکه مفاهیم به‌درستی جا بیفتند، هر کدام را به‌ترتیب بررسی می‌کنیم.

[بازگشت به بالا](#top)

---

<a id="string-pool"></a>
## ۱. String Pool و تفاوت `new String()` با String Literal

### ❌ Anti-Pattern
<div dir="ltr">

```java
String s = new String("hello");
```
</div>
این کد **دو** Object می‌سازد:

1. String Literal `"hello"` در String Pool
2. Object جدید در Heap

### ✅ Best Practice

<div dir="ltr">

```java
String s = "hello";
```
</div>
فقط **یک** Object در String Pool ساخته می‌شود.

### نحوه عملکرد String Pool

```
┌─────────────────────────────┐
│       String Pool           │
│  ┌─────────────────────┐   │
│  │     "hello"         │   │
│  └─────────────────────┘   │
│         ▲                   │
│         │                   │
│  ┌──────┴──────┐            │
│  │   String s  │────────────┘
│  └─────────────┘
```

### آیا همیشه باید از String Literal استفاده کنیم؟

تقریباً **بله**، مگر در موارد خاص مانند:

- استفاده از `String` به عنوان `synchronized` lock (که خودش Anti-Pattern است)
- نیاز به ایجاد یک `String` جدید با محتوای یکسان اما جدا از Pool

[بازگشت به بالا](#top)

---

<a id="static-factory-reuse"></a>
## ۲. Static Factory Method و Reuse

### ❌ Anti-Pattern
<div dir="ltr">

```java
Boolean b = new Boolean(true);  // Deprecated
```
</div>
### ✅ Best Practice
<div dir="ltr">

```java
Boolean b = Boolean.valueOf(true);  // از Cache استفاده می‌کند
```
</div>
### دلیل

`Boolean.valueOf()` همیشه از Cache استفاده می‌کند:
<div dir="ltr">

```java
public static Boolean valueOf(boolean b) {
    return b ? Boolean.TRUE : Boolean.FALSE;
}
```
</div>
### سایر کلاس‌های مشابه

| کلاس | روش صحیح | روش غلط |
|------|---------|---------|
| `Integer` | `Integer.valueOf(5)` | `new Integer(5)` |
| `Long` | `Long.valueOf(5)` | `new Long(5)` |
| `Character` | `Character.valueOf('a')` | `new Character('a')` |

> **نکته:** از Java 9 به بعد، `new Integer()` و `new Long()` به‌کلی Deprecated شده‌اند.

[بازگشت به بالا](#top)

---

<a id="caching"></a>
## ۳. Caching اشیای گران‌قیمت

### مثال: ObjectMapper
<div dir="ltr">

```java
// ❌ Anti-Pattern
public class UserService {
    public String toJson(User user) {
        ObjectMapper mapper = new ObjectMapper();  // هر بار ساخته می‌شود!
        return mapper.writeValueAsString(user);
    }
}

// ✅ Best Practice
public class UserService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public String toJson(User user) {
        return MAPPER.writeValueAsString(user);
    }
}
```
</div>
### مثال: HttpClient
<div dir="ltr">

```java
// ✅ Best Practice
public class HttpClientService {
    private static final HttpClient HTTP_CLIENT = 
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public String fetchData(String url) {
        // استفاده از HTTP_CLIENT
    }
}
```
</div>
[بازگشت به بالا](#top)

---

<a id="pattern-compile"></a>
## ۴. تحلیل کامل مثال `Pattern.compile()`

### ❌ Anti-Pattern
<div dir="ltr">

```java
public static boolean isRomanNumeral(String s) {
    return s.matches("^(?=.)M*(C[MD]|D?C{0,3})...$");
}
```
</div>
**مشکل:** هر بار که متد صدا زده می‌شود، یک `Pattern` جدید کامپایل می‌شود. این کار **گران** است.

### ✅ Best Practice
<div dir="ltr">

```java
public class RomanNumerals {
    private static final Pattern ROMAN = Pattern.compile(
        "^(?=.)M*(C[MD]|D?C{0,3})..."
    );
    
    public static boolean isRomanNumeral(String s) {
        return ROMAN.matcher(s).matches();
    }
}
```
</div>
### اندازه‌گیری Performance
<div dir="ltr">

```java
// Anti-Pattern: 3000 بار در ثانیه → 500ms
// Best Practice: 3000 بار در ثانیه → 50ms
```
</div>
### چرا `Pattern.compile()` گران است؟

1. تحلیل و Tokenization عبارت منظم
2. ساخت درخت نحو (Parse Tree)
3. بهینه‌سازی و تبدیل به کد ماشین
4. ذخیره‌سازی در حافظه

[بازگشت به بالا](#top)

---

<a id="lazy-initialization"></a>
## ۵. چرا Lazy Initialization در این مثال توصیه نمی‌شود؟

### گزینه Lazy
<div dir="ltr">

```java
public class RomanNumerals {
    private static Pattern ROMAN = null;
    
    public static boolean isRomanNumeral(String s) {
        if (ROMAN == null) {
            ROMAN = Pattern.compile("...");
        }
        return ROMAN.matcher(s).matches();
    }
}
```
</div>
### چرا Joshua Bloch آن را توصیه نمی‌کند؟

| مشکل | توضیح |
|-------|-------|
| **کد اضافی** | برای یک مزیت جزئی |
| **Thread-Safety** | نیاز به `synchronized` دارد |
| **پیچیدگی** | نگهداری سخت‌تر می‌شود |
| **سود کم** | اگر متد زیاد صدا زده شود، فقط یک بار صرفه‌جویی می‌کند |

> **نتیجه:** اگر کلاس در برنامه استفاده می‌شود، Pattern در اولین بار که کلاس بارگذاری می‌شود ساخته شود. Lazy فقط زمانی منطقی است که ساخت Object واقعاً سنگین باشد و کلاس ممکن است هرگز استفاده نشود.

[بازگشت به بالا](#top)

---

<a id="adapter-view"></a>
## ۶. Adapter/View Objects و مفهوم Shared View

### مثال: `Map.keySet()`
<div dir="ltr">

```java
Map<String, String> map = new HashMap<>();
Set<String> keys1 = map.keySet();
Set<String> keys2 = map.keySet();

System.out.println(keys1 == keys2);  // true!
```
</div>
**چرا؟**

`keySet()` یک View (نمای) از Map است، نه یک کپی. هر بار که صدا می‌شود، **همان** Object را برمی‌گرداند.

### سایر مثال‌ها
<div dir="ltr">

```java
List<String> list = Arrays.asList("a", "b", "c");
List<String> subList1 = list.subList(0, 2);
List<String> subList2 = list.subList(0, 2);
// subList1 == subList2? نه! این View جدید است!
```
</div>
### نکته مهم

- `Map.keySet()` → **همیشه یک Object واحد** برمی‌گرداند
- `List.subList()` → **هر بار یک View جدید** برمی‌گرداند

[بازگشت به بالا](#top)

---

<a id="autoboxing"></a>
## ۷. Autoboxing و هزینه پنهان

### ❌ Anti-Pattern
<div dir="ltr">

```java
Long sum = 0L;  // Long Object در Heap
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;  // Unboxing + Boxing در هر بار!
}
```
</div>
### ✅ Best Practice
<div dir="ltr">

```java
long sum = 0L;  // Primitive در Stack
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;  // هیچ Box/Unbox انجام نمی‌شود
}
```
</div>
### اندازه‌گیری Performance

| روش | زمان تقریبی |
|------|------------|
| `Long sum` (با Autoboxing) | ~10 ثانیه |
| `long sum` (Primitive) | ~1 ثانیه |

### قوانین Autoboxing

| عملیات | توضیح |
|--------|-------|
| `long → Long` | Boxing (ساخت Object) |
| `Long → long` | Unboxing (استخراج مقدار) |
| `Long` در محاسبات | Unboxing سپس Boxing مجدد |

[بازگشت به بالا](#top)

---

<a id="object-pool"></a>
## ۸. چرا Object Pool برای اشیای سبک Anti-Pattern است؟

### ❌ Object Pool برای `StringBuilder`
<div dir="ltr">

```java
public class StringBuilderPool {
    private static final Pool<StringBuilder> pool = ...;
    
    public String buildString() {
        StringBuilder sb = pool.borrow();
        sb.append("...");
        String result = sb.toString();
        pool.returnObject(sb);  // ⚠️ خطرناک!
        return result;
    }
}
```
</div>
### مشکلات Object Pool

| مشکل | توضیح |
|-------|-------|
| **GC بهینه‌تر است** | JVM در مدیریت Objectهای کوتاه‌عمر بسیار بهینه عمل می‌کند |
| **Thread-Safety** | نیاز به `synchronized` دارد |
| **Memory Leak** | فراموش کردن `return` باعث نشت حافظه می‌شود |
| **Code Complexity** | نگهداری سخت‌تر می‌شود |
| **Premature Optimization** | معمولاً نیازی نیست |

### چه زمانی Object Pool منطقی است؟

- **Database Connection** - ساخت اتصال بسیار گران است
- **Thread Pool** - ساخت Thread گران است
- **HTTP Connection** - TCP Handshake گران است
- **SSLContext** - ساخت SSL گران است

> **نکته:** Joshua Bloch می‌گوید هرگز برای Objectهای معمولی Object Pool نسازید. JVM بسیار بهتر از شما این کار را مدیریت می‌کند.

[بازگشت به بالا](#top)

---

<a id="vs-item50"></a>
## ۹. تفاوت فلسفی Item 6 با Item 50 (Reuse vs Defensive Copy)

### Item 6: Reuse

```
اگر Object Immutable است → Reuse کن
اگر Object گران است → Cache کن
```

### Item 50: Defensive Copy

```
اگر Object Mutable است → Defensive Copy کن
اگر شک داری → Copy کن
```

### جمع‌بندی فلسفی

```
Immutable Objects
    │
    ├── Reuse (Item 6)
    │
    └── No need for Defensive Copy

Mutable Objects
    │
    ├── Avoid Sharing (Item 50)
    │
    └── Always Defensive Copy when receiving
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جدول جمع‌بندی نهایی

| دسته Object | رفتار توصیه‌شده | مثال |
|-------------|----------------|-------|
| **Cheap Objects** | بسازید (بدون نگرانی) | `new Point()`, DTOها |
| **Immutable Objects** | Reuse کنید | `String`, `Boolean`, `Integer` |
| **Expensive Objects** | Cache کنید | `Pattern`, `ObjectMapper`, `HttpClient` |
| **View/Adapter** | Reuse کنید (اگر ممکن است) | `Map.keySet()` |
| **Mutable Objects** | از Defensive Copy استفاده کنید | آرایه‌ها، تاریخ‌ها |
| **Primitives vs Wrappers** | از Primitive استفاده کنید | `long` به جای `Long` |

### قانون طلایی Joshua Bloch

> **"Creating additional objects to enhance the clarity, simplicity, or power of a program is generally a good thing."**

اگر ساختن Object جدید کد را خواناتر می‌کند، **حتماً آن را بسازید**. هیچ‌گاه برای استفاده‌ی مجدد از Object، طراحی را فدا نکنید.

---

[بازگشت به بالا](#top)

</div>
```