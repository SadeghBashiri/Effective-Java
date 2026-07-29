<div dir="rtl">

<a id="top"></a>

# آیتم ۱۷: Mutability را تا حد ممکن کاهش دهید (Minimize Mutability)

اگر قرار باشد تنها **یک اصل طراحی** را انتخاب کنیم که بیشترین تأثیر را بر کیفیت نرم‌افزارهای Java Enterprise داشته باشد، احتمالاً همان **Immutable بودن Objectها** است.

Joshua Bloch حتی عنوان Item را اینگونه انتخاب کرده است:

> **Minimize Mutability**

نه

> Make Everything Immutable

دلیلش بسیار مهم است. Bloch نمی‌گوید همه چیز را Immutable کنید. بلکه می‌گوید:

> **تا جایی که امکان دارد Mutable بودن را کاهش دهید.**

زیرا هر مقدار Mutability، هزینه‌هایی به سیستم تحمیل می‌کند:

- افزایش پیچیدگی
- افزایش احتمال Bug
- کاهش Thread Safety
- سخت‌تر شدن Reasoning
- دشوار شدن Testing
- افزایش Coupling
- افزایش نیاز به Synchronization

در مقابل، Immutable Objectها بسیاری از این مشکلات را به‌صورت ذاتی حذف می‌کنند.

---

## فهرست مطالب

- [بخش اول: فلسفه Immutable Object و قوانین طراحی](#part1)
  - [Immutable یعنی چه؟](#what-is-immutable)
  - [Mutable در مقابل Immutable](#mutable-vs-immutable)
  - [چرا Bloch روی Immutable بودن تأکید می‌کند؟](#why-immutable)
  - [پنج قانون طلایی ساخت Immutable Class](#five-rules)
    - [قانون اول: هیچ Mutator ننویس](#rule1)
    - [قانون دوم: اجازه ارث‌بری نده](#rule2)
    - [قانون سوم: تمام فیلدها Final باشند](#rule3)
    - [قانون چهارم: تمام فیلدها Private باشند](#rule4)
    - [قانون پنجم: دسترسی به Mutable Objectها را کنترل کن](#rule5)
  - [Immutable با Read Only فرق دارد](#immutable-vs-readonly)
  - [مثال کلاس Complex](#complex-example)
  - [Functional Approach در برابر Imperative Approach](#functional-vs-imperative)
  - [جمع‌بندی بخش اول](#part1-summary)

- [بخش دوم: چرا Immutable Objectها ستون فقرات سیستم‌های مدرن هستند؟](#part2)
  - [مزیت اول: Thread Safety ذاتی](#advantage1)
  - [مزیت دوم: Object Sharing](#advantage2)
  - [مزیت سوم: Object Caching](#advantage3)
  - [مزیت چهارم: حذف Defensive Copy](#advantage4)
  - [مزیت پنجم: Clone بی‌معنی می‌شود](#advantage5)
  - [مزیت ششم: Sharing Internal State](#advantage6)
  - [مزیت هفتم: بهترین Map Key](#advantage7)
  - [مزیت هشتم: بهترین عضو Set](#advantage8)
  - [مزیت نهم: Failure Atomicity](#advantage9)
  - [مزیت دهم: Reasoning ساده‌تر](#advantage10)
  - [نقش Immutable در معماری‌های مدرن](#modern-architectures)
  - [جمع‌بندی بخش دوم](#part2-summary)

- [بخش سوم: طراحی Production-Grade بر پایه Immutable Objects](#part3)
  - [چرا Immutable بودن فقط یک ویژگی کلاس نیست؟](#beyond-class)
  - [Immutable و Java Memory Model](#jmm)
  - [Static Factory + Immutable](#static-factory-immutable)
  - [Immutable و Internal Sharing](#internal-sharing)
  - [مشکل اصلی Immutable](#main-problem)
  - [Mutable Companion Class](#mutable-companion)
  - [Immutable با Static Factory به جای final](#static-factory-instead-of-final)
  - [Lazy Initialization در Immutable Classes](#lazy-immutable)
  - [چه زمانی Immutable مناسب نیست؟](#when-not-immutable)
  - [جدول تصمیم‌گیری معماری](#architectural-decision)
  - [جمع‌بندی نهایی Item 17](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول — فلسفه Immutable Object و قوانین طراحی کلاس‌های Immutable

<a id="what-is-immutable"></a>
### Immutable یعنی چه؟

تعریف Bloch بسیار ساده است:

> شیئی Immutable است که پس از ساخته شدن، وضعیت (State) آن دیگر هرگز تغییر نکند.

یعنی تمام اطلاعات موجود داخل Object، از لحظه‌ی ساخت تا زمان Garbage Collection ثابت باقی می‌ماند.

مثال:

<div dir="ltr">

```java
String name = "Ali";
```
</div>

هیچ متدی وجود ندارد که String را تغییر دهد. مثلاً:

<div dir="ltr">

```java
name.toUpperCase();
```
</div>

خود String تغییر نمی‌کند. بلکه یک String جدید تولید می‌شود:

```
    "Ali"
      |
toUpperCase()
      |
      v
    "ALI"
```

شیء قبلی همچنان وجود دارد.

<a id="mutable-vs-immutable"></a>
### Mutable در مقابل Immutable

فرض کنید:

<div dir="ltr">

```java
class User {
    private String name;

    public void setName(String name) {
        this.name = name;
    }
}
```
</div>

اکنون:

<div dir="ltr">

```java
User user = new User();
user.setName("Ali");
user.setName("Sara");
user.setName("John");
```
</div>

همان Object دائماً تغییر می‌کند. State آن ثابت نیست. این کلاس Mutable است.

اما:

<div dir="ltr">

```java
public final class User {
    private final String name;

    public User(String name) {
        this.name = name;
    }
}
```
</div>

بعد از ساخته شدن `User("Ali")`، دیگر هیچ راهی برای تغییر آن وجود ندارد.

<a id="why-immutable"></a>
### چرا Bloch روی Immutable بودن تأکید می‌کند؟

زیرا Mutable Objectها دارای State Space بسیار بزرگی هستند.

فرض کنید کلاس زیر را داریم:

<div dir="ltr">

```java
class Account {
    private String owner;
    private double balance;
    private boolean active;
}
```
</div>

اگر Setter داشته باشیم، تعداد حالت‌های ممکن تقریباً نامحدود است. در نتیجه:

- تست سخت‌تر می‌شود
- اشکال‌یابی سخت‌تر می‌شود
- اثبات درستی سیستم دشوارتر می‌شود

اما Immutable Object فقط یک State دارد:

```
Constructor
    ↓
State ثابت
    ↓
پایان عمر Object
```

بنابراین Reasoning درباره‌ی آن بسیار ساده‌تر است.

<a id="five-rules"></a>
### پنج قانون طلایی ساخت Immutable Class

Bloch پنج قانون بسیار مهم معرفی می‌کند. این قوانین تقریباً در تمام کتابخانه‌های استاندارد جاوا رعایت شده‌اند.

<a id="rule1"></a>
#### قانون اول: هیچ Mutator ننویس

یعنی نداشته باش:

<div dir="ltr">

```java
setName()
setAge()
setSalary()
reset()
update()
change()
```
</div>

هر متدی که وضعیت داخلی Object را تغییر دهد ممنوع است.

**مثال اشتباه:**

<div dir="ltr">

```java
public class Money {
    private BigDecimal amount;

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
```
</div>

**مثال صحیح:**

<div dir="ltr">

```java
public final class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal amount() {
        return amount;
    }
}
```
</div>

<a id="rule2"></a>
#### قانون دوم: اجازه ارث‌بری نده

چرا؟ فرض کنید:

<div dir="ltr">

```java
public class Money {
    private final BigDecimal amount;
}
```
</div>

اکنون:

<div dir="ltr">

```java
class EvilMoney extends Money {
    public void hack() {
        // ...
    }
}
```
</div>

Subclass می‌تواند رفتار کلاس را تغییر دهد. حتی ممکن است ظاهر Immutable را حفظ کند اما در واقع Mutable باشد.

**راه‌حل اول:** کلاس را `final` کنید:

<div dir="ltr">

```java
public final class Money
```
</div>

**راه‌حل دوم:** Constructor را Private کنیم و فقط Static Factory ارائه دهیم.

<a id="rule3"></a>
#### قانون سوم: تمام فیلدها Final باشند

یعنی:

<div dir="ltr">

```java
private final String name;
private final int age;
private final Address address;
```
</div>

نه:

<div dir="ltr">

```java
private String name;
```
</div>

چرا؟ Final دو مزیت بسیار مهم دارد:

**۱- بیان Intent:** وقتی برنامه‌نویس کلاس را می‌بیند `private final`، فوراً متوجه می‌شود این مقدار تغییر نخواهد کرد.

**۲- Java Memory Model:** Bloch به نکته‌ای اشاره می‌کند که بسیاری از برنامه‌نویسان هرگز متوجه آن نمی‌شوند. وجود Final باعث می‌شود JVM هنگام انتشار (Publication) شیء بین Threadها تضمین‌های قوی‌تری ارائه دهد.

<a id="rule4"></a>
#### قانون چهارم: تمام فیلدها Private باشند

خیلی‌ها تصور می‌کنند:

<div dir="ltr">

```java
public final String name;
```
</div>

هم کافی است. اما Bloch مخالف است.

چرا؟ فرض کنید امروز `public final String firstName;` دارید. سال آینده تصمیم می‌گیرید به‌جای ذخیره‌ی نام و نام خانوادگی (`firstName`، `lastName`) فقط `fullName` را ذخیره کنید. اگر Field عمومی باشد، API شکسته می‌شود. ولی اگر Getter داشته باشید، می‌توانید Representation داخلی را هر زمان تغییر دهید، بدون اینکه Clientها متوجه شوند.

<a id="rule5"></a>
#### قانون پنجم: دسترسی به Mutable Objectها را کنترل کن

این مهم‌ترین قانون است. خیلی‌ها چهار قانون اول را رعایت می‌کنند اما این یکی را فراموش می‌کنند.

فرض کنید:

<div dir="ltr">

```java
public final class Employee {
    private final Address address;
}
```
</div>

آیا کلاس Immutable است؟ شاید نه. اگر `Address` خودش Mutable باشد و Getter این باشد:

<div dir="ltr">

```java
public Address getAddress() {
    return address;
}
```
</div>

کاربر می‌تواند بنویسد:

<div dir="ltr">

```java
employee.getAddress().setCity("Berlin");
```
</div>

خود Employee ظاهراً Immutable است، اما State داخلی آن تغییر کرد.

**راه‌حل: Defensive Copy**

<div dir="ltr">

```java
public Address getAddress() {
    return new Address(address);
}
```
</div>

یا خود `Address` نیز Immutable باشد که معمولاً بهترین راه است.

<a id="immutable-vs-readonly"></a>
### Immutable با Read Only فرق دارد

یکی از رایج‌ترین سوءتفاهم‌ها این است که Read Only == Immutable. در حالی که این دو مفهوم متفاوت‌اند.

فرض کنید:

<div dir="ltr">

```java
public List<String> getNames() {
    return Collections.unmodifiableList(names);
}
```
</div>

کاربر نمی‌تواند لیست را تغییر دهد. اما اگر خود کلاس بنویسد:

<div dir="ltr">

```java
names.add("Ali");
```
</div>

لیست تغییر می‌کند. پس شیء Immutable نیست؛ فقط یک نمای Read Only ارائه شده است.

Immutable یعنی **هیچ‌کس** (نه Client و نه خود کلاس پس از ساخت) نتواند وضعیت قابل مشاهده‌ی شیء را تغییر دهد.

<a id="complex-example"></a>
### مثال کلاس Complex

Bloch برای توضیح این مفهوم از کلاس معروف `Complex` استفاده می‌کند. به جای متدهایی مانند `add()` و `subtract()` که ذهن را به تغییر وضعیت شیء هدایت می‌کنند، از نام‌های زیر استفاده می‌کند:

<div dir="ltr">

```java
plus()
minus()
times()
dividedBy()
```
</div>

و هر متد، به جای تغییر شیء فعلی (`this`)، یک نمونه‌ی جدید برمی‌گرداند:

<div dir="ltr">

```java
Complex a = new Complex(2, 3);
Complex b = new Complex(1, 4);
Complex c = a.plus(b);
```
</div>

در اینجا:
- `a` بدون تغییر باقی می‌ماند
- `b` بدون تغییر باقی می‌ماند
- `c` نتیجه‌ی جدید عملیات است

این همان **Functional Style** است که پایه‌ی طراحی بسیاری از APIهای مدرن جاوا محسوب می‌شود.

<a id="functional-vs-imperative"></a>
### Functional Approach در برابر Imperative Approach

| Functional (Immutable) | Imperative (Mutable) |
|------------------------|----------------------|
| شیء جدید تولید می‌شود | همان شیء تغییر می‌کند |
| State ثابت می‌ماند | State دائماً تغییر می‌کند |
| Thread-Safe | نیازمند Synchronization |
| Reasoning ساده | Reasoning پیچیده |
| مناسب Stream و Parallel | مستعد Race Condition |
| قابل اشتراک‌گذاری | نیازمند Defensive Copy |

<a id="part1-summary"></a>
### جمع‌بندی بخش اول

تا اینجا مهم‌ترین مفاهیم پایه‌ای Item 17 را شناختیم:

- فلسفه‌ی اصلی Bloch، **کاهش Mutability** است، نه Immutable کردن کورکورانه‌ی همه چیز.
- Immutable Object تنها یک وضعیت در تمام طول عمر خود دارد و همین موضوع طراحی، تست و نگهداری را بسیار ساده‌تر می‌کند.
- پنج قانون طلایی ساخت کلاس Immutable عبارت‌اند از:
    1. نداشتن Mutator
    2. جلوگیری از ارث‌بری
    3. `final` بودن تمام فیلدها
    4. `private` بودن تمام فیلدها
    5. جلوگیری از نشت Reference به اجزای Mutable
- تفاوت مهمی بین **Read Only View** و **Immutable Object** وجود دارد.
- کلاس `Complex` نمونه‌ای از طراحی **Functional** است.

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم — چرا Immutable Objectها ستون فقرات سیستم‌های مدرن هستند؟

در بخش اول یاد گرفتیم چگونه یک کلاس Immutable طراحی کنیم. اما سؤال اصلی هنوز باقی است:

> **چرا Joshua Bloch تا این اندازه روی Immutable بودن تأکید می‌کند؟**

پاسخ فقط «زیبایی طراحی» نیست. بلکه Immutable بودن تقریباً تمام مشکلات رایج سیستم‌های Enterprise را ساده‌تر می‌کند.

### نمای معماری (Architectural View)
<div dir="ltr">

```
                 Mutable Objects
                       │
      ┌────────────────┼─────────────────┐
      │                │                 │
 Race Condition   Synchronization   Defensive Copy
      │                │                 │
 Deadlock         Lock Contention     Complexity
      │                │                 │
          Performance & Maintainability Problems
```
</div>
در مقابل:
<div dir="ltr">

```
               Immutable Objects
                      │
      ┌───────────────┼───────────────┐
      │               │               │
 Thread Safe     Shareable      Predictable
      │               │               │
 Lock-Free      Cacheable     Easy Testing
      │               │               │
        High Scalability
```
</div>

<a id="advantage1"></a>
### مزیت اول: Immutable Objects ذاتاً Thread-Safe هستند

این مهم‌ترین مزیت Immutable بودن است.

فرض کنید:

<div dir="ltr">

```java
public final class User {
    private final String name;
    private final int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```
</div>

اکنون ده‌ها Thread می‌توانند همزمان از این شیء استفاده کنند:

<div dir="ltr">

```java
executor.submit(() -> process(user));
executor.submit(() -> validate(user));
executor.submit(() -> audit(user));
```
</div>

هیچ مشکلی وجود ندارد. چرا؟ زیرا هیچ Threadای نمی‌تواند Object را تغییر دهد. در نتیجه:

- Lock لازم نیست
- `synchronized` لازم نیست
- `AtomicReference` لازم نیست
- `volatile` لازم نیست

<a id="advantage2"></a>
### مزیت دوم: Object Sharing

Bloch جمله بسیار مهمی می‌گوید:

> Immutable objects can be shared freely.

فرض کنید:

<div dir="ltr">

```java
Currency usd = Currency.getInstance("USD");
```
</div>

چند میلیون Object می‌توانند همین Reference را نگه دارند. هیچ مشکلی وجود ندارد. چون Currency Immutable است.

<a id="advantage3"></a>
### مزیت سوم: Object Caching

Immutable بودن اجازه می‌دهد Objectها Cache شوند.

مثال معروف:

<div dir="ltr">

```java
Integer.valueOf(10)
```
</div>

هر بار Object جدید نمی‌سازد. بلکه از Cache استفاده می‌کند. همین موضوع درباره `Boolean.TRUE`، `Boolean.FALSE`، `BigInteger.ZERO` و `BigInteger.ONE` نیز برقرار است.

<a id="advantage4"></a>
### مزیت چهارم: حذف Defensive Copy

اگر Address خودش Immutable باشد، نیازی به Defensive Copy نیست.

<div dir="ltr">

```java
return address;  // کاملأ امن
```
</div>

<a id="advantage5"></a>
### مزیت پنجم: Clone تقریباً بی‌معنی می‌شود

اگر Object Immutable باشد:

<div dir="ltr">

```java
Money m1 = new Money(...);
Money m2 = m1;  // کاملاً کافی است
```
</div>

حتی `clone()`، `copy constructor()` و `copy factory()` هم معمولاً لازم نیست.

<a id="advantage6"></a>
### مزیت ششم: Sharing Internal State

این قسمت از کتاب بسیار جالب است. فرض کنید `BigInteger` داخلش `sign` + `int[]` نگهداری می‌کند. حالا:

<div dir="ltr">

```java
negative = positive.negate();
```
</div>

آیا لازم است آرایه Copy شود؟ خیر. هر دو Object می‌توانند همان آرایه را Share کنند.

<a id="advantage7"></a>
### مزیت هفتم: Immutable بهترین Map Key است

اگر Key تغییر کند، کل ساختمان HashMap خراب می‌شود. به همین دلیل بهترین Keyها عبارت‌اند از:

- `String`
- `UUID`
- `Integer`
- `Long`
- `LocalDate`
- `LocalDateTime`
- `BigDecimal`
- `Enum`

<a id="advantage8"></a>
### مزیت هشتم: Immutable بهترین عضو Set است

همین مسئله برای `HashSet` نیز برقرار است. اگر Object بعد از ورود به Set تغییر کند، Invariant داخلی HashSet از بین می‌رود.

<a id="advantage9"></a>
### مزیت نهم: Failure Atomicity

Immutable Objectها Failure Atomicity را رایگان به شما می‌دهند. اگر Exception رخ دهد، Object اولیه اصلاً تغییر نکرده است.

<a id="advantage10"></a>
### مزیت دهم: Reasoning بسیار ساده‌تر

با Immutable، هر مرحله یک Object مستقل است. Debug کردن بسیار ساده‌تر می‌شود.

<a id="modern-architectures"></a>
### نقش Immutable در معماری‌های مدرن

| فناوری | استفاده از Immutable |
|--------|---------------------|
| Java Records | کاملاً Value-Oriented |
| Stream API | عملیات بدون تغییر State |
| CompletableFuture | انتقال امن داده بین Threadها |
| Reactor / WebFlux | پیام‌های Immutable |
| Kafka Events | Eventهای Immutable |
| Event Sourcing | رخدادها Immutable هستند |
| CQRS | Commandها و Eventها معمولاً Immutable |
| DDD | Value Objectها باید Immutable باشند |

<a id="part2-summary"></a>
### جمع‌بندی بخش دوم

مهم‌ترین مزایای Immutable بودن عبارت‌اند از:

- **Thread Safety ذاتی** بدون نیاز به Lock و Synchronization
- **امکان اشتراک‌گذاری ایمن Objectها** بین Threadها و بخش‌های مختلف سیستم
- **پشتیبانی از Object Caching و Flyweight** برای کاهش مصرف حافظه
- **حذف نیاز به Defensive Copy** در بسیاری از سناریوها
- **بی‌نیاز شدن از Clone و Copy** برای بسیاری از Value Objectها
- **اشتراک‌گذاری ساختارهای داخلی** برای افزایش Performance (مانند `BigInteger`)
- **ایده‌آل بودن به‌عنوان کلید Map و عضو Set**
- **Failure Atomicity رایگان** و جلوگیری از وضعیت‌های نیمه‌تغییریافته
- **Reasoning، Debugging و Testing بسیار ساده‌تر**

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم: طراحی Production-Grade بر پایه Immutable Objects

<a id="beyond-class"></a>
### چرا Immutable بودن فقط یک ویژگی کلاس نیست؟

یکی از بزرگ‌ترین اشتباهاتی که برنامه‌نویسان تازه‌کار مرتکب می‌شوند این است که تصور می‌کنند Immutable بودن صرفاً یعنی "Setter ننویسیم". در حالی که از دید معماری، Immutable بودن یعنی:

> **State هیچ‌گاه تغییر نمی‌کند؛ فقط نسخه‌های جدید ساخته می‌شوند.**
<div dir="ltr">

```
Mutable World:
Object → change state → same object

Immutable World:
Object A → operation → Object B
```
</div>
این تفاوت کوچک، پایه بسیاری از تکنولوژی‌های مدرن است.

<a id="jmm"></a>
### Immutable و Java Memory Model

یکی از دلایل مهم Rule شماره 3 (تمام فیلدها final باشند) این است که JVM برای final Fieldها تضمین ویژه‌ای دارد. وقتی Constructor تمام شود، تمام Threadها مقدار صحیح final Fieldها را مشاهده می‌کنند. بدون نیاز به `volatile`، `synchronized` یا Lock.

<a id="static-factory-immutable"></a>
### Static Factory + Immutable

اینجاست که Item 1 دوباره اهمیت پیدا می‌کند:

<div dir="ltr">

```java
Money.of(100)  // می‌تواند Cache داشته باشد
```
</div>

اما:

<div dir="ltr">

```java
new Money(100)  // نمی‌تواند
```
</div>

پس `Immutable` + `Static Factory` + `Caching` یک ترکیب فوق‌العاده است.

<a id="internal-sharing"></a>
### Immutable و Internal Sharing

یکی از زیباترین بخش‌های کتاب همین قسمت است. `BigInteger` در داخل خود یک آرایه دارد. اگر `negate()` فراخوانی شود، آیا لازم است آرایه دوباره کپی شود؟ خیر. چون هیچ‌کس اجازه تغییر آن را ندارد. در نتیجه هر دو `BigInteger` می‌توانند به یک آرایه اشاره کنند.

<a id="main-problem"></a>
### مشکل اصلی Immutable

تا اینجا همه چیز عالی است. اما ایراد چیست؟ **Object Creation.**

فرض کنید `1,000,000` بیت `BigInteger` داشته باشیم و فقط یک بیت را عوض کنیم. `flipBit()` در طراحی Immutable مجبور است یک `BigInteger` جدید بسازد که بسیار گران است.

<a id="mutable-companion"></a>
### Mutable Companion Class

راه‌حل Bloch چیست؟ یک کلاس Mutable داخلی که کاربر هرگز آن را نمی‌بیند. نمونه مشهور: `String` در مقابل `StringBuilder`.

کاربر `String` را می‌بیند، اما عملیات سنگین را `StringBuilder` انجام می‌دهد.

داخل JDK، `BigInteger` یک کلاس Mutable داخلی دارد. عملیاتی مثل Modular Exponentiation، Multiplication و Division ابتدا روی کلاس Mutable انجام می‌شوند و در پایان یک `Immutable BigInteger` برگردانده می‌شود.

<a id="static-factory-instead-of-final"></a>
### Immutable با Static Factory به جای final

بیشتر افراد فکر می‌کنند برای Immutable بودن باید `public final class Complex` بنویسیم. اما یک راه دیگر هم وجود دارد:

<div dir="ltr">

```java
public class Complex {
    private Complex(...) { }
    
    public static Complex of(...) {
        // ...
    }
}
```
</div>

چون Constructor عمومی وجود ندارد، هیچ Subclassی خارج از Package نمی‌تواند ایجاد شود. در نتیجه کلاس **Effectively Final** خواهد بود.

<a id="lazy-immutable"></a>
### Lazy Initialization در Immutable Classes

گاهی یک محاسبه بسیار پرهزینه است، مثلاً `hashCode()` برای یک شیء بزرگ. به جای محاسبه مکرر:

<div dir="ltr">

```java
@Override
public int hashCode() {
    if (hash == 0) {
        hash = computeHash();
    }
    return hash;
}
```
</div>

چون شیء Immutable است، می‌دانیم مقدار Hash هیچ‌وقت تغییر نمی‌کند. پس Cache کاملاً امن است.

<a id="when-not-immutable"></a>
### چه زمانی Immutable مناسب نیست؟

همه چیز را Immutable نکنید. کلاس‌هایی که:
- حجم داده بسیار بزرگی دارند
- تغییرات مکرر و تدریجی روی آن‌ها انجام می‌شود
- یا هزینه ساخت مجدد شیء بسیار زیاد است

ممکن است با طراحی کاملاً Immutable کارایی مناسبی نداشته باشند.

نمونه کلاس Mutable مناسب در JDK:
- `StringBuilder`
- `StringBuffer`
- `BitSet`
- `ByteBuffer` (در بسیاری از سناریوها)
- `CountDownLatch` (حالت‌های محدود اما قابل تغییر)

<a id="architectural-decision"></a>
### جدول تصمیم‌گیری معماری

| نوع کلاس | پیشنهاد | دلیل |
|----------|---------|------|
| Value Object | ✅ Immutable | ساده، ایمن، قابل اشتراک |
| Money | ✅ Immutable | جلوگیری از خطاهای مالی |
| Address | ✅ Immutable | مقدارمحور (Value-Based) |
| Configuration | ✅ Immutable | جلوگیری از تغییر ناخواسته |
| Event | ✅ Immutable | حفظ تاریخچه |
| DTO | ✅ در صورت امکان | Thread-safe و قابل اشتراک |
| Entityهای JPA | ⚠️ معمولاً Mutable | نیاز ORM به تغییر وضعیت |
| Builder | ✅ Mutable | ساخت تدریجی شیء |
| Aggregates با State پیچیده | ⚠️ بسته به دامنه | نیازمند تحلیل Trade-off |

<a id="final-summary"></a>
### جمع‌بندی نهایی Item 17

Joshua Bloch در این آیتم تنها یک توصیه برنامه‌نویسی ارائه نمی‌دهد؛ او یکی از بنیادی‌ترین اصول طراحی شیءگرا را بیان می‌کند:

> **تا زمانی که دلیل محکمی برای Mutable بودن ندارید، کلاس‌ها را Immutable طراحی کنید.**

قاعده‌ای که می‌توان از این آیتم استخراج کرد:

1. **Immutable را انتخاب پیش‌فرض (Default) بدانید.**
2. همه فیلدها را تا حد امکان `private final` نگه دارید.
3. از Setterهای غیرضروری اجتناب کنید.
4. عملیات را به‌صورت Functional طراحی کنید؛ به‌جای تغییر شیء، نمونه جدید برگردانید.
5. از Static Factory برای Caching و بهینه‌سازی‌های آینده استفاده کنید.
6. در صورت نیاز به عملکرد بالا، از الگوی **Mutable Companion** (مانند `StringBuilder` برای `String`) بهره بگیرید.
7. تنها زمانی به Mutable بودن روی بیاورید که تحلیل عملکرد، نیازهای دامنه یا محدودیت‌های فنی آن را توجیه کنند.

اگر بخواهیم پیام اصلی این آیتم را در یک جمله خلاصه کنیم، همان جمله‌ای است که بسیاری از طراحی‌های مدرن جاوا بر اساس آن شکل گرفته‌اند:
<div dir="ltr">

> **"Classes should be immutable unless there's a very good reason to make them mutable."**
</div>
این طرز فکر، پایه طراحی بسیاری از APIهای مدرن جاوا، سیستم‌های Concurrent، معماری‌های Cloud-Native و کتابخانه‌های Enterprise امروزی است.

---

[بازگشت به بالا](#top)

</div>
```