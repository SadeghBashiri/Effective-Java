<div dir="rtl">

<a id="top"></a>

# آیتم ۸: اجتناب از Finalizer و Cleaner (Avoid Finalizers and Cleaners)

اگر بخواهم فقط **سه Item اول فصل دوم** را از نظر اهمیت رتبه‌بندی کنم، به نظر من:

> **Item 8 یکی از مهم‌ترین Itemهای کل کتاب Effective Java است.**

چرا؟ چون تقریباً تمام Java Developerها در طول دوران کاری خود حداقل یک بار با یکی از این مفاهیم سروکار دارند:

- File Handle Leak
- Socket Leak
- Database Connection Leak
- Native Memory Leak
- Thread Leak
- AutoCloseable
- try-with-resources
- GC
- Finalizer
- Cleaner

و این Item دقیقاً توضیح می‌دهد که چرا طراحی قدیمی Java اشتباه بود و چگونه از Java 7 و Java 9 به بعد این مشکل حل شد.

---

## فهرست مطالب

- [بخش اول: چرا Finalizer بد هستند؟](#part1)
  - [اولین جمله کتاب](#first-sentence)
  - [Finalizer چیست؟](#what-is-finalizer)
  - [مقایسه Java و C++ Destructor](#java-vs-cpp)
  - [بزرگ‌ترین سوءتفاهم Java Developerها](#misunderstanding)
  - [مهم‌ترین اصل: Memory vs Resource](#main-principle)
  - [مثال: FileInputStream](#fileinputstream-example)
  - [ارتباط با Microservices](#microservices)
  - [ارتباط با Itemهای قبلی](#connection-to-items)
- [بخش دوم: چرا Finalizer غیرقابل پیش‌بینی است؟](#part2)
  - [چرخه واقعی JVM](#jvm-cycle)
  - [Finalizer Queue چیست؟](#finalizer-queue)
  - [مثال: ۱,۰۰۰,۰۰۰ Object](#million-objects)
  - [چرا JVM تضمین نمی‌دهد؟](#no-guarantee)
  - [تفاوت با try-with-resources](#vs-try-with-resources)
  - [Cleaner هم کامل نیست](#cleaner-limitations)
  - [جدول مقایسه Finalizer vs Cleaner](#comparison-table)
- [بخش سوم: عملکرد و Performance](#part3)
  - [هزینه‌های پنهان Finalizer](#hidden-costs)
  - [تصور داخلی HotSpot](#hotspot-internals)
  - [Cleaner چطور؟](#cleaner-performance)
  - [تأثیر بر Production](#production-impact)
  - [جمع‌بندی Performance](#performance-summary)
- [بخش چهارم: Finalizer Attack (حمله امنیتی)](#part4)
  - [حمله چگونه کار می‌کند؟](#how-attack-works)
  - [جلوگیری از حمله](#prevent-attack)
- [بخش پنجم: راه‌حل‌های صحیح](#part5)
  - [AutoCloseable](#autocloseable)
  - [try-with-resources](#try-with-resources)
  - [Cleaner به عنوان Safety Net](#cleaner-safety-net)
  - [نکته: Cleaner فقط برای Native Resources](#cleaner-native)
- [Best Practices نهایی](#best-practices)
- [Anti-Patternها](#anti-patterns)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول: چرا Finalizer بد هستند؟

<a id="first-sentence"></a>
### اولین جمله کتاب

Joshua Bloch کتاب را با جمله بسیار تندی شروع می‌کند:
<div dir="ltr">

> **Finalizers are unpredictable, often dangerous, and generally unnecessary.**
</div>
این یکی از شدیدترین جملات کل کتاب است. او عملاً می‌گوید:

```
Finalizer
↓
Unpredictable
↓
Dangerous
↓
Slow
↓
Don't use it
```

تقریباً هیچ جای دیگری در Effective Java این‌قدر صریح چیزی را رد نمی‌کند.

<a id="what-is-finalizer"></a>
### Finalizer چیست؟

در Java قدیمی، اگر کلاسی متد زیر را داشت:
<div dir="ltr">

```java
@Override
protected void finalize() throws Throwable {
    // ...
}
```
</div>
JVM قبل از آزاد شدن Object سعی می‌کرد این متد را اجرا کند.

بسیاری از برنامه‌نویسان تصور می‌کردند:

> این همان Destructor در C++ است.

Joshua Bloch می‌گوید: **کاملاً اشتباه است.**

<a id="java-vs-cpp"></a>
### مقایسه Java و C++ Destructor

**C++:**
<div dir="ltr">

```cpp
class File {
public:
    ~File() {
        close();  // همیشه اجرا می‌شود
    }
};
```
</div>
Destructor **همیشه** در زمان خروج Object اجرا می‌شود:

```
Object Dies
    ↓
Destructor
    ↓
Release Resource
```

کاملاً **Deterministic** (قطعی).

**Java:**
<div dir="ltr">

```java
class File {
    @Override
    protected void finalize() {
        close();
    }
}
```
</div>
چه زمانی اجرا می‌شود؟ **هیچ‌کس نمی‌داند.**

```
Object unreachable
    ↓
???
    ↓
Maybe GC
    ↓
???
    ↓
Maybe Finalizer
    ↓
???
    ↓
Maybe Never
```

این تفاوت بنیادی است.

<a id="misunderstanding"></a>
### بزرگ‌ترین سوءتفاهم Java Developerها

خیلی‌ها فکر می‌کنند:
<div dir="ltr">

```java
new File(...)
    ↓
file = null;
    ↓
finalize()  // ❌ هیچ تضمینی وجود ندارد
```
</div>
نه. اصلاً چنین تضمینی وجود ندارد.

<a id="main-principle"></a>
### مهم‌ترین اصل Item 8

Joshua Bloch یک قانون طلایی ارائه می‌کند:

> **Garbage Collection مسئول آزاد کردن Memory است، نه آزاد کردن Resource.**

| Memory | Resource |
|--------|----------|
| GC مدیریت می‌کند | GC مدیریت **نمی‌کند** |
| `new User()` | File Descriptor |
| | Socket |
| | Connection |
| | Lock |
| | Thread |
| | Native Memory |
| | GPU Memory |

این Resourceها باید توسط **خود برنامه** آزاد شوند.

<a id="fileinputstream-example"></a>
### مثال: FileInputStream
<div dir="ltr">

```java
FileInputStream input = new FileInputStream(...);
```
</div>
داخل سیستم‌عامل اتفاق افتاده:

```
Java Object
    ↓
OS File Descriptor
    ↓
Open File
```

اگر فقط Java Object حذف شود، File Descriptor هنوز باز است. سیستم‌عامل آن را نگه می‌دارد.

### اشتباه رایج قدیمی

بعضی‌ها می‌نوشتند:
<div dir="ltr">

```java
@Override
protected void finalize() {
    socket.close();  // ❌ ممکن است هرگز اجرا نشود
}
```
</div>
### بدترین سناریو
<div dir="ltr">

```java
for (int i = 0; i < 100000; i++) {
    new FileInputStream(...);  // همه منتظر finalize() هستند
}
```
</div>
نتیجه:

```
Too many open files
```

برنامه Crash می‌کند. در حالی که Heap ممکن است کاملاً سالم باشد.

<a id="microservices"></a>
### ارتباط با Microservices

در پروژه‌های Enterprise تقریباً تمام این Resourceها وجود دارند:

- JDBC Connection
- Kafka Producer/Consumer
- RabbitMQ Channel
- Redis Connection
- Netty Channel
- gRPC Stream
- File Handle
- ThreadPool

اگر منتظر Finalizer بمانی، Production دیر یا زود دچار مشکل می‌شود.

<a id="connection-to-items"></a>
### ارتباط با Itemهای قبلی

| Item | پیام |
|------|------|
| Item 6 | Objectهای غیرضروری نساز |
| Item 7 | Referenceهای منسوخ را حذف کن تا GC بتواند Object را آزاد کند |
| **Item 8** | حتی وقتی GC Object را آزاد می‌کند، **منابع خارجی (Resources)** را خودش آزاد نمی‌کند؛ مسئولیت آن با برنامه است |

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم: چرا Finalizer غیرقابل پیش‌بینی است؟

Joshua Bloch چندین بار در این Item کلمه **Unpredictable** را تکرار می‌کند. این اتفاقی نیست.

او می‌خواهد بگوید:

> **مشکل اصلی Finalizer این نیست که کند است؛ مشکل اصلی این است که زمان اجرای آن قابل پیش‌بینی نیست.**

<a id="jvm-cycle"></a>
### چرخه واقعی JVM

وقتی آخرین Reference حذف می‌شود:
<div dir="ltr">

```java
stream = null;
```
</div>
Object فقط وارد وضعیت **Unreachable** می‌شود.

چرخه واقعی:

```
Reachable
    ↓
Unreachable
    ↓
GC discovers object
    ↓
Has Finalizer?
    │
Yes │
    ▼
Finalizer Queue
    ↓
Finalizer Thread
    ↓
Run finalize()
    ↓
GC Later
    ↓
Memory Reclaimed
```

نکته مهم: GC مستقیماً Object را آزاد نمی‌کند. ابتدا باید Finalizer اجرا شود. بعداً شاید دوباره GC اجرا شود. بعد Memory آزاد شود.

<a id="finalizer-queue"></a>
### Finalizer Queue چیست؟

داخل JVM یک Queue وجود دارد:

```
GC
    ↓
Finalizer Queue
    ↓
Finalizer Thread
```

تمام Objectهایی که `finalize()` دارند، وارد این صف می‌شوند.

اگر این صف شلوغ شود، مشکل شروع می‌شود.

<a id="million-objects"></a>
### مثال: ۱,۰۰۰,۰۰۰ Object

فرض کن:
<div dir="ltr">

```java
for (int i = 0; i < 1_000_000; i++) {
    new LargeFile();
}
```
</div>
همه این Objectها دارای `finalize()` هستند.

در نتیجه:

```
GC
    ↓
1,000,000 Objects
    ↓
Finalizer Queue
```

حالا تنها یک Thread مسئول اجرای همه آن‌هاست: **Finalizer Thread**.

اگر این Thread عقب بیفتد، کل صف عقب می‌افتد.

نتیجه: Heap پر می‌شود. Objectها هنوز آزاد نشده‌اند. در نهایت:

```
OutOfMemoryError
```

دقیقاً همان اتفاقی که Joshua Bloch درباره برنامه GUI توضیح می‌دهد.

<a id="no-guarantee"></a>
### چرا JVM تضمین نمی‌دهد؟

GC خودش نیز قابل پیش‌بینی نیست.

فرض کن:

```
Object
    ↓
Unreachable
```

آیا GC همین حالا اجرا می‌شود؟ ممکن است:

- بله
- ۵ ثانیه بعد
- ۱ دقیقه بعد
- اصلاً اجرا نشود (اگر برنامه تمام شود)

پس اگر خود GC غیرقطعی باشد، Finalizer که به GC وابسته است، حتی غیرقطعی‌تر خواهد بود.

### رابطه زمانی

```
Object dies
    ↓
???
    ↓
GC
    ↓
???
    ↓
Finalizer
    ↓
???
    ↓
GC Again
    ↓
Memory Free
```

این علامت‌های سؤال همان چیزی هستند که Joshua Bloch از آن به عنوان **Unpredictable** یاد می‌کند.

<a id="vs-try-with-resources"></a>
### تفاوت با try-with-resources

حالا این را با `try-with-resources` مقایسه کن:
<div dir="ltr">

```java
try (FileInputStream in = ...) {
    // use file
}
// end block → close() → Done
```
</div>
```
Open File
    ↓
Use File
    ↓
End Block
    ↓
close()
    ↓
Done
```

هیچ ابهامی وجود ندارد.

<a id="cleaner-limitations"></a>
### Cleaner هم کامل نیست

بعضی‌ها فکر می‌کنند: "Finalizer حذف شد. پس Cleaner مشکل را حل کرده است."

Joshua Bloch می‌گوید: **خیر.**

Cleaner فقط:

- امن‌تر است
- پیاده‌سازی بهتری دارد
- مشکلات Reflection را ندارد

اما هنوز:

```
Cleaner
    ↓
GC
    ↓
Background Thread
```

وابسته است.

پس همچنان:

- زمان اجرا نامشخص است
- ممکن است دیر اجرا شود
- ممکن است اصلاً اجرا نشود

<a id="comparison-table"></a>
### جدول مقایسه Finalizer vs Cleaner

| ویژگی | Finalizer | Cleaner |
|--------|-----------|---------|
| Deprecated | ✅ (Java 9) | ❌ (اما فقط برای موارد خاص) |
| وابسته به GC | ✅ | ✅ |
| اجرای قطعی | ❌ | ❌ |
| Thread اختصاصی | JVM کنترل می‌کند | کتابخانه می‌تواند کنترل کند |
| Performance | ضعیف | بهتر، اما همچنان هزینه‌دار |
| جایگزین `close()` | ❌ | ❌ |

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم: عملکرد و Performance

### چرا Finalizer حدود 50 برابر کندتر است؟

Joshua Bloch می‌گوید:

> روی سیستم من:
> - Object معمولی: **12 ns**
> - Object دارای Finalizer: **550 ns**
>
> یعنی تقریباً **50 برابر کندتر**

<a id="hidden-costs"></a>
### هزینه‌های پنهان Finalizer

**هزینه شماره ۱: Register کردن Object**

وقتی Object ساخته می‌شود، JVM بررسی می‌کند:
<div dir="ltr">

```
Has finalize()?
```
</div>
اگر پاسخ Yes باشد، Object داخل ساختارهای داخلی JVM ثبت می‌شود.

**هزینه شماره ۲: GC نمی‌تواند مستقیم Object را آزاد کند**

Object باید وارد Finalizer Queue شود:

```
    GC
    ↓
  Queue
    ↓
Synchronization
    ↓
Reference Tracking
```

**هزینه شماره ۳: بیدار کردن Thread دیگر**

Finalizer Thread باعث:

- Context Switch
- Scheduling
- Synchronization

می‌شود.

**هزینه شماره ۴: اجرای Finalizer**

JVM هیچ اطلاعی ندارد که Finalizer چه کاری انجام می‌دهد. پس مجبور است تمام احتیاط‌های ممکن را انجام دهد.

**هزینه شماره ۵: دو بار GC**

بعد از اجرای Finalizer، باز هم Memory آزاد نمی‌شود. باید:

```
    GC
    ↓
Second Collection
```

انجام شود.

<a id="hotspot-internals"></a>
### تصور داخلی HotSpot

**بدون Finalizer:**

```
    GC
    ↓
Object Dead
    ↓
  Free
```

**با Finalizer:**

```
    GC
    ↓
Object Dead
    ↓
Has Finalizer
    ↓
Queue
    ↓
Keep Alive
    ↓
Run Finalizer
    ↓
Mark Again
    ↓
Collect Again
    ↓
  Free
```

این همان چیزی است که هزینه را زیاد می‌کند.

<a id="cleaner-performance"></a>
### Cleaner چطور؟

Cleaner بهتر است. اما هنوز:

```
  Cleaner
    ↓
Background Thread
    ↓
Runnable
    ↓
    GC
```

را دارد. پس هنوز Scheduling، Queue، Synchronization وجود دارد.

به همین دلیل Joshua می‌گوید: Cleaner تقریباً **500 ns** است.

### چرا Cleaner به عنوان Safety Net فقط 66ns است؟

فرض کن:
<div dir="ltr">

```java
try (Room room = new Room()) {
    // use room
}
```
</div>
کاربر همیشه `close()` را صدا می‌زند. در این حالت Cleaner **هیچ‌وقت اجرا نمی‌شود**.

فقط وجود دارد. مثل بیمه.

هزینه: `12 ns → 66 ns` یعنی فقط حدود **5x**، نه **50x**.

<a id="production-impact"></a>
### تأثیر بر Production

**مثال: JDBC Connection**
<div dir="ltr">

```java
class JdbcConnection {
    @Override
    protected void finalize() {
        connection.close();  // ❌
    }
}
```
</div>
در سیستم با `5000 req/sec`:

```
    GC
    ↓
Thousands of Finalizers
    ↓
Long Pause
    ↓
Latency Spike
```

**راه صحیح:**
<div dir="ltr">

```java
try (Connection c = datasource.getConnection()) {
    // use connection
}
```
</div>
Connection Pool مدیریت می‌کند: Borrow و Return، بدون وابستگی به GC.

### اثر روی Cloud-Native

فرض کن Kubernetes به Pod `512MB` RAM داده است.

اگر Finalizer باعث شود `GC Delay` ایجاد شود، Heap رشد می‌کند. در نهایت `OOM Killer` ممکن است Pod را از بین ببرد. در حالی که هیچ Memory Leak واقعی وجود نداشته است.

<a id="performance-summary"></a>
### جمع‌بندی Performance

| Object معمولی | Object دارای Finalizer |
|---------------|----------------------|
| Allocation ساده | Allocation + Registration |
| یک مرحله GC | دو مرحله GC |
| Queue ندارد | Finalizer Queue دارد |
| Thread اضافی ندارد | Finalizer Thread دارد |
| آزادسازی مستقیم | آزادسازی با تأخیر |
| Throughput بالا | Throughput پایین |
| Latency کم | Latency بیشتر |
| مناسب Production | نامناسب برای Production |

[بازگشت به بالا](#top)

---

<a id="part4"></a>
## بخش چهارم: Finalizer Attack (حمله امنیتی)

یکی از جذاب‌ترین مباحث این آیتم، حمله امنیتی از طریق `finalize()` است.

<a id="how-attack-works"></a>
### حمله چگونه کار می‌کند؟

فرض کن کلاس زیر وجود دارد:
<div dir="ltr">

```java
public class SensitiveClass {
    private Resource resource;
    
    public SensitiveClass(String secret) {
        this.resource = new Resource(secret);
    }
}
```
</div>
اگر کلاس `finalize()` داشته باشد، یک مهاجم می‌تواند:

1. یک Subclass از این کلاس بسازد
2. در `finalize()`، قبل از اینکه Object کامل ساخته شود، به Resource دسترسی پیدا کند
3. این کار باعث می‌شود یک **Partially Constructed Object** زنده بماند
<div dir="ltr">

```java
public class Attack extends SensitiveClass {
    public Attack() {
        super("");  // ممکن است Exception پرتاب کند
    }
    
    @Override
    protected void finalize() {
        // دسترسی به resource نیمه‌ساخته
    }
}
```
</div>
### راه‌های جلوگیری از حمله

1. **کلاس را `final` کنید** تا کسی نتواند Subclass بسازد
2. **متد `finalize()` را `final` کنید** تا کسی نتواند Override کند
3. **از `enum` برای Singleton استفاده کنید** (Enumها از `finalize()` محافظت می‌کنند)
4. **هرگز به `finalize()` برای مدیریت منابع امنیتی اعتماد نکنید**

Joshua Bloch می‌گوید:
<div dir="ltr">

> Classes that are not final should not have finalizers.
</div>
یعنی: کلاس‌هایی که `final` نیستند نباید `finalize()` داشته باشند.

[بازگشت به بالا](#top)

---

<a id="part5"></a>
## بخش پنجم: راه‌حل‌های صحیح

<a id="autocloseable"></a>
### ۱. AutoCloseable
<div dir="ltr">

```java
public class FileResource implements AutoCloseable {
    private final FileInputStream stream;
    
    public FileResource(String path) throws IOException {
        this.stream = new FileInputStream(path);
    }
    
    @Override
    public void close() throws IOException {
        stream.close();
    }
}
```
</div>
<a id="try-with-resources"></a>
### ۲. try-with-resources
<div dir="ltr">

```java
try (FileResource resource = new FileResource("data.txt")) {
    // use resource
} // close() automatic
```
</div>
مزایا:

- **قطعی (Deterministic)** - دقیقاً می‌دانی چه زمانی بسته می‌شود
- **Thread-safe** - هیچ Race Condition وجود ندارد
- **Exception-safe** - حتی اگر Exception رخ دهد، بسته می‌شود
- **شفاف** - کد خوانا و قابل نگهداری است

<a id="cleaner-safety-net"></a>
### ۳. Cleaner به عنوان Safety Net

Joshua Bloch استفاده از Cleaner را فقط برای یک مورد تأیید می‌کند:

> **به عنوان یک Safety Net (تور ایمنی)**

یعنی:
<div dir="ltr">

```java
public class Room implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;
    
    private static class CleaningAction implements Runnable {
        @Override
        public void run() {
            // release native resources
        }
    }
    
    public Room() {
        cleanable = CLEANER.register(this, new CleaningAction());
    }
    
    @Override
    public void close() {
        cleanable.clean();
    }
}
```
</div>
کاربر **باید** `close()` را صدا بزند. Cleaner فقط اگر کاربر فراموش کرد، Backup انجام می‌دهد.

<a id="cleaner-native"></a>
### نکته: Cleaner فقط برای Native Resources

Joshua Bloch تأکید می‌کند:
<div dir="ltr">

> Cleaner را فقط برای Native Resources استفاده کنید.
</div>
مثل:

- Memory خارج از Heap
- GPU Memory
- File Descriptor
- Socket

نه برای Objectهای معمولی Java.

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## Best Practices نهایی

| قانون | توضیح |
|-------|-------|
| **هرگز از `finalize()` استفاده نکنید** | Deprecated از Java 9 |
| **از `try-with-resources` استفاده کنید** | قطعی، امن، خوانا |
| **کلاس‌های خود را `AutoCloseable` کنید** | اگر Resource دارند |
| **Cleaner فقط به عنوان Safety Net** | فقط برای Native Resources |
| **کلاس‌های غیر `final` نباید `finalize()` داشته باشند** | جلوگیری از Attack |
| **در Constructorها Resource باز نکنید** | استفاده از Factory Method بهتر است |
| **همیشه `close()` را در `finally` صدا بزنید** | اگر نمی‌توانید از try-with-resources استفاده کنید |

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternها

| Anti-Pattern | دلیل |
|--------------|------|
| `@Override protected void finalize()` | غیرقابل پیش‌بینی، کند، خطرناک |
| اعتماد به Cleaner برای مدیریت منابع | نباید جایگزین `close()` شود |
| باز کردن Resource در Constructor | ممکن است نیمه‌ساخته باقی بماند |
| فراموش کردن `close()` در `finally` | باعث Leak می‌شود |
| استفاده از Object Pool برای Objectهای سبک | به‌جای آن از GC استفاده کنید |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

### پیام اصلی Joshua Bloch

1. **Memory ≠ Resource** - GC فقط Memory را مدیریت می‌کند
2. **Finalizer غیرقابل پیش‌بینی است** - زمان اجرا مشخص نیست
3. **Finalizer کند است** - حدود 50 برابر کندتر
4. **Finalizer خطرناک است** - حملات امنیتی ممکن است
5. **از `try-with-resources` استفاده کنید** - قطعی، امن، سریع
6. **Cleaner فقط به عنوان Safety Net** - و فقط برای Native Resources

### جدول نهایی

| روش | قطعی | امن | سریع | توصیه شده |
|-----|------|-----|------|-----------|
| `finalize()` | ❌ | ❌ | ❌ | هرگز |
| Cleaner (اصلی) | ❌ | ✅ | ⚠️ | فقط برای Native |
| Cleaner (Safety Net) | ❌ | ✅ | ✅ | به عنوان Backup |
| `try-with-resources` | ✅ | ✅ | ✅ | همیشه |
| `finally { close(); }` | ✅ | ✅ | ✅ | اگر try-with-resources ممکن نیست |

---

> **نکته کلیدی:** Frameworkهای مدرن مانند Spring، Quarkus، Netty، HikariCP همگی از `AutoCloseable` و `try-with-resources` استفاده می‌کنند و هیچ‌کدام برای مدیریت منابع به `finalize()` متکی نیستند. زیرا این روش‌ها **قطعی (Deterministic)**، قابل پیش‌بینی و سازگار با نیازهای سیستم‌های با توان عملیاتی بالا هستند.

[بازگشت به بالا](#top)

</div>
```