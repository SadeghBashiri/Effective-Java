<div dir="rtl">

<a id="top"></a>

# آیتم ۹: اولویت با try-with-resources بر try-finally (بخش کامل)

به نظر من **Item 9 یکی از مهم‌ترین آیتم‌های کل کتاب Effective Java** است. اگر Item 5 (Dependency Injection) درباره معماری بود و Item 8 درباره مدیریت چرخه عمر (Lifecycle) منابع، **Item 9 درباره مدیریت صحیح Resource در Java** است. تقریباً تمام Frameworkهای مدرن Java (Spring، Quarkus، Micronaut، Hibernate، Netty، Kafka Client، JDBC، AWS SDK و ...) بر پایه همین مفهوم طراحی شده‌اند.

نکته مهم این است که Joshua Bloch فقط نمی‌گوید:

> **از try-with-resources استفاده کنید.**

بلکه می‌خواهد بگوید:

> **مدیریت Resource باید Deterministic (قطعی) باشد، نه وابسته به Garbage Collector.**

---

## فهرست مطالب

- [دید معماری (Architectural View)](#architectural-view)
  - [نوع اول: Memory Resource](#memory-resource)
  - [نوع دوم: External Resource](#external-resource)
- [مشکل اصلی Item 9](#core-problem)
- [چرا finally سال‌ها بهترین راه بود؟](#why-finally)
  - [مشکل اول: کد تودرتو و غیرقابل خواندن](#problem1)
  - [مشکل دوم: از بین رفتن Exception اول](#problem2)
  - [مثال Production](#production-example)
- [Java 7 چه تغییری ایجاد کرد؟](#java7-change)
  - [Suppressed Exception](#suppressed-exception)
  - [getSuppressed()](#getSuppressed)
- [AutoCloseable چیست و چرا طراحی شد؟](#autocloseable)
  - [قبل از Java 7](#before-java7)
  - [Compiler چه کاری انجام می‌دهد؟](#compiler-transformation)
  - [چرا AutoCloseable به جای Closeable؟](#autocloseable-vs-closeable)
- [چند Resource و ترتیب بسته شدن](#multiple-resources)
  - [قانون مهم: LIFO](#lifo)
  - [مثال JDBC](#jdbc-example)
  - [اگر close Exception بدهد؟](#close-exception)
  - [مثال Production: Kafka](#kafka-example)
- [طراحی Library با AutoCloseable](#design-library)
  - [Spring و Quarkus](#spring-quarkus)
- [Resource Lifecycle و Ownership](#resource-lifecycle)
  - [Deterministic Lifecycle](#deterministic)
  - [Ownership](#ownership)
  - [Nested Resources](#nested-resources)
  - [Scope](#scope)
  - [ارتباط با Connection Pool](#connection-pool)
- [طراحی کلاس‌های خودمان](#custom-class-design)
- [ارتباط با معماری مدرن](#modern-architecture)
  - [Spring](#spring)
  - [Quarkus](#quarkus)
  - [Reactive Programming](#reactive)
- [مهم‌ترین پیام Item 9](#final-message)
- [ساختار پروژه پیشنهادی](#project-structure)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## دید معماری (Architectural View)

ابتدا باید دو مفهوم را از هم جدا کنیم.

در Java دو نوع Resource وجود دارد.

<a id="memory-resource"></a>
### نوع اول: Memory Resource

مثل:

<div dir="ltr">

```java
new User();
new Order();
new ArrayList<>();
```
</div>

اینها فقط حافظه Heap مصرف می‌کنند.

مدیریت آن‌ها بر عهده GC است.

```
Program
    ↓
Object
    ↓
    GC
    ↓
Memory Free
```

<a id="external-resource"></a>
### نوع دوم: External Resource

اینها خارج از Heap هستند. مثلاً:

- File
- Socket
- Database Connection
- Kafka Producer/Consumer
- Thread
- InputStream/OutputStream
- SSL Session
- Native Handle

این منابع را GC مدیریت نمی‌کند.

مثلاً:

<div dir="ltr">

```java
Connection connection = dataSource.getConnection();
```
</div>

Connection داخل Heap نیست. بلکه در Database Server نیز Resource ایجاد شده است.

```
Java Object
    ↓
TCP Socket
    ↓
Database Session
    ↓
Server Resource
```

اگر Object از بین برود، ممکن است Session هنوز باز باشد.

[بازگشت به بالا](#top)

---

<a id="core-problem"></a>
## مشکل اصلی Item 9

فرض کن:


نتیجه؟ Database Connection همچنان باز است.

در سیستم‌های Production این بدترین نوع Memory Leak است. در واقع **Resource Leak**، نه Memory Leak.

[بازگشت به بالا](#top)

---

<a id="why-finally"></a>
## چرا finally سال‌ها بهترین راه بود؟

قبل از Java 7 تنها روش مطمئن:

<div dir="ltr">

```java
try {
    // کار با Resource
} finally {
    close();
}
```
</div>

مثلاً:

<div dir="ltr">

```java
BufferedReader reader = new BufferedReader(...);
try {
    return reader.readLine();
} finally {
    reader.close();
}
```
</div>

مهم نیست: Exception، Return، Break یا هر اتفاق دیگری بیفتد. `finally` همیشه اجرا می‌شود.

از نظر تئوری عالی است. اما...

<a id="problem1"></a>
### مشکل اول: کد تودرتو و غیرقابل خواندن

وقتی فقط یک Resource داشته باشی، کد هنوز قابل تحمل است. اما دو Resource:

<div dir="ltr">

```java
try {
    try {
        // کار با منابع
    } finally {
        out.close();
    }
} finally {
    in.close();
}
```
</div>

هر Resource جدید، یک `finally` جدید.

سه Resource:

<div dir="ltr">

```java
try {
    try {
        try {
            // کار با منابع
        } finally {}
    } finally {}
} finally {}
```
</div>

کد عملاً غیرقابل خواندن می‌شود.

<a id="problem2"></a>
### مشکل دوم: از بین رفتن Exception اول

Joshua این قسمت را شاهکار توضیح داده است.

فرض کن:

<div dir="ltr">

```java
reader.readLine();  // ← Exception #1 (مثلاً IOException)
```
</div>

حالا وارد `finally` می‌شویم:

<div dir="ltr">

```java
reader.close();     // ← Exception #2 (مثلاً IOException)
```
</div>

چه اتفاقی می‌افتد؟

```
readLine()
    ↓
IOException #1
    ↓
finally
    ↓
close()
    ↓
IOException #2
```

در Java قبل از try-with-resources، **Exception دوم، Exception اول را از بین می‌برد**.

یعنی Log فقط این را نشان می‌دهد: `close failed`

در حالی که مشکل اصلی `readLine` بوده است. Debug تقریباً غیرممکن می‌شود.

<a id="production-example"></a>
### مثال Production

فرض کن: "Read Customer File"

Disk خراب است:

<div dir="ltr">

```java
read() → IOException
```
</div>

بعد `close()` هم به خاطر همان Disk Exception می‌دهد.

Log فقط این را نشان می‌دهد: `Close failed`

در حالی که مشکل واقعی Read بوده است.

Developer ساعت‌ها اشتباه Debug می‌کند.

[بازگشت به بالا](#top)

---

<a id="java7-change"></a>
## Java 7 چه تغییری ایجاد کرد؟

Java 7 گفت: بیایید این Pattern را داخل Language قرار دهیم.

به جای:

<div dir="ltr">

```java
try { } finally { }
```
</div>

نوشتیم:

<div dir="ltr">

```java
try (Resource) { }
```
</div>

Compiler خودش `finally` تولید می‌کند.

یعنی این:

<div dir="ltr">

```java
try (BufferedReader reader = ...) {
    // use
}
```
</div>

تقریباً تبدیل می‌شود به:

<div dir="ltr">

```java
try {
    // use
} finally {
    reader.close();
}
```
</div>

اما **هوشمندتر**.

<a id="suppressed-exception"></a>
### Suppressed Exception

اگر `readLine()` و `close()` هر دو Exception رخ دهند، Java این کار را می‌کند:

1. **Primary Exception** حفظ می‌شود
2. Exception دوم داخل آن به عنوان **Suppressed** ذخیره می‌شود

```
IOException
    │
    +--- Suppressed: IOException (close failed)
```

یعنی هیچ Exceptionی از بین نمی‌رود.

<a id="getSuppressed"></a>
### getSuppressed()

اگر بخواهی تمام Exceptionهای ثانویه را ببینی:

<div dir="ltr">

```java
catch (IOException e) {
    for (Throwable t : e.getSuppressed()) {
        logger.error(t.getMessage());
    }
}
```
</div>

این قابلیت در Java 7 اضافه شد.

[بازگشت به بالا](#top)

---

<a id="autocloseable"></a>
## AutoCloseable چیست و چرا طراحی شد؟

<a id="before-java7"></a>
### قبل از Java 7

فرض کنید کتابخانه‌ای نوشته‌اید:

<div dir="ltr">

```java
public class NetworkConnection {
    public void close() { ... }
}
```
</div>

حالا کاربر می‌خواهد بنویسد:

<div dir="ltr">

```java
try (NetworkConnection conn = ...) { }
```
</div>

اما Compiler از کجا بفهمد این کلاس قابل بسته شدن است؟ هیچ راهی وجود نداشت.

به همین دلیل Java یک Contract معرفی کرد:

<div dir="ltr">

```java
public interface AutoCloseable {
    void close() throws Exception;
}
```
</div>

همین. فقط یک متد.

هر کلاسی که این Interface را پیاده‌سازی کند:

<div dir="ltr">

```java
class MyResource implements AutoCloseable
```
</div>

به Compiler می‌گوید: "من یک Resource هستم و باید بسته شوم."

<a id="compiler-transformation"></a>
### Compiler چه کاری انجام می‌دهد؟

فرض کنید بنویسیم:

<div dir="ltr">

```java
try (MyResource resource = new MyResource()) {
    work();
}
```
</div>

Compiler تقریباً این کد را تولید می‌کند:

<div dir="ltr">

```java
MyResource resource = new MyResource();
Throwable primaryException = null;

try {
    work();
} catch (Throwable t) {
    primaryException = t;
    throw t;
} finally {
    if (resource != null) {
        if (primaryException != null) {
            try {
                resource.close();
            } catch (Throwable closeException) {
                primaryException.addSuppressed(closeException);
            }
        } else {
            resource.close();
        }
    }
}
```
</div>

تمام آن منطق پیچیده‌ای که قبلاً خودمان باید می‌نوشتیم، الان توسط Compiler تولید می‌شود.

<a id="autocloseable-vs-closeable"></a>
### چرا AutoCloseable به جای Closeable؟

قبل از Java 7 فقط این Interface وجود داشت:

<div dir="ltr">

```java
java.io.Closeable
```
</div>

مشکل: `void close() throws IOException;` فقط `IOException`.

اما منابع زیادی وجود دارند که اصلاً IOException ندارند. مثلاً:

- Lock
- JDBC
- Native Resource
- GPU Resource
- Thread Context
- Transaction

پس Java گفت: Interface عمومی‌تر می‌خواهیم.

به همین دلیل `AutoCloseable` آمد که می‌گوید:

<div dir="ltr">

```java
void close() throws Exception;
```
</div>

سلسله مراتب:

```
        AutoCloseable
              ▲
              │
        Closeable
```

یعنی `Closeable` از `AutoCloseable` ارث‌بری می‌کند.

[بازگشت به بالا](#top)

---

<a id="multiple-resources"></a>
## چند Resource و ترتیب بسته شدن

یکی از شاهکارهای try-with-resources همین است.

مثلاً:

<div dir="ltr">

```java
try (
    Connection connection = ...;
    PreparedStatement statement = ...;
    ResultSet resultSet = ...;
) {
    // کار با منابع
}
```
</div>

Compiler همه را مدیریت می‌کند.

<a id="lifo"></a>
### قانون مهم: LIFO

Resources دقیقاً برعکس ترتیب ایجاد بسته می‌شوند.

مثلاً:

<div dir="ltr">

```java
try (A; B; C) { }
```
</div>

ترتیب Create:

```
A → B → C
```

ترتیب Close:

```
C → B → A
```

چرا؟ چون ممکن است C به B وابسته باشد.

مثلاً:

```
Connection
    ↓
Statement
    ↓
ResultSet
```

اگر اول Connection را ببندی، ResultSet خراب می‌شود.

پس JVM همیشه آخرین Resource را اول می‌بندد. مثل Stack:

```
Push: A → B → C
Pop:  C → B → A
```

<a id="jdbc-example"></a>
### مثال JDBC

<div dir="ltr">

```java
try (
    Connection connection = ...;
    PreparedStatement statement = ...;
    ResultSet resultSet = ...;
) {
    // کار با منابع
}
```
</div>

ترتیب باز شدن:

```
Connection → Statement → ResultSet
```

ترتیب بسته شدن:

```
ResultSet.close()
    ↓
Statement.close()
    ↓
Connection.close() (بازگشت به Pool)
```

<a id="close-exception"></a>
### اگر close Exception بدهد؟

فرض کن:

```
ResultSet.close() → IOException
    ↓
Statement.close() → SQLException
    ↓
Connection.close() → SQLException
```

آیا فقط یکی ثبت می‌شود؟ خیر.

Java همه را نگه می‌دارد:

```
Primary Exception
    │
    +-- Suppressed 1
    │
    +-- Suppressed 2
```

<a id="kafka-example"></a>
### مثال Production: Kafka

<div dir="ltr">

```java
try (
    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
    MetricsReporter metrics = new MetricsReporter();
) {
    // کار با Consumer
}
```
</div>

ترتیب بسته شدن:

```
Metrics.close()  ← اول
    ↓
Consumer.close() ← دوم
```

[بازگشت به بالا](#top)

---

<a id="design-library"></a>
## طراحی Library با AutoCloseable

اگر خودت کتابخانه بنویسی، مثلاً:

<div dir="ltr">

```java
class CsvReader
```
</div>

و داخلش `File`، `Socket`، `Buffer` باشد، حتماً باید:

<div dir="ltr">

```java
implements AutoCloseable
```
</div>

داشته باشد.

بعد:

<div dir="ltr">

```java
@Override
public void close() {
    // بستن منابع داخلی
}
```
</div>

کاربر:

<div dir="ltr">

```java
try (CsvReader reader = new CsvReader(...)) {
    // کار با reader
}
```
</div>

بدون اینکه هیچ چیز دیگری بداند.

<a id="spring-quarkus"></a>
### Spring و Quarkus

`ApplicationContext` در Spring نیز `AutoCloseable` است:

<div dir="ltr">

```java
try (AnnotationConfigApplicationContext context = ...) {
    // کار با Spring
}
// پایان بلوک → تمام Beanها Destroy → Context بسته می‌شود
```
</div>

Quarkus نیز برای بسیاری از Resourceها همین Pattern را استفاده می‌کند.

[بازگشت به بالا](#top)

---

<a id="resource-lifecycle"></a>
## Resource Lifecycle و Ownership

Joshua در Item 9 در واقع دارد اصل زیر را آموزش می‌دهد:

```
Acquire
    ↓
   Use
    ↓
Release
```

این همان الگوی معروف **RAII (Resource Acquisition Is Initialization)** در C++ است، اما در جاوا به‌جای Destructor از `try-with-resources` و `AutoCloseable` استفاده می‌شود. تفاوت مهم این است که در جاوا **آزادسازی Resource قطعی است** (با خروج از بلوک)، اما **آزادسازی حافظه غیرقطعی** و بر عهده GC باقی می‌ماند.

<a id="deterministic"></a>
### Deterministic Lifecycle

Joshua در Item 8 گفت: به GC اعتماد نکن.

Item 9 می‌گوید: Lifecycle باید **Deterministic** باشد.

یعنی:

```
  Start
    ↓
   Use
    ↓
Exactly Here ← Release
```

نه اینکه:

```
    Use
    ↓
Maybe GC
    ↓
Maybe Finalizer
    ↓
Maybe Close
```

<a id="ownership"></a>
### Ownership

یکی از مهم‌ترین مفاهیم معماری، **Ownership** است. همیشه باید مشخص باشد: چه کسی مسئول آزاد کردن Resource است؟

مثلاً:

<div dir="ltr">

```java
Connection connection = datasource.getConnection();
```
</div>

چه کسی باید `close()` را صدا بزند؟

قاعده ساده است: **کسی که Resource را ایجاد می‌کند، مالک آن است.**

<div dir="ltr">

```java
Connection connection = datasource.getConnection();
// همان Scope باید connection.close() را نیز انجام دهد
```
</div>

به همین دلیل try-with-resources بهترین انتخاب است.

#### مثال اشتباه

<div dir="ltr">

```java
public Connection open() {
    return datasource.getConnection();
}
```
</div>

بعد:

<div dir="ltr">

```java
Connection c = service.open();
```
</div>

حالا چه کسی باید ببندد؟ Service؟ Caller؟ Repository؟ مشخص نیست. Ownership گم شده است.

#### مثال صحیح

<div dir="ltr">

```java
try (Connection connection = datasource.getConnection()) {
    repository.save(connection);
}
```
</div>

واضح است. مالک همان Scope است.

<a id="nested-resources"></a>
### Nested Resources

فرض کن:

```
Connection
    ↓
Statement
    ↓
ResultSet
```

Ownership نیز Nested است:

- Connection مالک Statement است
- Statement مالک ResultSet است

به همین دلیل بستن نیز برعکس انجام می‌شود:

```
Close
    ↓
ResultSet
    ↓
Statement
    ↓
Connection
```

اگر اول Connection را ببندی، بعد Statement می‌خواهد بسته شود اما دیگر Connection وجود ندارد. بنابراین Dependencyها از بین می‌روند. به همین دلیل Java همیشه LIFO را انتخاب کرده است.

<a id="scope"></a>
### Scope

یکی از پیام‌های پنهان Joshua این است: Resource باید **کوچک‌ترین Scope ممکن** را داشته باشد.

#### اشتباه

<div dir="ltr">

```java
Connection connection = datasource.getConnection();
// ... ۵۰۰ خط کد
// ... ۵۰۰ خط کد
connection.close();
```
</div>

اینجا Connection بی‌دلیل زنده مانده است.

#### بهتر

<div dir="ltr">

```java
try (Connection connection = datasource.getConnection()) {
    repository.save();
}
```
</div>

Connection فقط همان چند خط وجود دارد. این همان اصل Item 57 است: Small Scope.

<a id="connection-pool"></a>
### ارتباط با Connection Pool

بعضی‌ها فکر می‌کنند: `connection.close()` یعنی Connection Destroy می‌شود.

خیر. در HikariCP اتفاق واقعی:

```
  Borrow
    ↓
   Use
    ↓
Return to Pool
```

پس `close()` در واقع **Release Ownership** است، نه Destroy. این نکته در سیستم‌های Enterprise بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="custom-class-design"></a>
## طراحی کلاس‌های خودمان

فرض کن کلاس زیر را نوشته‌ای:

<div dir="ltr">

```java
public class CsvExporter {
    // داخل آن: File, Buffer, Writer
}
```
</div>

#### طراحی اشتباه

<div dir="ltr">

```java
public class CsvExporter {
    public void export() { }
}
```
</div>

هیچ Lifecycle مشخصی ندارد.

#### طراحی صحیح

<div dir="ltr">

```java
public class CsvExporter implements AutoCloseable {
    private final BufferedWriter writer;
    
    public CsvExporter(Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path);
    }
    
    public void export(List<Record> records) throws IOException {
        // نوشتن داده‌ها
    }
    
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
```
</div>

اکنون استفاده از آن استاندارد می‌شود:

<div dir="ltr">

```java
try (CsvExporter exporter = new CsvExporter(Path.of("data.csv"))) {
    exporter.export(records);
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="modern-architecture"></a>
## ارتباط با معماری مدرن

<a id="spring"></a>
### Spring

در Spring Beanها نیز همین اصل وجود دارد:

```
Create Bean
    ↓
 Inject
    ↓
   Use
    ↓
Destroy Bean
```

Spring فقط Lifecycle را مدیریت می‌کند.

`@PreDestroy` و `@PostConstruct` دقیقاً برای همین طراحی شده‌اند:

<div dir="ltr">

```java
@Component
public class DatabaseConnection implements AutoCloseable {
    @PostConstruct
    public void init() {
        // Acquire
    }
    
    @Override
    public void close() {
        // Release
    }
}
```
</div>

<a id="quarkus"></a>
### Quarkus

Quarkus نیز CDI Context را دقیقاً بر همین اساس ساخته است:

```
Create Bean
    ↓
  Inject
    ↓
  Destroy
```

<a id="reactive"></a>
### Reactive Programming

حتی در Reactor و Mutiny نیز همین مفهوم وجود دارد. فقط Resource ممکن است Asynchronous باشد. اما Lifecycle همچنان وجود دارد:

<div dir="ltr">

```java
Mono.using(
    () -> new Connection(),
    connection -> Mono.fromCallable(connection::query),
    Connection::close
)
```
</div>

اینجا `using` دقیقاً همان `try-with-resources` در دنیای Reactive است.

[بازگشت به بالا](#top)

---

<a id="final-message"></a>
## مهم‌ترین پیام Item 9

اگر بخواهیم کل Item 9 را در قالب یک اصل معماری خلاصه کنیم، می‌توان آن را این‌گونه بیان کرد:

```
هر Resource باید:

  Acquire
    ↓
   Use
    ↓
  Release

را در یک Scope مشخص و قابل پیش‌بینی طی کند.
```

`try-with-resources` صرفاً یک قابلیت زبانی نیست؛ بلکه مکانیزمی است که این اصل را به‌صورت خودکار و ایمن پیاده‌سازی می‌کند. به همین دلیل تقریباً تمام کتابخانه‌ها و Frameworkهای مدرن جاوا که با منابع خارجی کار می‌کنند، `AutoCloseable` را به‌عنوان قرارداد استاندارد مدیریت چرخه عمر منابع پذیرفته‌اند.

[بازگشت به بالا](#top)

---

<a id="project-structure"></a>
## ساختار پروژه پیشنهادی

پیشنهاد می‌کنم برای **Item 9** نیز یک پروژه آموزشی با ساختار زیر بسازیم:

```
effectivejava/
└── item09/
    ├── antipattern/
    │   ├── bad1_missing_close/
    │   │   └── MissingCloseExample.java
    │   ├── bad2_try_finally_nested/
    │   │   └── NestedTryFinallyExample.java
    │   ├── bad3_exception_lost/
    │   │   └── LostExceptionExample.java
    │   └── bad4_connection_leak/
    │       └── ConnectionLeakExample.java
    │
    ├── bestpractice/
    │   ├── good1_basic_try_with_resources/
    │   │   └── BasicTryWithResources.java
    │   ├── good2_multiple_resources/
    │   │   └── MultipleResourcesExample.java
    │   ├── good3_custom_autocloseable/
    │   │   └── CustomResource.java
    │   ├── good4_jdbc_production/
    │   │   └── JdbcProductionExample.java
    │   ├── good5_suppressed_exception/
    │   │   └── SuppressedExceptionDemo.java
    │   └── good6_connection_pool/
    │       └── ConnectionPoolExample.java
    │
    └── production/
        ├── CsvExporter/
        │   ├── CsvExporter.java
        │   └── CsvExporterTest.java
        ├── FileProcessor/
        │   ├── FileProcessor.java
        │   └── FileProcessorTest.java
        ├── JdbcRepository/
        │   ├── JdbcRepository.java
        │   └── JdbcRepositoryTest.java
        └── ResourceLifecycleDemo/
            └── ResourceLifecycleDemo.java
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

### جدول مقایسه

| معیار | try-finally | try-with-resources |
|-------|-------------|-------------------|
| خوانایی کد | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| کد تودرتو | بله (با هر Resource جدید) | خیر |
| خطر فراموشی `close()` | بالا | صفر |
| مدیریت Exception اول | ❌ از بین می‌رود | ✅ حفظ می‌شود |
| Suppressed Exceptions | ❌ پشتیبانی نمی‌شود | ✅ پشتیبانی کامل |
| پشتیبانی از چند Resource | تودرتوی زشت | ساده و خوانا |
| پشتیبانی از catch | ✅ بله | ✅ بله |
| معرفی در Java | نسخه ۱ | نسخه ۷ |
| وضعیت فعلی | منسوخ (برای بستن منابع) | روش ارجح |

### قانون طلایی

```
اگر کلاس شما AutoCloseable است → از try-with-resources استفاده کنید
اگر کلاس شما AutoCloseable نیست → آن را AutoCloseable کنید
هرگز از try-finally برای بستن منابع استفاده نکنید
```

### ۵ اصل کلیدی Item 9

1. **Memory ≠ Resource** - GC فقط Memory را مدیریت می‌کند، نه External Resources را
2. **Lifecycle باید Deterministic باشد** - نه وابسته به GC
3. **Ownership باید مشخص باشد** - کسی که Resource را ایجاد می‌کند، مسئول Release آن است
4. **هر Resource باید AutoCloseable باشد** - تا بتوان از try-with-resources استفاده کرد
5. **Scope تا حد امکان کوچک باشد** - Resource را فقط تا زمانی که نیاز است نگه دارید

---

> **نکته نهایی:** Joshua Bloch در این Item در واقع یک **الگوی طراحی (Design Pattern)** را آموزش می‌دهد، نه صرفاً یک Syntax جدید. `try-with-resources` تجسم زبانی یک اصل مهم طراحی است: **مدیریت قطعی چرخه عمر منابع**. امروزه در تمام سیستم‌های Enterprise، از JDBC Connection Pool گرفته تا Kafka Consumer، فایل‌ها، Socketها و حتی APIهای Reactive، این اصل را می‌بینید.

[بازگشت به بالا](#top)

</div>
```