<div dir="rtl">

<a id="top"></a>

# آیتم ۳۹: Annotationها را به Naming Patternها ترجیح دهید

## (Prefer annotations to naming patterns)

موضوع این بخش از Effective Java, 3rd Edition این است که وقتی می‌خواهیم یک ابزار، فریم‌ورک یا سیستم از روی کد تشخیص دهد کدام بخش‌ها باید رفتار خاصی داشته باشند، به‌جای قراردادهای مبتنی بر نام‌گذاری، از Annotation استفاده کنیم.

این موضوع در ظاهر ساده است، اما در طراحی فریم‌ورک‌ها، تست خودکار، Dependency Injection، اعتبارسنجی، ORM و ابزارهای تحلیل ایستا نقش اساسی دارد.

---

## فهرست مطالب

- [۱. مشکل Naming Pattern چیست؟](#naming-pattern-problem)
  - [مشکل اول: خطای تایپی بدون هشدار](#problem1)
  - [مشکل دوم: نبود محدودیت نوعی](#problem2)
  - [مشکل سوم: انتقال پارامتر از طریق نام](#problem3)
  - [مقایسه اولیه](#initial-comparison)
- [۲. اولین Annotation: علامت‌گذاری یک متد تست](#first-annotation)
  - [الف) چرا از `@interface` استفاده می‌کنیم؟](#why-interface)
  - [ب) `@Retention(RetentionPolicy.RUNTIME)`](#retention)
  - [ج) `@Target(ElementType.METHOD)`](#target)
  - [نکته مهم: `@Target` همه محدودیت‌ها را بررسی نمی‌کند](#target-limitation)
- [۳. اجرای Annotation با Reflection](#reflection-execution)
  - [کلاس نمونه](#sample-class)
  - [پیاده‌سازی Test Runner](#test-runner)
  - [مرحله اول: بارگذاری کلاس](#step1)
  - [مرحله دوم: کشف متدها](#step2)
  - [مرحله سوم: تشخیص Annotation](#step3)
  - [مرحله چهارم: اجرای متد](#step4)
  - [مرحله پنجم: مدیریت Exception](#step5)
  - [چرا این طراحی از Naming Pattern بهتر است؟](#why-better)
- [۴. Annotation پارامتردار: تستی که باید Exception مشخصی پرتاب کند](#parameterized-annotation)
  - [تعریف `ExceptionTest`](#exception-test)
  - [چرا نوع پارامتر `Class<? extends Throwable>` است؟](#throwable)
  - [چرا نام `value` انتخاب شده است؟](#value-name)
- [۵. منطق اجرای تست‌های Exception](#exception-logic)
  - [سه حالت مهم](#three-states)
- [۶. توسعه بیشتر: پذیرش چند Exception مختلف](#multiple-exceptions)
  - [تعریف Annotation با پارامتر آرایه‌ای](#array-annotation)
  - [چرا Annotation قبلی همچنان معتبر است؟](#backward-compatible)
  - [پردازش آرایه در Test Runner](#array-processing)
  - [یک نکته طراحی درباره نوع Exception](#exception-type-note)
- [۷. Annotationهای تکرارپذیر در Java 8 به بعد](#repeatable)
  - [الف) تعریف Annotation تکرارپذیر](#repeatable-definition)
  - [ب) تعریف Container](#container)
  - [ج) تفاوت مهم در Reflection](#reflection-difference)
  - [روش مناسب‌تر: `getAnnotationsByType`](#getannotationsbytype)
  - [مقایسه دو روش](#repeatable-comparison)
- [۸. نکته مهم در طراحی یک Test Runner واقعی](#real-test-runner)
  - [مشکل اول: اشتباه گرفتن شکست تست با خطای اجرای فریم‌ورک](#problem-infra)
  - [مشکل دوم: اعتبارسنجی امضای متد](#method-signature)
  - [مشکل سوم: تفاوت بین Exception مورد انتظار و خطای زیرساخت](#exception-vs-infra)
- [۹. ارتباط Item 39 با Spring، JUnit و فریم‌ورک‌های مدرن](#modern-frameworks)
- [۱۰. چه زمانی باید Annotation سفارشی تعریف کنیم؟](#when-to-define)
  - [چارچوب تصمیم‌گیری](#decision-framework)
- [جمع‌بندی نهایی Item 39](#final-summary)

[بازگشت به بالا](#top)

---

<a id="naming-pattern-problem"></a>
## ۱. مشکل Naming Pattern چیست؟

در نسخه‌های قدیمی JUnit، برای اینکه یک متد به‌عنوان تست شناخته شود، باید نام آن با `test` شروع می‌شد:

<div dir="ltr">

```java
public void testSafetyOverride() {
    // Test logic
}
```
</div>

فریم‌ورک با بررسی نام متد تشخیص می‌داد که باید آن را اجرا کند. این روش به‌ظاهر ساده است؛ اما چند مشکل اساسی دارد.

<a id="problem1"></a>
### مشکل اول: خطای تایپی بدون هشدار

<div dir="ltr">

```java
public void tsetSafetyOverride() {  // ❌ typo
    // Test logic
}
```
</div>

کد ممکن است کامپایل شود، اما فریم‌ورک متد را به‌عنوان تست شناسایی نمی‌کند.

> **خطر اصلی:** ممکن است تصور کنی تست اجرا شده و موفق بوده است، درحالی‌که اصلاً اجرا نشده است. این وضعیت یک **False Sense of Security** ایجاد می‌کند: اطمینان کاذب از صحت سیستم.

<a id="problem2"></a>
### مشکل دوم: نبود محدودیت نوعی

<div dir="ltr">

```java
public class TestSafetyMechanisms {
    public void verifyAccessControl() {
        // Test logic
    }
}
```
</div>

صرفاً نام کلاس با `Test` شروع می‌شود؛ اما این موضوع به معنای آن نیست که فریم‌ورک باید تمام متدهای آن را اجرا کند. قرارداد نام‌گذاری نمی‌تواند به‌خوبی مشخص کند که دقیقاً کدام عضو کلاس تست است و کدام عضو نیست.

<a id="problem3"></a>
### مشکل سوم: انتقال پارامتر از طریق نام

فرض کن می‌خواهی تستی بنویسی که فقط در صورت پرتاب یک Exception مشخص موفق شود:

<div dir="ltr">

```java
testArithmeticExceptionDivisionByZero()
```
</div>

اکنون نوع Exception بخشی از رشته نام متد شده است. مشکل اینجاست که کامپایلر نمی‌تواند تضمین کند که نام موردنظر واقعاً به یک کلاس Exception معتبر اشاره می‌کند. خطا ممکن است تنها هنگام اجرای ابزار تست آشکار شود.

<a id="initial-comparison"></a>
### مقایسه اولیه

| معیار | Naming Pattern | Annotation |
|--------|----------------|------------|
| تشخیص هدف | بر اساس رشته نام | بر اساس متادیتای صریح |
| خطای تایپی | ممکن است بی‌صدا نادیده گرفته شود | نام Annotation نامعتبر باعث خطای کامپایل می‌شود |
| محدودکردن محل استفاده | ضعیف | با `@Target` |
| تعریف پارامترهای نوع‌امن | دشوار | با اعضای Annotation |
| قابلیت تحلیل ابزارها | وابسته به قرارداد متنی | دارای API استاندارد Reflection |

> **نکته مهم:** Annotation جایگزین تمام قراردادهای نام‌گذاری نیست؛ بلکه زمانی مناسب است که ابزار یا فریم‌ورک باید اطلاعاتی صریح و ساختاریافته درباره عناصر برنامه دریافت کند.

[بازگشت به بالا](#top)

---

<a id="first-annotation"></a>
## ۲. اولین Annotation: علامت‌گذاری یک متد تست

<div dir="ltr">

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
```
</div>

<a id="why-interface"></a>
### الف) چرا از `@interface` استفاده می‌کنیم؟

در جاوا، برای تعریف Annotation از سینتکس `public @interface Test { }` استفاده می‌شود. این ساختار شبیه تعریف interface است، اما یک نوع Annotation جدید ایجاد می‌کند.

Annotation در این مثال هیچ رفتار اجرایی ندارد. هدفش این است که اطلاعاتی درباره متدی که آن را دریافت می‌کند، در اختیار ابزارهای دیگر قرار دهد:

<div dir="ltr">

```java
@Test
public static void shouldCalculateCorrectly() {
    // Test logic
}
```
</div>

خود `@Test` باعث اجرای متد نمی‌شود. یک Test Runner باید آن را تشخیص دهد و تصمیم بگیرد چه کاری انجام دهد.

<a id="retention"></a>
### ب) `@Retention(RetentionPolicy.RUNTIME)`

این Meta-Annotation مشخص می‌کند Annotation تا چه زمانی حفظ شود:

| مقدار | مفهوم |
|--------|-------|
| `SOURCE` | فقط در سورس کد در دسترس است و در فایل کلاس حفظ نمی‌شود |
| `CLASS` | در فایل کلاس حفظ می‌شود، اما الزاماً در زمان اجرا از طریق Reflection قابل مشاهده نیست |
| `RUNTIME` | در زمان اجرا نیز از طریق Reflection قابل مشاهده است |

در مثال کتاب، Test Runner از Reflection استفاده می‌کند؛ بنابراین به `@Retention(RetentionPolicy.RUNTIME)` نیاز داریم. اگر این سیاست را مشخص نکنیم، مقدار پیش‌فرض `CLASS` است.

<a id="target"></a>
### ج) `@Target(ElementType.METHOD)`

<div dir="ltr">

```java
@Target(ElementType.METHOD)
```
</div>

یعنی Annotation فقط برای اعلان متدها مجاز است:

<div dir="ltr">

```java
@Test
public static void validTest() { }     // ✅ معتبر

@Test
private String name;                   // ❌ Compile Error
```
</div>

<a id="target-limitation"></a>
### نکته مهم: `@Target` همه محدودیت‌ها را بررسی نمی‌کند

`@Target(ElementType.METHOD)` فقط محل استفاده را محدود می‌کند. این کد همچنان از نظر تعریف Annotation معتبر است:

<div dir="ltr">

```java
@Test
public void invalidTest(int input) { }  // ✅ Compile، ❌ Semantics
```
</div>

چرا؟ چون این متد همچنان یک اعلان متد است. کامپایلر به‌طور خودکار از روی مستندات Annotation نمی‌فهمد که متد باید `static` باشد و پارامتری نداشته باشد.

برای اعمال چنین محدودیت‌هایی باید از یکی از این روش‌ها استفاده کنیم:

- بررسی در Test Runner هنگام اجرا
- Annotation Processor در زمان کامپایل
- ابزار تحلیل ایستا

> **این تفاوت بین محدودیت‌های قابل بیان در مدل Annotation و قواعد معنایی اختصاصی فریم‌ورک بسیار مهم است.**

[بازگشت به بالا](#top)

---

<a id="reflection-execution"></a>
## ۳. اجرای Annotation با Reflection

<a id="sample-class"></a>
### کلاس نمونه

<div dir="ltr">

```java
public class Sample {
    @Test
    public static void m1() { }

    public static void m2() { }

    @Test
    public static void m3() {
        throw new RuntimeException("Boom");
    }

    @Test
    public void m5() { }  // ❌ Invalid: not static

    @Test
    public static void m7() {
        throw new RuntimeException("Crash");
    }
}
```
</div>

- `m1`: تست معتبر موفق
- `m3` و `m7`: تست‌های معتبری که Exception پرتاب می‌کنند
- `m5`: از نظر قرارداد فریم‌ورک نامعتبر (چون `static` نیست)
- `m2`: اصلاً تست نیست

<a id="test-runner"></a>
### پیاده‌سازی Test Runner

<div dir="ltr">

```java
import java.lang.reflect.*;

public class RunTests {
    public static void main(String[] args) throws Exception {
        int tests = 0;
        int passed = 0;

        Class<?> testClass = Class.forName(args[0]);

        for (Method method : testClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Test.class)) {
                continue;
            }

            tests++;

            try {
                method.invoke(null);
                passed++;
            } catch (InvocationTargetException wrappedExc) {
                Throwable cause = wrappedExc.getCause();
                System.out.println(method + " failed: " + cause);
            } catch (Exception exc) {
                System.out.println("Invalid @Test: " + method);
            }
        }

        System.out.printf("Passed: %d, Failed: %d%n",
                passed, tests - passed);
    }
}
```
</div>

<a id="step1"></a>
### مرحله اول: بارگذاری کلاس

<div dir="ltr">

```java
Class<?> testClass = Class.forName(args[0]);
```
</div>

```bash
java RunTests com.example.Sample
```

<a id="step2"></a>
### مرحله دوم: کشف متدها

<div dir="ltr">

```java
testClass.getDeclaredMethods()
```
</div>

این متد، متدهای تعریف‌شده مستقیماً در کلاس را برمی‌گرداند؛ شامل متدهای `public`، `protected`، `private` و package-private. اما متدهای ارث‌رسیده از کلاس والد را **برنمی‌گرداند**.

<a id="step3"></a>
### مرحله سوم: تشخیص Annotation

<div dir="ltr">

```java
method.isAnnotationPresent(Test.class)
```
</div>

دیگر نیازی به بررسی نام متد و جست‌وجوی پیشوند `test` نداریم.

<a id="step4"></a>
### مرحله چهارم: اجرای متد

<div dir="ltr">

```java
method.invoke(null);
```
</div>

آرگومان اول `invoke` شیء هدف است. چون متدهای تست ما `static` هستند، نیازی به نمونه‌ای از کلاس نداریم و `null` ارسال می‌شود. اگر متد یک متد نمونه‌ای باشد، باید یک شیء از کلاس هدف را ارسال کنیم.

<a id="step5"></a>
### مرحله پنجم: مدیریت Exception

<div dir="ltr">

```java
catch (InvocationTargetException wrappedExc) {
    Throwable cause = wrappedExc.getCause();
}
```
</div>

Reflection اگر متد فراخوانی‌شده خودش Exception پرتاب کند، آن را داخل `InvocationTargetException` قرار می‌دهد. `getCause()` علت اصلی شکست تست را استخراج می‌کند.

<a id="why-better"></a>
### چرا این طراحی از Naming Pattern بهتر است؟

Test Runner یک قرارداد ساختاریافته دارد: متد باید `@Test` داشته باشد. اطلاعاتی که برای تشخیص تست نیاز داریم، دیگر از نام آن استخراج نمی‌شود.

> **نکته فنی:** این Test Runner فقط یک نمونه آموزشی است. در یک فریم‌ورک واقعی باید وضعیت تست‌های نامعتبر را جدا از شکست تست‌ها گزارش کنیم؛ زیرا خطای پیکربندی و شکست Assertion یک مفهوم واحد نیستند. همچنین `getDeclaredMethods()` ترتیب تضمین‌شده‌ای برای اجرای متدها ارائه نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="parameterized-annotation"></a>
## ۴. Annotation پارامتردار: تستی که باید Exception مشخصی پرتاب کند

<a id="exception-test"></a>
### تعریف `ExceptionTest`

<div dir="ltr">

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
    Class<? extends Throwable> value();
}
```
</div>

در زمان استفاده:

<div dir="ltr">

```java
@ExceptionTest(ArithmeticException.class)
public static void shouldThrowArithmeticException() {
    int result = 10 / 0;
}
```
</div>

<a id="throwable"></a>
### چرا نوع پارامتر `Class<? extends Throwable>` است؟

- `Class` نماینده نوع یک کلاس در زمان اجرا است
- `? extends Throwable` تضمین می‌کند که نوع معرفی‌شده از `Throwable` مشتق شده باشد

<div dir="ltr">

```java
ArithmeticException.class      // ✅
NullPointerException.class     // ✅
IllegalArgumentException.class // ✅
String.class                   // ❌ Compile Error
```
</div>

این مثال کاربرد دیگری از **Bounded Type Token** است (ارتباط با Item 33). به‌جای انتقال نام کلاس به‌صورت یک رشته، مستقیماً شیء `Class` آن را در اختیار داریم. مزیت: کامپایلر می‌تواند ناسازگاری نوع را تشخیص دهد.

<a id="value-name"></a>
### چرا نام `value` انتخاب شده است؟

در Annotationهایی که تنها یک عضو دارند، اگر نام عضو `value` باشد، هنگام استفاده می‌توانیم مقدار آن را بدون نوشتن نام عضو مشخص کنیم:

<div dir="ltr">

```java
@ExceptionTest(ArithmeticException.class)                    // ✅ خواناتر
@ExceptionTest(value = ArithmeticException.class)            // ✅ معتبر
```
</div>

[بازگشت به بالا](#top)

---

<a id="exception-logic"></a>
## ۵. منطق اجرای تست‌های Exception

<div dir="ltr">

```java
if (method.isAnnotationPresent(ExceptionTest.class)) {
    tests++;

    try {
        method.invoke(null);
        System.out.printf("Test %s failed: no exception%n", method);
    } catch (InvocationTargetException wrappedEx) {
        Throwable cause = wrappedEx.getCause();

        Class<? extends Throwable> expected =
            method.getAnnotation(ExceptionTest.class).value();

        if (expected.isInstance(cause)) {
            passed++;
        } else {
            System.out.printf("Test %s failed: expected %s, got %s%n",
                    method, expected.getName(), cause);
        }
    } catch (Exception exc) {
        System.out.println("Invalid @Test: " + method);
    }
}
```
</div>

`isInstance` بررسی می‌کند که آیا شیء `cause` نمونه‌ای از نوع مورد انتظار یا یکی از زیرنوع‌های آن است یا خیر:

<div dir="ltr">

```java
expected.isInstance(cause)
```
</div>

> **نکته:** اگر Exception مورد انتظار `RuntimeException` باشد و متد `IllegalArgumentException` پرتاب کند، نتیجه `true` است؛ زیرا `IllegalArgumentException` زیرنوع `RuntimeException` است.

<a id="three-states"></a>
### سه حالت مهم

| متد | رفتار | نتیجه |
|------|-------|-------|
| `m1` | `ArithmeticException` | موفق |
| `m2` | `ArrayIndexOutOfBoundsException` | ناموفق |
| `m3` | هیچ Exception‌ای رخ نمی‌دهد | ناموفق |

> **توجه:** موفقیت تست به اجرای بدون خطا وابسته نیست؛ بلکه به مشاهده Exception مورد انتظار وابسته است.

[بازگشت به بالا](#top)

---

<a id="multiple-exceptions"></a>
## ۶. توسعه بیشتر: پذیرش چند Exception مختلف

<a id="array-annotation"></a>
### تعریف Annotation با پارامتر آرایه‌ای

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
    Class<? extends Exception>[] value();
}
```
</div>

استفاده:

<div dir="ltr">

```java
@ExceptionTest({
    IndexOutOfBoundsException.class,
    NullPointerException.class
})
public static void doublyBad() {
    List<String> list = new ArrayList<>();
    list.addAll(5, null);
}
```
</div>

<a id="backward-compatible"></a>
### چرا Annotation قبلی همچنان معتبر است؟

در جاوا، هنگام تعریف اعضای Annotation از نوع آرایه، می‌توان برای آرایه تک‌عضوی، مقدار را بدون آکولاد مشخص کرد:

<div dir="ltr">

```java
@ExceptionTest(ArithmeticException.class)  // ✅ همچنان معتبر
```
</div>

از نظر مقدار، معادل آرایه‌ای با یک عضو است. این قابلیت باعث می‌شود تغییر تعریف Annotation از یک مقدار به آرایه‌ای از مقادیر، در بسیاری از محل‌های استفاده به تغییر سورس کد نیاز نداشته باشد.

<a id="array-processing"></a>
### پردازش آرایه در Test Runner

<div dir="ltr">

```java
Class<? extends Exception>[] expectedTypes =
    method.getAnnotation(ExceptionTest.class).value();

boolean matched = false;

for (Class<? extends Exception> expectedType : expectedTypes) {
    if (expectedType.isInstance(cause)) {
        matched = true;
        break;
    }
}
```
</div>

<a id="exception-type-note"></a>
### یک نکته طراحی درباره نوع Exception

| نسخه | نوع | پوشش |
|-------|-----|-------|
| اولیه | `Class<? extends Throwable>` | هم `Exception` هم `Error` |
| آرایه‌ای | `Class<? extends Exception>[]` | فقط `Exception` |

نسخه آرایه‌ای این مثال اجازه نمی‌دهد `AssertionError` یا انواع دیگر `Error` را به‌عنوان نوع مورد انتظار تعریف کنیم. این موضوع باید بر اساس قرارداد فریم‌ورک انتخاب شود، نه صرفاً بر اساس راحتی پیاده‌سازی.

[بازگشت به بالا](#top)

---

<a id="repeatable"></a>
## ۷. Annotationهای تکرارپذیر در Java 8 به بعد

به‌جای این:

<div dir="ltr">

```java
@ExceptionTest({
    IndexOutOfBoundsException.class,
    NullPointerException.class
})
public static void doublyBad() { }
```
</div>

می‌توانیم بنویسیم:

<div dir="ltr">

```java
@ExceptionTest(IndexOutOfBoundsException.class)
@ExceptionTest(NullPointerException.class)
public static void doublyBad() { }
```
</div>

<a id="repeatable-definition"></a>
### الف) تعریف Annotation تکرارپذیر

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExceptionTestContainer.class)
public @interface ExceptionTest {
    Class<? extends Exception> value();
}
```
</div>

`@Repeatable(ExceptionTestContainer.class)` به کامپایلر می‌گوید که چند نمونه از `ExceptionTest` می‌توانند روی یک عنصر قرار بگیرند و نوع Container آن‌ها `ExceptionTestContainer` است.

<a id="container"></a>
### ب) تعریف Container

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTestContainer {
    ExceptionTest[] value();
}
```
</div>

| نکته | توضیح |
|------|-------|
| نوع عضو `value` | باید آرایه‌ای از Annotation تکرارپذیر باشد |
| Retention و Target | باید با کاربرد موردنظر سازگار باشد |

<a id="reflection-difference"></a>
### ج) تفاوت مهم در Reflection

وقتی یک Annotation تکرارپذیر چند بار روی یک متد اعمال می‌شود، نمایش آن در فایل کلاس و Reflection با Annotationهای معمولی تفاوت‌هایی دارد. Annotationهای تکرارشده می‌توانند در قالب Annotation کانتینر نگهداری شوند.

<div dir="ltr">

```java
method.isAnnotationPresent(ExceptionTest.class)  // ❌ ممکن است false برگرداند
```
</div>

اگر فقط چند Annotation تکرارشده وجود داشته باشند، ممکن است این بررسی `false` برگرداند؛ زیرا Reflection وجود کانتینر را مشاهده می‌کند، نه لزوماً یک Annotation منفرد از نوع موردنظر را. همچنین بررسی صرف کانتینر نیز برای تمام حالت‌ها کافی نیست.

<a id="getannotationsbytype"></a>
### روش مناسب‌تر: `getAnnotationsByType`

<div dir="ltr">

```java
ExceptionTest[] tests =
    method.getAnnotationsByType(ExceptionTest.class);
```
</div>

این متد هم Annotation معمولی و هم Annotationهای تکرارشده را در قالب آرایه‌ای از نوع `ExceptionTest` در اختیار ما قرار می‌دهد.

<a id="repeatable-comparison"></a>
### مقایسه دو روش

| معیار | آرایه در Annotation | `@Repeatable` |
|--------|---------------------|----------------|
| خوانایی یک مقدار | خوب | خوب |
| خوانایی چند مقدار | آکولاد و فهرست مقادیر | یک Annotation برای هر مقدار |
| پیچیدگی تعریف | کمتر | بیشتر |
| پیچیدگی پردازش | نسبتاً ساده | نیازمند توجه به Container و Reflection |
| کاربرد مناسب | مجموعه‌ای از مقادیر مرتبط | تکرار منطقی یک نوع Annotation |

> **توصیه کتاب:** Annotation تکرارپذیر را زمانی استفاده کن که خوانایی سورس کد را بهتر می‌کند؛ اما پیچیدگی بیشتر در تعریف و پردازش آن را نادیده نگیر.

[بازگشت به بالا](#top)

---

<a id="real-test-runner"></a>
## ۸. نکته مهم در طراحی یک Test Runner واقعی

<a id="problem-infra"></a>
### مشکل اول: اشتباه گرفتن شکست تست با خطای اجرای فریم‌ورک

| دسته | توضیح |
|-------|-------|
| **Test Failure** | تست اجرا شده و رفتار مورد انتظار را برآورده نکرده است |
| **Test Configuration Error** | تست به شکل معتبر تعریف نشده یا ابزار نمی‌تواند آن را اجرا کند |
| **Infrastructure Error** | مشکلی در خود Test Runner یا محیط اجرا رخ داده است |

در یک فریم‌ورک واقعی بهتر است این وضعیت‌ها را جداگانه گزارش کنیم.

<a id="method-signature"></a>
### مشکل دوم: اعتبارسنجی امضای متد

اگر قرارداد `@Test` این باشد که فقط متدهای `static` و بدون پارامتر مجاز باشند، بهتر است این شرط را پیش از اجرا بررسی کنیم:

<div dir="ltr">

```java
if (!Modifier.isStatic(method.getModifiers())
        || method.getParameterCount() != 0) {
    // Report invalid test declaration
}
```
</div>

همچنین باید مشخص کنیم آیا متدهای `private` مجاز هستند یا خیر.

<a id="exception-vs-infra"></a>
### مشکل سوم: تفاوت بین Exception مورد انتظار و خطای زیرساخت

در تست‌های مبتنی بر Exception، نباید هر خطایی را به‌عنوان نتیجه تست در نظر بگیریم. اگر متد موردنظر قابل دسترسی نباشد و Reflection با `IllegalAccessException` مواجه شود، این اتفاق به معنای آن نیست که تست Exception مورد انتظار را تولید کرده است.

> در نسخه آموزشی کتاب، تمرکز روی مفهوم Annotation است؛ در پیاده‌سازی حرفه‌ای باید این مرزها دقیق‌تر تعریف شوند.

[بازگشت به بالا](#top)

---

<a id="modern-frameworks"></a>
## ۹. ارتباط Item 39 با Spring، JUnit و فریم‌ورک‌های مدرن

در JUnit مدرن:

<div dir="ltr">

```java
@Test
void shouldCalculateCorrectly() { }

@Test
void shouldThrowExpectedException() {
    assertThrows(ArithmeticException.class,
            () -> { int result = 10 / 0; });
}
```
</div>

Annotationها به فریم‌ورک می‌گویند کدام متد باید به‌عنوان تست شناخته شود؛ درحالی‌که Assertionها رفتار مورد انتظار را بررسی می‌کنند.

در Spring نیز Annotationها اطلاعات ساختاریافته‌ای برای فریم‌ورک فراهم می‌کنند:

<div dir="ltr">

```java
@Component
public class OrderService { }

@Transactional
public void processOrder() { }
```
</div>

| روش پردازش | مثال |
|------------|------|
| **Reflection در زمان اجرا** | Spring Boot در بخش بزرگی از کشف Beanها |
| **پردازشگر زمان کامپایل** | بعضی Annotation Processorها |
| **ترکیبی** | بعضی فریم‌ورک‌های مدرن |

> **نکته معماری:** Annotation به‌تنهایی رفتار جدیدی به برنامه اضافه نمی‌کند؛ این ابزار یا فریم‌ورکی است که Annotation را پردازش می‌کند و بر اساس آن تصمیم می‌گیرد.

[بازگشت به بالا](#top)

---

<a id="when-to-define"></a>
## ۱۰. چه زمانی باید Annotation سفارشی تعریف کنیم؟

<a id="decision-framework"></a>
### چارچوب تصمیم‌گیری

| سؤال | تصمیم |
|------|-------|
| **۱. آیا ابزار باید یک عنصر برنامه را شناسایی کند؟** | اگر فقط یک نام متد یا کلاس برای قرارداد کافی نیست، Annotation می‌تواند انتخاب مناسبی باشد |
| **۲. آیا اطلاعات اضافی لازم است؟** | اگر ابزار به پارامترهایی مانند نوع Exception، سطح دسترسی یا سیاست اجرایی نیاز دارد، اعضای Annotation را بررسی کن |
| **۳. آیا محدودیت‌ها در زمان کامپایل قابل بررسی‌اند؟** | از `@Target` و در صورت نیاز Annotation Processor استفاده کن |
| **۴. آیا Annotation سفارشی واقعاً ارزش دارد؟** | اگر Annotation استاندارد یا قابلیت موجود فریم‌ورک نیاز را برآورده می‌کند، معمولاً تعریف نوع جدید ضرورتی ندارد |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی Item 39

مهم‌ترین درس این Item، انتخاب یک قرارداد صریح و قابل بررسی به‌جای قراردادهای ضمنی مبتنی بر نام‌گذاری است.

| مفهوم | نتیجه |
|--------|-------|
| Naming Pattern | ساده است، اما خطاهای تایپی و معنایی ممکن است بدون هشدار باقی بمانند |
| Marker Annotation | برای علامت‌گذاری یک عنصر، بدون نیاز به پارامتر، مناسب است |
| Parameterized Annotation | امکان انتقال اطلاعات نوع‌امن به ابزار را فراهم می‌کند |
| Array-valued Annotation | چند مقدار مرتبط را در یک Annotation نگهداری می‌کند |
| Repeatable Annotation | امکان اعمال چند نمونه از یک Annotation را فراهم می‌کند |
| Reflection | امکان کشف و پردازش Annotationها در زمان اجرا را فراهم می‌کند |
| Annotation Processor | امکان بررسی بعضی محدودیت‌ها و قراردادها را در زمان کامپایل فراهم می‌کند |

### سه اصل نهایی

| # | اصل |
|---|-----|
| **۱** | اگر یک ابزار برای شناسایی عناصر برنامه به اطلاعات ساختاریافته نیاز دارد، معمولاً Annotation از Naming Pattern مناسب‌تر است |
| **۲** | Annotation فقط متادیتا فراهم می‌کند؛ اعتبارسنجی و اجرای رفتار به ابزار مصرف‌کننده آن وابسته است |
| **۳** | در طراحی Annotation سفارشی، نوع پارامترها، Retention، Target و نحوه پردازش در زمان اجرا را بخشی از قرارداد API در نظر بگیر |

### نتیجه معماری

> **Annotationها راهی برای جداکردن اطلاعات توصیفی از منطق اجرایی برنامه هستند. این جداسازی به فریم‌ورک‌ها اجازه می‌دهد رفتار برنامه را از روی متادیتای صریح و قابل تحلیل تعیین کنند، بدون اینکه مجبور باشند قراردادهای شکننده‌ای مانند پیشوند نام متدها را تفسیر کنند.**

---

[بازگشت به بالا](#top)

</div>