<div dir="rtl">

<a id="top"></a>

# آیتم ۱۹: برای ارث‌بری طراحی کنید یا اصلاً اجازه ارث‌بری ندهید

## (Design for Inheritance or Else Prohibit It)

---

## فهرست مطالب

- [بخش اول: مفهوم Self-Use و اهمیت مستندسازی Implementation](#part1)
  - [بزرگ‌ترین سوءتفاهم درباره Inheritance](#misunderstanding)
  - [مفهوم Self-Use](#self-use)
  - [چرا Self-Use خطرناک است؟](#why-self-use-dangerous)
  - [API Contract در مقابل Implementation Contract](#contracts)
  - [تناقض Inheritance و Encapsulation](#paradox)
  - [@implSpec](#implspec)
  - [مثال کتاب: AbstractCollection.remove()](#abstractcollection)
  - [ارتباط Item 18 و Item 19](#connection)
  - [Self-Use Pattern در Frameworkها](#framework-self-use)
  - [قانون طلایی بخش اول](#part1-golden-rule)

- [بخش دوم: Hook Methodها، Protected Members و Constructor ممنوع](#part2)
  - [Hook Method چیست؟](#hook-method)
  - [مثال `AbstractList.removeRange()`](#removerange)
  - [Protected Member یک تعهد دائمی است](#protected-commitment)
  - [چرا Constructor نباید متد Override شدنی را صدا بزند؟](#constructor-rule)
  - [چرا؟ تحلیل عمیق](#why-constructor-rule)
  - [Cloneable و Serializable در کلاس‌های قابل ارث‌بری](#cloneable-serializable)
  - [جمع‌بندی بخش دوم](#part2-summary)

- [بخش سوم: اگر طراحی نکرده‌ای، ممنوع کن](#part3)
  - [چرا؟ Fragile Base Class Problem](#why-prohibit)
  - [اگر Interface وجود دارد، اصلاً چرا Inheritance؟](#interface-over-inheritance)
  - [اگر Interface وجود ندارد چه؟](#no-interface)
  - [Private Helper Method](#private-helper)
  - [گزینه‌های طراحی کلاس](#design-options)
  - [نگاه معماری در پروژه‌های Enterprise](#architectural-view)
  - [ارتباط با Spring](#spring-connection)
  - [جمع‌بندی نهایی Item 19](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول: مفهوم Self-Use و اهمیت مستندسازی Implementation

در Item 18، Joshua Bloch یک هشدار بسیار مهم داد:

> **از کلاس‌هایی که برای ارث‌بری طراحی نشده‌اند، ارث‌بری نکن.**

اما اکنون یک سؤال بسیار مهم مطرح می‌شود:

> **پس یک کلاس "طراحی‌شده برای ارث‌بری" دقیقاً چه ویژگی‌هایی دارد؟**

کل Item 19 در پاسخ به همین سؤال نوشته شده است.

<a id="misunderstanding"></a>
### بزرگ‌ترین سوءتفاهم درباره Inheritance

بیشتر برنامه‌نویسان تصور می‌کنند:

```
اگر کلاس final نباشد → پس می‌توان از آن ارث‌بری کرد.
```

اما Bloch می‌گوید:

> **اشتباه!**

قابل ارث‌بری بودن (Not Final) با **طراحی شدن برای ارث‌بری** دو مفهوم کاملاً متفاوت هستند.

فرض کنید کلاس زیر را نوشته‌ایم:

<div dir="ltr">

```java
public class OrderService {
    public void process(Order order) {
        validate(order);
        save(order);
        publish(order);
    }

    protected void validate(Order order) { }
    protected void save(Order order) { }
    protected void publish(Order order) { }
}
```
</div>

ظاهراً همه چیز خوب است. اما آیا این کلاس برای Inheritance طراحی شده؟ هنوز نه.

چرا؟ زیرا هیچ‌کس نمی‌داند:

- `validate` چه زمانی اجرا می‌شود؟
- `save` چند بار اجرا می‌شود؟
- `publish` ممکن است اجرا نشود؟
- اگر `validate` Exception بدهد چه اتفاقی می‌افتد؟
- آیا `save` دوباره `validate` را صدا می‌زند؟
- آیا `publish` از Thread دیگری اجرا می‌شود؟

Subclass هیچ اطلاعی ندارد.

<a id="self-use"></a>
### مفهوم Self-Use

اصلی‌ترین مفهوم این آیتم **Self-Use** است.

Self-Use یعنی: یک متد داخل همان کلاس، متد قابل Override دیگری را صدا بزند.

مثلاً:

<div dir="ltr">

```java
public class Cache {
    public void put(String key, Object value) {
        beforePut(key);
        internalPut(key, value);
        afterPut(key);
    }

    protected void beforePut(String key) { }
    protected void afterPut(String key) { }
}
```
</div>

اینجا `put()` متدهای `beforePut()` و `afterPut()` را صدا زده است. این دقیقاً Self-Use است.

<a id="why-self-use-dangerous"></a>
### چرا Self-Use خطرناک است؟

زیرا Subclass روی رفتار داخلی کلاس اثر می‌گذارد.

مثلاً:

<div dir="ltr">

```java
class LoggingCache extends Cache {
    @Override
    protected void beforePut(String key) {
        System.out.println(key);
    }
}
```
</div>

سؤال: چند بار `beforePut` اجرا می‌شود؟

Subclass نمی‌داند. ممکن است:
<div dir="ltr">

```
put() → beforePut()
```
</div>
یا:
<div dir="ltr">

```
put() → internalPut() → beforePut()
```
</div>
یا حتی:
<div dir="ltr">

```
put() → beforePut() → retry → beforePut()
```
</div>
همه این‌ها ممکن است. اگر مستند نشده باشند، Subclass روی حدس و گمان نوشته می‌شود.

<a id="contracts"></a>
### API Contract در مقابل Implementation Contract

در طراحی معمول API می‌گوییم: "این متد چه کاری انجام می‌دهد؟"

مثلاً `boolean add(E e)` می‌گوییم: عنصر را اضافه می‌کند. تمام. این همان API Contract است.

اما برای Inheritance کافی نیست. باید بگویی:

```
داخل این متد، چه متدهایی، در چه ترتیبی صدا زده می‌شوند.
```

یعنی **How**، نه فقط **What**.

Bloch صریحاً می‌گوید: برای کلاس‌های قابل ارث‌بری مجبوریم Implementation را مستند کنیم. در حالی که در طراحی API معمول نباید درباره Implementation صحبت کنیم.

<a id="paradox"></a>
### چرا این یک تناقض است؟

در تمام کتاب گفته می‌شود: API باید Behavior را توضیح دهد، نه Implementation را.

اما اینجا خلاف آن عمل می‌کنیم. چرا؟ چون Inheritance خودش Encapsulation را شکسته است. وقتی Subclass داخل کلاس را تغییر می‌دهد، دیگر نمی‌توان Implementation را پنهان کرد.

<a id="implspec"></a>
### @implSpec

از Java 8، تگی به نام `@implSpec` به Javadoc اضافه شد. هدف آن توضیح دادن Implementation Contract است.

مثلاً:

<div dir="ltr">

```java
/**
 * Adds element.
 *
 * @implSpec
 * This implementation first validates the key,
 * then invokes beforeInsert(),
 * then stores the value,
 * finally invokes afterInsert().
 */
```
</div>

این دقیقاً همان چیزی است که Bloch می‌خواهد.

<a id="abstractcollection"></a>
### مثال کتاب: AbstractCollection.remove()

Bloch مثال `AbstractCollection.remove()` را بررسی می‌کند. در Javadoc آن نوشته شده: `Implementation Requirements` و توضیح داده:
<div dir="ltr">

```
remove() → iterator() → iterator.remove()
```
</div>
یعنی اگر کسی `iterator()` را Override کند، رفتار `remove()` نیز تغییر می‌کند.

این مستندسازی فوق‌العاده ارزشمند است. چرا؟ چون دیگر Subclass حدس نمی‌زند. همه چیز شفاف است.

<a id="connection"></a>
### ارتباط Item 18 و Item 19

- **Item 18** گفت: Inheritance شکننده است.
- **Item 19** می‌گوید: اگر می‌خواهی Inheritance امن باشد، باید تمام الگوهای Self-Use را مستند کنی.

<a id="framework-self-use"></a>
### Self-Use Pattern در Frameworkها

تقریباً تمام Frameworkهای بزرگ دنیا همین کار را انجام می‌دهند. مثلاً Spring:
<div dir="ltr">

```
refresh()
    ↓
prepareRefresh()
    ↓
obtainFreshBeanFactory()
    ↓
prepareBeanFactory()
    ↓
postProcessBeanFactory()
    ↓
finishRefresh()
```
</div>
اگر این ترتیب مستند نبود، هیچ‌کس نمی‌توانست Spring را Extend کند.

<a id="part1-golden-rule"></a>
### قانون طلایی بخش اول

Joshua Bloch می‌گوید:

> اگر قرار است کلاس شما قابل ارث‌بری باشد، فقط مستند کردن API کافی نیست؛ باید **الگوی فراخوانی داخلی (Self-Use Pattern)**، ترتیب اجرای متدهای قابل Override و تمام نقاط Extension را نیز به‌عنوان بخشی از قرارداد کلاس مستند کنید.

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم: Hook Methodها، Protected Members و Constructor ممنوع

در بخش اول با مفهوم Self-Use و اهمیت مستندسازی Implementation آشنا شدیم. در این بخش به سه موضوع عملی می‌پردازیم.

<a id="hook-method"></a>
### Hook Method چیست؟

Hook Method یک متد `protected` است که در کلاس پایه تعریف می‌شود و Subclass می‌تواند آن را Override کند تا رفتار داخلی کلاس را تغییر دهد، بدون اینکه کل متد اصلی را بازنویسی کند.

مثال:

<div dir="ltr">

```java
public abstract class TemplateProcessor {
    public final void process() {
        step1();
        step2();
        step3();
    }

    protected void step1() { }
    protected void step2() { }
    protected abstract void step3();
}
```
</div>

Subclass فقط `step3()` را پیاده‌سازی می‌کند و بقیه مراحل را به کلاس پایه می‌سپارد.

<a id="removerange"></a>
### مثال `AbstractList.removeRange()`

یکی از معروف‌ترین Hookها در JDK، متد `removeRange()` در `AbstractList` است:

<div dir="ltr">

```java
protected void removeRange(int fromIndex, int toIndex) {
    // implementation
}
```
</div>

چرا این متد `protected` است و نه `public`؟

زیرا:

- به Subclass اجازه می‌دهد عملیات حذف بازه را بهینه‌سازی کند.
- مثلاً `ArrayList` می‌تواند با `System.arraycopy()` این کار را خیلی سریع‌تر انجام دهد.
- اما این متد به‌عنوان بخشی از API عمومی در `List` قرار داده نشده است.

این دقیقاً نمونۀ کامل از طراحی برای Inheritance است.

<a id="protected-commitment"></a>
### Protected Member یک تعهد دائمی است

Bloch هشدار جدی می‌دهد:

> **هر عضو Protected، بخشی از قرارداد عمومی API است و تا ابد باید پشتیبانی شود.**

یعنی اگر یک متد را `protected` کردی، در نسخه‌های بعدی نمی‌توانی آن را حذف یا تغییر دهی. Subclassها روی آن حساب می‌کنند.

پس:

```
protected = تعهد بلندمدت
```

نه:

```
protected = جزییات داخلی
```

<a id="constructor-rule"></a>
### چرا Constructor نباید متد Override شدنی را صدا بزند؟

این یکی از مشهورترین قوانین Bloch است:

> **Constructor هرگز نباید متد قابل Override را فراخوانی کند.**

مثال اشتباه:

<div dir="ltr">

```java
public class Parent {
    public Parent() {
        doInit();  // ❌
    }

    protected void doInit() { }
}

public class Child extends Parent {
    private final String value;

    public Child(String value) {
        this.value = value;
    }

    @Override
    protected void doInit() {
        System.out.println(value.length());  // ❌ NPE
    }
}
```
</div>

نتیجه: `NullPointerException`

<a id="why-constructor-rule"></a>
### چرا؟ تحلیل عمیق

ترتیب اجرای Constructorها در جاوا:
<div dir="ltr">

```
Child Constructor
    ↓
super() → Parent Constructor
    ↓
doInit() (که در Child Override شده)
    ↓
Child Constructor کامل می‌شود
```
</div>
مشکل: در لحظه‌ای که `doInit()` در Parent صدا زده می‌شود، Constructor Child هنوز کامل نشده است. پس فیلدهای Child هنوز مقداردهی نشده‌اند.

<a id="cloneable-serializable"></a>
### Cloneable و Serializable در کلاس‌های قابل ارث‌بری

دو مکانیزم دیگر که با Constructor مشکل دارند، `Cloneable` و `Serializable` هستند.

**Cloneable:**

<div dir="ltr">

```java
class Parent implements Cloneable {
    public Parent clone() {
        return super.clone();  // ❌ ممکن است Subclass را خراب کند
    }
}
```
</div>

**Serializable:**

<div dir="ltr">

```java
class Parent implements Serializable {
    private void readObject(ObjectInputStream in) {
        // ❌ ممکن است متد Override شدنی را صدا بزند
    }
}
```
</div>

Bloch می‌گوید: اگر کلاس را برای Inheritance طراحی می‌کنید، پیاده‌سازی `Cloneable` و `Serializable` بسیار دشوار است و معمولاً توصیه نمی‌شود.

<a id="part2-summary"></a>
### جمع‌بندی بخش دوم

- **Hook Methodها** نقاط ورودی برای Subclassها هستند تا رفتار داخلی را تغییر دهند.
- **Protected Member** بخشی از API است و باید تا ابد پشتیبانی شود.
- **Constructor هرگز نباید متد Override شدنی را صدا بزند**؛ این باعث وابستگی به حالت نیمه‌ساخته می‌شود.
- **Cloneable و Serializable** در کلاس‌های قابل ارث‌بری بسیار پیچیده هستند و معمولاً باید اجتناب شوند.

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم — اگر برای Inheritance طراحی نکرده‌ای، آن را ممنوع کن

این جمله شاید مهم‌ترین جمله کل Item باشد.

Joshua Bloch می‌گوید:

> اگر کلاس را برای ارث‌بری طراحی نکرده‌ای،
>
> **اصلاً اجازه ارث‌بری نده.**

نه اینکه: "فعلاً final نکن بعداً درستش می‌کنیم." بلکه از همان روز اول باید تصمیم بگیری.

<a id="why-prohibit"></a>
### چرا؟ Fragile Base Class Problem

فرض کنیم چنین کلاسی نوشته‌ای:

<div dir="ltr">

```java
public class PaymentProcessor {
    public void process(Payment payment) {
        validate(payment);
        save(payment);
        notifyCustomer(payment);
    }

    protected void validate(Payment payment) { }
    private void save(Payment payment) { }
    private void notifyCustomer(Payment payment) { }
}
```
</div>

فکر می‌کنی این کلاس قابل Extend شدن است. یکی می‌آید:

<div dir="ltr">

```java
class CustomPaymentProcessor extends PaymentProcessor {
    @Override
    protected void validate(Payment payment) { }
}
```
</div>

الان مشکلی نیست.

اما شش ماه بعد تیم شما تصمیم می‌گیرد: "قبل از validate یک Fraud Check هم انجام شود."

پس کلاس را تغییر می‌دهید:
<div dir="ltr">

```
process() → fraudCheck() → validate() → save()
```
</div>
حالا تمام Subclassهای دنیا رفتارشان عوض شده است. بدون اینکه حتی یک خط از کدشان تغییر کرده باشد.

<a id="interface-over-inheritance"></a>
### اگر Interface وجود دارد، اصلاً چرا Inheritance؟

فرض کنید: `List`، `Set`، `Map`، `Cache`، `Repository` همه Interface هستند.

حالا اگر بخواهیم قابلیت جدید اضافه کنیم، آیا باید از کلاس Concrete ارث ببریم؟ خیر.

به جای این:
<div dir="ltr">

```
HashMap → MyMap
```
</div>
از این استفاده می‌کنیم:
<div dir="ltr">
```
MyMap → contains → HashMap
```
</div>
یعنی Composition.

<a id="no-interface"></a>
### اگر Interface وجود ندارد چه؟

اینجا Joshua Bloch می‌گوید: اگر Interface نداری و واقعاً مجبور هستی Extend را باز بگذاری، حداقل این کار را انجام بده:

> هیچ متد Override شدنی را از داخل کلاس خودت صدا نزن.

مثلاً این بد است:

<div dir="ltr">

```java
public class Engine {
    public void start() {
        initialize();  // ❌ Self-use
    }

    protected void initialize() { }
}
```
</div>

نسخه بهتر:

<div dir="ltr">

```java
public class Engine {
    public final void start() {
        doInitialize();
    }

    protected void initialize() {
        doInitialize();
    }

    private void doInitialize() { }  // ✅ Helper خصوصی
}
```
</div>

<a id="private-helper"></a>
### Private Helper Method

Joshua اسم این روش را مستقیماً بیان نمی‌کند، اما ایده آن همان **Private Helper Method** است:
<div dir="ltr">

```
public method
    ↓
private helper
    ↓
  logic
```
</div>
نه:
<div dir="ltr">

```
public method
    ↓
protected method
```
</div>
چرا این روش بهتر است؟ فرض کنید `calculate()` → `normalize()` → `validate()` هر دو Override شدنی هستند. بعداً یکی Override می‌کند `normalize()`، ناگهان `calculate()` هم رفتار جدید پیدا می‌کند. در حالی که شاید اصلاً انتظارش را نداشته باشیم.

اما اگر `calculate()` → `private normalizeInternal()` باشد، هیچ Subclassی نمی‌تواند رفتار داخلی را خراب کند.

<a id="design-options"></a>
### گزینه‌های طراحی کلاس

Bloch در نهایت سه انتخاب مشخص را پیشنهاد می‌کند:

| گزینه | مثال | ویژگی‌ها |
|-------|------|-----------|
| **۱. طراحی برای ارث‌بری** | `AbstractList` | مستندات کامل (`@implSpec`)، Hookهای محافظت‌شده، تست با چند Subclass |
| **۲. کلاس نهایی** | `public final class Money` | ساده، امن، بدون Fragile Base Class |
| **۳. سازنده خصوصی + Static Factory** | `User.create()` | عملاً غیرقابل ارث‌بری از بیرون |

<a id="architectural-view"></a>
### نگاه معماری در پروژه‌های Enterprise

| نوع کلاس | ارث‌بری؟ | دلیل |
|----------|----------|------|
| Value Object | ❌ خیر | تغییرناپذیر و پایدار |
| Service | ❌ خیر | رفتار مشخص، Composition مناسب‌تر است |
| Utility | ❌ خیر | فقط متدهای استاتیک |
| Abstract Template | ✅ بله | برای ایجاد Extension Point |
| Framework Base Class | ✅ بله | با طراحی و مستندسازی کامل |

<a id="spring-connection"></a>
### ارتباط با Spring

به همین دلیل در Spring کمتر می‌بینید که از کلاس‌های Concrete ارث ببرید.

به جای:

<div dir="ltr">

```java
extends JdbcTemplate
```
</div>

اغلب این الگو را می‌بینید:

<div dir="ltr">

```java
@Service
class OrderService {
    private final JdbcTemplate jdbcTemplate;
}
```
</div>

یا:

<div dir="ltr">

```java
class CacheDecorator implements Cache
```
</div>

به جای:

<div dir="ltr">

```java
extends ConcurrentMapCache
```
</div>

این همان اصل **Composition over Inheritance** است که در Item 18 و Item 19 به اوج خود می‌رسد.

<a id="final-summary"></a>
### جمع‌بندی نهایی Item 19

قانون طلایی این Item را می‌توان در چهار اصل خلاصه کرد:

| قانون | توضیح |
|-------|-------|
| **۱. تصمیم‌گیری از ابتدا** | اگر کلاس برای ارث‌بری طراحی نشده است، آن را `final` کن یا سازنده‌های عمومی را حذف کن. |
| **۲. مستندسازی Self-Use** | اگر قرار است قابل ارث‌بری باشد، تمام الگوهای Self-use را با `@implSpec` مستند کن. |
| **۳. تست Extension Pointها** | کلاس را با نوشتن چند Subclass واقعی آزمایش کن تا مطمئن شوی Extension Pointها کافی و پایدار هستند. |
| **۴. Composition اولویت دارد** | در بیشتر پروژه‌های مدرن، **Composition** راه‌حل پیش‌فرض است و **Inheritance** فقط زمانی استفاده می‌شود که واقعاً رابطه‌ی «is-a» وجود داشته باشد و کلاس از ابتدا برای توسعه طراحی شده باشد. |

---

[بازگشت به بالا](#top)

</div>
```