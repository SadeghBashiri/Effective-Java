<div dir="rtl">

<a id="top"></a>

# آیتم ۴: جلوگیری از نمونه‌سازی (Noninstantiable Classes)

**Item 4** در نگاه اول بسیار ساده به نظر می‌رسد (فقط یک private constructor!) اما در واقع درباره‌ی یکی از اصول مهم طراحی API است:

> **بیان صریح Intent (Design by Intent)**

یعنی کلاس باید به گونه‌ای طراحی شود که **امکان استفاده‌ی اشتباه از آن وجود نداشته باشد.**

این دقیقاً همان فلسفه‌ای است که در تمام کتاب Effective Java تکرار می‌شود.

---

## فهرست مطالب

- [معماری و مسئله اصلی](#architectural-view)
  - [چرا کلاسی وجود داشته باشد که هیچ‌وقت ساخته نشود؟](#why-noninstantiable)
- [Utility Class چیست؟](#utility-class)
- [چرا new کردن آن اشتباه است؟](#why-new-is-wrong)
- [اشتباه رایج: Abstract Class](#abstract-mistake)
  - [مشکل اول: ارث‌بری](#problem1)
  - [مشکل دوم: گمراه‌کننده بودن API](#problem2)
- [راه‌حل: Private Constructor](#solution)
- [چرا throw new AssertionError()؟](#assertion-error)
- [جلوگیری از Inheritance](#prevent-inheritance)
- [Best Practice: Utility Class کامل](#best-practice)
- [Anti-Patternها](#anti-patterns)
  - [Anti-Pattern ۱: بدون Constructor](#antipattern1)
  - [Anti-Pattern ۲: Abstract Utility](#antipattern2)
  - [Anti-Pattern ۳: Constructor عمومی](#antipattern3)
- [چه کلاس‌هایی Utility Class هستند؟](#jdk-examples)
- [آیا همیشه Utility Class انتخاب خوبی است؟](#when-not-to-use)
- [نکته معماری مدرن](#modern-architecture)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## معماری و مسئله اصلی

قبل از اینکه Joshua Bloch درباره private constructor صحبت کند، ابتدا سؤال مهمی را مطرح می‌کند:

<a id="why-noninstantiable"></a>
### چرا باید کلاسی وجود داشته باشد که هیچ‌وقت ساخته نشود؟

در طراحی شیءگرا، معمولاً هر کلاس نماینده‌ی یک Object است.

مثلاً:
<div dir="ltr">

```java
User
Order
Invoice
Employee
```
</div>
همه اینها باید Instantiate شوند.

اما بعضی کلاس‌ها اصلاً Object نیستند.

آنها فقط مجموعه‌ای از Utilityها هستند.

مثلاً:
<div dir="ltr">

```java
Math
Arrays
Collections
Objects
Files
Paths
```
</div>
هیچ‌وقت نمی‌نویسیم:
<div dir="ltr">

```java
new Math();
```
</div>
یا
<div dir="ltr">

```java
new Arrays();
```
</div>
زیرا این کلاس‌ها هیچ Stateای ندارند.

[بازگشت به بالا](#top)

---

<a id="utility-class"></a>
## Utility Class چیست؟

Utility Class یعنی:

> **کلاسی که فقط رفتار (Behavior) دارد و هیچ وضعیت (State) ندارد.**

مثال:
<div dir="ltr">

```java
public class StringUtils {

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String capitalize(String s) {
        // ...
    }
}
```
</div>
- هیچ فیلدی ندارد.
- تمام متدها `static` هستند.
- پس ساختن Object هیچ معنایی ندارد.

[بازگشت به بالا](#top)

---

<a id="why-new-is-wrong"></a>
## چرا new کردن آن اشتباه است؟

فرض کن:
<div dir="ltr">

```java
StringUtils utils = new StringUtils();
```
</div>
بعد:
<div dir="ltr">

```java
utils.capitalize(...)
```
</div>
در حالی که همان متد static است.

این Object:

- حافظه اشغال می‌کند.
- هیچ State ندارد.
- هیچ ارزشی ایجاد نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="abstract-mistake"></a>
## اشتباه رایج: Abstract Class

خیلی‌ها فکر می‌کنند:
<div dir="ltr">

```java
abstract class StringUtils
```
</div>
راه‌حل است.

Joshua Bloch می‌گوید:

❌ خیر.

<a id="problem1"></a>
### مشکل اول: ارث‌بری
<div dir="ltr">

```java
public abstract class StringUtils {
    public static boolean isBlank(...) {
        // ...
    }
}
```
</div>
اکنون:
<div dir="ltr">

```java
public class MyUtils extends StringUtils {
}
```
</div>
کاملاً قانونی است.

و حالا:
<div dir="ltr">

```java
new MyUtils();
```
</div>
امکان‌پذیر است.

پس هدف ما نقض شده است.

<a id="problem2"></a>
### مشکل دوم: گمراه‌کننده بودن API

از دید API:

وقتی کلاس Abstract است، کاربر تصور می‌کند:

> این کلاس برای ارث‌بری طراحی شده است.

در حالی که اصلاً چنین هدفی نداشته‌ایم.

این دقیقاً همان چیزی است که Joshua Bloch به آن اشاره می‌کند:

> It misleads the user.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## راه‌حل: Private Constructor

تنها راه صحیح:
<div dir="ltr">

```java
private UtilityClass() {
}
```
</div>
چرا؟

Compiler فقط زمانی Constructor پیش‌فرض می‌سازد که:

```
هیچ Constructorای وجود نداشته باشد.
```

وقتی این را می‌نویسی:
<div dir="ltr">

```java
private UtilityClass() {
}
```
</div>
دیگر:
<div dir="ltr">

```java
public UtilityClass()
```
</div>
تولید نمی‌شود.

[بازگشت به بالا](#top)

---

<a id="assertion-error"></a>
## چرا throw new AssertionError()؟

خیلی‌ها این قسمت را متوجه نمی‌شوند.

ظاهر Constructor:
<div dir="ltr">

```java
private UtilityClass() {
}
```
</div>
کافی است.

پس چرا Exception؟

فرض کن داخل همان کلاس:
<div dir="ltr">

```java
public static void test() {
    new UtilityClass();
}
```
</div>
چون داخل همان کلاس هستیم، `private` قابل دسترسی است.

حالا Object ساخته شد.

در حالی که هدف ما:

```
Never Instantiate
```

بود.

به همین دلیل:
<div dir="ltr">

```java
private UtilityClass() {
    throw new AssertionError();
}
```
</div>
حتی اگر کسی از داخل کلاس هم اشتباه کند، بلافاصله برنامه متوقف می‌شود.

### چرا AssertionError؟

چرا مثلاً `UnsupportedOperationException` نه؟

چون:

- این وضعیت اصلاً نباید اتفاق بیفتد.
- یعنی Bug برنامه‌نویس است، نه ورودی اشتباه کاربر.
- `AssertionError` دقیقاً برای چنین موقعیت‌هایی طراحی شده است.

[بازگشت به بالا](#top)

---

<a id="prevent-inheritance"></a>
## جلوگیری از Inheritance

یک مزیت جالب دیگر.

فرض کن:
<div dir="ltr">

```java
public class MyMath extends MathUtils
```
</div>
اولین خط Constructor فرزند:
<div dir="ltr">

```java
super();
```
</div>
است.

اما:
<div dir="ltr">

```java
private MathUtils() {
}
```
</div>
قابل دسترسی نیست.

Compiler:

```
Cannot access constructor
```

می‌دهد.

پس:

هیچ Subclassی هم ساخته نمی‌شود.

[بازگشت به بالا](#top)

---

<a id="best-practice"></a>
## Best Practice: Utility Class کامل
<div dir="ltr">

```java
public final class DateUtils {

    private DateUtils() {
        throw new AssertionError("Utility class");
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }

    public static int age(LocalDate birthDate) {
        return Period.between(
                birthDate,
                LocalDate.now())
                .getYears();
    }
}
```
</div>
استفاده:
<div dir="ltr">

```java
boolean weekend = DateUtils.isWeekend(LocalDate.now());
```
</div>
هرگز:
<div dir="ltr">

```java
new DateUtils();
```
</div>

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternها

<a id="antipattern1"></a>
### Anti-Pattern ۱: بدون Constructor
<div dir="ltr">

```java
public class DateUtils {

    public static boolean isWeekend(...) {
        // ...
    }
}
```
</div>
Compiler:
<div dir="ltr">

```java
public DateUtils() {
}
```
</div>
را خودش اضافه می‌کند.

در نتیجه:
<div dir="ltr">

```java
DateUtils utils = new DateUtils();
```
</div>
کاملاً مجاز است.

<a id="antipattern2"></a>
### Anti-Pattern ۲: Abstract Utility
<div dir="ltr">

```java
public abstract class DateUtils {

    public static boolean isWeekend(...) {
        // ...
    }
}
```
</div>
حالا:
<div dir="ltr">

```java
public class MyUtils extends DateUtils {
}
```
</div>
و سپس:
<div dir="ltr">

```java
new MyUtils();
```
</div>
ممکن است.

<a id="antipattern3"></a>
### Anti-Pattern ۳: Constructor عمومی
<div dir="ltr">

```java
public class ArrayUtils {

    public ArrayUtils() {
    }

    public static void sort(...) {
        // ...
    }
}
```
</div>
Client:
<div dir="ltr">

```java
ArrayUtils utils = new ArrayUtils();
utils.sort(...);
```
</div>
از نظر خوانایی، این استفاده القا می‌کند که کلاس دارای State است، در حالی که تمام متدها `static` هستند.

[بازگشت به بالا](#top)

---

<a id="jdk-examples"></a>
## چه کلاس‌هایی Utility Class هستند؟

نمونه‌های JDK:
<div dir="ltr">

```java
java.lang.Math
java.util.Arrays
java.util.Collections
java.util.Objects
java.nio.file.Files
java.nio.file.Paths
```
</div>
همه‌ی این کلاس‌ها:

- Constructor خصوصی دارند.
- فقط متدهای `static` ارائه می‌کنند.
- هیچ Stateای نگه نمی‌دارند.

[بازگشت به بالا](#top)

---

<a id="when-not-to-use"></a>
## آیا همیشه Utility Class انتخاب خوبی است؟

خیر.

یکی از Anti-Patternهای رایج این است که Utility Class را جایگزین Object-Oriented Design کنیم.

❌ مثال:
<div dir="ltr">

```java
public class UserUtils {

    public static void save(User user) { ... }
    public static void delete(User user) { ... }
    public static void update(User user) { ... }
    public static void validate(User user) { ... }
    public static void notify(User user) { ... }
}
```
</div>
اگر Utility Class دائماً بزرگ‌تر می‌شود و عملیات مرتبط با یک موجودیت دامنه را در خود جمع می‌کند، معمولاً نشانه‌ای است که این رفتارها باید به کلاس‌های مناسب (مانند `UserService`، `UserValidator` یا خود `User`) منتقل شوند.

[بازگشت به بالا](#top)

---

<a id="modern-architecture"></a>
## نکته معماری مدرن

> نکته‌ای که Joshua Bloch مستقیماً نمی‌گوید اما از متن او برداشت می‌شود

در معماری مدرن (Spring Boot، Quarkus و CDI)، **Utility Class باید فقط برای توابع کاملاً Stateless و مستقل استفاده شود**؛ مانند تبدیل داده، محاسبات یا توابع کمکی.

اگر کلاس نیاز به Dependency، تنظیمات، Logging، Cache یا همکاری با سایر سرویس‌ها دارد، دیگر Utility Class گزینه‌ی مناسبی نیست و بهتر است به‌صورت یک Service مدیریت‌شده توسط Container طراحی شود. این همان مرز مهمی است که بین **Utility Class** و **Service** در طراحی نرم‌افزار حرفه‌ای وجود دارد.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

| معیار | Utility Class | Abstract Class | Regular Class |
|--------|---------------|----------------|---------------|
| Instantiation | ❌ (با private constructor) | ❌ (اما Subclass ممکن است) | ✅ |
| Inheritance | ❌ (با `final`) | ✅ | ✅ |
| وضوح Intent | ✅ کاملاً مشخص | ❌ گمراه‌کننده | بستگی دارد |
| Reflection Attack | ✅ ایمن (با `AssertionError`) | ⚠️ ممکن است | ⚠️ ممکن است |

### نکات کلیدی

1. **از `abstract` برای Utility Class استفاده نکنید** - کاربران را گمراه می‌کند
2. **همیشه Constructor را `private` کنید** - حتی اگر کلاس `final` باشد
3. **حتماً `throw new AssertionError()`** - حتی از داخل کلاس هم جلوگیری می‌کند
4. **کلاس را `final` کنید** - لایه‌ی امنیتی اضافی
5. **Utility Class را فقط برای عملیات کاملاً Stateless استفاده کنید** - وگرنه Service طراحی کنید

---

[بازگشت به بالا](#top)

</div>
```