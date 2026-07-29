<div dir="rtl">

<a id="top"></a>

# آیتم ۲۰: Interface را بر Abstract Class ترجیح دهید

## (Prefer Interfaces to Abstract Classes)

اگر بخواهم فقط **یک اصل معماری** را انتخاب کنم که بیشترین تأثیر را روی طراحی Frameworkهای مدرن جاوا گذاشته باشد، احتمالاً همین اصل است.

امروزه تقریباً تمام Frameworkهای بزرگ JVM بر اساس این قانون طراحی شده‌اند:

- Spring Framework
- Spring Boot
- Quarkus
- Micronaut
- Jakarta EE
- Netty
- SLF4J
- JPA
- JDBC
- Java Collections Framework

همه یک ویژگی مشترک دارند:
<div dir="ltr">

> **Programming to Interfaces, not Implementations**
</div>
این همان چیزی است که Joshua Bloch در این Item توضیح می‌دهد.

---

## فهرست مطالب

- [بخش اول: چرا Interface مهم‌ترین ابزار طراحی در Java مدرن است؟](#part1)
  - [قبل از Java 8 چه تفاوتی وجود داشت؟](#pre-java8)
  - [اولین تفاوت اساسی: Single Inheritance](#single-inheritance)
  - [مشکل Abstract Class](#problem-abstract)
  - [نگاه معماری](#architectural-view)
  - [Retrofit چیست؟](#retrofit)
  - [مثال واقعی از JDK](#jdk-example)
  - [Mixin چیست؟](#mixin)
  - [چرا Abstract Class برای Mixin مناسب نیست؟](#mixin-abstract)
  - [مثال واقعی از Spring](#spring-example)
  - [انفجار ترکیبی (Combinatorial Explosion)](#combinatorial-explosion)
  - [خلاصه بخش اول](#part1-summary)

- [بخش دوم: Default Method و Skeletal Implementation](#part2)
  - [مشکل Interface چیست؟](#interface-problem)
  - [راه‌حل اول: Default Method](#default-method)
  - [محدودیت Default Method](#default-limitations)
  - [Skeletal Implementation چیست؟](#skeletal-intro)
  - [چرا اسمش Skeletal است؟](#skeletal-name)
  - [مثال ساده](#simple-example)
  - [Primitive Methods](#primitive-methods)
  - [Template Method Pattern](#template-method)
  - [مثال واقعی JDK: AbstractList](#abstractlist)
  - [Adapter Pattern](#adapter-pattern)
  - [Simulated Multiple Inheritance](#simulated-multiple)
  - [Default Method یا Skeletal Implementation؟](#default-vs-skeletal)
  - [نگاه معماری](#architectural-view2)

- [بخش سوم: Skeletal Implementation Pattern](#part3)
  - [مشکل Interface بزرگ](#large-interface)
  - [ساختار کلاسیک](#classic-structure)
  - [مثال ساده Shape](#shape-example)
  - [Primitive Methods](#primitive-methods2)
  - [طراحی صحیح](#proper-design)
  - [مزایا](#advantages)
  - [رابطه با Template Method Pattern](#template-relationship)

- [بخش چهارم: تحلیل `AbstractMapEntry`](#part4)
  - [Map.Entry چیست؟](#map-entry)
  - [Skeletal Implementation](#skeletal-entry)
  - [چرا equals از getKey استفاده می‌کند؟](#why-getkey)
  - [چرا equals این‌گونه نوشته شده؟](#why-equals)
  - [Objects.equals](#objects-equals)
  - [hashCode با XOR](#hashcode-xor)
  - [toString](#tostring)
  - [چرا setValue استثنا پرتاب می‌کند؟](#setvalue-exception)
  - [مقایسه حجم کار](#work-comparison)
  - [DRY Principle](#dry)
  - [چرا این روش فوق‌العاده است؟](#why-great)
  - [محدودیت Default Method](#default-limitation2)

- [بخش پنجم: Production Decision Guide](#part5)
  - [گزینه اول: فقط Interface](#option1)
  - [گزینه دوم: Interface + Default Method](#option2)
  - [گزینه سوم: Interface + Abstract Class](#option3)
  - [گزینه چهارم: فقط Abstract Class](#option4)
  - [جدول تصمیم‌گیری](#decision-table)
  - [دیدگاه Spring Framework](#spring-view)
  - [دیدگاه Quarkus](#quarkus-view)
  - [دیدگاه Netty](#netty-view)
  - [اشتباهات رایج](#common-mistakes)
  - [قانون طلایی Bloch](#golden-rule)
  - [نگاه معمار نرم‌افزار](#architecture-view)
  - [خلاصه نهایی Item 20](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول — چرا Interface مهم‌ترین ابزار طراحی در Java مدرن است؟

<a id="pre-java8"></a>
### قبل از Java 8 چه تفاوتی وجود داشت؟

قبل از Java 8 وضعیت این‌گونه بود:

**Interface:** فقط قرارداد (Contract)

<div dir="ltr">

```java
interface Cache {
    void put(String key, Object value);
    Object get(String key);
}
```
</div>

هیچ پیاده‌سازی نداشت.

**Abstract Class:** هم قرارداد، هم پیاده‌سازی

<div dir="ltr">

```java
abstract class AbstractCache {
    public void log() { }
    abstract void put(...);
}
```
</div>

اما از Java 8 به بعد، Interface هم می‌تواند پیاده‌سازی داشته باشد:

<div dir="ltr">

```java
interface Cache {
    Object get(String key);
    void put(String key, Object value);

    default boolean contains(String key) {
        return get(key) != null;
    }
}
```
</div>

پس سؤال پیش می‌آید: اگر Interface هم Implementation دارد، پس Abstract Class چه کاربردی دارد؟ این همان سؤال اصلی Item 20 است.

<a id="single-inheritance"></a>
### اولین تفاوت اساسی: Java فقط Single Inheritance دارد

<div dir="ltr">

```java
class A extends B        // ✅ مجاز است
class A extends B, C     // ❌ غیرمجاز
```
</div>

اما Interface چنین محدودیتی ندارد:

<div dir="ltr">

```java
class A implements X, Y, Z   // ✅ کاملاً مجاز است
interface X extends A, B, C  // ✅ کاملاً مجاز است
```
</div>

این تفاوت کوچک، یکی از بزرگ‌ترین تفاوت‌های معماری Java است.

<a id="problem-abstract"></a>
### مشکل Abstract Class

فرض کنید داریم:

<div dir="ltr">

```java
abstract class DatabaseClient
abstract class Retryable
```
</div>

حالا می‌خواهیم `MyRepository` هر دو ویژگی را داشته باشد. امکان‌پذیر نیست:

<div dir="ltr">

```java
class MyRepository extends DatabaseClient, Retryable  // ❌ Compiler Error
```
</div>

در نتیجه Abstract Class همیشه شما را مجبور می‌کند یک انتخاب انجام دهید.

Interface این محدودیت را ندارد:

<div dir="ltr">

```java
class MyRepository
        implements Repository, Retryable, Closeable, Serializable
```
</div>

هر تعداد Interface که بخواهیم.

<a id="architectural-view"></a>
### نگاه معماری

فرض کنید در یک سیستم بانکی داریم:
<div dir="ltr">

```
Printable
Exportable
Auditable
Versionable
Searchable
Encryptable
Compressible
```
</div>
اگر همه Abstract Class باشند، تقریباً هیچ کلاسی قابل طراحی نیست. چون `Invoice` هم Printable است، هم Exportable، هم Auditable، هم Searchable، هم Encryptable و ...

اما با Interface:

<div dir="ltr">

```java
class Invoice
        implements Printable, Exportable, Searchable, Auditable, Encryptable
```
</div>

همه چیز ساده می‌شود.

<a id="retrofit"></a>
### Retrofit چیست؟

Joshua Bloch یک نکته بسیار مهم می‌گوید.

فرض کنید ده سال پیش چنین کلاسی نوشته‌اید:

<div dir="ltr">

```java
class Order { }
```
</div>

بعداً Interface جدیدی معرفی می‌شود:

<div dir="ltr">

```java
Comparable<Order>
```
</div>

کافی است:

<div dir="ltr">

```java
class Order implements Comparable<Order>
```
</div>

را اضافه کنید. تمام شد.

اما اگر بخواهیم Abstract Class اضافه کنیم:

<div dir="ltr">

```java
abstract class AbstractOrder
```
</div>

دیگر تقریباً غیرممکن است. چون `Order` ممکن است همین الان `extends Entity` داشته باشد.

پس Interface قابلیت بسیار مهمی دارد: **Retrofitting** - یعنی بعداً هم می‌توان آن را به کلاس‌های موجود اضافه کرد، بدون اینکه سلسله‌مراتب ارث‌بری را به هم بزنیم.

<a id="jdk-example"></a>
### مثال واقعی از JDK

Java در نسخه‌های مختلف دقیقاً همین کار را انجام داده است.

مثلاً `Iterable` بعداً اضافه شد. ولی `ArrayList`، `LinkedList`، `HashSet`، `TreeSet` همه بدون مشکل `implements Iterable` شدند.

همین اتفاق برای `Comparable`، `AutoCloseable` و `Serializable` نیز افتاد. اگر این‌ها Abstract Class بودند، تقریباً اضافه کردنشان غیرممکن بود.

<a id="mixin"></a>
### Mixin چیست؟

یکی از مفاهیم مهم این Item.

Mixin یعنی:

> یک قابلیت اختیاری که می‌تواند به هر کلاسی اضافه شود، بدون اینکه نوع اصلی آن را تغییر دهد.

مثلاً `Comparable` یک قابلیت است: "This object can be compared."

`Employee` نوع اصلی است، اما می‌تواند `Comparable` هم باشد. یا `Invoice` می‌تواند `Serializable`، `Cloneable`، `AutoCloseable` هم باشد. این‌ها همه Mixin هستند.

<a id="mixin-abstract"></a>
### چرا Abstract Class برای Mixin مناسب نیست؟

فرض کنید:

<div dir="ltr">

```java
abstract class SerializableObject
```
</div>

بعد:

<div dir="ltr">

```java
class Invoice extends SerializableObject
```
</div>

اما Invoice همین حالا `extends BaseEntity` است. پس تمام شد. دیگر SerializableObject قابل استفاده نیست.

به همین دلیل Mixin تقریباً همیشه Interface است.

<a id="spring-example"></a>
### مثال واقعی از Spring

در Spring تقریباً همه چیز Interface است. مثلاً:
<div dir="ltr">

```
BeanFactory
ApplicationContext
Resource
Environment
Validator
Converter
Formatter
BeanPostProcessor
FactoryBean
Lifecycle
MessageSource
```
</div>
چرا؟ چون هر کلاس می‌تواند چندین نقش مختلف داشته باشد:

<div dir="ltr">

```java
class MyBean
        implements BeanPostProcessor, BeanFactoryAware, Ordered, InitializingBean
```
</div>

اگر این‌ها Abstract Class بودند، طراحی Spring تقریباً غیرممکن می‌شد.

<a id="combinatorial-explosion"></a>
### انفجار ترکیبی (Combinatorial Explosion)

Joshua Bloch به یک مشکل کلاسیک اشاره می‌کند.

فرض کنید سه ویژگی داریم: `Singer`، `SongWriter`، `Dancer`.

اگر Abstract Class استفاده کنیم باید برای هر ترکیب، یک کلاس جدید بسازیم:
<div dir="ltr">

```
Singer
SongWriter
SingerSongWriter
SingerDancer
SongWriterDancer
SingerSongWriterDancer
...
```
</div>
با افزایش تعداد ویژگی‌ها، تعداد ترکیب‌ها به صورت نمایی رشد می‌کند (برای n ویژگی، تا 2ⁿ ترکیب ممکن است).

اما با Interface:

<div dir="ltr">

```java
class Artist implements Singer, SongWriter, Dancer
```
</div>

همه چیز ساده باقی می‌ماند.

<a id="part1-summary"></a>
### خلاصه بخش اول

از دید Joshua Bloch، **Interface باید انتخاب پیش‌فرض برای تعریف Type باشد**:

| قابلیت | Interface | Abstract Class |
|--------|-----------|----------------|
| Multiple Implementation | ✅ | ❌ |
| قابل اضافه شدن به کلاس‌های موجود (Retrofit) | ✅ | ❌ |
| مناسب برای Mixin | ✅ | ❌ |
| مناسب برای طراحی نقش‌ها (Roles) | ✅ | ❌ |
| جلوگیری از انفجار سلسله‌مراتب | ✅ | ❌ |
| انعطاف در معماری | بسیار زیاد | محدود |

**نتیجه کلیدی:** Abstract Class زمانی ارزشمند است که بخواهید **اشتراک پیاده‌سازی (implementation reuse)** داشته باشید، اما Interface زمانی انتخاب مناسب است که بخواهید **یک قرارداد (contract) یا قابلیت (capability)** را تعریف کنید که بتواند مستقل از سلسله‌مراتب کلاس‌ها در هر جایی استفاده شود.

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم — Default Method و Skeletal Implementation

<a id="interface-problem"></a>
### مشکل Interface چیست؟

فرض کنید یک Interface طراحی کرده‌ایم:

<div dir="ltr">

```java
public interface Cache {
    void put(String key, Object value);
    Object get(String key);
    boolean contains(String key);
    void clear();
    int size();
}
```
</div>

حالا ده تیم مختلف می‌خواهند آن را پیاده‌سازی کنند: RedisCache، EhCache، InMemoryCache، CaffeineCache، HazelcastCache و ...

مشکل چیست؟ همه مجبورند `contains()` را دوباره بنویسند. در حالی که `contains()` کاملاً واضح است: `return get(key) != null;`

پس چرا همه باید این کد را دوباره بنویسند؟

<a id="default-method"></a>
### راه‌حل اول: Default Method

از Java 8:

<div dir="ltr">

```java
public interface Cache {
    Object get(String key);

    default boolean contains(String key) {
        return get(key) != null;
    }
}
```
</div>

الان تمام Implementorها رایگان این قابلیت را دریافت می‌کنند.

این دقیقاً همان چیزی است که Joshua Bloch می‌گوید:

> اگر یک متد بتواند بر اساس متدهای دیگر Interface پیاده‌سازی شود، آن را Default Method کن.

<a id="default-limitations"></a>
### محدودیت Default Method

همه چیز را نمی‌توان داخل Interface نوشت. مثلاً `equals()`، `hashCode()` یا `toString()` را نمی‌توان Default کرد. دلیل؟ چون Interface اجازه Override کردن متدهای Object را ندارد.

همچنین Interface نمی‌تواند `instance field` داشته باشد. مثلاً `private int count;` غیرممکن است.

پس اگر State داشته باشیم، Default Method دیگر کافی نیست.

<a id="skeletal-intro"></a>
### Skeletal Implementation چیست؟

ایده بسیار ساده است. به جای اینکه Interface همه چیز را انجام دهد، یک Abstract Class کنارش قرار می‌دهیم:
<div dir="ltr">

```
Interface → AbstractInterface → Concrete Classes
```
</div>
مثلاً `List` کنارش `AbstractList` وجود دارد. `Set` کنارش `AbstractSet`. `Map` کنارش `AbstractMap`. این‌ها همان Skeletal Implementation هستند.

<a id="skeletal-name"></a>
### چرا اسمش Skeletal است؟

چون اسکلت اصلی را آماده می‌کند. مثل اسکلت ساختمان.

Concrete Class فقط قسمت‌های خاص خودش را می‌نویسد. بقیه آماده است.

<a id="simple-example"></a>
### مثال ساده

فرض کنید Interface داریم:

<div dir="ltr">

```java
public interface Counter {
    void increment();
    int value();
}
```
</div>

اگر صد کلاس بخواهند این را پیاده‌سازی کنند، همه مجبورند `toString()`، `equals()` و `hashCode()` را دوباره بنویسند.

راه بهتر:

<div dir="ltr">

```java
public abstract class AbstractCounter implements Counter {
    @Override
    public String toString() {
        return String.valueOf(value());
    }
}
```
</div>

الان هر Implementor فقط `increment()` و `value()` را می‌نویسد.

<a id="primitive-methods"></a>
### Primitive Methods

Joshua Bloch یک مفهوم بسیار مهم معرفی می‌کند. تمام متدها ارزش یکسان ندارند. بعضی متدها Primitive هستند. یعنی اگر آن‌ها را داشته باشیم، بقیه متدها قابل تولید هستند.

مثلاً در Map.Entry، Primitiveها: `getKey()`، `getValue()`، `setValue()` هستند. اما `equals()`، `hashCode()` و `toString()` همه از همین سه متد ساخته می‌شوند.

به همین دلیل AbstractMapEntry این سه Primitive را Abstract نگه می‌دارد. بقیه را خودش می‌سازد.

<a id="template-method"></a>
### Template Method Pattern

Joshua Bloch می‌گوید: Skeletal Implementation در واقع همان **Template Method Pattern** است.

ساختار کلی:
<div dir="ltr">

```
Abstract Class
    ↓
Primitive Methods
    ↓
Concrete Algorithm
```
</div>

مثلاً `sort()` → `compare()` یا `save()` → `serialize()`

Framework Algorithm را می‌داند. Programmer فقط Primitiveها را می‌نویسد.

<a id="abstractlist"></a>
### مثال واقعی JDK: AbstractList

نگاه کنید به `AbstractList`. شما فقط `get(index)` و `size()` را پیاده‌سازی می‌کنید. ولی رایگان دریافت می‌کنید:
<div dir="ltr">

```
iterator()
contains()
indexOf()
lastIndexOf()
subList()
equals()
hashCode()
toArray()
...
```
</div>
تقریباً ده‌ها متد. به جای نوشتن ۳۰ متد، فقط ۲ متد می‌نویسید.

<a id="adapter-pattern"></a>
### Adapter Pattern

Joshua Bloch مثال معروفی دارد: `int[]` را تبدیل می‌کند به `List<Integer>` بدون Copy.
<div dir="ltr">

```
int[] → AbstractList → List<Integer>
```
</div>
این دقیقاً یک Adapter است. یعنی یک API → API دیگر.

امروزه همین ایده را در Spring زیاد می‌بینیم. مثلاً `HttpServletRequest → NativeWebRequest` یا `ResultSet → RowMapper`.

<a id="simulated-multiple"></a>
### Simulated Multiple Inheritance

این قسمت خیلی مهم است. فرض کنید کلاس شما قبلاً `extends SomeFrameworkClass` دارد. دیگر `extends AbstractList` ممکن نیست.

Joshua Bloch راه‌حل جالبی می‌دهد. به جای Extend کردن، داخل کلاس `AbstractList` را نگه می‌داریم:
<div dir="ltr">

```
MyClass → contains → Private AbstractList
```
</div>
و تمام فراخوانی‌ها را به آن Forward می‌کنیم. به این روش می‌گوید **Simulated Multiple Inheritance**. یعنی ظاهرش شبیه Multiple Inheritance است، اما در واقع از Composition و Forwarding استفاده می‌کند؛ همان ایده‌ای که در Item 18 با Wrapper Pattern آشنا شدیم.

<a id="default-vs-skeletal"></a>
### Default Method یا Skeletal Implementation؟

| معیار | Default Method | Skeletal Implementation |
|-------|----------------|------------------------|
| نیاز به State | ❌ | ✅ |
| نیاز به Field | ❌ | ✅ |
| Override متدهای Object | ❌ | ✅ |
| پیاده‌سازی ساده بر پایه سایر متدها | ✅ | ✅ |
| اشتراک کد پیچیده | محدود | بسیار مناسب |
| اجبار به ارث‌بری | ❌ | ✅ (در صورت استفاده مستقیم) |

<a id="architectural-view2"></a>
### نگاه معماری

در طراحی APIهای Production معمولاً این الگو استفاده می‌شود:
<div dir="ltr">

```
Interface
    │
    ├──────── Default Methods
    │
    ▼
Abstract Skeletal Class
    │
    ▼
Concrete Implementations
```
</div>
این ساختار بهترین تعادل را بین **انعطاف‌پذیری Interface** و **اشتراک پیاده‌سازی** ایجاد می‌کند.

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم — Skeletal Implementation Pattern (الگوی پیاده‌سازی اسکلت)

<a id="large-interface"></a>
### مشکل Interface بزرگ

فرض کنیم Interface بزرگی داریم که ۲۰ متد دارد. اگر کسی بخواهد آن را پیاده‌سازی کند باید همه را بنویسید. حتی اگر نصف آنها قابل محاسبه از روی بقیه باشند.

مثلاً `isEmpty()` همیشه می‌تواند باشد: `return size() == 0;` یا `contains()` می‌تواند فقط Iterator را استفاده کند.

یعنی Implementor بی‌خود دارد دوباره کد می‌نویسد.

<a id="classic-structure"></a>
### ساختار کلاسیک
<div dir="ltr">

```
Interface
    ↓
Abstract Interface
    ↓
Concrete Class
```
</div>
مثلاً:
<div dir="ltr">

```
List → AbstractList → ArrayList → LinkedList
Map → AbstractMap → HashMap → TreeMap → ConcurrentHashMap
```
</div>
این دقیقاً چیزی است که Collections Framework انجام داده است.

<a id="shape-example"></a>
### مثال ساده Shape

فرض کنیم Interface زیر را داریم:

<div dir="ltr">

```java
public interface Shape {
    double area();
    double perimeter();
    boolean isEmpty();
}
```
</div>

تمام Shapeها باید Area و Perimeter داشته باشند. اما `isEmpty()` تقریباً همیشه `area() == 0` است.

پس Abstract Class:

<div dir="ltr">

```java
public abstract class AbstractShape implements Shape {
    @Override
    public boolean isEmpty() {
        return area() == 0;
    }
}
```
</div>

حالا Circle:

<div dir="ltr">

```java
public class Circle extends AbstractShape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }
}
```
</div>

Implementor فقط دو متد نوشت. بقیه رایگان.

<a id="primitive-methods2"></a>
### Primitive Methods

Bloch یک اصطلاح مهم معرفی می‌کند: Primitive Method.

یعنی کمترین تعداد متدی که اگر Implement شوند، تمام Interface قابل ساختن باشد.

مثلاً در List، Primitiveها تقریباً اینها هستند: `get()`، `size()`، `set()`، `add()`، `remove()`. بقیه متدها مثل `contains()`، `isEmpty()`، `indexOf()`، `lastIndexOf()`، `iterator()` همه از روی همین‌ها ساخته می‌شوند.

پس AbstractList فقط Primitiveها را Abstract می‌گذارد.

<a id="proper-design"></a>
### طراحی صحیح

فرض کن Interface:

<div dir="ltr">

```java
interface Cache {
    void put(String key, Object value);
    Object get(String key);
    boolean contains(String key);
    boolean isEmpty();
    int size();
}
```
</div>

Primitiveها: `put`، `get`، `size`

سپس AbstractCache می‌نویسد:

<div dir="ltr">

```java
public abstract class AbstractCache implements Cache {
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object key) {
        return get(key) != null;
    }
}
```
</div>

حالا Implementor فقط `put`، `get`، `size` را پیاده‌سازی می‌کند.

<a id="advantages"></a>
### مزایا

**۱) کاهش حجم کد:** به جای ۲۰ متد، شاید فقط ۵ متد.

**۲) همه Implementها رفتار یکسان دارند:** همه `contains()` دقیقاً یک رفتار دارند. هیچ باگی بین پیاده‌سازی‌ها ایجاد نمی‌شود.

**۳) تغییر آسان:** اگر فردا `contains()` بهبود پیدا کند، فقط Abstract Class تغییر می‌کند. همه کلاس‌ها سود می‌برند.

**۴) کاهش Duplicate Code:** اصل معروف DRY کاملاً رعایت می‌شود.

<a id="template-relationship"></a>
### رابطه با Template Method Pattern

Skeletal Implementation در واقع یک کاربرد از الگوی طراحی **Template Method** است.

در این الگو:

- مراحل کلی الگوریتم در کلاس پایه تعریف می‌شود.
- بخش‌های متغیر (Primitive Methods) به زیرکلاس‌ها واگذار می‌شود.
<div dir="ltr">

```
AbstractCache
│
├── contains()   ← الگوریتم ثابت
│      │
│      └── get() ← بخش قابل سفارشی‌سازی
│
└── isEmpty()
       │
       └── size() ← بخش قابل سفارشی‌سازی
```
</div>
در نتیجه، کلاس پایه رفتار مشترک را تضمین می‌کند و پیاده‌سازی‌های مختلف فقط جزئیات لازم را فراهم می‌کنند.

[بازگشت به بالا](#top)

---

<a id="part4"></a>
## بخش چهارم — تحلیل `AbstractMapEntry` و طراحی یک Skeletal Implementation واقعی

<a id="map-entry"></a>
### Map.Entry چیست؟

تقریباً همه فکر می‌کنند: `HashMap` → `Map.Entry`. اما در واقع:
<div dir="ltr">

```
Map → Map.Entry
```
</div>
یعنی Entry فقط یک Interface کوچک است:

<div dir="ltr">

```java
public interface Entry<K,V> {
    K getKey();
    V getValue();
    V setValue(V value);
}
```
</div>

تمام. یعنی فقط سه Primitive Method دارد.

<a id="skeletal-entry"></a>
### Skeletal Implementation

Bloch کلاس زیر را معرفی می‌کند:

<div dir="ltr">

```java
public abstract class AbstractMapEntry<K,V>
        implements Map.Entry<K,V> {

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map.Entry)) return false;

        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

        return Objects.equals(e.getKey(), getKey())
                && Objects.equals(e.getValue(), getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey())
                ^ Objects.hashCode(getValue());
    }

    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
```
</div>

<a id="why-getkey"></a>
### چرا equals از getKey استفاده می‌کند؟

<div dir="ltr">

```java
Objects.equals(e.getKey(), getKey())
```
</div>

نه اینکه بنویسد `key.equals(...)`. چرا؟ چون AbstractMapEntry اصلاً نمی‌داند کلید را کجا ذخیره کرده‌ای. ممکن است `private K key;` باشد، یا `private Node<K,V> node;` باشد، یا `ByteBuffer` باشد، یا حتی Database باشد.

Abstract Class هیچ اطلاعی ندارد. تنها قرارداد Interface را می‌شناسد. یعنی `getKey()`.

به همین دلیل تمام الگوریتم فقط از Primitive Methodها استفاده می‌کند.

<a id="why-equals"></a>
### چرا equals این‌گونه نوشته شده؟

<div dir="ltr">

```java
if (o == this)          // سریع‌ترین حالت
if (!(o instanceof Map.Entry))  // نه AbstractMapEntry
```
</div>

چون قرارداد Interface مهم است، نه کلاس. ممکن است `HashMap.Entry`، `TreeMap.Entry`، `ConcurrentHashMap.Entry` همه با هم برابر باشند. چون Interface رفتار را تعریف کرده است.

<a id="objects-equals"></a>
### Objects.equals

به جای `key.equals(other)` از `Objects.equals()` استفاده شده. چرا؟ اگر Key برابر `null` باشد، کد قبلی `NullPointerException` می‌دهد. اما `Objects.equals()` این را مدیریت می‌کند.

<a id="hashcode-xor"></a>
### hashCode با XOR

<div dir="ltr">

```java
Objects.hashCode(getKey()) ^ Objects.hashCode(getValue())
```
</div>

چرا XOR؟ زیرا قرارداد Map.Entry می‌گوید: HashCode باید ترکیب Key و Value باشد.

نکته مهم: باز هم `getKey()` استفاده شده، نه Field.

<a id="tostring"></a>
### toString

<div dir="ltr">

```java
return getKey() + "=" + getValue();
```
</div>

باز هم Primitive Method.

<a id="setvalue-exception"></a>
### چرا setValue استثنا پرتاب می‌کند؟

<div dir="ltr">

```java
throw new UnsupportedOperationException();
```
</div>

چون همه Entryها Mutable نیستند. مثلاً `Map.of(...)` Immutable است. بنابراین Implementation پیش‌فرض `UnsupportedOperationException` است. اگر کسی Mutable باشد Override می‌کند.

<a id="work-comparison"></a>
### مقایسه حجم کار

بدون Skeletal: ۶ متد (`getKey`، `getValue`، `setValue`، `equals`، `hashCode`، `toString`)

با Skeletal: فقط ۳ متد (`getKey`، `getValue`، `setValue`)

نصف کار حذف شد.

<a id="dry"></a>
### DRY Principle

Don't Repeat Yourself. تمام منطق مشترک در یک جا.

<a id="why-great"></a>
### چرا این روش فوق‌العاده است؟

فرض کن فردا HashCode نیاز به Bug Fix داشته باشد. اگر ۲۰۰ پیاده‌سازی وجود داشته باشد، همه باید اصلاح شوند. اما حالا فقط `AbstractMapEntry` اصلاح می‌شود. همه کلاس‌ها درست می‌شوند.

<a id="default-limitation2"></a>
### محدودیت Default Method

در انتهای این مثال، Bloch به یک محدودیت مهم اشاره می‌کند: چرا این منطق را مستقیماً داخل Interface قرار نداد؟

زیرا متدهای `equals`، `hashCode` و `toString` متعلق به کلاس پایه `Object` هستند و **Java اجازه نمی‌دهد Interface برای این متدها Default Method ارائه کند.**

بنابراین:
<div dir="ltr">

- `default equals(...)` ❌
- `default hashCode()` ❌
- `default toString()` ❌
</div>
به همین دلیل هنوز هم در بسیاری از APIهای حرفه‌ای، در کنار Interface یک Abstract Skeletal Implementation وجود دارد.

[بازگشت به بالا](#top)

---

<a id="part5"></a>
## بخش پنجم — Production Decision Guide

تا اینجا سه ابزار مختلف را دیدیم: Interface، Abstract Class، Skeletal Implementation (Interface + Abstract Class).

اما سؤال اصلی این است: در پروژه‌های واقعی کدام را انتخاب کنیم؟

<a id="option1"></a>
### گزینه اول: فقط Interface

<div dir="ltr">

```java
public interface PaymentService {
    PaymentResult pay(PaymentRequest request);
}
```
</div>

**مزایا:** کاملاً Flexible. هر کسی هر طور خواست پیاده‌سازی می‌کند.

**مناسب برای:** SPI، Plugin، Microservice Contract، SDK

**نمونه‌های JDK:** `Runnable`، `Callable`، `Comparator`، `Predicate`، `Supplier`، `Consumer`

چرا؟ چون هیچ Behavior مشترکی ندارند.

<a id="option2"></a>
### گزینه دوم: Interface + Default Method

<div dir="ltr">

```java
public interface Cache {
    void put(...);
    Object get(...);
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }
}
```
</div>

**چه زمانی؟** وقتی Behavior ساده است. مثل `isEmpty()`، `contains()`، `isPresent()`، `isBlank()`. همه فقط چند خط هستند. نیازی به Abstract Class نیست.

<a id="option3"></a>
### گزینه سوم: Interface + Abstract Class

این همان توصیه اصلی Bloch است.
<div dir="ltr">

```
Cache → AbstractCache → RedisCache → CaffeineCache → EhCache
```
</div>
Abstract Class تمام منطق مشترک را نگه می‌دارد. Subclass فقط Primitive Methodها را پیاده‌سازی می‌کند.

<a id="option4"></a>
### گزینه چهارم: فقط Abstract Class

<div dir="ltr">

```java
abstract class Animal {
    void eat() { }
    void sleep() { }
    abstract void move();
}
```
</div>

Bloch می‌گوید: این انتخاب معمولاً ضعیف‌تر از Interface است. چرا؟ چون Single Inheritance. اگر بعداً بخواهی `Serializable`، `Comparable`، `Cloneable` یا هر Interface دیگری را اضافه کنی مشکلی نیست، اما اگر از قبل از یک Abstract Class ارث برده باشی، دیگر نمی‌توانی از Abstract Class دیگری ارث ببری.

<a id="decision-table"></a>
### جدول تصمیم‌گیری

| سناریو | بهترین انتخاب | دلیل |
|--------|---------------|------|
| فقط تعریف قرارداد | Interface | بیشترین انعطاف |
| چند متد ساده قابل اشتراک | Interface + Default Method | کمترین پیچیدگی |
| منطق مشترک زیاد | Interface + Abstract Class | کاهش تکرار کد |
| فقط یک خانواده بسته از کلاس‌ها | Abstract Class | زمانی که وراثت واقعاً لازم است |

<a id="spring-view"></a>
### دیدگاه Spring Framework

در Spring تقریباً همه چیز با Interface شروع می‌شود:
<div dir="ltr">

```
BeanFactory → ListableBeanFactory → ConfigurableBeanFactory → DefaultListableBeanFactory
```
</div>
یا:
<div dir="ltr">

```
ApplicationContext → AbstractApplicationContext → GenericApplicationContext → AnnotationConfigApplicationContext
```
</div>
دقیقاً همان Skeletal Implementation.

<a id="quarkus-view"></a>
### دیدگاه Quarkus

Quarkus نیز تقریباً همین الگو را دنبال می‌کند. ابتدا Interface، سپس Base Implementation:
<div dir="ltr">

```
Recorder Interface → Abstract Recorder → Runtime Recorder
```
</div>
هدف: حداکثر انعطاف، حداقل Coupling.

<a id="netty-view"></a>
### دیدگاه Netty

در Netty نیز بارها این ساختار را می‌بینیم:
<div dir="ltr">

```
Channel → AbstractChannel → AbstractNioChannel → SocketChannel
```
</div>
یا:
<div dir="ltr">

```
ByteBuf → AbstractByteBuf → PooledByteBuf
```
</div>
این دقیقاً همان الگوی Bloch است.

<a id="common-mistakes"></a>
### اشتباهات رایج

**اشتباه شماره ۱:** نوشتن Abstract Class بدون Interface

مثلاً `AbstractPaymentService` اما `PaymentService` وجود ندارد. بعداً Testing، Mocking، Decorating تقریباً سخت می‌شود.

**اشتباه شماره ۲:** Interface با ۵۰ متد، بدون Default Method، بدون Skeletal Implementation. Implement کردن آن کابوس است. این Interface بیش از حد بزرگ است و معمولاً نشان‌دهنده‌ی نقض **اصل Interface Segregation (ISP)** است.

**اشتباه شماره ۳:** قرار دادن State داخل Interface. Interface برای Behavior است، نه State. به همین دلیل Interface نمی‌تواند Instance Field داشته باشد.

**اشتباه شماره ۴:** قرار دادن Business Logic پیچیده داخل Default Method. Default Method قرار نیست جای Service شود. اگر منطق ۲۰۰ خط است، به احتمال زیاد باید داخل Abstract Class باشد.

<a id="golden-rule"></a>
### قانون طلایی Bloch

می‌توان توصیه‌های این Item را به چند قانون عملی تبدیل کرد:

1. **اول Interface را طراحی کن.**
2. اگر چند متد ساده از روی سایر متدها قابل پیاده‌سازی هستند، از **Default Method** استفاده کن.
3. اگر منطق مشترک قابل‌توجهی وجود دارد، یک **Skeletal Implementation (Abstract Class)** ارائه بده.
4. اگر کاربران واقعاً به پیاده‌سازی‌های متعدد نیاز ندارند، پیچیدگی اضافی ایجاد نکن.

<a id="architecture-view"></a>
### نگاه معمار نرم‌افزار

در معماری مدرن Java، این سه ابزار نقش‌های متفاوتی دارند:

- **Interface** → تعریف قرارداد (Contract) و کاهش Coupling
- **Default Method** → اشتراک رفتارهای ساده و مستقل از State
- **Abstract Class** → اشتراک State و الگوریتم‌های پیچیده

به همین دلیل در فریم‌ورک‌های بزرگی مانند Spring، Hibernate، Netty و Quarkus معمولاً هر سه را در کنار هم می‌بینید، نه اینکه یکی جایگزین دیگری باشد.

<a id="final-summary"></a>
### خلاصه نهایی Item 20

```
                 آیا فقط قرارداد می‌خواهی؟
                        │
               ┌────────┴────────┐
               │                 │
              بله              خیر
               │                 │         Interface         منطق مشترک داری؟
                                 │
                     ┌───────────┴───────────┐
                     │                       │
                   کم                     زیاد
                     │                       │
        Default Methods      Interface + Abstract Class
```

### نتیجه‌گیری کلیدی Effective Java

این Item یکی از مهم‌ترین تغییرات ذهنی در طراحی APIهای جاوا را آموزش می‌دهد:

- **Interface باید نقطه شروع طراحی باشد.**
- **Abstract Class ابزار اشتراک پیاده‌سازی است، نه ابزار اصلی تعریف نوع (Type).**
- **ترکیب Interface و Skeletal Implementation، در صورت نیاز، انعطاف‌پذیرترین و قابل‌نگهداری‌ترین طراحی را ایجاد می‌کند.**

این دقیقاً همان رویکردی است که در کتابخانه‌های استاندارد JDK و اغلب فریم‌ورک‌های Enterprise جاوا به‌کار گرفته شده است.

---

[بازگشت به بالا](#top)

</div>
```