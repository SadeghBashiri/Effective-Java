<div dir="rtl">

<a id="top"></a>

# آیتم ۷: حذف Referenceهای منسوخ (Eliminate Obsolete Object References)

به نظر من Item 7 یکی از مهم‌ترین فصل‌های کتاب برای هر Java Backend Developer و Software Architect است، چون مستقیماً با JVM Memory Management، GC، Heap، Cache، Listener، ThreadLocal و Memory Leak ارتباط دارد.

نکته جالب این است که اکثر برنامه‌نویسان تصور می‌کنند:

> Java Garbage Collector دارد، پس Memory Leak دیگر وجود ندارد.

اما Joshua Bloch دقیقاً می‌خواهد ثابت کند که این تصور **اشتباه** است.

در Java چیزی به نام Memory Leak به معنای C/C++ (فراموش کردن `free()`) کمتر وجود دارد، اما چیزی که بسیار رایج است **Unintentional Object Retention** است؛ یعنی شیء هنوز Reference دارد، بنابراین GC اجازه آزاد کردن آن را ندارد، حتی اگر برنامه دیگر هرگز به آن نیاز نداشته باشد.

---

## فهرست مطالب

- [آیا Java واقعاً Memory Leak دارد؟](#does-java-have-memory-leak)
  - [تفاوت C و Java](#c-vs-java)
- [مفهوم اصلی: Obsolete Reference](#obsolete-reference)
  - [مثال ساده](#simple-example)
- [چرا GC اشتباه نمی‌کند؟](#why-gc-is-correct)
- [معماری ذهنی Item 7](#mental-architecture)
- [سه منبع اصلی Memory Leak](#three-sources)
- [ارتباط با JVM و GC Roots](#gc-roots)
- [مثال واقعی کتاب: Stack](#stack-example)
  - [نسخه اشتباه](#stack-wrong)
  - [نسخه صحیح](#stack-correct)
- [چرا GC خودش این را نمی‌فهمد؟](#why-gc-cant-know)
- [دسته اول: Self-Managed Memory](#self-managed-memory)
  - [مثال Production: ConnectionPool](#production-pool)
  - [Anti-Pattern](#antipattern-pool)
- [دسته دوم: Cache](#cache-memory-leak)
  - [راه‌حل: WeakHashMap](#weak-hashmap)
  - [مقایسه Caching Strategies](#caching-comparison)
- [دسته سوم: Listener ها](#listener-memory-leak)
  - [مثال Spring](#spring-example)
- [آیا باید همیشه Referenceها را null کنیم؟](#should-we-always-null)
- [تصمیم‌گیری مهندسی](#engineering-decision)
- [ارتباط با توسعه Enterprise](#enterprise-context)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="does-java-have-memory-leak"></a>
## آیا Java واقعاً Memory Leak دارد؟

اول باید یک سوءتفاهم بسیار رایج را برطرف کنیم.

**در C:**
<div dir="ltr">

```c
malloc(...)
    ↓
free(...)   // اگر فراموش شود → Memory Leak
```
</div>
**اما در Java:**
<div dir="ltr">

```java
User user = new User();
    ↓
// وقتی Reference از بین برود
Reference
    ↓
GC
    ↓
Memory Free
```
</div>
پس خیلی‌ها نتیجه می‌گیرند: **Java Memory Leak ندارد.**

اما Joshua Bloch می‌گوید: **اشتباه است.**

<a id="c-vs-java"></a>
### تفاوت C و Java

| زبان | علت Memory Leak |
|------|-----------------|
| **C/C++** | فراموش کردن `free()` یا `delete` |
| **Java** | Reference هنوز وجود دارد (Obsolete Reference) |

GC فقط یک سؤال می‌پرسد:

> آیا هنوز کسی به این Object اشاره می‌کند؟

اگر پاسخ "بله" باشد، GC حق ندارد آن Object را حذف کند.  
مهم نیست که آن Reference دیگر هیچ کاربردی نداشته باشد.

[بازگشت به بالا](#top)

---

<a id="obsolete-reference"></a>
## مفهوم اصلی: Obsolete Reference

Joshua Bloch یک اصطلاح مهم معرفی می‌کند:

> **Obsolete Reference**

یعنی:

> Referenceای که هنوز وجود دارد، اما برنامه دیگر هرگز از آن استفاده نخواهد کرد.

این مهم‌ترین جمله کل Item است.

<a id="simple-example"></a>
### مثال ساده
<div dir="ltr">

```java
User user = new User();
// بعد:
user = null;  // GC می‌تواند User را جمع کند
```
</div>
اما اگر:

<div dir="ltr">

```java
List<User> users = new ArrayList<>();
users.add(new User());

        
// بعد: دیگر هرگز از عنصر اول استفاده نمی‌کنیم
// ولی آن را حذف نکنیم:
users.get(0)  // هنوز Reference وجود دارد
```
</div>
GC **نمی‌تواند** آن User را آزاد کند.

[بازگشت به بالا](#top)

---

<a id="why-gc-is-correct"></a>
## چرا GC اشتباه نمی‌کند؟

GC نمی‌تواند حدس بزند که:

> "برنامه‌نویس دیگر به این Object نیاز ندارد."

GC فقط Graph حافظه را بررسی می‌کند.  
اگر Reference وجود داشته باشد:

```
Root
  ↓
Object A
  ↓
Object B
  ↓
Object C
```

همه زنده هستند.

### پیام اصلی Joshua Bloch

Memory Leak در Java تقریباً همیشه یعنی:

```
Object
  ↑
Reference
  ↑
Reference
  ↑
Reference
  ↑
GC نمی‌تواند حذف کند
```

نه اینکه GC ضعیف باشد.

[بازگشت به بالا](#top)

---

<a id="mental-architecture"></a>
## معماری ذهنی Item 7

```
             Object
                ▲
                │
         Reference Exists?
          │             │
         Yes           No
          │             │
      Object Alive     GC می‌تواند حذف کند
```

[بازگشت به بالا](#top)

---

<a id="three-sources"></a>
## سه منبع اصلی Memory Leak

Joshua Bloch سه دسته را معرفی می‌کند:

1. **Self-Managed Memory**  
   مثل Stack کتاب، Queue، Pool، Buffer

2. **Cache**  
   که بعداً `WeakHashMap` را معرفی می‌کند

3. **Listener / Callback**  
   که معمولاً در Eventها اتفاق می‌افتد

این سه مورد هنوز هم رایج‌ترین دلایل Memory Leak در پروژه‌های Enterprise هستند.

[بازگشت به بالا](#top)

---

<a id="gc-roots"></a>
## ارتباط با JVM و GC Roots

برای درک این Item باید مدل GC را بشناسیم.

GC از **Rootها** شروع می‌کند:

```
GC Root
    ↓
Static Field
    ↓
Singleton
    ↓
Thread
    ↓
Stack Frame
    ↓
Object
```

اگر مسیر وجود داشته باشد، Object حذف نمی‌شود.

### نکته مهم برای Backend Developers

بزرگ‌ترین منابع GC Root در پروژه‌های Spring Boot معمولاً این‌ها هستند:

- Static Fieldها
- Singleton Beanها
- Thread Poolها
- Executorها
- ThreadLocalها
- Cacheها
- Sessionها

به همین دلیل، یک Reference اشتباه در هر یک از این بخش‌ها می‌تواند باعث شود هزاران Object برای مدت طولانی در Heap باقی بمانند.

[بازگشت به بالا](#top)

---

<a id="stack-example"></a>
## مثال واقعی کتاب: Stack

<a id="stack-wrong"></a>
### نسخه اشتباه
<div dir="ltr">

```java
public class Stack {

    private Object[] elements = new Object[10];
    private int size = 0;

    public void push(Object object) {
        elements[size++] = object;
    }

    public Object pop() {
        if (size == 0)
            throw new IllegalStateException();
        return elements[--size];
    }
}
```
</div>
در نگاه اول همه چیز درست است.

اما... فرض کنید:

```java
push(A)
push(B)
push(C)
```

آرایه:

```
index
0 -> A
1 -> B
2 -> C
size = 3
```

حالا `pop()` انجام می‌دهیم.

`size` می‌شود `2`، اما آرایه هنوز این شکلی است:

```
0 -> A
1 -> B
2 -> C   // ← C دیگر داخل Stack نیست، اما Reference هنوز وجود دارد!
```

GC این را می‌بیند:

```
elements[]
    ↓
C
```

پس نتیجه می‌گیرد: **Object Reachable**

در نتیجه: GC **نمی‌تواند** آن را حذف کند.

این همان Memory Leak منطقی است.

<a id="stack-correct"></a>
### نسخه صحیح
<div dir="ltr">

```java
public class Stack {

    private Object[] elements = new Object[10];
    private int size = 0;

    public void push(Object object) {
        elements[size++] = object;
    }

    public Object pop() {
        if (size == 0)
            throw new IllegalStateException();

        Object result = elements[--size];
        elements[size] = null;   // eliminate obsolete reference
        return result;
    }
}
```
</div>
دقت کنید: `elements[size] = null;`

همین یک خط باعث می‌شود:

```
Array
0 -> A
1 -> B
2 -> null
```

دیگر Referenceای به C وجود ندارد.  
GC آزاد است آن را حذف کند.

[بازگشت به بالا](#top)

---

<a id="why-gc-cant-know"></a>
## چرا GC خودش این را نمی‌فهمد؟

چون GC فقط Referenceها را می‌بیند.  
او نمی‌داند `size = 2` به معنی چیست.

برای GC:

```
Object[]
0
1
2
3
...
```

همه خانه‌ها معتبر هستند.

فقط برنامه‌نویس می‌داند که بعد از `size` دیگر داده‌ای معتبر نیست. به همین دلیل، کلاس‌هایی که حافظه خود را مدیریت می‌کنند باید Referenceهای منسوخ را حذف کنند.

[بازگشت به بالا](#top)

---

<a id="self-managed-memory"></a>
## دسته اول: Self-Managed Memory

این مشکل دقیقاً کجا رخ می‌دهد؟

- `Stack`
- `Queue`
- `Pool`
- `Buffer`
- `RingBuffer`
- `Object Pool`
- `Custom Collection`

اگر Reference را حذف نکنی:

```
Object هیچ‌وقت GC نمی‌شود
```

<a id="production-pool"></a>
### مثال Production: ConnectionPool
<div dir="ltr">

```java
public class ConnectionPool {

    private final Connection[] pool;
    private int size;

    public Connection borrow() {
        if (size == 0)
            return null;

        Connection connection = pool[--size];
        pool[size] = null;  // ✅ حذف Reference
        return connection;
    }
}
```
</div>

<a id="antipattern-pool"></a>
### Anti-Pattern
<div dir="ltr">

```java
Connection connection = pool[--size];
return connection;  // ❌ Reference هنوز داخل Pool باقی مانده است
```
</div>
[بازگشت به بالا](#top)

---

<a id="cache-memory-leak"></a>
## دسته دوم: Cache

یکی از بزرگ‌ترین منابع Memory Leak.

مثال:
<div dir="ltr">

```java
Map<String, User> cache = new HashMap<>();
```
</div>
هر بار `cache.put(...)` انجام می‌دهیم.  
ولی هیچوقت `remove()` نمی‌کنیم.

نتیجه:

```
Cache
  ↓
100
  ↓
1000
  ↓
100000
  ↓
1000000
```

تمام Objectها هنوز Reachable هستند.  
GC نمی‌تواند حذفشان کند.

<a id="weak-hashmap"></a>
### راه‌حل: WeakHashMap
<div dir="ltr">

```java
Map<String, User> cache = new WeakHashMap<>();
```
</div>
وقتی کلید (Key) دیگر در جای دیگری Reference ندارد،  
Entry به‌طور خودکار از Cache حذف می‌شود.

> **نکته مهم:** کتاب پیشنهاد می‌کند اگر طول عمر Entryها باید وابسته به وجود Referenceهای خارجی باشد، از `WeakHashMap` یا سایر Referenceهای ضعیف استفاده شود. هرچند برای Cacheهای حرفه‌ای معمولاً کتابخانه‌هایی مانند **Caffeine** سیاست‌های Eviction مناسب‌تری دارند.

<a id="caching-comparison"></a>
### مقایسه Caching Strategies

| روش | مزایا | معایب |
|------|-------|-------|
| `HashMap` بدون Eviction | ساده | Memory Leak |
| `WeakHashMap` | خودکار با GC | کنترل کم |
| `Caffeine` | سیاست‌های پیشرفته | وابستگی کتابخانه |
| `Redis` | اشتراک‌گذاری | خارج از JVM |

[بازگشت به بالا](#top)

---

<a id="listener-memory-leak"></a>
## دسته سوم: Listener ها

یکی از رایج‌ترین Memory Leakهای Enterprise.

فرض کنید:
<div dir="ltr">

```java
eventBus.register(listener);
```
</div>
ولی هیچوقت:
<div dir="ltr">

```java
eventBus.unregister(listener);
```
</div>
را صدا نمی‌زنیم.

نتیجه:

```
Listener
    ↓
Activity
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Huge Graph
```

تمام Graph در حافظه باقی می‌ماند.

<a id="spring-example"></a>
### مثال Spring

**اشتباه:**
<div dir="ltr">

```java
@Component
public class NotificationListener {
    // ثبت Listener
}
```
</div>
```
ApplicationEventPublisher
    ↓
NotificationListener
```

اگر Listener هرگز Unregister نشود:

```
کل Bean قابل GC نیست
```

#### راه‌حل در Spring

1. استفاده از `@EventListener` با مدیریت خودکار
2. استفاده از `ApplicationListener` با Unregister صریح
3. استفاده از `WeakReference` در Listenerها

[بازگشت به بالا](#top)

---

<a id="should-we-always-null"></a>
## آیا باید همیشه Referenceها را null کنیم؟

**خیر.**  
این یکی از مهم‌ترین نکات کتاب است.

Joshua Bloch می‌گوید:

> Nulling references should be the exception, not the rule.

یعنی: **هر Referenceای را Null نکن.**

### اشتباه
<div dir="ltr">

```java
User user = repository.find(id);
process(user);
user = null;  // کاملاً بی‌فایده
```
</div>
چون:

```
Method
    ↓
تمام شد
    ↓
Variable از Scope خارج شد
```

GC خودش آن را حذف می‌کند.

### چه زمانی باید null کنیم؟

فقط زمانی که **کلاس خودش حافظه را مدیریت می‌کند**:

- `Object[]`
- Custom Pool
- Buffer
- Reusable Objects
- Collections

[بازگشت به بالا](#top)

---

<a id="engineering-decision"></a>
## تصمیم‌گیری مهندسی

| وضعیت | Null کردن Reference | دلیل |
|--------|---------------------|------|
| Local Variable | ❌ خیر | خروج از Scope کافی است |
| Method Parameter | ❌ خیر | JVM خودش مدیریت می‌کند |
| Instance Field معمولی | ❌ معمولاً خیر | تا وقتی خود شیء زنده است طبیعی است |
| Custom Collection | ✅ بله | کلاس حافظه را خودش مدیریت می‌کند |
| Object Pool | ✅ بله | جلوگیری از نگه‌داری بی‌دلیل اشیاء |
| Cache | ❌ به‌جای null از Eviction/Weak References استفاده کنید | طراحی مناسب‌تر و قابل نگهداری‌تر |
| Event Listener | ❌ از `unregister()` استفاده کنید | رفع ریشه‌ای مشکل به‌جای پاک کردن Reference |

[بازگشت به بالا](#top)

---

<a id="enterprise-context"></a>
## ارتباط با توسعه Enterprise

در پروژه‌های Spring Boot یا Quarkus، این آیتم را معمولاً به شکل مستقیم در کلاس‌های روزمره نمی‌بینید، زیرا فریم‌ورک‌ها از مجموعه‌های استاندارد استفاده می‌کنند که این مسائل را مدیریت کرده‌اند.

اما در سناریوهای زیر اهمیت زیادی پیدا می‌کند:

- پیاده‌سازی Object Pool یا Connection Pool اختصاصی
- طراحی Cache اختصاصی
- ساخت Data Structure سفارشی
- پیاده‌سازی Event Bus
- Listenerها و Callbackها
- Bufferها و Queueهای با کارایی بالا

### ارتباط با Itemهای قبلی

این Item در واقع ادامه منطقی Itemهای قبل است:

| Item | پیام |
|------|------|
| Item 1 | اگر می‌توانی Object را به‌درستی Reuse کن |
| Item 5 | وابستگی‌ها را درست مدیریت کن |
| Item 6 | Objectهای غیرضروری نساز |
| **Item 7** | **Objectهایی را که دیگر لازم نیست، ناخواسته زنده نگه ندار** |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

پیام اصلی Item 7 این نیست که "همیشه null بنویس".

پیام اصلی این است:

1. **وجود Garbage Collector به معنای نبود Memory Leak نیست.**
2. **هر Reference اضافه می‌تواند مانع جمع‌آوری کل یک گراف از اشیاء شود.**
3. **تنها زمانی Referenceها را به‌صورت دستی حذف کنید که کلاس شما حافظه را خودش مدیریت می‌کند.**
4. **سه منبع اصلی نشت حافظه در Java عبارت‌اند از:**
    - ساختارهای داده‌ای که حافظه را خودشان مدیریت می‌کنند
    - Cacheهایی که سیاست حذف (Eviction) ندارند
    - Listenerها و Callbackهایی که هرگز Deregister نمی‌شوند

### جدول خلاصه

| منبع Memory Leak | نشانه | راه‌حل |
|------------------|-------|--------|
| Self-Managed Memory | `Object[]` با `size` | Reference را `null` کنید |
| Cache | `Map` بدون Eviction | `WeakHashMap` یا Caffeine |
| Listener/Callback | Event Bus بدون Unregister | Deregister در زمان مناسب |

[بازگشت به بالا](#top)

---

</>
```