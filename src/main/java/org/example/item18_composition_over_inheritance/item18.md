<div dir="rtl">

<a id="top"></a>

# آیتم ۱۸: Composition را بر Inheritance ترجیح دهید (Favor Composition over Inheritance)

اگر بخواهیم تنها **یک اصل طراحی** را نام ببریم که بیشترین تأثیر را بر کیفیت نرم‌افزارهای Enterprise داشته است، احتمالاً آن اصل این جمله خواهد بود:

> **Favor Composition over Inheritance**

این جمله یکی از مشهورترین قوانین طراحی شیءگراست و تقریباً در تمام کتاب‌های معتبر مانند Effective Java، Design Patterns (GoF)، Clean Architecture، Clean Code و Domain-Driven Design بارها تکرار شده است.

---

## فهرست مطالب

- [بخش اول: چرا Composition تقریباً همیشه از Inheritance بهتر است؟](#part1)
  - [هدف اصلی این آیتم](#goal)
  - [چرا Inheritance این‌قدر محبوب شد؟](#why-popular)
  - [مشکل اصلی Inheritance چیست؟](#main-problem)
  - [Encapsulation یعنی چه؟](#encapsulation)
  - [اما Inheritance چه می‌کند؟](#what-inheritance-does)
  - [Fragile Base Class Problem](#fragile-base-class)
  - [تفاوت Interface Inheritance و Implementation Inheritance](#interface-vs-implementation)
  - [چرا Interface Inheritance امن است؟](#why-interface-safe)
  - [چرا داخل یک Package ارث‌بری نسبتاً امن است؟](#within-package)
  - [پیام اصلی بخش اول](#part1-summary)

- [بخش دوم: Composition + Forwarding (راه‌حل واقعی)](#part2)
  - [چرا Composition مشکل را حل می‌کند؟](#why-composition-works)
  - [Composition یعنی چه؟](#what-is-composition)
  - [Forwarding چیست؟](#what-is-forwarding)
  - [Forwarding Class](#forwarding-class)
  - [InstrumentedSet با Forwarding](#instrumented-set)
  - [چرا این نسخه دیگر خراب نمی‌شود؟](#why-fixed)
  - [مهم‌ترین مزیت Composition](#main-advantage)
  - [انعطاف‌پذیری فوق‌العاده](#flexibility)
  - [Wrapper Pattern و Decorator Pattern](#wrapper-decorator)
  - [Delegation و تفاوت آن با Forwarding](#delegation)
  - [مزایای معماری Composition](#architectural-advantages)
  - [جمع‌بندی بخش دوم](#part2-summary)

- [بخش سوم: چه زمانی Inheritance انتخاب درستی است؟](#part3)
  - [مشکل Self Problem (Callback Problem)](#self-problem)
  - [آیا Wrapperها کند هستند؟](#performance)
  - [آیا Wrapper حافظه بیشتری مصرف می‌کند؟](#memory)
  - [سوال طلایی Bloch](#golden-question)
  - [مثال صحیح: Dog extends Animal](#good-example)
  - [مثال اشتباه: Square extends Rectangle](#bad-example)
  - [مثال مشهور JDK: Stack extends Vector](#stack-vector)
  - [مثال مشهور JDK: Properties extends Hashtable](#properties-hashtable)
  - [یکی دیگر از سوال‌های مهم](#another-question)
  - [مقایسه معماری](#comparison)
  - [ارتباط با SOLID](#solid)
  - [Production-Grade Architecture](#production-architecture)
  - [جمع‌بندی نهایی Item 18](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول — چرا Composition تقریباً همیشه از Inheritance بهتر است؟

<a id="goal"></a>
### هدف اصلی این آیتم

Bloch نمی‌گوید:

> از Inheritance استفاده نکن.

بلکه می‌گوید:

> **تا زمانی که واقعاً رابطه‌ی "IS-A" وجود ندارد، از Composition استفاده کن.**

این تفاوت بسیار مهم است.

<a id="why-popular"></a>
### چرا Inheritance این‌قدر محبوب شد؟

در ابتدای ظهور OOP تقریباً همه تصور می‌کردند:
<div dir="ltr">

```
Code Reuse = Inheritance
```
</div>
مثلاً:

<div dir="ltr">

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }
```
</div>

همه چیز طبیعی به نظر می‌رسد. اما کم‌کم برنامه‌ها بزرگ شدند و مشکل واقعی خودش را نشان داد.

<a id="main-problem"></a>
### مشکل اصلی Inheritance چیست؟

Joshua Bloch تنها یک جمله می‌گوید که تمام ماجرا را توضیح می‌دهد:
<div dir="ltr">

> **Inheritance violates encapsulation.**
</div>
همین جمله، کل این آیتم است.

<a id="encapsulation"></a>
### Encapsulation یعنی چه؟

در Item 15 گفتیم: Encapsulation یعنی:
<div dir="ltr">

```
Implementation Hidden → API
```
</div>
کاربر فقط API را ببیند، نه داخل کلاس را.

مثلاً:

<div dir="ltr">

```java
class Database {
    public User findById(long id) { ... }
}
```
</div>

کاربر فقط می‌داند `database.findById(id)`. او نباید بداند از JDBC استفاده شده، Hibernate، JPA، Redis یا MongoDB. تمام این‌ها Implementation Detail هستند.

<a id="what-inheritance-does"></a>
### اما Inheritance چه می‌کند؟

فرض کنید:

<div dir="ltr">

```java
class Parent {
    public void process() {
        validate();
        save();
    }
}

class Child extends Parent { }
```
</div>

در ظاهر Child فقط API را گرفته است. اما در واقع نه. Child به Implementation داخلی Parent نیز وابسته شده است.

یعنی Child می‌داند:
<div dir="ltr">

```
process()
    ↓
validate()
    ↓
  save()
```
</div>
اگر Parent فردا ترتیب را عوض کند:
<div dir="ltr">

```
  save()
    ↓
validate()
```
</div>
ممکن است Child خراب شود. در حالی که API اصلاً تغییر نکرده است.

**این دقیقاً یعنی نقض Encapsulation.**

<a id="fragile-base-class"></a>
### Fragile Base Class Problem

این یکی از معروف‌ترین مشکلات OOP است. نام رسمی آن **Fragile Base Class Problem** است.

تعریف: هر تغییری داخل Superclass ممکن است Subclass را خراب کند. حتی اگر API اصلاً تغییر نکرده باشد.

```
Version 1: Parent → Subclass → همه چیز درست است
Version 2: Parent فقط داخل متدها تغییر کرده است → Subclass → خراب شد
```

این یعنی Subclass به جای API به Implementation وابسته شده است.

<a id="interface-vs-implementation"></a>
### تفاوت Interface Inheritance و Implementation Inheritance

وقتی Bloch می‌گوید Inheritance، منظور چیست؟ **Implementation Inheritance** یعنی `extends`، نه `implements`.

دو نوع ارث‌بری داریم:

#### ۱) Interface Inheritance

<div dir="ltr">

```java
interface Payment {
    void pay();
}

class Paypal implements Payment { }
```
</div>

اینجا هیچ Implementationای وجود ندارد. فقط Contract وجود دارد. این کاملاً امن است.

#### ۲) Implementation Inheritance

<div dir="ltr">

```java
class Parent {
    public void process() { }
}

class Child extends Parent { }
```
</div>

اینجا تمام پیاده‌سازی Parent به Child منتقل می‌شود. مشکل Item 18 دقیقاً همین نوع دوم است.

<a id="why-interface-safe"></a>
### چرا Interface Inheritance امن است؟

چون فقط قرارداد منتقل می‌شود. مثلاً:
<div dir="ltr">

```
Payment → pay()
```
</div>
اما نحوه انجام آن کاملاً آزاد است. در نتیجه Implementation وابسته نیست.

<a id="within-package"></a>
### چرا داخل یک Package ارث‌بری نسبتاً امن است؟

Bloch می‌گوید اگر Parent و Child داخل یک Package باشند، معمولاً یک تیم روی هر دو کار می‌کند. در نتیجه اگر Parent تغییر کند، Child نیز همزمان اصلاح می‌شود.

اما اگر Parent کتابخانه JDK باشد، یا Spring، یا Hibernate، دیگر چنین کنترلی وجود ندارد.

<a id="part1-summary"></a>
### پیام اصلی بخش اول

تا اینجا Joshua Bloch می‌خواهد یک اصل بنیادی را در ذهن ما تثبیت کند:

- **ارث‌بری صرفاً یک ابزار برای استفاده مجدد از کد (Code Reuse) نیست.**
- هر بار که از `extends` استفاده می‌کنید، کلاس فرزند را به جزئیات پیاده‌سازی کلاس والد گره می‌زنید.
- این وابستگی باعث ایجاد **Fragile Base Class Problem** می‌شود.
- **Interface Inheritance (`implements`)** فقط یک قرارداد را منتقل می‌کند و معمولاً امن است.
- **Implementation Inheritance (`extends`)** رفتار و فرضیات داخلی را نیز منتقل می‌کند.

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم — Composition + Forwarding (راه‌حل واقعی Effective Java)

بعد از اینکه Bloch ضعف‌های Inheritance را نشان می‌دهد، راه‌حل اصلی خود را معرفی می‌کند:

> **به جای اینکه از کلاس ارث‌بری کنید، آن را درون کلاس خود قرار دهید (Composition) و فراخوانی‌ها را به آن Forward کنید.**

<a id="why-composition-works"></a>
### چرا Composition مشکل را حل می‌کند؟

فرض کنید می‌خواهیم قابلیت شمارش عملیات Add را به یک Set اضافه کنیم.

به جای اینکه:

<div dir="ltr">

```java
class InstrumentedHashSet extends HashSet
```
</div>

بنویسیم:

<div dir="ltr">

```java
class InstrumentedSet {
    private final Set<E> set;
}
```
</div>

یعنی به جای اینکه خودمان HashSet باشیم، می‌گوییم یک HashSet داریم. این دقیقاً تفاوت دو رابطه معروف UML است:
<div dir="ltr">

```
Inheritance:     InstrumentedHashSet ▲ is-a HashSet
Composition:     InstrumentedSet ─── has-a HashSet
```
</div>
Bloch می‌گوید: اگر رابطه واقعی "has-a" باشد، هیچ وقت از "is-a" استفاده نکن.

<a id="what-is-composition"></a>
### Composition یعنی چه؟

Composition یعنی: یک شیء، شیء دیگری را به عنوان بخشی از خودش نگه دارد.

مثلاً:
<div dir="ltr">

```
Car
 ├── Engine
 ├── Gearbox
 └── Tire
```
</div>
ماشین Engine نیست. بلکه Engine دارد.

همین موضوع درباره Set هم صادق است: `InstrumentedSet` یک `HashSet` دارد.

<a id="what-is-forwarding"></a>
### Forwarding چیست؟

وقتی کلاس داخلی داریم، تقریباً تمام متدها را به آن پاس می‌دهیم.

مثلاً:

<div dir="ltr">

```java
public boolean add(E e) {
    return set.add(e);
}

public int size() {
    return set.size();
}

public void clear() {
    set.clear();
}
```
</div>

یعنی:
<div dir="ltr">

```
Client → InstrumentedSet → Forward() → HashSet
```
</div>
به این تکنیک **Forwarding** می‌گویند.

<a id="forwarding-class"></a>
### Forwarding Class

برای اینکه مجبور نباشیم این متدها را همیشه دوباره بنویسیم، Bloch یک کلاس کمکی معرفی می‌کند: `ForwardingSet` که تقریباً فقط این کار را انجام می‌دهد.

<div dir="ltr">

```java
public class ForwardingSet<E> implements Set<E> {
    private final Set<E> s;

    public ForwardingSet(Set<E> s) {
        this.s = s;
    }

    @Override
    public boolean add(E e) {
        return s.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return s.remove(o);
    }

    @Override
    public int size() {
        return s.size();
    }
    // ... سایر متدها
}
```
</div>

تمام متدها فقط Forward می‌شوند. هیچ Logic خاصی وجود ندارد.

<a id="instrumented-set"></a>
### InstrumentedSet حالا بسیار ساده می‌شود

حالا کافی است فقط متدی که می‌خواهیم تغییر کند Override کنیم.

<div dir="ltr">

```java
public class InstrumentedSet<E> extends ForwardingSet<E> {
    private int addCount;

    public InstrumentedSet(Set<E> set) {
        super(set);
    }

    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }
}
```
</div>

<a id="why-fixed"></a>
### چرا این نسخه دیگر خراب نمی‌شود؟

چون دیگر `HashSet` را تغییر نداده‌ایم. بلکه فقط قبل از صدا زدن آن کار خودمان را انجام می‌دهیم.
<div dir="ltr">

```
Client → InstrumentedSet → addCount++ → ForwardingSet → HashSet
```
</div>
HashSet هر تغییری هم بکند، Instrumentation ما مستقل است.

<a id="main-advantage"></a>
### مهم‌ترین مزیت Composition

در نسخه Inheritance، کلاس ما به Implementation داخلی HashSet وابسته بود. اما حالا فقط به Interface وابسته است: `Set`، نه `HashSet`.

<a id="flexibility"></a>
### انعطاف‌پذیری فوق‌العاده

به همین دلیل کلاس جدید فقط مخصوص HashSet نیست. می‌تواند هر Setای را Wrap کند:

<div dir="ltr">

```java
Set<String> set = new InstrumentedSet<>(new HashSet<>());
Set<String> set2 = new InstrumentedSet<>(new TreeSet<>());
Set<String> set3 = new InstrumentedSet<>(new LinkedHashSet<>());
Set<String> set4 = new InstrumentedSet<>(new ConcurrentSkipListSet<>());
```
</div>

این همان **برنامه‌نویسی بر اساس Abstraction** است.

<a id="wrapper-decorator"></a>
### Wrapper Pattern و Decorator Pattern

Bloch این کلاس را **Wrapper Class** می‌نامد. چون `HashSet` را Wrap کرده است.

این دقیقاً همان الگوی مشهور GoF یعنی **Decorator** است. Decorator می‌گوید:

> بدون تغییر کلاس اصلی، قابلیت جدید اضافه کن.
<div dir="ltr">

```
Coffee → MilkDecorator → SugarDecorator → ChocolateDecorator
```
</div>
هر کدام فقط قابلیت جدید اضافه می‌کنند.

<a id="delegation"></a>
### Delegation و تفاوت آن با Forwarding

بسیاری این دو را یکی می‌دانند، اما از نظر دقیق تفاوت دارند.

**Forwarding:** فقط فراخوانی را منتقل می‌کند.

<div dir="ltr">

```java
public int size() {
    return set.size();
}
```
</div>

**Delegation:** شیء داخلی می‌داند چه کسی او را صدا زده و ممکن است دوباره Wrapper را فراخوانی کند (معمولاً با ارسال `this`).

Bloch اشاره می‌کند که مثال کتاب **Forwarding** است، نه Delegation واقعی.

<a id="architectural-advantages"></a>
### مزایای معماری Composition

| ویژگی | Inheritance | Composition |
|-------|-------------|-------------|
| وابستگی به Implementation | زیاد | بسیار کم |
| رعایت Encapsulation | ❌ | ✅ |
| امکان استفاده با انواع مختلف Set | ❌ | ✅ |
| تأثیر تغییرات نسخه جدید JDK | زیاد | ناچیز |
| تست‌پذیری | متوسط | بالا |
| نگهداری | دشوار | ساده |
| توسعه‌پذیری | محدود | بسیار بالا |

<a id="part2-summary"></a>
### جمع‌بندی بخش دوم

- **Composition** وابستگی به جزئیات پیاده‌سازی کلاس والد را از بین می‌برد.
- **Forwarding** راهی ساده برای واگذاری عملیات به شیء داخلی است.
- **Wrapper/Decorator** بدون تغییر کلاس اصلی، رفتار جدید اضافه می‌کند.
- این رویکرد نسبت به Inheritance، **پایدارتر، انعطاف‌پذیرتر، قابل تست‌تر و سازگارتر با اصول معماری مدرن** است.

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم — چه زمانی Inheritance انتخاب درستی است؟

تا اینجا Bloch نشان داد که چرا Inheritance از کلاس‌های Concrete خطرناک است و چرا Composition معمولاً انتخاب بهتری است. اما این به معنای حذف کامل Inheritance نیست. در این بخش، او مرز دقیق استفاده از ارث‌بری را مشخص می‌کند.

<a id="self-problem"></a>
### مشکل Self Problem (Callback Problem)

یکی از معدود ایرادهای Wrapperها، چیزی است که Bloch آن را **SELF Problem** می‌نامد.

فرض کنید این Interface را داریم:

<div dir="ltr">

```java
public interface EventListener {
    void onEvent(String message);
}
```
</div>

و یک EventBus:

<div dir="ltr">

```java
public class EventBus {
    private EventListener listener;

    public void register(EventListener listener) {
        this.listener = listener;
    }

    public void fire() {
        listener.onEvent("Hello");
    }
}
```
</div>

حالا یک Wrapper می‌نویسیم:

<div dir="ltr">

```java
public class LoggingListener implements EventListener {
    private final EventListener delegate;

    public LoggingListener(EventListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onEvent(String message) {
        System.out.println("Before");
        delegate.onEvent(message);
        System.out.println("After");
    }
}
```
</div>

تا اینجا همه چیز درست است.

#### اما مشکل از کجا شروع می‌شود؟

فرض کنید شیء داخلی (Delegate) خودش را ثبت کند:

<div dir="ltr">

```java
bus.register(this);  // this به Delegate اشاره دارد، نه Wrapper
```
</div>

در نتیجه هنگام Callback:
<div dir="ltr">

```
EventBus → Delegate (Wrapper دور زده می‌شود)
```
</div>
در نتیجه Logging اصلاً اجرا نمی‌شود.

چرا؟ زیرا Delegate اصلاً از وجود Wrapper خبر ندارد. او فقط خودش را می‌شناسد.

Bloch می‌گوید:
<div dir="ltr">

> Wrapped object passes **this** instead of wrapper.
</div>

#### آیا این مشکل رایج است؟

خیر. اکثر برنامه‌های Enterprise هرگز با آن روبه‌رو نمی‌شوند. اما در Frameworkهایی مثل Swing، JavaFX، بعضی Event Busها و Listener Frameworkها ممکن است دیده شود.

<a id="performance"></a>
### آیا Wrapperها کند هستند؟

یکی از اعتراض‌های رایج: "هر بار یک متد اضافه صدا زده می‌شود."

Bloch می‌گوید: تقریباً هیچ اهمیتی ندارد. JIT Compiler تقریباً همیشه این Callها را Inline می‌کند. بنابراین Forwarding تقریباً رایگان است.

<a id="memory"></a>
### آیا Wrapper حافظه بیشتری مصرف می‌کند؟

بله. هر Wrapper یک Reference اضافه نگه می‌دارد. مثلاً `InstrumentedSet` شامل `addCount` و `Set delegate` است. ولی این هزینه در مقابل مزایای معماری تقریباً ناچیز است.

<a id="golden-question"></a>
### سوال طلایی Bloch

قبل از هر Inheritance از خودت بپرس:
<div dir="ltr">

> Is every B really an A?
</div>
یعنی: آیا هر B واقعاً یک A است؟

اگر جواب ۱۰۰٪ بله نیست، نباید از `extends` استفاده شود.

<a id="good-example"></a>
### مثال صحیح
<div dir="ltr">

```
Dog extends Animal
```
</div>
هر سگ یک حیوان است. کاملاً درست.

<a id="bad-example"></a>
### مثال اشتباه
<div dir="ltr">

```
Square extends Rectangle
```
</div>
از دید ریاضی شاید درست باشد. اما از دید برنامه‌نویسی خیر. زیرا رفتار Rectangle را می‌شکند. نمونه معروف نقض LSP.

<a id="stack-vector"></a>
### مثال مشهور JDK: Stack extends Vector
<div dir="ltr">

```
Stack extends Vector
```
</div>
آیا Stack واقعاً Vector است؟ خیر. Stack از Vector استفاده می‌کند، ولی Vector نیست.

نتیجه؟ تمام متدهای Vector وارد API Stack شدند. مثلاً `insertElementAt()`، `removeElementAt()`، `setElementAt()` در حالی که Stack باید فقط `push()`، `pop()` و `peek()` داشته باشد.

در نتیجه کاربر می‌تواند:

<div dir="ltr">

```java
stack.insertElementAt(...)
```
</div>

را صدا بزند و قوانین Stack را دور بزند.

<a id="properties-hashtable"></a>
### مثال مشهور JDK: Properties extends Hashtable
<div dir="ltr">

```
Properties extends Hashtable
```
</div>
Properties قرار بود فقط `String → String` نگه دارد. اما چون Hashtable است، کاربر می‌تواند:

<div dir="ltr">

```java
properties.put(1, new Object());
```
</div>

انجام دهد و تمام فرضیات کلاس را بشکند.

Bloch می‌گوید: این اشتباه دیگر قابل اصلاح نبود. چون میلیون‌ها برنامه به همین رفتار وابسته شده بودند.

<a id="another-question"></a>
### یکی دیگر از سوال‌های مهم

قبل از Inheritance از خودت بپرس: Superclass API مشکل دارد؟

اگر بله و تو از آن ارث‌بری کنی، تمام مشکلاتش به API تو منتقل می‌شود. اما Composition اجازه می‌دهد API خودت را طراحی کنی.

<a id="comparison"></a>
### مقایسه معماری

| معیار | Inheritance | Composition |
|-------|-------------|-------------|
| وابستگی به Implementation | زیاد | بسیار کم |
| رعایت Encapsulation | ❌ | ✅ |
| توسعه در نسخه‌های بعدی | شکننده | پایدار |
| امکان تغییر API | محدود | کامل |
| امکان استفاده مجدد | کم | زیاد |
| تست‌پذیری | متوسط | بالا |
| انعطاف | کم | بسیار زیاد |

<a id="solid"></a>
### ارتباط با SOLID

این آیتم تقریباً چند اصل SOLID را هم‌زمان پوشش می‌دهد:

| اصل | توضیح |
|-----|-------|
| **Open/Closed** | به جای تغییر کلاس اصلی، Wrapper می‌سازیم |
| **Dependency Inversion** | وابسته به `Set` هستیم نه `HashSet` |
| **Liskov Substitution** | اگر "is-a" واقعی نباشد، LSP شکسته می‌شود |
| **Single Responsibility** | Wrapper فقط یک مسئولیت جدید اضافه می‌کند |

<a id="production-architecture"></a>
### Production-Grade Architecture

امروزه تقریباً تمام Frameworkهای معروف دنیا بر پایه Composition ساخته شده‌اند:

| Framework | مثال |
|-----------|------|
| **Spring** | `Transaction Proxy` → `Service` |
| **Hibernate** | `Lazy Proxy` → `Entity` |
| **Micrometer** | `Metrics Wrapper` → `DataSource` |
| **gRPC** | `Interceptor` → `Service` |
| **Servlet Filters** | `Filter` → `Servlet` |

همه آن‌ها از همین ایده استفاده می‌کنند: `Wrap` → `Delegate` → `Forward`.

<a id="final-summary"></a>
### جمع‌بندی نهایی Item 18

Joshua Bloch با این آیتم یکی از مهم‌ترین قوانین طراحی شی‌گرا را مطرح می‌کند:

* **Inheritance** تنها زمانی مناسب است که یک رابطه واقعی **"is-a"** بین دو کلاس وجود داشته باشد و کلاس پایه نیز برای ارث‌بری طراحی شده باشد.
* ارث‌بری از کلاس‌های Concrete معمولی باعث وابستگی به جزئیات پیاده‌سازی، شکنندگی در برابر تغییرات نسخه‌های بعدی و نقض Encapsulation می‌شود.
* **Composition + Forwarding** این وابستگی را حذف می‌کند، API را تحت کنترل شما نگه می‌دارد و توسعه، تست و نگهداری سیستم را بسیار ساده‌تر می‌کند.
* Wrapperها پایه بسیاری از الگوهای طراحی مانند **Decorator** هستند و در Frameworkهای مدرن جاوا (مانند Spring و Hibernate) به‌طور گسترده استفاده می‌شوند.

#### قانون طلایی این آیتم:

> **اگر بین دو کلاس رابطه واقعی "is-a" وجود ندارد، به جای `extends` از Composition استفاده کن.**

این اصل، یکی از بنیادی‌ترین تصمیم‌های معماری در طراحی سیستم‌های Enterprise و Cloud-Native است و رعایت آن از بسیاری از مشکلات نگهداری و توسعه در آینده جلوگیری می‌کند.

---

[بازگشت به بالا](#top)

</div>
```